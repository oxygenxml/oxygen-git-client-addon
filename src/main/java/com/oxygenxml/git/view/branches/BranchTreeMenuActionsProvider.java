package com.oxygenxml.git.view.branches;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;

import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.FileStatusDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Action provider for the contextual menu of the branches tree. 
 */
public class BranchTreeMenuActionsProvider {
  /**
   * Translator instance.
   */
  private static Translator translator = Translator.getInstance();
  
  /**
   * A list with all the possible actions for a specific node in the tree.
   */
  private List<AbstractAction> nodeActions;
  /**
   * Git operation controller.
   */
  private GitControllerBase ctrl;
  
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
    if (nodeContent.contains(Constants.R_HEADS)) {
      nodeActions.add(createCheckoutLocalBranchAction(nodeContent));
      nodeActions.add(createNewBranchAction(nodeContent));
      nodeActions.add(createDeleteLocalBranchAction(nodeContent));
    } else if (nodeContent.contains(Constants.R_REMOTES)) {
      nodeActions.add(createCheckoutRemoteBranchAction(nodeContent));
    }
  }
  
  /**
   * Gets the checkout action for a specific node, depending if it is a local or
   * a remote branch.
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
    if(node.isLeaf()) {
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
    return new AbstractAction(translator.getTranslation(Tags.CHECKOUT)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.asyncTask(() -> {
              ctrl.getGitAccess().setBranch(
                  BranchesUtil.createBranchPath(nodePath, BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL));
              return null;
            },
            ex -> {
              if (ex instanceof CheckoutConflictException) {
                PluginWorkspaceProvider.getPluginWorkspace()
                    .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
              } else if (ex instanceof GitAPIException || ex instanceof JGitInternalException) {
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
    return new AbstractAction(translator.getTranslation(Tags.CHECKOUT) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        String branchPath = BranchesUtil.createBranchPath(nodePath,
            BranchManagementConstants.REMOTE_BRANCH_NODE_TREE_LEVEL);
        try {
          CreateBranchDialog dialog = new CreateBranchDialog(
              translator.getTranslation(Tags.CHECKOUT_BRANCH),
              branchPath,
              BranchesUtil.getLocalBranches(),
              true);
          if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
            ctrl.asyncTask(() -> {
              ctrl.getGitAccess().checkoutRemoteBranchWithNewName(dialog.getBranchName(), branchPath);
              return null;
            }, ex -> {
              if (ex instanceof CheckoutConflictException) {
                PluginWorkspaceProvider.getPluginWorkspace()
                .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
              } else if (ex instanceof GitAPIException) {
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
              }
            });
          }
        } catch (NoRepositorySelected ex) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
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
    return new AbstractAction(translator.getTranslation(Tags.CREATE_BRANCH) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          CreateBranchDialog dialog = new CreateBranchDialog(
              translator.getTranslation(Tags.CREATE_BRANCH),
              null,
              BranchesUtil.getLocalBranches(),
              false);
          if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
            ctrl.asyncTask(() ->
                {
                  ctrl.getGitAccess().createBranchFromLocalBranch(
                      dialog.getBranchName(),
                      nodePath,
                      dialog.shouldCheckoutNewBranch());
                  return null;
                },
                null);
          }
        } catch (NoRepositorySelected ex) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
        }
      }
    };
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
    return new AbstractAction(translator.getTranslation(Tags.DELETE) + "...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (FileStatusDialog.showQuestionMessage(
            translator.getTranslation(Tags.DELETE_BRANCH),
            MessageFormat.format(
                translator.getTranslation(Tags.CONFIRMATION_MESSAGE_DELETE_BRANCH),
                nodePath.substring(nodePath.indexOf("refs/heads/") != -1 ? "refs/heads/".length() : 0)),
            translator.getTranslation(Tags.YES),
            translator.getTranslation(Tags.NO)) == OKCancelDialog.RESULT_OK) {
          ctrl.asyncTask(() -> {
                String branch = BranchesUtil.createBranchPath(nodePath, BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
                ctrl.getGitAccess().deleteBranch(branch);
                return null;
              },
              ex -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex)
          );
        }
      }
    };
  }
  
}