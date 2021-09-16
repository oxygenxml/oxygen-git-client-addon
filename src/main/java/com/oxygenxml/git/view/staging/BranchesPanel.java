package com.oxygenxml.git.view.staging;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.BranchSwitchConfirmationDialog;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.stash.StashUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Panel in Git Staging view from where to change the current branch.
 */
public class BranchesPanel extends JPanel {

  /**
   * Branch names combo.
   */
  private JComboBox<String> branchNamesCombo;
  
  /**
   * <code>true</code> if the combo popup is showing.
   */
  private boolean isComboPopupShowing;
  
  /**
   * <code>true</code> to inhibit branch selection listener.
   */
  private boolean inhibitBranchSelectionListener;
  
  /**
   * Creates the panel.
   * 
   * @param gitController Git Controller.
   */
  public BranchesPanel(GitController gitController) {
    createGUI();
    
    branchNamesCombo.addItemListener(event -> {
      if (!inhibitBranchSelectionListener && event.getStateChange() == ItemEvent.SELECTED) {
        treatBranchSelectedEvent(event);
      }
    });
    
    branchNamesCombo.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        isComboPopupShowing = true;
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        isComboPopupShowing = false;
      }
      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        isComboPopupShowing = false;
      }
    });
    
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.OPEN_WORKING_COPY
            || operation == GitOperation.ABORT_REBASE 
            || operation == GitOperation.CONTINUE_REBASE) {
          refresh();
        }
      }
    });
    
  }

  /**
   * Treat a branch name selection event.
   * 
   * @param event The event to treat.
   */
  private void treatBranchSelectedEvent(ItemEvent event) {
    String branchName = (String) event.getItem();
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    if (branchName.equals(currentBranchName)) {
      return;
    }
    
    RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
    if(RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
      BranchSwitchConfirmationDialog dialog = new BranchSwitchConfirmationDialog(branchName);

      dialog.setVisible(true);

      int answer = dialog.getResult();

      if(answer == OKOtherAndCancelDialog.RESULT_OTHER) {
        tryCheckingOutBranch(branchName);
      } else if(answer == OKOtherAndCancelDialog.RESULT_OK) {
        boolean wasStashCreated = StashUtil.stashChanges();
        if(wasStashCreated) {
          tryCheckingOutBranch(branchName);
        }
      } else {
        restoreCurrentBranchSelectionInMenu();
      }
    } else {
      tryCheckingOutBranch(branchName);
    }
  }

  /**
   * Create the graphical user interface.
   */
  private void createGUI() {
    setLayout(new GridBagLayout());
    
    // Branch label
    JLabel currentBranchLabel = new JLabel(Translator.getInstance().getTranslation(Tags.BRANCH));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
    add(currentBranchLabel, gbc);
    
    // Branches combo
    branchNamesCombo = new JComboBox<>();
    gbc.gridx++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    add(branchNamesCombo, gbc);
  }
  
  /**
   * Refresh.
   */
  public void refresh() {
    SwingUtilities.invokeLater(this::updateBranchesMenu);
  }
  
  /**
   * Adds the branches given as a parameter to the branchSplitMenuButton.
   * 
   * @param branches A list with the branches to be added.
   */
  private void addBranchesToCombo(List<String> branches) {
    inhibitBranchSelectionListener = true;
    branches.forEach(branchNamesCombo::addItem);
    inhibitBranchSelectionListener = false;
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    branchNamesCombo.setSelectedItem(currentBranchName);
  }
  
  /**
   * Updates the local branches in the split menu button where you can checkout them.
   */
  private void updateBranchesMenu() {
    boolean isVisible = isComboPopupShowing;
    branchNamesCombo.hidePopup();

    branchNamesCombo.removeAllItems();
    addBranchesToCombo(getBranches());

    branchNamesCombo.revalidate();
    if (isVisible) {
      branchNamesCombo.showPopup();
    }
  }
  
  /**
   * Gets all the local branches from the current repository.
   * 
   * @return The list of local branches.
   */
  private List<String> getBranches() {
    List<String> localBranches = new ArrayList<>();
    try {
      localBranches = BranchesUtil.getLocalBranches();
    } catch (NoRepositorySelected e1) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e1.getMessage(), e1);
    }
    return localBranches;
  }
  
  /**
   * The action performed for this Abstract Action
   * 
   * @param branchName Branch name.
   */
  private void tryCheckingOutBranch(String branchName) {
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        GitAccess.getInstance().setBranch(branchName);
        BranchesUtil.fixupFetchInConfig(GitAccess.getInstance().getRepository().getConfig());
      } catch (CheckoutConflictException ex) {
        restoreCurrentBranchSelectionInMenu();
        BranchesUtil.showBranchSwitchErrorMessage();
      } catch (GitAPIException | JGitInternalException | IOException | NoRepositorySelected ex) {
        restoreCurrentBranchSelectionInMenu();
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
  }
  
  /**
   * Restore current branch selection in branches menu.
   */
  private void restoreCurrentBranchSelectionInMenu() {
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    int itemCount = branchNamesCombo.getItemCount();
    for (int i = 0; i < itemCount; i++) {
      String branch = branchNamesCombo.getItemAt(i);
      if (branch.equals(currentBranchName)) {
        branchNamesCombo.setSelectedItem(branch);
        break;
      }
    }
  }
  
  /**
   * @return the branches combo.
   */
  public JComboBox<String> getBranchNamesCombo() {
    return branchNamesCombo;
  }
}
