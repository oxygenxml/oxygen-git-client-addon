package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Ref;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.historycomponents.HistoryPanel;

import ro.sync.exml.workspace.api.standalone.ui.Tree;

public class BranchPanel extends JPanel{
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class);
  /**
   * Git API access.
   */
  private static final GitAccess gitAccess = GitAccess.getInstance();
  /**
   * A filter for the branches.
   */
  private JTextField searchBar;
  /**
   * The tree in which the branches will be presented.
   */
  private Tree branchesTree;
  public BranchPanel(GitController gitCtrl) {
    List<Ref> localBranchList = gitAccess.getLocalBranchList();
    List<Ref> remoteBrachListForCurrentRepo = gitAccess.getRemoteBrachListForCurrentRepo(); 
    createGUI(gitCtrl);
    
  }
  
  private void createGUI(GitController gitCtrl) {
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    searchBar = createSearchBar(gbc);
    add(searchBar,gbc);
    branchesTree = new Tree();
    
    setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
    setVisible(false);
  }

  private JTextField createSearchBar(GridBagConstraints gbc) {
    gbc.insets = new Insets(0, 5, 0, 5);
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 1;
    JTextField searchBar = new JTextField("Search");
    searchBar.setEditable(true);
    searchBar.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
    searchBar.setToolTipText("Type here what you want to find");
    searchBar.setVisible(true);    
    return searchBar;
  }

  public void showBranches() {
    setVisible(true);
  }
  
  public void hideBranches() {
    setVisible(false);
  }

}
