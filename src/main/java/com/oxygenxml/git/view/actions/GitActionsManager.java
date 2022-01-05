package com.oxygenxml.git.view.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.log4j.Logger;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.common.collect.Sets;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.internal.CloneRepositoryAction;
import com.oxygenxml.git.view.actions.internal.EditConfigFileAction;
import com.oxygenxml.git.view.actions.internal.ListStashesAction;
import com.oxygenxml.git.view.actions.internal.ManageRemoteRepositoriesAction;
import com.oxygenxml.git.view.actions.internal.OpenPreferencesAction;
import com.oxygenxml.git.view.actions.internal.OpenRepositoryAction;
import com.oxygenxml.git.view.actions.internal.PullAction;
import com.oxygenxml.git.view.actions.internal.PushAction;
import com.oxygenxml.git.view.actions.internal.ResetAllCredentialsAction;
import com.oxygenxml.git.view.actions.internal.SetRemoteAction;
import com.oxygenxml.git.view.actions.internal.ShowBranchesAction;
import com.oxygenxml.git.view.actions.internal.ShowHistoryAction;
import com.oxygenxml.git.view.actions.internal.ShowStagingAction;
import com.oxygenxml.git.view.actions.internal.ShowTagsAction;
import com.oxygenxml.git.view.actions.internal.StashChangesAction;
import com.oxygenxml.git.view.actions.internal.SubmodulesAction;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;
import com.oxygenxml.git.view.tags.GitTagsManager;


/**
 * Manage the Git actions for current repository.
 * 
 * @author Alex_Smarandache
 *
 */
public class GitActionsManager  {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(GitActionsManager.class);

  /**
   * The translator for translations.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Actions that needs to be refreshed.
   */
  private static final Set<GitOperation> REFRESH_AWARE_ACTIONS = Sets.newHashSet(
      GitOperation.ABORT_REBASE, 
      GitOperation.CONTINUE_REBASE, 
      GitOperation.COMMIT,
      GitOperation.DISCARD,
      GitOperation.CHECKOUT,
      GitOperation.CHECKOUT_COMMIT,
      GitOperation.CREATE_TAG,
      GitOperation.DELETE_TAG);

	/**
	 * Clone new repository action.
	 */
	private CloneRepositoryAction cloneRepositoryAction;
	
	/**
	 * Open repository action.
	 */
	private OpenRepositoryAction openRepositoryAction;

	/**
	 * Push action.
	 */
	private AbstractAction pushAction;

	/**
	 * Pull merge action.
	 */
	private AbstractAction pullMergeAction;

	/**
	 * Pull rebase action.
	 */
	private AbstractAction pullRebaseAction;
	
	/**
	 * Action to show git staging.
	 */
	private ShowStagingAction showStagingAction;

	/**
	 * Action to show branches.
	 */
	private AbstractAction showBranchesAction;

	/**
	 * Action to show history.
	 */
	private AbstractAction showHistoryAction;

	/**
	 * Action to show tags.
	 */
	private AbstractAction showTagsAction;

	/**
	 * Action for submodule.
	 */
	private AbstractAction submoduleAction;

	/**
	 * Action to stash changes.
	 */
	private AbstractAction stashChangesAction;

	/**
	 * Action to list stahses.
	 */
	private AbstractAction listStashesAction; 

	/**
	 * Action for edit config file.
	 */
	private AbstractAction editConfigFileAction;

	/**
	 * Action to manage the remote repositories.
	 */
	private AbstractAction manageRemoteRepositoriesAction; 

	/**
	 * Action to track remote branch.
	 */
	private AbstractAction trackRemoteBranchAction;
	
	/**
	 * Action to open preferences page for Git Plugin.
	 */
	private OpenPreferencesAction openPreferencesAction;
	
	/**
	 * Action to reset all credentials.
	 */
	private ResetAllCredentialsAction resetAllCredentialsAction;

	/**
	 * The Git Controller.
	 */
	private final GitController gitController;    

	/**
	 * The history controller.
	 */
	private final HistoryController historyController;

	/**
	 *  Branch management view presenter.
	 */
	private final BranchManagementViewPresenter  branchManagementViewPresenter;
	
	/**
	 *  Refresh support. Needed by some actions.
	 */
	private final GitRefreshSupport refreshSupport;

	/**
	 * All actions.
	 */
	private final List<AbstractAction> allActions = new ArrayList<>();

	/**
	 * The current repository.
	 */
	private Repository repository;

