package com.oxygenxml.git.view.actions;

import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(GitActionsManager.class);

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
	public GitActionsManager(final GitController gitController, 
	    final HistoryController historyController,
			final BranchManagementViewPresenter branchManagementViewPresenter, 
			final GitRefreshSupport refreshSupport) {
	  
		this.gitController = gitController;
		this.historyController = historyController;
		this.branchManagementViewPresenter = branchManagementViewPresenter;
		this.refreshSupport = refreshSupport;
		
		try {
			this.repository = gitController.getGitAccess().getRepository();
		} catch (NoRepositorySelected e) {
			LOGGER.debug(e.getMessage(), e);
		}

		gitController.addGitListener(new GitEventAdapter() {
			@Override
			public void operationSuccessfullyEnded(GitEventInfo info) {
				GitOperation operation = info.getGitOperation();
				if (operation == GitOperation.OPEN_WORKING_COPY) {
					if(submoduleAction != null) {
					  final boolean hasSubmodules = hasRepositorySubmodules();
					  SwingUtilities.invokeLater(() -> 
		          submoduleAction.setEnabled(hasSubmodules));
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
			final boolean hasRepository = hasRepository();
      SwingUtilities.invokeLater(() -> pushAction.setEnabled(hasRepository));
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
			final boolean hasRepository = hasRepository();
	    SwingUtilities.invokeLater(() -> pullMergeAction.setEnabled(hasRepository));
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
			final boolean hasRepository = hasRepository();
			SwingUtilities.invokeLater(() -> pullRebaseAction.setEnabled(hasRepository));
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
			final boolean couldShowHistory = couldShowBranchesOrHistory();
      SwingUtilities.invokeLater(() -> showHistoryAction.setEnabled(couldShowHistory));
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
			final boolean couldShowBranches = couldShowBranchesOrHistory();
      SwingUtilities.invokeLater(() -> showBranchesAction.setEnabled(couldShowBranches));
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
			try {
        final boolean enableTags = hasRepository() &&  GitTagsManager.getNoOfTags() > 0;
        SwingUtilities.invokeLater(() -> showTagsAction.setEnabled(enableTags));
      } catch (GitAPIException e) {
        LOGGER.debug(e.getMessage(),  e);
        SwingUtilities.invokeLater(() -> showTagsAction.setEnabled(false));
      }
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
			final boolean hasRepo = hasRepository();
			SwingUtilities.invokeLater(() -> stashChangesAction.setEnabled(hasRepo));
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
			final boolean hasRepository = hasRepository();
	    SwingUtilities.invokeLater(() -> listStashesAction.setEnabled(hasRepository));
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
			final boolean hasRepository = hasRepository();
      SwingUtilities.invokeLater(() -> editConfigFileAction.setEnabled(hasRepository));
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
			final boolean hasRepository = hasRepository();
      SwingUtilities.invokeLater(() -> manageRemoteRepositoriesAction.setEnabled(
          hasRepository));
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
			 final boolean hasRepository = hasRepository();
	      SwingUtilities.invokeLater(() -> trackRemoteBranchAction.setEnabled(
	          hasRepository));
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
			final boolean enableAction = hasRepository() && hasRepositorySubmodules();
			SwingUtilities.invokeLater(() -> 
			  submoduleAction.setEnabled(enableAction));
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
			LOGGER.debug(e.getMessage(), e);
		}

		final boolean isRepoOpen = hasRepository();

		if(isRepoOpen) {
		  treatOpenedRepository();
		} else {
		  treatNoRepository();
		}
		
	}


	/**
	 * Checks if a repository exists. 
	 * Protected for tests.
	 * 
	 * @return <code>true</code> if a repository exists.
	 */
  protected boolean hasRepository() {
    return repository != null;
  }
  
	/**
	 * @return <code>true</code> if the repository has submodules.
	 */
	protected boolean hasRepositorySubmodules() {
	  return !gitController.getGitAccess().getSubmoduleAccess().getSubmodules().isEmpty();
	}
	
	/**
	 * @return <code>true</code> if is possible to show Git Branch Manager or Git History.
	 */
	private boolean couldShowBranchesOrHistory() {
	  return gitController.getGitAccess().isRepoInitialized() || !"".equals(OptionsManager.getInstance().getSelectedRepository());
	}
    
	/**
	 * Update actions when no repository is opened.
	 */
	private void treatNoRepository() {
	  
	  if(pushAction != null) {
	    SwingUtilities.invokeLater(() -> pushAction.setEnabled(false));
    }
	  
	  if(pullMergeAction != null) {
	    SwingUtilities.invokeLater(() -> pullMergeAction.setEnabled(false));
	  }
	  
	  if(pullRebaseAction != null) {
	    SwingUtilities.invokeLater(() -> pullRebaseAction.setEnabled(false));
	  }
	  
	  if(showBranchesAction != null) {
	    SwingUtilities.invokeLater(() -> showBranchesAction.setEnabled(false));
	  }
	  
	  if(showHistoryAction != null) {
	    SwingUtilities.invokeLater(() -> showHistoryAction.setEnabled(false));
	  }
	  
	  if(showTagsAction != null) {
	    SwingUtilities.invokeLater(() -> showTagsAction.setEnabled(false));
	  }
	  
	  if(submoduleAction != null) {
	    SwingUtilities.invokeLater(() -> submoduleAction.setEnabled(false));
	  }
	  
	  if(stashChangesAction != null) {
	    SwingUtilities.invokeLater(() -> stashChangesAction.setEnabled(false));
	  }
	  
	  if(listStashesAction != null) {
	    SwingUtilities.invokeLater(() -> listStashesAction.setEnabled(false));
	  }
	  
	  if(manageRemoteRepositoriesAction != null) {
	    SwingUtilities.invokeLater(() -> manageRemoteRepositoriesAction.setEnabled(false));
	  }
	  
	  if(trackRemoteBranchAction != null) {
	    SwingUtilities.invokeLater(() -> trackRemoteBranchAction.setEnabled(false));
	  }
	  
	  if(editConfigFileAction != null) {
	    SwingUtilities.invokeLater(() -> editConfigFileAction.setEnabled(false));
	  }
	  
	}
	
	/**
   * Update actions when a repository is opened.
   */
  private void treatOpenedRepository() {
    
    if(pushAction != null) {
      SwingUtilities.invokeLater(() -> pushAction.setEnabled(true));
    }
    
    if(pullMergeAction != null) {
      SwingUtilities.invokeLater(() -> pullMergeAction.setEnabled(true));
    }
    
    if(pullRebaseAction != null) {
      SwingUtilities.invokeLater(() -> pullRebaseAction.setEnabled(true));
    }
    
    if(manageRemoteRepositoriesAction != null) {
      SwingUtilities.invokeLater(() -> manageRemoteRepositoriesAction.setEnabled(true));
    }
    
    if(trackRemoteBranchAction != null) {
      SwingUtilities.invokeLater(() -> trackRemoteBranchAction.setEnabled(true));
    }
    
    if(editConfigFileAction != null) {
      SwingUtilities.invokeLater(() -> editConfigFileAction.setEnabled(true));
    }
   
    updateActionsStatus();
  }
	
  /**
   * Update the actions status when repository is open.
   */
  private void updateActionsStatus() {
    
    if(showBranchesAction != null || showHistoryAction != null) {
         final boolean enableAction = couldShowBranchesOrHistory();
         if(showBranchesAction != null) {
           SwingUtilities.invokeLater(() -> showBranchesAction.setEnabled(enableAction));
         }
         if(showHistoryAction != null) {
           SwingUtilities.invokeLater(() -> showHistoryAction.setEnabled(enableAction));
         }
    }
    
    if(showTagsAction != null) {
      try {
        final int noOfTags = GitTagsManager.getNoOfTags();
        SwingUtilities.invokeLater(() -> showTagsAction.setEnabled(noOfTags > 0));
      } catch (GitAPIException e) {
        LOGGER.debug(e.getMessage(), e);
        SwingUtilities.invokeLater(() -> showTagsAction.setEnabled(false));
      }
    }

    if(submoduleAction != null) {
      final boolean hasRepoSubmodules = hasRepositorySubmodules();
      SwingUtilities.invokeLater(() -> submoduleAction.setEnabled(hasRepoSubmodules));   
    }

    if(stashChangesAction != null) {
      final boolean hasFileChanged = gitController.getGitAccess().hasFilesChanged();
      SwingUtilities.invokeLater(() -> stashChangesAction.setEnabled(hasFileChanged));    
    }

    if(listStashesAction != null) {
      final boolean hasStashes = gitController.getGitAccess().hasStashes();
      SwingUtilities.invokeLater(() -> listStashesAction.setEnabled(hasStashes)); 
    }
    
  }

}
