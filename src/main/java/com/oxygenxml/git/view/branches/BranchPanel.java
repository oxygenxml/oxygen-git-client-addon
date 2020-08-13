package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.historycomponents.HistoryPanel;

import ro.sync.exml.workspace.api.standalone.ui.Tree;

@SuppressWarnings("serial")
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
   * A field for searching branches in the current repository.
   */
  private JTextField searchBar;
  
  /**
   * The tree in which the branches will be presented.
   */
  private JTree branchesTree;
  
  /**
   * The list with the branches from the current repository.
   */
  private List<BranchType> branchesToBeAdded;
  
  /**
   * Public constructor
   * 
   */
  public BranchPanel() {
    createBranchTree();
    
    ToolTipManager.sharedInstance().registerComponent(branchesTree);
    createGUI();
  }
  /**
   * Creates the tree for the branches in the current repository.
   */
  private void createBranchTree() {
    branchesToBeAdded = getBranches();
    
    branchesTree = new Tree(new BranchManagementTreeModel(null, branchesToBeAdded));
    branchesTree.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    branchesTree.setDragEnabled(false);
  }

  /**
   * Creates the components and adds listeners to some of them.
   */
  private void createGUI() {
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    createSearchBar(gbc);
    add(searchBar, gbc);
    add(createScrollPane4Tree(gbc), gbc);
    addTreeExpandListener();
    
    setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
    setVisible(false);
  }
  
  /**
   * Creates the scroll pane for the tree.
   * @param gbc GridBagConstraints.
   * @return The scroll pane.
   */
  private Component createScrollPane4Tree(GridBagConstraints gbc) {
    JScrollPane treeView = new JScrollPane(branchesTree); 
    gbc.insets = new Insets(3, 5, 3, 5);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    treeView.setVisible(true);
    return treeView;
  }

  /**
   * @return The tree that renders resources.
   */
  JTree getTreeView() {
    return branchesTree;
  }
  
  /**
   * Creates a list with all branches, local and remote, for the current repository.
   * @return The branches in the repository.
   */
  public List<BranchType> getBranches() {
      Repository repository = getCurrentRepository();
      List<BranchType> branchList = new ArrayList<>();
      if(repository != null) {
        List<Ref> localBranches = gitAccess.getLocalBranchList();
        for(Ref localBranchIterator : localBranches) {
          branchList.add(new BranchType(localBranchIterator.getName(),BranchType.BranchLocation.LOCAL));
        }
        List<Ref> remoteBranches = gitAccess.getRemoteBrachListForCurrentRepo();
        for(Ref remoteBranchIterator : remoteBranches) {
          branchList.add(new BranchType(remoteBranchIterator.getName(), BranchType.BranchLocation.REMOTE));
        }
      }
      return branchList;
  }
  
  /**
   * Updates a tree structure with the given branches. 
   * 
   * @param branchList The branches used to generate the nodes.
   */
  private void updateTreeView(List<BranchType> branchList) {
    if (branchesTree != null) {
      Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
      TreePath[] selectionPaths = branchesTree.getSelectionPaths();

      // Create the tree with the new model
      branchesTree.setModel(
          new BranchManagementTreeModel(
              GitAccess.getInstance().getWorkingCopyName(), 
              branchList));

      // restore last expanded paths after refresh
      TreeFormatter.restoreLastExpandedPaths(expandedPaths, branchesTree);
      branchesTree.setSelectionPaths(selectionPaths);
    }
  }
  
  /**
   * @return the current repository or <code>null</code> if there's no repository selected.
   */
  private Repository getCurrentRepository() {
    Repository repo = null;
    try {
      repo = GitAccess.getInstance().getRepository();
    } catch (NoRepositorySelected e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e, e);
      }
    }
    return repo;
  }
  
  /**
   * Returns all the current expanded paths
   * 
   * @return - the current expanded paths
   */
  private Enumeration<TreePath> getLastExpandedPaths() {
    BranchManagementTreeModel treeModel = (BranchManagementTreeModel) branchesTree.getModel();
    GitTreeNode rootNode = (GitTreeNode) treeModel.getRoot();
    Enumeration<TreePath> expandedPaths = null;
    if (rootNode != null) {
      TreePath rootTreePath = new TreePath(rootNode);
      expandedPaths = branchesTree.getExpandedDescendants(rootTreePath);
    }
    return expandedPaths;
  }
  
  /**
   * Adds an expand listener to the tree: When the user expands a node, the node
   * will expand as long as it has only one child.
   */
  private void addTreeExpandListener() {
    branchesTree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        BranchManagementTreeModel model = (BranchManagementTreeModel) branchesTree.getModel();
        GitTreeNode node = (GitTreeNode) path.getLastPathComponent();
        if (!model.isLeaf(node)) {
          int children = node.getChildCount();
          if (children == 1) {
            GitTreeNode child = (GitTreeNode) node.getChildAt(0);
            TreePath childPath = new TreePath(child.getPath());
            branchesTree.expandPath(childPath);
          }
        }
      }
      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        // Nothing
      }
    });
  }

  /**
   * Creates the search bar for the branches in the current repository. 
   * @param gbc GridBagConstraints.
   */
  private void createSearchBar(GridBagConstraints gbc) {
    gbc.insets = new Insets(0, 5, 0, 5);
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 0;
    searchBar = UIUtil.createTextField();
    searchBar.setText("Search");
    searchBar.setEditable(true);
    searchBar.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
    searchBar.setToolTipText("Type here the branch you want to find");
    searchBar.setVisible(true);    
  }
  
  /**
   * Shows the branch panel with all its components.
   */
  public void showBranches() {
    branchesToBeAdded = getBranches();
    updateTreeView(branchesToBeAdded);
    setVisible(true);
  }
  
  /**
   * Hides the panel.
   */
  public void hideBranches() {
    setVisible(false);
  }

}
