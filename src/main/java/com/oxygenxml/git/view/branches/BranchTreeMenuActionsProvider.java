package com.oxygenxml.git.view.branches;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.Constants;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.FileStatusDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

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
   * A branch tree refresher.
   */
  private BranchTreeRefresher branchTreeRefresher;
  
  /**
   * Constructor.
   * 
   * @param branchesTree The tree used for creating actions.
   */
  public BranchTreeMenuActionsProvider(BranchTreeRefresher branchTreeRefresher) {
    this.branchTreeRefresher = branchTreeRefresher;
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
      addCheckoutLocalBranchAction(nodeContent);
      addCreateBranchAction(nodeContent);
      addDeleteLocalBranchAction(nodeContent);
    } else if (nodeContent.contains(Constants.R_REMOTES)) {
      addCheckoutRemoteBranchAction(nodeContent);
    }
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
   * Creates the path to a branch without having its type node, starting from the
   * full path of the node that contains the branch.
   * 
   * @param nodePath                The path of the node that contains the branch.
   * 
   * @param startingIndexBranchType The position from which to start to add to the
   *                                branch path, depending on type of the branch.
   *                                This parameter can only be
   *                                {@link com.oxygenxml.git.view.branches.BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL} or
   *                                {@link com.oxygenxml.git.view.branches.BranchManagementConstants.REMOTE_BRANCH_NODE_TREE_LEVEL}
   * 
   * @return The branch path in string format.
   */
  private String createBranchPath(String nodePath, int startingIndexBranchType) {
    StringBuilder branchPath = new StringBuilder();
    String[] split = nodePath.split("/");
    for (int i = startingIndexBranchType; i < split.length; i++) {
      branchPath.append(split[i]);
      if (i < split.length - 1) {
        branchPath.append("/");
      }
    }
    return branchPath.toString();
  }
  
  /**
   * Creates the checkout action for local branches and adds it to a
   * list of actions.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   */
  private void addCheckoutLocalBranchAction(String nodePath) {
    nodeActions.add(new AbstractAction(translator.getTranslation(Tags.CHECKOUT_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        GitOperationScheduler.getInstance().schedule(() -> {
          try {
            gitAccess.setBranch(createBranchPath(nodePath, BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL));
          } catch (CheckoutConflictException ex) {
            PluginWorkspaceProvider.getPluginWorkspace()
                .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
          } catch (GitAPIException | JGitInternalException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        });
      }
    });
  }
  

  /**
   * Creates the checkout action for remote branches and adds it to a
   * list of actions.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   * 
   */
  private void addCheckoutRemoteBranchAction(String nodePath) {
    nodeActions.add(new AbstractAction(translator.getTranslation(Tags.CHECKOUT_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean branchAlreadyExists = false;
        String newBranchPath = createBranchPath(nodePath, BranchManagementConstants.REMOTE_BRANCH_NODE_TREE_LEVEL);
        String newBranchName = newBranchPath;
        // TODO: replace JOptionPane with a dialog of ours
        do {
          try {
            newBranchName = (String) JOptionPane.showInputDialog(
                (Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
                Translator.getInstance().getTranslation(Tags.BRANCH_NAME),
                Translator.getInstance().getTranslation(Tags.CHECKOUT_BRANCH), JOptionPane.PLAIN_MESSAGE, null, null,
                newBranchPath);
            branchAlreadyExists = false;
            if (newBranchName != null && !newBranchName.isEmpty()) {
              gitAccess.checkoutRemoteBranchWithNewName(newBranchPath, newBranchName);
            }
          } catch (CheckoutConflictException ex) {
            PluginWorkspaceProvider.getPluginWorkspace()
                .showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
          } catch (RefAlreadyExistsException ex) {
            //PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            branchAlreadyExists = true;
          } catch (GitAPIException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        } while (branchAlreadyExists);
      }
    });
  }
  
  /**
   * Create the new branch action for a local branch and adds it to a list of
   * actions.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   */
  private void addCreateBranchAction(String nodePath) {
    nodeActions.add(new AbstractAction(translator.getTranslation(Tags.CREATE_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        GitOperationScheduler.getInstance().schedule(() -> {
          try {
            gitAccess.setBranch(createBranchPath(nodePath, BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL));
            String newBranchName = JOptionPane.showInputDialog(
                (Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
                Translator.getInstance().getTranslation(Tags.BRANCH_NAME),
                Translator.getInstance().getTranslation(Tags.CREATE_BRANCH), JOptionPane.PLAIN_MESSAGE);
            if (newBranchName != null && !newBranchName.isEmpty()) {
              GitAccess.getInstance().checkoutCommitAndCreateBranch(
                  newBranchName,
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
   * Create the delete local branch action and adds it to a list of actions.
   * 
   * @param nodePath A string that contains the full path to the node that
   *                 represents the branch.
   */
  private void addDeleteLocalBranchAction(String nodePath) {
    nodeActions.add(new AbstractAction(translator.getTranslation(Tags.DELETE_BRANCH)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (FileStatusDialog.showQuestionMessage(translator.getTranslation(Tags.DELETE_BRANCH),
            translator.getTranslation(Tags.CONFIRMATION_DIALOG_DELETE_BRANCH), translator.getTranslation(Tags.YES),
            translator.getTranslation(Tags.NO)) == OKCancelDialog.RESULT_OK) {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              String branch = createBranchPath(nodePath, BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
              gitAccess.deleteBranch(branch);
              branchTreeRefresher.refreshBranchesTree();
            } catch (JGitInternalException ex) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        }
      }
    });
  }
  
}