	/**
	 * Constructor.
	 * 
	 * @param gitController     Git controller.
	 * @param historyController History controller.
	 * @param branchManagementViewPresenter Branch management view presenter.
	 */
	public GitActionsManager(
			final GitController                 gitController, 
			final HistoryController             historyController,
			final BranchManagementViewPresenter branchManagementViewPresenter,
			final GitRefreshSupport             refreshSupport) {

		this.gitController                 = gitController;
		this.historyController             = historyController;
		this.branchManagementViewPresenter = branchManagementViewPresenter;
		this.refreshSupport                = refreshSupport;
		
		try {
			this.repository                = gitController.getGitAccess().getRepository();
		} catch (NoRepositorySelected e) {
			LOGGER.debug(e, e);
		}

		gitController.addGitListener(new GitEventAdapter() {
			@Override
			public void operationSuccessfullyEnded(GitEventInfo info) {
				GitOperation operation = info.getGitOperation();
				if (operation == GitOperation.OPEN_WORKING_COPY) {
					if(submoduleAction != null) {
						submoduleAction.setEnabled(gitController.getGitAccess()
								.getSubmoduleAccess().getSubmodules().isEmpty());
					}
				} else if (REFRESH_AWARE_ACTIONS.contains(operation)) {
				  refreshActionsStates();
				}
			}
		});
	}


	/**
	 * @return The clone repository action.
	 */
	@NonNull
	public AbstractAction getCloneRepositoryAction() {
		if(cloneRepositoryAction == null) {
			cloneRepositoryAction = new CloneRepositoryAction();
			cloneRepositoryAction.putValue(Action.SMALL_ICON, Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON));
			allActions.add(cloneRepositoryAction);
		}

