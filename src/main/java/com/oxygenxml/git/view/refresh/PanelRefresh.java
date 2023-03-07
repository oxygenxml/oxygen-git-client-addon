package com.oxygenxml.git.view.refresh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.ProjectHelper;
import com.oxygenxml.git.auth.login.ILoginStatusInfo;
import com.oxygenxml.git.auth.login.LoginMediator;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.RemoteRepositoryChangeWatcher;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.PrivateRepositoryException;
import com.oxygenxml.git.service.exceptions.RepositoryUnavailableException;
import com.oxygenxml.git.service.exceptions.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepositoryStatusInfo;
import com.oxygenxml.git.utils.RepositoryStatusInfo.RepositoryStatus;
import com.oxygenxml.git.view.actions.UpdateActionsStatesListener;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.PassphraseDialog;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.staging.BranchSelectionCombo;
import com.oxygenxml.git.view.staging.ChangesPanel;
import com.oxygenxml.git.view.staging.StagingPanel;

/**
 * Synchronize the models with the Git repository state. 
 * 
 * @author alex_jitianu
 */
public class PanelRefresh implements GitRefreshSupport {
	/**
	 * Refresh events are executed after this delay. Milliseconds.
	 */
	public static final int EXECUTION_DELAY = 500;
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PanelRefresh.class);

	/**
	 * The staging panel.
	 */
	private StagingPanel stagingPanel;
	/**
	 * Git access.
	 */
	private final GitAccess gitAccess = GitAccess.getInstance();
	/**
	 * The last opened project in the Project side-view.
	 */
	private String lastOpenedProject;
	/**
	 * Translation support.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	/**
	 * Refresh executor.
	 */
	private final GitOperationScheduler refreshExecutor = GitOperationScheduler.getInstance();
	/**
	 * Refresh future (representing pending completion of the task).
	 */
	private ScheduledFuture<?> refreshFuture;
	/**
	 * Repository change watcher.
	 */
	private RemoteRepositoryChangeWatcher watcher;
	/**
	 * Branch management panel.
	 */
	private BranchManagementPanel branchesPanel;
	/**
	 * History panel.
	 */
	private HistoryPanel historyPanel;
	
	/**
	 * Supplies a listener that will be used to notify when different 
	 * actions and buttons states should be updated (enabled or disabled) 
	 */
	private Supplier<UpdateActionsStatesListener> updateActionsStatesListenerSupplier = null;
	
	/**
	 * Refresh task.
	 */
	private Runnable refreshRunnable = () -> {
		LOGGER.debug("Start refresh on thread.");

		boolean isAfterRestart = !ProjectHelper.getInstance().wasProjectLoaded();
		if (!ProjectHelper.getInstance().wasRepoJustChanged() || isAfterRestart) {
			try {
				Repository repository = gitAccess.getRepository();
				if (repository != null) {
					if (stagingPanel != null) {
					  // refresh the states of the actions
					  stagingPanel.getGitActionsManager().refreshActionsStates();
					  
					  // call the listener; can be null from tests
					  Optional.ofNullable(updateActionsStatesListenerSupplier)
  					  .filter(t -> Objects.nonNull(t.get()))
  					  .map(Supplier<UpdateActionsStatesListener>::get)
  					  .ifPresent(UpdateActionsStatesListener::updateButtonStates);
					  
					  Optional.ofNullable(stagingPanel.getBranchesCombo()).ifPresent(BranchSelectionCombo::refresh);
					  
					  // refresh the buttons
					  stagingPanel.updateConflictButtonsPanelBasedOnRepoState();
					  stagingPanel.updateToolbarsButtonsStates();
						
						GitStatus status = GitAccess.getInstance().getStatus();
						updateFiles(
								stagingPanel.getUnstagedChangesPanel(), 
								status.getUnstagedFiles());
						updateFiles(
								stagingPanel.getStagedChangesPanel(), 
								status.getStagedFiles());

						RepositoryStatusInfo rstatus = fetch();
						updateCounters(rstatus);

						if (OptionsManager.getInstance().isNotifyAboutNewRemoteCommits()) {
							// Make the check more frequently.
							watcher.checkRemoteRepository(false);
						}
						
					}
					if(branchesPanel != null && branchesPanel.isShowing()) {
						branchesPanel.refreshBranches();
					}
					if (historyPanel != null && historyPanel.isShowing()) {
						historyPanel.scheduleRefreshHistory();
					}

					// EXM-47079 Rewrite the fetch property with wildcards.
					BranchesUtil.fixupFetchInConfig(GitAccess.getInstance().getRepository().getConfig());
				}
			} catch (NoRepositorySelected | IOException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}

		LOGGER.debug("End refresh on thread.");
	};

	/**
	 * Constructor.
	 * 
	 * @param watcher repository change watcher.
	 */
	public PanelRefresh(RemoteRepositoryChangeWatcher watcher) {
	  this(watcher, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param watcher repository change watcher.
	 * @param updateActionsStatesListenerSupplier Supplies a listener that will be used to notify when different 
   * actions and buttons states should be updated (enabled or disabled) 
	 */
	public PanelRefresh(RemoteRepositoryChangeWatcher watcher, Supplier<UpdateActionsStatesListener> updateActionsStatesListenerSupplier) {
		this.watcher = watcher;
		this.updateActionsStatesListenerSupplier = updateActionsStatesListenerSupplier;
	}
	
	/**
	 * @see com.oxygenxml.git.utils.GitRefreshSupport.call()
	 */
	@Override
	public void call() {
		if (refreshFuture != null && !refreshFuture.isDone()) {
			LOGGER.debug("cancel refresh task");
			refreshFuture.cancel(true);
		}
		refreshFuture = refreshExecutor.schedule(refreshRunnable, getScheduleDelay());
	}

	/**
	 * @return The coalescing event delay, in milliseconds.
	 */
	protected int getScheduleDelay() {
		return EXECUTION_DELAY;
	}

	/**
	 * Update the counters presented on the Pull/Push toolbar action.
	 * 
	 * @param status The current status.
	 */
	private void updateCounters(RepositoryStatusInfo status) {
		stagingPanel.getCommitPanel().setRepoStatus(status);
	}

	/**
	 * Fetch the latest changes from the remote repository.
	 * 
	 * @return Repository status.
	 */
	private RepositoryStatusInfo fetch() {
		// Connect to the remote.
		RepositoryStatusInfo statusInfo = new RepositoryStatusInfo(RepositoryStatus.AVAILABLE);
		try {
			GitAccess.getInstance().fetch();
		} catch (RepositoryUnavailableException e) {
			statusInfo = new RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, computeStatusExtraInfo(e));
		} catch (SSHPassphraseRequiredException e) {
			statusInfo = new RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, computeStatusExtraInfo(e));

			String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
			if (sshPassphrase != null && !sshPassphrase.isEmpty()) {
				// If the passphrase is null or empty, it is already treated by
				// com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider.get(URIish, CredentialItem...)

				String message =  TRANSLATOR.getTranslation(Tags.PREVIOUS_PASS_PHRASE_INVALID)
						+ " "
						+ TRANSLATOR.getTranslation(Tags.PLEASE_TRY_AGAIN);
				String passphrase = new PassphraseDialog(message).getPassphrase();
				if(passphrase != null) {
					return fetch();
				}
			}
		} catch (PrivateRepositoryException e) {
			statusInfo = new RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, computeStatusExtraInfo(e));

			 Optional<ILoginStatusInfo> loginInfoOpt = LoginMediator.getInstance().requestLogin(GitAccess.getInstance().getHostName(),
			     TRANSLATOR.getTranslation(Tags.LOGIN_DIALOG_PRIVATE_REPOSITORY_MESSAGE));
			if (loginInfoOpt.isPresent() && loginInfoOpt.get().getCredentials() != null) {
				return fetch();
			}
		} catch (Exception e) {
			statusInfo = new RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, computeStatusExtraInfo(e));
			LOGGER.error(e.getMessage(), e);
		}
		return statusInfo;
	}

	/**
	 * Compute status extra info.
	 * 
	 * @param e Exception.
	 * 
	 * @return The extra info about the current repo status.
	 */
	private String computeStatusExtraInfo(Throwable e) {
		String remoteURLFromConfig = null;
		try {
			remoteURLFromConfig = gitAccess.getRemoteURLFromConfig();
		} catch (NoRepositorySelected ex) {
			LOGGER.debug(ex.getMessage(), ex);
		}
		String extraInfo = e.getMessage();
		if (remoteURLFromConfig != null && !extraInfo.contains(remoteURLFromConfig)) {
			extraInfo += "\n" + TRANSLATOR.getTranslation(Tags.REMOTE_REPO_URL) + " " + remoteURLFromConfig;
		}
		return extraInfo;
	}

	/**
	 * Updates the files in the model. 
	 * 
	 * @param panelToUpdate The panel to update: staged or unstaged resources panel.
	 * @param newfiles The new files to be presented in the panel.
	 */
	private void updateFiles(ChangesPanel panelToUpdate, final List<FileStatus> newfiles) {
		// The current files presented in the panel.
		List<FileStatus> filesInModel = panelToUpdate.getFilesStatuses();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("New files      " + newfiles);
			LOGGER.debug("Files in model " + filesInModel);
		}

		// Quick change detection.
		boolean changeDetected = newfiles.size() != filesInModel.size();
		if (!changeDetected) {
			// Same size. Sort and compare files.
			Collections.sort(newfiles, (o1, o2) -> o1.getFileLocation().compareTo(o2.getFileLocation()));
			List<FileStatus> sortedModel = new ArrayList<>(filesInModel.size());
			Collections.sort(sortedModel, (o1, o2) -> o1.getFileLocation().compareTo(o2.getFileLocation()));

			changeDetected = !newfiles.equals(sortedModel);
		}

		if (changeDetected) {
			SwingUtilities.invokeLater(() -> panelToUpdate.update(newfiles));
		}
	}

	/**
	 * Links the refresh support with the staging panel.
	 * 
	 * @param stagingPanel Staging panel.
	 */
	public void setStagingPanel(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
	}

	/**
	 * Links the refresh support with branch manager view.
	 * 
	 * @param branchesPanel The branch manager panel.
	 */
	public void setBranchPanel(BranchManagementPanel branchesPanel) {
		this.branchesPanel = branchesPanel;
	}

	/**
	 * Links the refresh support with teh history view.
	 * 
	 * @param historyPanel The history panel.
	 */
	public void setHistoryPanel(HistoryPanel historyPanel) {
		this.historyPanel = historyPanel;
	}

	/**
	 * Attempts to shutdown any running refresh tasks.
	 */
	public void shutdown() {
		if (refreshFuture != null) {
			// Just in case the task isn't running yet.
			refreshFuture.cancel(false);
		}
		refreshExecutor.shutdown();
	}

	/**
	 * @return The last scheduled task for refresing the Git status.
	 */
	@TestOnly
	public ScheduledFuture<?> getScheduledTaskForTests() { // NOSONAR
		return refreshFuture;
	}

}
