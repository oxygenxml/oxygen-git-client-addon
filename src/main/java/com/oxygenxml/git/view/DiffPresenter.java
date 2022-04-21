package com.oxygenxml.git.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import javax.swing.JFrame;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RepositoryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;

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
	 * @param fileStatus Changes. 
	 * @param gitCtrl    Git controller.
	 * 
	 */
	public static void showDiff(FileStatus fileStatus, GitControllerBase gitCtrl) {
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
	        showDiffViewForModified(fileStatus.getFileLocation());
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
	  showDiffFrame(url, null, null, fileStatus.getFileLocation());
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
	  showDiffFrame(null, lastCommitedFileURL, null, path);
	}

	/**
	 * Submodule diff.
	 * 
	 * @param path The path of the file to compare. Relative to the working tree.
	 */
	private static void showSubmoduleDiff(String path) {
		try {
			URL currentSubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.CURRENT_SUBMODULE, path);
			URL previouslySubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.PREVIOUSLY_SUBMODULE,
			    path);
			
			showDiffFrame(currentSubmoduleCommit, previouslySubmoduleCommit, previouslySubmoduleCommit, path);
		} catch (MalformedURLException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
	}

	/**
	 * Presents a 2-way diff
	 * 
	 * @param path The path of the file to compare. Relative to the working tree.
	 * 
	 * @throws NoRepositorySelected 
	 */
	private static void showDiffViewForModified(String path) throws NoRepositorySelected {
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
		
		showDiffFrame(fileURL, lastCommitedFileURL, lastCommitedFileURL, path);
	}
	
  /**
   * Presents a 2-way diff
   * 
   * @param path The path of the file to compare. Relative to the working tree.
   *  
   * @throws NoRepositorySelected 
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

    showDiffFrame(leftSideURL, rightSideURL, rightSideURL, path);

  }

	/**
	 * Presents a 3-way diff.
	 * 
	 * @param file The file for which to show the Diff.
	 * @param gitController Git controller. 
	 */
	private static void showConflictDiff(FileStatus file, GitControllerBase gitController) {
		try {
		  URL base = GitRevisionURLHandler.encodeURL(VersionIdentifier.BASE, file.getFileLocation());
		  URL left = null;
		  URL right = null;
		  // builds the URL for the files depending if we are in rebasing state or not
      RepositoryState repositoryState = GitAccess.getInstance().getRepository().getRepositoryState();
      boolean isRebase = repositoryState.equals(RepositoryState.REBASING)
          || repositoryState.equals(RepositoryState.REBASING_INTERACTIVE)
          || repositoryState.equals(RepositoryState.REBASING_MERGE)
          || repositoryState.equals(RepositoryState.REBASING_REBASING);
      if (isRebase) {
        // An unfinished rebased. 
        left = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE_RESOLVED, file.getFileLocation());
        right = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE_ORIGINAL, file.getFileLocation());
      } else {
        left = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE, file.getFileLocation());
        right = GitRevisionURLHandler.encodeURL(VersionIdentifier.THEIRS, file.getFileLocation());
      }

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());

			// time stamp used for detecting if the file was changed in the diff view
			final long diffStartedTimeStamp = localCopy.lastModified();

			Optional<JFrame> diffFrame = showDiffFrame(left, right, base, file.getFileLocation());
			// checks if the file in conflict has been resolved or not after the diff
			// view was closed
			diffFrame.ifPresent(d -> 
			  d.addComponentListener(new ComponentAdapter() {
			    @Override
			    public void componentHidden(ComponentEvent e) {
			      long diffClosedTimeStamp = localCopy.lastModified();
			      if (diffClosedTimeStamp == diffStartedTimeStamp) {
			        String message = isRebase ? TRANSLATOR.getTranslation(Tags.KEEP_RESOLVED_VERSION_FOR_REBASE_CONFLICT)
			            : TRANSLATOR.getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED);
			        int response = MessagePresenterProvider.getPresenter().showWarningMessageWithConfirmation(
			            TRANSLATOR.getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED_TITLE),
			            message,
			            TRANSLATOR.getTranslation(Tags.RESOLVE_ANYWAY),
			            TRANSLATOR.getTranslation(Tags.KEEP_CONFLICT));
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
	 * @param leftCommitID The first revision to compare.
	 * @param leftPath The path of the file. Relative to the working tree.
	 * @param rightCommitID The second revision to comapre.
	 * @param rightPath The path of the file. Relative to the working tree.
	 * 
	 * @throws MalformedURLException Unable to build the URL.
	 */
	public static void showTwoWayDiff(
	    String leftCommitID,
	    String leftPath,
	    String rightCommitID, 
	    String rightPath) throws MalformedURLException {
	  URL left = GitRevisionURLHandler.encodeURL(leftCommitID, leftPath);
	  URL right = GitRevisionURLHandler.encodeURL(rightCommitID, rightPath);
	  
	  showDiffFrame(left, right, null, leftPath);
	}

	/**
	 * Create diff frame.
	 * 
	 * @param localURL  URL to the local resource.
	 * @param remoteUL  URL to the remote resource.
	 * @param baseURL   URL to the base version of the resource.
	 * @param filePath The path of the file. Relative to the working tree.
	 * 
	 * @return The DIFF frame.
	 */
	private static Optional<JFrame> showDiffFrame(URL localURL, URL remoteUL, URL baseURL, String filePath) {
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Local  " + localURL);
	    LOGGER.debug("Remote " + remoteUL);
	    LOGGER.debug("Base   " + baseURL);
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
	  if (threeWays) {
      diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
	        .openDiffFilesApplication(localURL, remoteUL, baseURL);
	  } else {
	    diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
	        .openDiffFilesApplication(localURL, remoteUL);
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
    
    showDiffFrame(left, right, null, filePath);
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
   
   showDiffFrame(null, right, null, filePath);
 }
  

  /**
   * Show a two way compare over 2 revisions. 
   * 
   * @param fileStatus File data.
   * 
   * @throws MalformedURLException Unable to build the URLs required to compare. 
   */
  public static void showTwoWayDiff(FileStatusOverDiffEntry fileStatus) throws MalformedURLException {
    URL left = GitRevisionURLHandler.encodeURL(fileStatus.getNewRevId(), fileStatus.getDiffEntry().getNewPath());
    URL right = GitRevisionURLHandler.encodeURL(fileStatus.getOldRevId(), fileStatus.getDiffEntry().getOldPath());
    
    showDiffFrame(left, right, null, fileStatus.getDiffEntry().getNewPath());    
  }
}