		return cloneRepositoryAction;
	}

	
	/**
	 * @return The open repository action.
	 */
	@NonNull
	public AbstractAction getOpenRepositoryAction() {
		if(openRepositoryAction == null) {
			openRepositoryAction = new OpenRepositoryAction(gitController);
			allActions.add(openRepositoryAction);
		}

		return openRepositoryAction;
	}

	
	/**
	 * @return The push action.
	 */
	@NonNull
	public AbstractAction getPushAction() {
		if(pushAction == null) {
			pushAction = new PushAction(gitController);
			pushAction.setEnabled(repository != null);
			allActions.add(pushAction);
		}

		return pushAction;
	}


	/**
	 * @return The pull merge action.
	 */
	@NonNull
	public AbstractAction getPullMergeAction() {
		if(pullMergeAction == null) {
			pullMergeAction = new PullAction(gitController, 
					TRANSLATOR.getTranslation(Tags.PULL_MERGE), PullType.MERGE_FF);
			pullMergeAction.setEnabled(repository != null);
			allActions.add(pullMergeAction);
		}

		return pullMergeAction;
	}


	/**
	 * @return The pull merge action.
	 */
	@NonNull
	public AbstractAction getPullRebaseAction() {
		if(pullRebaseAction == null) {
			pullRebaseAction = new PullAction(gitController, 
					TRANSLATOR.getTranslation(Tags.PULL_REBASE), PullType.REBASE);
			pullRebaseAction.setEnabled(repository != null);
			allActions.add(pullRebaseAction);
		}

		return pullRebaseAction;
	}


	/**
	 * @return The show staging action.
	 */
	@NonNull
	public AbstractAction getShowStagingAction() {
		if(showStagingAction == null) {
			showStagingAction = new ShowStagingAction();
			allActions.add(showStagingAction);
		}

		return showStagingAction;
	}
	
	
	/**
	 * @return The show history action.
	 */
	@NonNull
	public AbstractAction getShowHistoryAction() {
		if(showHistoryAction == null) {
			showHistoryAction = new ShowHistoryAction(historyController);
			showHistoryAction.setEnabled(repository != null);
			allActions.add(showHistoryAction);
		}

		return showHistoryAction;
	}


	/**
	 * @return The show branches action.
	 */
	@NonNull
	public AbstractAction getShowBranchesAction() {
		if(showBranchesAction == null) {
			showBranchesAction = new ShowBranchesAction(branchManagementViewPresenter);
			showBranchesAction.setEnabled(repository != null);
			allActions.add(showBranchesAction);
		}

		return showBranchesAction;
	}


	/**
	 * @return The show tags action.
	 */
	@NonNull
	public AbstractAction getShowTagsAction() {
		if(showTagsAction == null) {
			showTagsAction = new ShowTagsAction();
			showTagsAction.setEnabled(repository != null);
			allActions.add(showTagsAction);
		}

		return showTagsAction;
	}


	/**
	 * @return The stash changes action.
	 */
	@NonNull
	public AbstractAction getStashChangesAction() {
		if(stashChangesAction == null) {
			stashChangesAction = new StashChangesAction();
			stashChangesAction.setEnabled(repository != null);
			allActions.add(stashChangesAction);
		}

		return stashChangesAction;
	}


	/**
	 * @return The list stashes action.
	 */
	@NonNull
	public AbstractAction getListStashesAction() {
		if(listStashesAction == null) {
			listStashesAction = new ListStashesAction();
			listStashesAction.setEnabled(repository != null);
			allActions.add(listStashesAction);
		}

		return listStashesAction;
	}


	/**
	 * @return The edit config file action.
	 */
	@NonNull
	public AbstractAction getEditConfigAction() {
		if(editConfigFileAction == null) {
			editConfigFileAction = new EditConfigFileAction();
			editConfigFileAction.setEnabled(repository != null);
			allActions.add(editConfigFileAction);
		}

		return editConfigFileAction;
	}


	/**
	 * @return The manage remote repositories action.
	 */
	@NonNull
	public AbstractAction getManageRemoteRepositoriesAction() {
		if(manageRemoteRepositoriesAction == null) {
			manageRemoteRepositoriesAction = new ManageRemoteRepositoriesAction();
			manageRemoteRepositoriesAction.setEnabled(repository != null);
			allActions.add(manageRemoteRepositoriesAction);
		}

		return manageRemoteRepositoriesAction;
	}


	/**
	 * @return The track remote branch action.
	 */
	@NonNull
	public AbstractAction getTrackRemoteBranchAction() {
		if(trackRemoteBranchAction == null) {
			trackRemoteBranchAction = new SetRemoteAction();
			trackRemoteBranchAction.setEnabled(repository != null);
			allActions.add(trackRemoteBranchAction);
		}

		return trackRemoteBranchAction;
	}


	/**
	 * @return The submodule action.
	 */
	@NonNull
	public AbstractAction getSubmoduleAction() {
		if(submoduleAction == null) {
			submoduleAction = new SubmodulesAction();
			submoduleAction.setEnabled(repository != null && hasRepositorySubmodules());
			allActions.add(submoduleAction);
		}

		return submoduleAction;
	}


	/**
	 * @return The open git client preferences page action.
	 */
	@NonNull
	public AbstractAction getOpenPreferencesAction() {
		if(openPreferencesAction == null) {
			openPreferencesAction = new OpenPreferencesAction();
			allActions.add(openPreferencesAction);
		}

		return openPreferencesAction;
	}
	
	
	/**
	 * @return The reset all credentials action.
	 */
	@NonNull
	public AbstractAction getResetAllCredentialsAction() {
		if(resetAllCredentialsAction == null) {
			resetAllCredentialsAction = new ResetAllCredentialsAction(refreshSupport);
			allActions.add(resetAllCredentialsAction);
		}

		return resetAllCredentialsAction;
	}
	
	
	/**
	 * Refresh actions after a git operation that could affect the action.
	 */
	public void refreshActionsStates() {
		repository = null;

		try {
			repository = gitController.getGitAccess().getRepository();
		} catch (NoRepositorySelected e) {
			LOGGER.debug(e, e);
		}

		final boolean isRepoOpen = repository != null;

		allActions.forEach(action -> action.setEnabled(isRepoOpen));

		if(isRepoOpen) {
			updateActionsStatus();
		}

	}


	/**
	 * Update the actions status when repository is open.
	 */
	private void updateActionsStatus() {
		if(showTagsAction != null) {
			try {
				int noOfTags = GitTagsManager.getNoOfTags();
				showTagsAction.setEnabled(noOfTags > 0);
			} catch (GitAPIException e) {
				LOGGER.debug(e,e);
				showTagsAction.setEnabled(false);
			}
		}

		if(submoduleAction != null) {
			submoduleAction.setEnabled(hasRepositorySubmodules());
		}

		if(stashChangesAction != null) {
			List<FileStatus> unstagedFiles = gitController.getGitAccess().getUnstagedFiles();
			boolean existsLocalFiles = unstagedFiles != null && !unstagedFiles.isEmpty();

			if(!existsLocalFiles) {
				List<FileStatus> stagedFiles = gitController.getGitAccess().getStagedFiles();
				existsLocalFiles = stagedFiles != null && !stagedFiles.isEmpty();
			}

			stashChangesAction.setEnabled(existsLocalFiles);	    
		}

		if(listStashesAction != null) {
			Collection<RevCommit> stashes =  gitController.getGitAccess().listStashes();
			int noOfStashes = stashes == null ? 0 : stashes.size();
			listStashesAction.setEnabled(noOfStashes > 0);
		}
	}

	
	/**
	 * @return <code>true</code> if the repository has submodules.
	 */
	protected boolean hasRepositorySubmodules() {
	  return !gitController.getGitAccess().getSubmoduleAccess().getSubmodules().isEmpty();
	}
    
}
