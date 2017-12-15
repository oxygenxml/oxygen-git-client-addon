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
import com.oxygenxml.git.view.event.StageController;

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
	 * The file on which the diffPresenter works
	 */
	private FileStatus file;

	/**
	 * The frame of the oxygen's diff
	 */
	private JFrame diffFrame;

	/**
	 * Controller used for staging and unstaging
	 */
	private StageController stageController;

	/**
	 * Constructor.
	 * 
	 * @param file Fiel to DIFF.
	 * @param stageController Stage controller to issue commands.
	 */
	public DiffPresenter(FileStatus file, StageController stageController) {
		this.stageController = stageController;
		this.file = file;
	}

	/**
	 * Perform different actions depending on the file change type. If the file is
	 * a conflict file then a 3-way diff is presented. If the file is a modified
	 * one then a 2-way diff is presented. And if a file is added then the file is
	 * opened
	 * 
	 */
	public void showDiff() {
	  try {
	    GitChangeType changeType = file.getChangeType();
	    switch (changeType) {
	      case CONFLICT:
	        showConflictDiff();
	        break;
	      case CHANGED:
	        showDiffIndexWithHead();
	        break;
	      case MODIFIED:
	        showDiffViewForModified();
	        break;
	      case ADD:
	      case UNTRACKED:
	        diffViewForAddedAndUntracked();
	        break;
	      case SUBMODULE:
	        showSubmoduleDiff();
	        break;
	      case MISSING:
	      case REMOVED:
	        diffViewForMissingAndRemoved();
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
	 */
	private void diffViewForAddedAndUntracked() {
	  URL url = null;
	  try {
	    File localFile = new File(
	        GitAccess.getInstance().getWorkingCopy().getAbsolutePath(),
	        file.getFileLocation());
	    url = localFile.toURI().toURL();
	  } catch (MalformedURLException | NoRepositorySelected e) {
	    // Shouldn't rreally happen
	    if (logger.isDebugEnabled()) {
	      logger.debug(e, e);
	    }
	  }
	  showDiffFrame(url, null, null);
	}

	/**
	 * Diff for missing/deleted resources.
	 */
	private void diffViewForMissingAndRemoved() {
	  URL lastCommitedFileURL = null;
	  try {
	    lastCommitedFileURL = GitRevisionURLHandler.encodeURL(
	        VersionIdentifier.INDEX_OR_LAST_COMMIT, file.getFileLocation());
	  } catch (MalformedURLException e1) {
	    if (logger.isDebugEnabled()) {
	      logger.debug(e1, e1);
	    }
	  }
	  showDiffFrame(null, lastCommitedFileURL, null);
	}

	/**
	 * Submodule diff.
	 */
	private void showSubmoduleDiff() {
		GitAccess.getInstance().submoduleCompare(file.getFileLocation(), true);
		try {
			URL currentSubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.CURRENT_SUBMODULE, file.getFileLocation());
			URL previouslySubmoduleCommit = GitRevisionURLHandler.encodeURL(VersionIdentifier.PREVIOUSLY_SUBMODULE,
					file.getFileLocation());
			
			showDiffFrame(currentSubmoduleCommit, previouslySubmoduleCommit, null);
		} catch (MalformedURLException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Presents a 2-way diff
	 * 
	 * @throws NoRepositorySelected 
	 */
	private void showDiffViewForModified() throws NoRepositorySelected {
	  // The local (WC) version.
		URL fileURL = FileHelper.getFileURL(file.getFileLocation());
		URL lastCommitedFileURL = null;

		try {
			lastCommitedFileURL = GitRevisionURLHandler.encodeURL(
			    VersionIdentifier.INDEX_OR_LAST_COMMIT, file.getFileLocation());
		} catch (MalformedURLException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		}
		
		showDiffFrame(fileURL, lastCommitedFileURL, null);
	}
	
  /**
   * Presents a 2-way diff
   * 
   * @param changeType The type of change.
   *  
   * @throws NoRepositorySelected 
   */
  private void showDiffIndexWithHead() throws NoRepositorySelected {    
    // The local (WC) version.
    URL leftSideURL = FileHelper.getFileURL(file.getFileLocation());
    URL rightSideURL = null;

    try {
      leftSideURL  = GitRevisionURLHandler.encodeURL(VersionIdentifier.INDEX_OR_LAST_COMMIT, file.getFileLocation());
      
      rightSideURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.LAST_COMMIT, file.getFileLocation());
    } catch (MalformedURLException e1) {
      if (logger.isDebugEnabled()) {
        logger.debug(e1, e1);
      }
    }

    showDiffFrame(leftSideURL, rightSideURL, null);

  }

	/**
	 * Presents a 3-way diff
	 */
	private void showConflictDiff() {
		try {
			// builds the URL for the files
			URL local = GitRevisionURLHandler.encodeURL(VersionIdentifier.MINE, file.getFileLocation());
			URL remote = GitRevisionURLHandler.encodeURL(VersionIdentifier.THEIRS, file.getFileLocation());
			URL base = GitRevisionURLHandler.encodeURL(VersionIdentifier.BASE, file.getFileLocation());

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());

			// time stamp used for detecting if the file was changed in the diff view
			final long diffStartedTimeStamp = localCopy.lastModified();

			showDiffFrame(local, remote, base);
			// checks if the file in conflict has been resolved or not after the diff
			// view was closed
			diffFrame.addComponentListener(new ComponentAdapter() {
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
							stageController.doGitCommand(Arrays.asList(file), GitCommand.RESOLVE_USING_MINE);
						}
					} else {
					  // Instead of requesting the file status again, we just mark it as modified.
						file.setChangeType(GitChangeType.MODIFIED);
						
						stageController.doGitCommand(Arrays.asList(file), GitCommand.STAGE);
					}
					
					diffFrame.removeComponentListener(this);
				}
			});

		} catch (MalformedURLException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		}
	}

	/**
	 * Create diff frame.
	 * 
	 * @param localURL  URL to the local resource.
	 * @param remoteUL  URL to the remote resource.
	 * @param baseURL   URL to the base version of the resource.
	 */
	private void showDiffFrame(URL localURL, URL remoteUL, URL baseURL) {
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
	      ObjectId baseCommit = GitAccess.getInstance().getBaseCommit();
	      if (baseCommit == null 
	          || GitAccess.getInstance().getLoaderFrom(baseCommit, file.getFileLocation()) == null) {
	        threeWays = false;
	      }
	    }
	  } catch (IOException e) {
	    threeWays = false;
	    logger.error(e, e);
	  }

	  if (threeWays) {
	    diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
	        .openDiffFilesApplication(localURL, remoteUL, baseURL);
	  } else {
	    diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
	        .openDiffFilesApplication(localURL, remoteUL);
	  }
	  
	  diffFrame.addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowOpened(WindowEvent e) {
	      diffFrame.removeWindowListener(this);
	      AuthenticationInterceptor.install();
	    }
	  });
	}
}
