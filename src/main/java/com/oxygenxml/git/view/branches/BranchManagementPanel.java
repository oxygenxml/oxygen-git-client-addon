package com.oxygenxml.git.view.branches;

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

import ro.sync.exml.workspace.api.standalone.ui.Tree;

@SuppressWarnings("serial")
// TODO: We should extract as much duplicated code as possible. Some code is also found in ChangesPanel. 
// Perhaps extract some utility methods.
public class BranchManagementPanel extends JPanel {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(BranchManagementPanel.class);
  
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
   */
  public BranchManagementPanel() {
    createGUI();
  }
  
  /**
   * Creates the tree for the branches in the current repository.
   */
  private void createBranchesTree() {
    branchesToBeAdded = getBranches();
    
    branchesTree = new Tree(new BranchManagementTreeModel(null, branchesToBeAdded));
    branchesTree.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    branchesTree.setDragEnabled(false);
    
    ToolTipManager.sharedInstance().registerComponent(branchesTree);
  }

  /**
   * Creates the components and adds listeners to some of them.
   */
  private void createGUI() {
    setLayout(new GridBagLayout());
    
    createSearchBar();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 5, 0, 5);
    add(searchBar, gbc);
    
    createBranchesTree();
    JScrollPane branchesTreeScrollPane = new JScrollPane(branchesTree); 
    gbc.insets = new Insets(3, 5, 3, 5);
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    add(branchesTreeScrollPane, gbc);
    
    addTreeExpandListener();
    
    setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
    
    // TODO: this is probably only temporary. Perhaps we should also make the view work fine when Oxygen starts.
    // We should probably populate it when the Git Staging panel refreshes and isAfterRefresh = true. See PanelRefresh.
    setVisible(false); 
    
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
          branchList.add(new BranchType(localBranchIterator.getName(), BranchType.BranchLocation.LOCAL));
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
   */
  private void createSearchBar() {
    searchBar = UIUtil.createTextField();
    searchBar.setText("Search");
    searchBar.setToolTipText("Type here the name of the branch you want to find");
  }
  
  /**
   * Shows the branch panel with all its components.
   */
  public void showBranches() {
    branchesToBeAdded = getBranches();
    updateTreeView(branchesToBeAdded);
    setVisible(true);
  }
  
}
