package com.oxygenxml.git.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.ObjectId;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Displays the diff depending on the what change type the file is.
 * 
 * @author Beniamin Savu
 *
 */
public class DiffPresenter {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(DiffPresenter.class);
	
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
	public static void showDiff(FileStatus fileStatus, GitController gitCtrl) {
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
	    
	    if (logger.isDebugEnabled()) {
	      logger.debug(ex, ex);
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
	      url = FileHelper.getFileURL(fileStatus.getFileLocation());  
	    }
	  } catch (MalformedURLException | NoRepositorySelected e) {
	    // Shouldn't rreally happen
	    if (logger.isDebugEnabled()) {
	      logger.debug(e, e);
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
	    if (logger.isDebugEnabled()) {
	      logger.debug(e1, e1);
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
		GitAccess.getInstance().submoduleCompare(path, true);
		try {
			URL currentSubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.CURRENT_SUBMODULE, path);
			URL previouslySubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.PREVIOUSLY_SUBMODULE,
			    path);
			
			showDiffFrame(currentSubmoduleCommit, previouslySubmoduleCommit, previouslySubmoduleCommit, path);
		} catch (MalformedURLException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
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
		URL fileURL = FileHelper.getFileURL(path);
		URL lastCommitedFileURL = null;

		try {
			lastCommitedFileURL = GitRevisionURLHandler.encodeURL(
			    VersionIdentifier.INDEX_OR_LAST_COMMIT, path);
		} catch (MalformedURLException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
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
    URL leftSideURL = FileHelper.getFileURL(path);
    URL rightSideURL = null;

    try {
      leftSideURL  = GitRevisionURLHandler.encodeURL(VersionIdentifier.INDEX_OR_LAST_COMMIT, path);
      
      rightSideURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.LAST_COMMIT, path);
    } catch (MalformedURLException e1) {
      if (logger.isDebugEnabled()) {
        logger.debug(e1, e1);
      }
    }

    showDiffFrame(leftSideURL, rightSideURL, rightSideURL, path);

  }

	/**
	 * Presents a 3-way diff
	 * @param file 
	 */
	private static void showConflictDiff(FileStatus file, GitController stageController) {
		try {
			// builds the URL for the files
			URL local = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE, file.getFileLocation());
			URL remote = GitRevisionURLHandler.encodeURL(VersionIdentifier.THEIRS, file.getFileLocation());
			URL base = GitRevisionURLHandler.encodeURL(VersionIdentifier.BASE, file.getFileLocation());

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());

			// time stamp used for detecting if the file was changed in the diff view
			final long diffStartedTimeStamp = localCopy.lastModified();

			Optional<JFrame> diffFrame = showDiffFrame(local, remote, base, file.getFileLocation());
			// checks if the file in conflict has been resolved or not after the diff
			// view was closed
			diffFrame.ifPresent(d -> 
			  d.addComponentListener(new ComponentAdapter() {
			    @Override
			    public void componentHidden(ComponentEvent e) {
			      long diffClosedTimeStamp = localCopy.lastModified();
			      
			      if (diffClosedTimeStamp == diffStartedTimeStamp) {
			        
			        String[] options = new String[] { "   Yes   ", "   No   " };
			        int[] optonsId = new int[] { 0, 1 };
			        int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
			            Translator.getInstance().getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED_TITLE),
			            Translator.getInstance().getTranslation(Tags.CHECK_IF_CONFLICT_RESOLVED), options, optonsId);
			        if (response == 0) {
			          stageController.doGitCommand(
			              Arrays.asList(file),
			              GitCommand.RESOLVE_USING_MINE);
			        }
			      } else {
			        // Instead of requesting the file status again, we just mark it as modified.
			        file.setChangeType(GitChangeType.MODIFIED);
			        
			        stageController.doGitCommand(Arrays.asList(file), GitCommand.STAGE);
			      }
			      
			      d.removeComponentListener(this);
			    }
			  })
			);

		} catch (MalformedURLException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		}
	}
	
	/**
	 * Shows a two way diff between the two revisions of a file.
	 * 
	 * @param leftCommitID The first revision to compare.
	 * @param rightCommitID The second revision to comapre.
	 * @param filePath The path of the file. Relative to the working tree.
	 * 
	 * @throws MalformedURLException Unable to build the URL.
	 */
	public static void showTwoWayDiff(String leftCommitID, String rightCommitID, String filePath) throws MalformedURLException {
	  URL left = GitRevisionURLHandler.encodeURL(leftCommitID, filePath);
	  URL right = GitRevisionURLHandler.encodeURL(rightCommitID, filePath);
	  
	  showDiffFrame(left, right, null, filePath);
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
	  if (logger.isDebugEnabled()) {
	    logger.debug("Local  " + localURL);
	    logger.debug("Remote " + remoteUL);
	    logger.debug("Base   " + baseURL);
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
	    logger.error(e, e);
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
	 * @throws MalformedURLException
	 */
  public static void showTwoWayDiffWithLocal(String filePath, String commitId) throws NoRepositorySelected, MalformedURLException {
    URL left = FileHelper.getFileURL(filePath);
    URL right = GitRevisionURLHandler.encodeURL(commitId, filePath);
    
    showDiffFrame(left, right, null, filePath);
  }
}
