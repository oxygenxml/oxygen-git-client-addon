package com.oxygenxml.git.view.branches;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTree;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.GitTreeNode;

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
   * A list with all the possible actions for a specific node in the tree.
   */
  private List<AbstractAction> nodeActions;
  
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
   * Creates the actions for a specific node in the tree and stores them.
   * 
   * @param node The node for which to create actions.
   */
  private void createBranchTreeActions(GitTreeNode node) {
    nodeActions = new ArrayList<>();
    String nodeContent = (String) node.getUserObject();
    addCheckoutBranchAction(nodeContent);
    //if the current node is a local branch, add new branch action.
    if(nodeContent.contains(Constants.R_HEADS)) {
      addNewBranchAction(nodeContent);
    }
    createDeleteBranchAction(nodeContent);
  }
  
  /**
   * Gets the actions created for the specific node.
   * 
   * @return A list of actions.
   */
  public List<AbstractAction> getActionsForBranchNode() {
    return nodeActions;
  }
  
  /**
   * Creates the checkout action for local and remote branches and adds it to a
   * list of actions.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   */
  private void addCheckoutBranchAction(String nodePath) {
    nodeActions.add(new AbstractAction(translator.getTranslation(Tags.CHECKOUT_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        String[] split = nodePath.split("/");
        String branchName = split[split.length - 1];
        if (nodePath.contains(Constants.R_HEADS)) {
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
        } else if (nodePath.contains(Constants.R_REMOTES)) {
          try {
            gitAccess.checkoutRemoteBranch(branchName);
          } catch (GitAPIException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        }
      }
    });
  }
  
  /**
   * Create the new branch action for a local branch and adds it to a list of actions. 
   * 
   @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   */
  private void addNewBranchAction(String nodePath) {
    String[] split = nodePath.split("/");
    String branchName = split[split.length - 1];
    nodeActions.add(new AbstractAction("Create new branch") {
      @Override
      public void actionPerformed(ActionEvent e) {
        GitOperationScheduler.getInstance().schedule(() -> {
          try {
            gitAccess.setBranch(branchName);
            String result = JOptionPane.showInputDialog(
                (Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
                Translator.getInstance().getTranslation(Tags.BRANCH_NAME),
                Translator.getInstance().getTranslation(Tags.CREATE_BRANCH), JOptionPane.PLAIN_MESSAGE);
            if (result != null && !result.isEmpty()) {
              GitAccess.getInstance().checkoutCommitAndCreateBranch(result,
                  gitAccess.getLatestCommitOnCurrentBranch().getName());
            }
          } catch (CheckoutConflictException ex) {
            PluginWorkspaceProvider.getPluginWorkspace()
                .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
          } catch (GitAPIException | JGitInternalException | IOException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        });
      }
    });
  }
  
  /**
   * Create the delete branch action and adds it to a list of actions.
   * 
   * @param nodePath Node A string that contains the full path to the node that
   *                 represents the branch.
   */
  private void createDeleteBranchAction(String nodePath) {
    String[] split = nodePath.split("/");
    String branchName = split[split.length - 1];
    nodeActions.add(new AbstractAction("Delete branch - option in progress") {
      @Override
      public void actionPerformed(ActionEvent e) {
        
        if (nodePath.contains(Constants.R_HEADS)) {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              gitAccess.setBranch(branchName);
              //TODO the delete action for local
            } catch (CheckoutConflictException ex) {
              PluginWorkspaceProvider.getPluginWorkspace()
                  .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
            } catch (GitAPIException | JGitInternalException ex) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        } else if (nodePath.contains(Constants.R_REMOTES)) {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              gitAccess.setBranch(branchName);
              //TODO the delete action for remote
            } catch (CheckoutConflictException ex) {
              PluginWorkspaceProvider.getPluginWorkspace()
                  .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
            } catch (GitAPIException | JGitInternalException ex) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        }
      }
    });
  }
}
