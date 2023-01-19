package com.oxygenxml.git.view.branches;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.exceptions.NoChangesInSquashedCommitException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.BranchSwitchConfirmationDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.dialog.SquashMergeDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.stash.StashUtil;
import com.oxygenxml.git.view.util.ExceptionHandlerUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Action provider for the contextual menu of the branches tree.
 */
public class BranchTreeMenuActionsProvider {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BranchTreeMenuActionsProvider.class.getName());
  /**
   * Translator instance.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  /**
   * A list with all the possible actions for a specific node in the tree.
   */
  private List<AbstractAction> nodeActions;
  /**
   * Git operation controller.
   */
  private final GitControllerBase ctrl;


  /**
   * Constructor.
   * 
   * @param ctrl Git operation controller.
   */
  public BranchTreeMenuActionsProvider(GitControllerBase ctrl) {
    this.ctrl = ctrl;
  }

  /**
   * Creates the actions for a specific node in the tree and stores them.
   * 
   * @param node The node for which to create actions.
   */
  private void createBranchTreeActions(GitTreeNode node) {
    String nodeContent = (String) node.getUserObject();
    // Adds either the local branch actions, or the remote branch actions.
    boolean isLocalBranch = nodeContent.contains(Constants.R_HEADS);
    boolean isRemoteBranch = nodeContent.contains(Constants.R_REMOTES);
    if (isLocalBranch) {
      boolean isCurrentBranch = nodeContent.contains(GitAccess.getInstance().getBranchInfo().getBranchName());
      if (isCurrentBranch) {
        nodeActions.add(createNewBranchAction(nodeContent));
      } else {
        nodeActions.add(createCheckoutLocalBranchAction(nodeContent));
        nodeActions.add(createNewBranchAction(nodeContent));
        nodeActions.add(createMergeAction(nodeContent));
        nodeActions.add(createSquashMergeAction(nodeContent));
        nodeActions.add(null);
        nodeActions.add(createDeleteLocalBranchAction(nodeContent));
      }
    } else if (isRemoteBranch) {
      nodeActions.add(createCheckoutRemoteBranchAction(nodeContent));
    }
  }

  /**
   * Gets the checkout action for a specific node, depending if it is a local or a
   * remote branch.
   * 
   * @param node The node for which to get the action.
   * 
   * @return The action for the node.
   */
  public AbstractAction getCheckoutAction(GitTreeNode node) {
    AbstractAction action = null;
    if (node.isLeaf()) {
      String nodeContent = (String) node.getUserObject();
      if (nodeContent.contains(Constants.R_HEADS)) {
        action = createCheckoutLocalBranchAction(nodeContent);
      } else if (nodeContent.contains(Constants.R_REMOTES)) {
        action = createCheckoutRemoteBranchAction(nodeContent);
      }
    }
    return action;
  }

  /**
   * Gets the actions created for the specific node.
   * 
   * @param node The current node.
   * 
   * @return A list of actions.
   */
  public List<AbstractAction> getActionsForNode(GitTreeNode node) {
    nodeActions = new ArrayList<>();
    if (node.isLeaf()) {
      createBranchTreeActions(node);
    }
    return nodeActions;
  }


  /**
   * Creates the checkout action for local branches.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   * @return The action created.
   */
  private AbstractAction createCheckoutLocalBranchAction(String nodePath) {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.CHECKOUT)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.asyncTask(() -> {
          RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
          String branchToSet = BranchesUtil.createBranchPath(
              nodePath,
              BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
          if (RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
            int answer = showUncommittedChangesWhenChangingBranchMsg(branchToSet);
            if (answer == OKOtherAndCancelDialog.RESULT_OTHER) {
              ctrl.getGitAccess().setBranch(branchToSet);
              BranchesUtil.fixupFetchInConfig(ctrl.getGitAccess().getRepository().getConfig());
            } else if (answer == OKOtherAndCancelDialog.RESULT_OK) {
              boolean wasStashCreated = StashUtil.stashChanges();
              if(wasStashCreated) {
                ctrl.getGitAccess().setBranch(branchToSet);
              }
            }
          } else {
            ctrl.getGitAccess().setBranch(branchToSet);
            BranchesUtil.fixupFetchInConfig(ctrl.getGitAccess().getRepository().getConfig());
          }
          return null;

        }, ex -> {
          if (ex instanceof CheckoutConflictException) {
            LOGGER.debug(ex.getMessage(), ex);
            BranchesUtil.showBranchSwitchErrorMessage();
          } else {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        });
      }
    };
  }


  /**
   * Creates the checkout action for remote branches.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   * @return The action created.
   */
  private AbstractAction createCheckoutRemoteBranchAction(String nodePath) {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.CHECKOUT) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        String branchPath = BranchesUtil.createBranchPath(nodePath,
            BranchManagementConstants.REMOTE_BRANCH_NODE_TREE_LEVEL);
        CreateBranchDialog dialog = new CreateBranchDialog(TRANSLATOR.getTranslation(Tags.CHECKOUT_BRANCH), branchPath,
            true);
        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
          ctrl.asyncTask(() -> {
            ctrl.getGitAccess().checkoutRemoteBranchWithNewName(dialog.getBranchName(), branchPath, BranchesUtil.getRemoteForBranch(nodePath));
            BranchesUtil.fixupFetchInConfig(ctrl.getGitAccess().getRepository().getConfig());

            return null;
          }, ex -> {
            if (ex instanceof CheckoutConflictException) {
              treatCheckoutConflictForNewlyCreatedBranche((CheckoutConflictException) ex);
            } else {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        }
      }
    };
  }

  /**
   * Create the new branch action for a local branch.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   * @return The action created.
   */
  private AbstractAction createNewBranchAction(String nodePath) {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.CREATE_BRANCH) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        CreateBranchDialog dialog = new CreateBranchDialog(TRANSLATOR.getTranslation(Tags.CREATE_BRANCH), null, false);

        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
          ctrl.asyncTask(
              () -> doCreateBranch(nodePath, dialog.getBranchName(), dialog.shouldCheckoutNewBranch()),
              ex -> {
                if (ex instanceof CheckoutConflictException) {
                  treatCheckoutConflictForNewlyCreatedBranche((CheckoutConflictException) ex);
                } else if (ex instanceof GitAPIException || ex instanceof JGitInternalException) {
                  PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
                }
              }
              );
        }
      }

      /**
       * Do create the new branch. 
       *  
       * @param nodePath         A string that contains the full path to the node that represents the branch.
       * @param branchName       The name of the new branch.
       * @param isCheckoutBranch <code>true</code> to also checkout the branch.
       * 
       * @return <code>null</code>.
       * 
       * @throws GitAPIException 
       */
      private Object doCreateBranch(String nodePath, String branchName, boolean isCheckoutBranch) throws GitAPIException {
        ctrl.getGitAccess().createBranchFromLocalBranch(branchName, nodePath);

        if (!isCheckoutBranch) {
          return null;
        }

        RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
        if (RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
          int answer = showUncommittedChangesWhenChangingBranchMsg(branchName);
          if (answer == OKOtherAndCancelDialog.RESULT_OTHER) {
            ctrl.getGitAccess().setBranch(branchName);
          }
        } else {
          ctrl.getGitAccess().setBranch(branchName);
        }

        return null;
      }
    };
  }

  /**
   * Create the squash and merge action.
   * 
   * @param nodePath The node path of the selected branch.
   * 
   * @return The squash merge action.
   */
  private AbstractAction createSquashMergeAction(final String nodePath) {
    final String selectedBranch = BranchesUtil.createBranchPath(
        nodePath,
        BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
    final String currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();

    final String squashMergeActionName = MessageFormat.format(
        Translator.getInstance().getTranslation(Tags.SQUASH_MERGE_ACTION_NAME),
        TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."),
        TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")
    ) + "...";

    return new AbstractAction(squashMergeActionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.asyncTask(
            () -> {
              if (RepoUtil.isUnfinishedConflictState(ctrl.getGitAccess().getRepository().getRepositoryState())) {
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
              } else {
                final SquashMergeDialog squashMergeDialog = new SquashMergeDialog();
                try {
                  squashMergeDialog.performSquashMerge(currentBranch, selectedBranch, 
                      ctrl.getGitAccess().getRepository().resolve(selectedBranch));
                } catch(NoChangesInSquashedCommitException ex) {
                  MessagePresenterProvider.getBuilder(
                      TRANSLATOR.getTranslation(Tags.SQUASH_MERGE), DialogType.INFO)
                      .setMessage(ex.getMessage())
                      .setCancelButtonVisible(false)
                      .buildAndShow();       
                }
              }
              return null;
            },
            ex -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex));
      }
    };
  }
  
  /**
   * Create merge action for [selected_branch] into [current_branch].
   * 
   * @param nodePath The node path of the selected branch.
   * 
   * @return The merge action.
   */
  private AbstractAction createMergeAction(String nodePath) {
    String selectedBranch = BranchesUtil.createBranchPath(
        nodePath,
        BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
    String currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();

    String mergeActionName = MessageFormat.format(
        Translator.getInstance().getTranslation(Tags.MERGE_BRANCH1_INTO_BRANCH2),
        TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."),
        TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")
    ) + "...";

    return new AbstractAction(mergeActionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.asyncTask(
            () -> {
              if (RepoUtil.isUnfinishedConflictState(ctrl.getGitAccess().getRepository().getRepositoryState())) {
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
              } else {
                final String questionMessage = 
                    MessageFormat.format(
                        TRANSLATOR.getTranslation(Tags.MERGE_INFO),
                        TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."),
                        TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")) 
                    + "\n\n"
                    + MessageFormat.format(TRANSLATOR.getTranslation(Tags.MERGE_BRANCHES_QUESTION_MESSAGE),
                    TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."), 
                    TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."));
                   
                final int answer = MessagePresenterProvider.getBuilder(
                    TRANSLATOR.getTranslation(Tags.MERGE_BRANCHES), DialogType.QUESTION)
                    .setQuestionMessage(questionMessage)
                    .setOkButtonName(TRANSLATOR.getTranslation(Tags.MERGE))
                    .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CANCEL))
                    .buildAndShow().getResult();       
                   
                if (answer == OKCancelDialog.RESULT_OK) {
                  try {
                    ctrl.getGitAccess().mergeBranch(nodePath);
                  } catch (GitAPIException | IOException | NoRepositorySelected ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    ExceptionHandlerUtil.handleMergeException(ex);
                  }
                  
                }
              }
              return null;
            },
            ex -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex));
      }
    };
  }
  
  /**
   * Treat the checkout conflict exception thrown for a newly created branch (when
   * the checkout is to be automatically performed after branch creation).
   * 
   * @param ex The exception.
   */
  private void treatCheckoutConflictForNewlyCreatedBranche(CheckoutConflictException ex) {
    LOGGER.debug(ex.getMessage(), ex);
    BranchesUtil.showCannotCheckoutNewBranchMessage();
  }


  /**
   * Create the delete local branch action.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   * @return The action created.
   */
  private AbstractAction createDeleteLocalBranchAction(String nodePath) {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.DELETE) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        final int result = MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.DELETE_BRANCH), DialogType.QUESTION)
            .setQuestionMessage(MessageFormat.format(TRANSLATOR.getTranslation(Tags.CONFIRMATION_MESSAGE_DELETE_BRANCH),
                nodePath.substring(nodePath.contains("refs/heads/") ? "refs/heads/".length() : 0)))
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.YES))
            .setCancelButtonName(TRANSLATOR.getTranslation(Tags.NO))
            .buildAndShow().getResult();
        if (result == OKCancelDialog.RESULT_OK) {
          ctrl.asyncTask(() -> {
            String branch = BranchesUtil.createBranchPath(nodePath,
                BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
            ctrl.getGitAccess().deleteBranch(branch);
            return null;
          }, ex -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex));
        }
      }
    };
  }


  /**
   * Show a message when there are uncommitted changes and we try to switch repo.
   * 
   * @param newBranch The branch to set.
   * 
   * @return The option chosen by the user. OKCancelDialog#RESULT_OK or OKCancelDialog#RESULT_CANCEL.
   */
  private int showUncommittedChangesWhenChangingBranchMsg(String newBranch) {

    BranchSwitchConfirmationDialog dialog = new BranchSwitchConfirmationDialog(newBranch);

    dialog.setVisible(true);

    return dialog.getResult();
  }

}