package com.oxygenxml.git.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.swing.JFrame;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.Commit;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.RevCommitUtilBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;

import ro.sync.basic.xml.BasicXmlUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Displays the diff depending on the what change type the file is.
 * 
 * @author Beniamin Savu
 *
 */
public class DiffPresenter {

  /**
   * Max number of characters to be presented in commit message at the top of the diff.
   */
  private static final int MAX_COMMIT_MESSAGE_CHARS_IN_DIFF = 120;

  /**
   * i18n
   */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
  /**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(DiffPresenter.class);
	
	/**
	 * Avoid instantiation.
	 */
	private DiffPresenter() {
	  // Nada
	}

	/**
   * Perform different actions depending on the file change type. If the file is
   * a conflict file then a 3-way diff is presented. If the file is a modified
   * one then a 2-way diff is presented. And if a file is added then the file is
   * opened or removed only one side of the diff panel is populated.
   * 
   * @param fileStatus       Changes. 
   * @param gitCtrl          Git controller.
   * 
   */
	public static void showDiff(FileStatus fileStatus, GitControllerBase gitCtrl) {
	  showDiff(fileStatus, gitCtrl, null);
	}
	
	/**
	 * Perform different actions depending on the file change type. If the file is
	 * a conflict file then a 3-way diff is presented. If the file is a modified
	 * one then a 2-way diff is presented. And if a file is added then the file is
	 * opened or removed only one side of the diff panel is populated.
	 * 
	 * @param fileStatus       Changes. 
	 * @param gitCtrl          Git controller.
	 * @param refreshManager   Responsible to  refresh components after file modification in diff if it is a local one.
	 * 
	 */
	public static void showDiff(FileStatus fileStatus, GitControllerBase gitCtrl, @Nullable final GitRefreshSupport refreshManager) {
	  try {
	    GitChangeType changeType = fileStatus.getChangeType();
	    switch (changeType) {
	      case CONFLICT:
	        showConflictDiff(fileStatus, gitCtrl);
	        break;
	      case CHANGED:
	        showDiffIndexWithHead(fileStatus.getFileLocation());
	        break;
	      case MODIFIED:
	        showDiffViewForModified(fileStatus.getFileLocation(), refreshManager);
	        break;
	      case ADD:
	      case UNTRACKED:
	        diffViewForAddedAndUntracked(fileStatus);
	        break;
	      case SUBMODULE:
	        showSubmoduleDiff(fileStatus.getFileLocation());
	        break;
	      case MISSING:
	      case REMOVED:
	        diffViewForMissingAndRemoved(fileStatus.getFileLocation());
	        break;
	      default:
	        break;
	    }
	  } catch (Exception ex) {
	    PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage());
	    
	    if (LOGGER.isDebugEnabled()) {
	      LOGGER.debug(ex.getMessage(), ex);
	    }
	  }
	}

	/**
	 * Diff for added/untracked resources.
	 * 
	 * @param fileStatus File to compare.
	 */
	private static void diffViewForAddedAndUntracked(FileStatus fileStatus) {
	  URL url = null;
	  try {
	    GitChangeType changeType = fileStatus.getChangeType();
	    if (changeType == GitChangeType.ADD) {
	      url = GitRevisionURLHandler.encodeURL(
	          VersionIdentifier.INDEX_OR_LAST_COMMIT, 
	          fileStatus.getFileLocation());
	    } else {
	      url = FileUtil.getFileURL(fileStatus.getFileLocation());
	    }
	  } catch (MalformedURLException | NoRepositorySelected e) {
	    // Shouldn't rreally happen
	    if (LOGGER.isDebugEnabled()) {
	      LOGGER.debug(e.getMessage(), e);
	    }
	  }
	  showDiffFrame(url, null, null, null, null, fileStatus.getFileLocation());
	}

	/**
	 * Diff for missing/deleted resources.
	 * 
	 * @param path The path of the file to compare. Relative to the working tree.
	 */
	private static void diffViewForMissingAndRemoved(String path) {
	  URL lastCommitedFileURL = null;
	  try {
	    lastCommitedFileURL = GitRevisionURLHandler.encodeURL(
	        VersionIdentifier.INDEX_OR_LAST_COMMIT, path);
	  } catch (MalformedURLException e1) {
	    if (LOGGER.isDebugEnabled()) {
	      LOGGER.debug(e1.getMessage(), e1);
	    }
	  }
	  showDiffFrame(null, null, lastCommitedFileURL, null, null, path);
	}

	/**
	 * Submodule diff.
	 * 
	 * @param path The path of the file to compare. Relative to the working tree.
	 */
	private static void showSubmoduleDiff(String path) {
		try {
			URL currentSubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.CURRENT_SUBMODULE, path);
			URL previouslySubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.PREVIOUSLY_SUBMODULE, path);
			showDiffFrame(currentSubmoduleCommit, null, previouslySubmoduleCommit, null, previouslySubmoduleCommit, path);
		} catch (MalformedURLException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
	}

	/**
	 * Presents a 2-way diff
	 * 
	 * @param path               The path of the file to compare. Relative to the working tree.
	 * @param refreshSupport     Used for refreshes when needed.
	 * 
	 * @throws NoRepositorySelected 
	 * @throws URISyntaxException 
	 */
	private static void showDiffViewForModified(String path, @Nullable final GitRefreshSupport refreshSupport) throws NoRepositorySelected, URISyntaxException {
	  // The local (WC) version.
		URL fileURL = FileUtil.getFileURL(path);
		URL lastCommitedFileURL = null;

		try {
		  lastCommitedFileURL = GitRevisionURLHandler.encodeURL(
		      VersionIdentifier.INDEX_OR_LAST_COMMIT, path);
		} catch (MalformedURLException e1) {
		  if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug(e1.getMessage(), e1);
		  }
		}

		// time stamp used for detecting if the file was changed in the diff view
		final long diffStartedTimeStamp = new File(fileURL.toURI()).lastModified(); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN

		final Optional<JFrame> frame = showDiffFrame(fileURL, null, lastCommitedFileURL, null, lastCommitedFileURL, path);
		refreshIfFileWasModified(refreshSupport, diffStartedTimeStamp, frame, fileURL);
	}

	/**
	 * Call refresh support if the file was modified.
	 * 
	 * @param refreshSupport          Responsible for refresh management.
	 * @param diffStartedTimeStamp    The time when the file was modified before diff showing. 
	 * @param frame                   Used to show the differences.
	 * @param fileURL                 The URL of the file.
	 */
  private static void refreshIfFileWasModified(@Nullable final GitRefreshSupport refreshSupport, final long diffStartedTimeStamp,
      final Optional<JFrame> frame, final URL fileURL) {
    frame.ifPresent(d -> 
		d.addComponentListener(new ComponentAdapter() {
		  @Override
		  public void componentHidden(ComponentEvent e) {
		    long diffClosedTimeStamp = 0;
		    try {
		      diffClosedTimeStamp = new File(fileURL.toURI()).lastModified(); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
		      if(diffStartedTimeStamp < diffClosedTimeStamp && refreshSupport != null) {
		        refreshSupport.call();
		      }
		    } catch (URISyntaxException e1) {
		      if (LOGGER.isDebugEnabled()) {
		        LOGGER.debug(e1.getMessage(), e1);
		      }
		    } 
		  }
		}));
  }

	
	
  /**
   * Presents a 2-way diff
   * 
   * @param path            The path of the file to compare. Relative to the working tree.
   *  
   * @throws NoRepositorySelected 
   * @throws URISyntaxException 
   */
  private static void showDiffIndexWithHead(String path) throws NoRepositorySelected { 
    // The local (WC) version.
    URL leftSideURL = FileUtil.getFileURL(path);
    URL rightSideURL = null;

    try {
      leftSideURL  = GitRevisionURLHandler.encodeURL(VersionIdentifier.INDEX_OR_LAST_COMMIT, path);
      
      rightSideURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.LAST_COMMIT, path);
    } catch (MalformedURLException e1) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e1.getMessage(), e1);
      }
    }

    showDiffFrame(leftSideURL, null, rightSideURL, null, rightSideURL, path);
  }

	/**
	 * Presents a 3-way diff.
	 * 
	 * @param file The file for which to show the Diff.
	 * @param gitController Git controller. 
	 */
	private static void showConflictDiff(FileStatus file, GitControllerBase gitController) {
		try {
		  String fileLocation = file.getFileLocation();
      URL base = GitRevisionURLHandler.encodeURL(VersionIdentifier.BASE, fileLocation);
		  URL leftURL = null;
		  String leftLabel = null;
		  URL rightURL = null;
		  String rightLabel = null;
		  
		  // builds the URL for the files depending if we are in rebasing state or not
      GitAccess gitAccess = GitAccess.getInstance();
      RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
      boolean isUnfinishedRebase = repositoryState.equals(RepositoryState.REBASING)
          || repositoryState.equals(RepositoryState.REBASING_INTERACTIVE)
          || repositoryState.equals(RepositoryState.REBASING_MERGE)
          || repositoryState.equals(RepositoryState.REBASING_REBASING);
      // TODO EXM-54873: add commit info for rebase and merge
      if (isUnfinishedRebase) {
        leftURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE_RESOLVED, fileLocation);
        rightURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE_ORIGINAL, fileLocation);
      } else {
//        ObjectId myCommit = gitAccess.getCommit(Commit.MINE, fileLocation);
//        ObjectId theirCommit = gitAccess.getCommit(Commit.THEIRS, fileLocation);
//        
//        try { // NOSONAR
//          RevCommit mine = RevCommitUtil.getCommit(myCommit.getName());
//          leftLabel = getCommitInfoLabelForDiffSidePanel(fileLocation, mine);
//          
//          RevCommit theirs = RevCommitUtil.getCommit(theirCommit.getName());
//          rightLabel = getCommitInfoLabelForDiffSidePanel(fileLocation, theirs);
//        } catch (IOException e) {
//          LOGGER.error(e, e);
//        }
        
        leftURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE, fileLocation);
        rightURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.THEIRS, fileLocation);
      }

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, fileLocation); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN

			// time stamp used for detecting if the file was changed in the diff view
			final long diffStartedTimeStamp = localCopy.lastModified();

			Optional<JFrame> diffFrame = showDiffFrame(leftURL, leftLabel, rightURL, rightLabel, base, fileLocation);
			// checks if the file in conflict has been resolved or not after the diff
			// view was closed
			diffFrame.ifPresent(d -> 
			  d.addComponentListener(new ComponentAdapter() {
			    @Override
			    public void componentHidden(ComponentEvent e) {
			      long diffClosedTimeStamp = localCopy.lastModified();
			      if (diffClosedTimeStamp == diffStartedTimeStamp) {
			        String message = isUnfinishedRebase ? TRANSLATOR.getTranslation(Tags.KEEP_RESOLVED_VERSION_FOR_REBASE_CONFLICT)
			            : TRANSLATOR.getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED);
			        final int response = MessagePresenterProvider.getBuilder(
			            TRANSLATOR.getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED_TITLE), DialogType.WARNING)
			            .setQuestionMessage(message)
			            .setOkButtonName(TRANSLATOR.getTranslation(Tags.RESOLVE_ANYWAY))
			            .setCancelButtonName(TRANSLATOR.getTranslation(Tags.KEEP_CONFLICT))
			            .buildAndShow().getResult();
			            
			        if (response == OKCancelDialog.RESULT_OK) {
			          gitController.asyncResolveUsingMine(Collections.singletonList(file));
			        }
			      } else {
			        // Instead of requesting the file status again, we just mark it as modified.
			        file.setChangeType(GitChangeType.MODIFIED);
			        
			        gitController.asyncAddToIndex(Collections.singletonList(file));
			      }
			      
			      d.removeComponentListener(this);
			    }
			  })
			);

		} catch (MalformedURLException | NoRepositorySelected e1) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e1.getMessage(), e1);
			}
		}
	}
	
	/**
	 * Shows a two way diff between the two revisions of a file.
	 * 
	 * @param commit1   The first revision to compare.
	 * @param leftPath  The path of the file. Relative to the working tree.
	 * @param commit2   The second revision to compare.
	 * @param rightPath The path of the file. Relative to the working tree.
	 * 
	 * @throws MalformedURLException Unable to build the URL.
	 */
	public static void showTwoWayDiff(
	    CommitCharacteristics commit1,
	    String leftPath,
	    CommitCharacteristics commit2, 
	    String rightPath) throws MalformedURLException {
	  URL left = GitRevisionURLHandler.encodeURL(commit1.getCommitId(), leftPath);
	  URL right = GitRevisionURLHandler.encodeURL(commit2.getCommitId(), rightPath);
	  
	  String leftLabel = getCommitInfoLabelForDiffSidePanel(leftPath, commit1);
    String rightLabel = getCommitInfoLabelForDiffSidePanel(rightPath, commit2);
	  
	  showDiffFrame(left, leftLabel, right, rightLabel, null, leftPath);
	}
	
	/**
   * Shows a two way diff between the two revisions of a file.
   * 
   * @param commit1   The first revision to compare.
   * @param leftPath  The path of the file. Relative to the working tree.
   * @param commit2   The second revision to compare.
   * @param rightPath The path of the file. Relative to the working tree.
   * 
   * @throws MalformedURLException Unable to build the URL.
   */
  public static void showTwoWayDiff(
      CommitCharacteristics commit1,
      String leftPath,
      RevCommit commit2, 
      String rightPath) throws MalformedURLException {
    URL left = GitRevisionURLHandler.encodeURL(commit1.getCommitId(), leftPath);
    URL right = GitRevisionURLHandler.encodeURL(commit2.name(), rightPath);
    
    String leftLabel = getCommitInfoLabelForDiffSidePanel(leftPath, commit1);
    String rightLabel = getCommitInfoLabelForDiffSidePanel(rightPath, commit2);
    
    showDiffFrame(left, leftLabel, right, rightLabel, null, leftPath);
  }

	/**
	 * Create diff frame.
	 * 
	 * @param leftURL     Left URL.
	 * @param leftLabel   Left label.
	 * @param rightURL    Right URL.
	 * @param rightLabel  Right label.
	 * @param baseURL     URL to the base version of the resource.
	 * @param filePath    The path of the file. Relative to the working tree.
	 * 
	 * @return The DIFF frame.
	 */
	private static Optional<JFrame> showDiffFrame(
	    URL leftURL,
	    String leftLabel,
	    URL rightURL,
	    String rightLabel,
	    URL baseURL,
	    String filePath) {
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Left:  {}", leftURL);
	    LOGGER.debug("Right: {}", rightURL);
	    LOGGER.debug("Base:   {}", baseURL);
	  }
	  
	  boolean threeWays = baseURL != null;
	  try {
	    if (threeWays) {
	      // checks whether a base commit exists or not. If not, then the a 2-way
	      // diff is presented
	      ObjectId baseCommit = GitAccess.getInstance().getBaseCommit(null);
	      if (baseCommit == null 
	          || GitAccess.getInstance().getLoaderFrom(baseCommit, filePath) == null) {
	        threeWays = false;
	      }
	    }
	  } catch (IOException e) {
	    threeWays = false;
	    LOGGER.error(e.getMessage(), e);
	  }

	  JFrame diffFrame = null;
	  StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    if (threeWays) {
      diffFrame = (JFrame) pluginWS.openDiffFilesApplication(leftLabel, leftURL, rightLabel, rightURL, baseURL, false);
	  } else {
	    diffFrame = (JFrame) pluginWS.openDiffFilesApplication(leftLabel, leftURL, rightLabel, rightURL);
	  }
	  
	  // The "openDiffFilesApplication()" API may return null
	  if (diffFrame != null) {
	    final JFrame fDiffFrame = diffFrame;
	    diffFrame.addWindowListener(new WindowAdapter() {
	      @Override
	      public void windowOpened(WindowEvent e) {
	        fDiffFrame.removeWindowListener(this);
	        AuthenticationInterceptor.install();
	      }
	    });
	  }
	  
	  return Optional.ofNullable(diffFrame);
	}

	/**
	 * Shows a two-way diff between the local copy and the copy at the given revision.
	 * 
	 * @param filePath File to compare.
	 * @param commitId Revision ID.
	 * 
	 * @throws NoRepositorySelected
	 * @throws GitAPIException 
	 * @throws IOException 
	 */
  public static void showTwoWayDiffWithLocal(String filePath, String commitId) throws NoRepositorySelected, IOException, GitAPIException {
    String localFilePath = RevCommitUtil.getNewPathInWorkingCopy(
        GitAccess.getInstance().getGit(), 
        filePath, 
        commitId);
    
    URL left = FileUtil.getFileURL(localFilePath);
    URL right = GitRevisionURLHandler.encodeURL(commitId, filePath);
    
    showDiffFrame(left, null, right, null, null, filePath);
  }
  
  
  /**
  * Shows a two-way diff between the local copy and the copy at the given revision.
  * 
  * @param filePath File to compare.
  * @param commitId Revision ID.
  *
  * @throws IOException 
  */
 public static void showTwoWayDiffOnlyGitFile(String filePath, String commitId) throws IOException {
  
   URL right = GitRevisionURLHandler.encodeURL(commitId, filePath);
   
   showDiffFrame(null, null, right, null, null, filePath);
 }
  

  /**
   * Show a two way compare over 2 revisions. 
   * 
   * @param fileStatus File data.
   * 
   * @throws MalformedURLException Unable to build the URLs required to compare. 
   */
  public static void showTwoWayDiff(FileStatusOverDiffEntry fileStatus) throws MalformedURLException {
    String newRevId = fileStatus.getNewRevId();
    String newPath = fileStatus.getDiffEntry().getNewPath();
    
    String oldRevId = fileStatus.getOldRevId();
    String oldPath = fileStatus.getDiffEntry().getOldPath();
    
    URL left = GitRevisionURLHandler.encodeURL(newRevId, newPath);
    URL right = GitRevisionURLHandler.encodeURL(oldRevId, oldPath);
    
    String leftLabel = null;
    String rightLabel = null;
    
    try {
      RevCommit newCommit = RevCommitUtil.getCommit(newRevId);
      RevCommit oldCommit = RevCommitUtil.getCommit(oldRevId);
      
      leftLabel = getCommitInfoLabelForDiffSidePanel(newPath, newCommit);
      rightLabel = getCommitInfoLabelForDiffSidePanel(oldPath, oldCommit);
    } catch (NoRepositorySelected | IOException e) {
      LOGGER.error(e, e);
    }
    
    showDiffFrame(left, leftLabel, right, rightLabel, null, newPath);    
  }
  
  /**
   * Get a label about the given file and commit to be rendered above the editor in a diff side panel.
   * 
   * @param filePath The file path.
   * @param commit   The commit.
   * 
   * @return the label.
   */
  private static String getCommitInfoLabelForDiffSidePanel(String filePath, CommitCharacteristics commit) {
    return getCommitInfoLabelForDiffSidePanel(
        filePath,
        commit.getCommitAbbreviatedId(),
        BasicXmlUtil.escape(commit.getAuthor()),
        commit.getDate(),
        getCommitMessageForCommitInfoLabel(commit.getCommitMessage()));
  }

  /**
   * Get a label about the given file and commit to be rendered above the editor in a diff side panel.
   * 
   * @param filePath The file path.
   * @param commit   The commit.
   * 
   * @return the label.
   */
  private static String getCommitInfoLabelForDiffSidePanel(String filePath, RevCommit commit) {
    PersonIdent authorIdent = commit.getAuthorIdent();
    
    return getCommitInfoLabelForDiffSidePanel(
        filePath,
        commit.abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name(),
        authorIdent.getName() + (authorIdent.getEmailAddress().isEmpty() ? "" : " <" + authorIdent.getEmailAddress() + ">"),
        commit.getAuthorIdent().getWhen(),
        getCommitMessageForCommitInfoLabel(commit.getShortMessage()));
  }
  
  /**
   * Get a label about the given file and commit to be rendered above the editor in a diff side panel.
   * 
   * @param filepath       The filepath.
   * @param commitID       The abbreviated ID of the commit.
   * @param author         The author of the commit.
   * @param date           The commit date.
   * @param commitMessage  The commit message.
   * 
   * @return the label.
   */
  @SuppressWarnings("java:S1192")
  private static String getCommitInfoLabelForDiffSidePanel(
      String filepath,
      String commitID,
      String author,
      Date date,
      String commitMessage) {
    return "<html>"
        + "<p><b>" + TRANSLATOR.getTranslation(Tags.FILE) + ":</b> " + filepath + "</p>"
        + "<p><b>" + TRANSLATOR.getTranslation(Tags.COMMIT) + ":</b> " + commitID + "</p>"
        + "<p><b>" + TRANSLATOR.getTranslation(Tags.AUTHOR) + ":</b> " + author +  "</p>"
        + "<p><b>" + TRANSLATOR.getTranslation(Tags.DATE) + ":</b> " + date + "</p>"
        + "<p><b>" + TRANSLATOR.getTranslation(Tags.MESSAGE_LABEL) + ":</b> " +  commitMessage + "</p>" 
        + "</html>";
  }
  
  /**
   * Get the commit message to show in the commit info label at the top of a diff editor. May be truncated.
   * 
   * @param commitMessage The initial commit message, which may be long.
   * 
   * @return the updated message.
   */
  private static String getCommitMessageForCommitInfoLabel(String commitMessage) {
    int maxNoOfMessageChars = Math.min(commitMessage.length(), MAX_COMMIT_MESSAGE_CHARS_IN_DIFF);
    String suffix = commitMessage.length() > MAX_COMMIT_MESSAGE_CHARS_IN_DIFF ? " [...]" : "";
    return commitMessage.substring(0, maxNoOfMessageChars).trim() + suffix;
  }
}
