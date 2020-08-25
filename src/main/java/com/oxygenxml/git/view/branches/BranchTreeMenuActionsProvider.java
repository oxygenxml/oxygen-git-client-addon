package com.oxygenxml.git.view.branches;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.FileStatusDialog;

import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Action provider for the contextual menu of the branches tree. 
 */
public class BranchTreeMenuActionsProvider {
  /**
   * Git access instance.
   */
  private static final GitAccess gitAccess = GitAccess.getInstance();
  /**
   * Translator instance.
   */
  private static final Translator translator = Translator.getInstance();
  /**
   * Action used to checkout a branch.
   */
  private AbstractAction checkoutBranchAction;
  /**
   * Action for creating a new branch.
   */
  private AbstractAction newBranchAction;
  
  /**
   * Constructor.
   * 
   * @param branchesTree The tree used for creating actions.
   */
  public BranchTreeMenuActionsProvider(JTree branchesTree) {
    GitTreeNode node = (GitTreeNode) branchesTree.getSelectionPath().getLastPathComponent();
    if(node.isLeaf()) {
      createBranchTreeActions(node);
    }
  }
  
  /**
   * Creates the actions for the tree and stores them.
   * 
   * @param node The node for which to create actions.
   */
  private void createBranchTreeActions(GitTreeNode node) {
    checkoutBranchAction = createCheckoutBranchAction(node);
    newBranchAction = createNewBranchAction(node);
    //TODO add the other actions
  }
  
  /**
   * Gets the actions created and adds them to a list.
   * 
   * @return The list of actions.
   */
  public List<AbstractAction> getActionsForBranchTree() {
    List<AbstractAction> treeActions = new ArrayList<>();
    if (checkoutBranchAction != null) {
      treeActions.add(checkoutBranchAction);
      treeActions.add(newBranchAction);
    }
    return treeActions;
  }
  
  /**
   * Creates the checkout action for local and remote branches. 
   * 
   * @return The action created.
   */
  private AbstractAction createCheckoutBranchAction(GitTreeNode node) {
    return new AbstractAction(translator.getTranslation(Tags.CHECKOUT_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        String branchName = (String) node.getUserObject();
        TreeNode[] path = node.getPath();
        if (path[BranchManagementConstants.BRANCH_TYPE_NODE_TREE_LEVEL].toString()
            .equals(BranchManagementConstants.LOCAL)) {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              gitAccess.setBranch(branchName);
            } catch (CheckoutConflictException ex) {
              PluginWorkspaceProvider.getPluginWorkspace()
                  .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
            } catch (GitAPIException | JGitInternalException ex) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        } else if (path[BranchManagementConstants.BRANCH_TYPE_NODE_TREE_LEVEL].toString()
            .equals(BranchManagementConstants.REMOTE)) {
          try {
            gitAccess.checkoutRemoteBranch(branchName);
          } catch (GitAPIException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        }
      }
    };
  }
  
  /**
   * Create the action that creates a new branch starting from the given one.
   * 
   * @param node Node corresponding to a branch.
   * 
   * @return
   */
  private AbstractAction createNewBranchAction(GitTreeNode node) {
    return new AbstractAction("Create new branch - option in progress") {
      @Override
      public void actionPerformed(ActionEvent e) {
        String branchName = (String) node.getUserObject();
        TreeNode[] path = node.getPath();

        if (path[BranchManagementConstants.BRANCH_TYPE_NODE_TREE_LEVEL].toString()
            .equals(BranchManagementConstants.LOCAL)) {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              gitAccess.setBranch(branchName);
              if (FileStatusDialog.showQuestionMessage("", "Name the new branch: ", "Confirm",
                  "Cancel") == OKCancelDialog.RESULT_OK) {
                // TODO create and checkout the new branch.
              }
            } catch (CheckoutConflictException ex) {
              PluginWorkspaceProvider.getPluginWorkspace()
                  .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
            } catch (GitAPIException | JGitInternalException ex) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        } else if (path[BranchManagementConstants.BRANCH_TYPE_NODE_TREE_LEVEL].toString()
            .equals(BranchManagementConstants.REMOTE)) {
          try {
            gitAccess.checkoutRemoteBranch(branchName);
            if (FileStatusDialog.showQuestionMessage("", "Name the new branch: ", "Confirm",
                "Cancel") == OKCancelDialog.RESULT_OK) {
              // TODO create and checkout the new branch.
            }
          } catch (GitAPIException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        }
      }
    };
  }
}
