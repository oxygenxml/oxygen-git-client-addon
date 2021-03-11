package com.oxygenxml.git.view.branches;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TreeUtil;
import com.oxygenxml.git.view.FilterTextField;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.exml.workspace.api.standalone.ui.Tree;

/**
 * Branch management panel. Contains the branches tree (local + remote branches).
 */
public class BranchManagementPanel extends JPanel {

  /**
   * Inset.
   */
  private static final int INSET_3_PX = 3;

  /**
   * Logger for this class.
   */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(BranchManagementPanel.class);

  /**
   * A field for searching branches in the current repository.
   */
  private FilterTextField searchField;
  
  /**
   * The refresh button for the tree.
   */
  private ToolbarButton refreshButton;

  /**
   * Translator for translation.
   */
  private static Translator translator = Translator.getInstance();

  /**
   * The tree in which the branches will be presented.
   */
  private JTree branchesTree;

  /**
   * The list with the branches from the current repository.
   */
  private List<String> allBranches;
  
  /**
   * The name of the current branch.
   */
  private String currentBranchName;
  
  /**
   * <code>true</code> if the context menu is showing.
   */
  private boolean isContextMenuShowing;

  /**
   * Provides the actions for a node in the branches tree.
   */
  private BranchTreeMenuActionsProvider branchesTreeActionProvider;

  /**
   * Constructor.
   * 
   * @param ctrl Git controller.
   */
  public BranchManagementPanel(GitControllerBase ctrl) {
    createGUI();
    ctrl.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.OPEN_WORKING_COPY
            || operation == GitOperation.CREATE_BRANCH
            || operation == GitOperation.CHECKOUT
            || operation == GitOperation.DELETE_BRANCH) {
          BranchManagementPanel.this.refreshBranches();
        }
      }
    });
    addTreeListeners();
    
    branchesTreeActionProvider = new BranchTreeMenuActionsProvider(ctrl);
  }
  
  /**
   * @return <code>true</code> if the context menu is showing.
   */
  public boolean isContextMenuShowing() {
    return isContextMenuShowing;
  }
  
  /**
   * Adds the tree listeners.
   */
  private void addTreeListeners() {
    // Expand several levels at once when only one child on each level
    branchesTree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        TreeUtil.expandSingleChildPath(branchesTree, event);
      }
      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        // Nothing
      }
    });
    
    // Show context menu when pressing the Meny key
    branchesTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
          TreePath selectionPath = branchesTree.getSelectionPath();
          if (selectionPath != null) {
            Rectangle pathBounds = branchesTree.getPathBounds(selectionPath);
            if (pathBounds != null) {
              Point nodePoint = new Point(pathBounds.x, pathBounds.y + pathBounds.height);
              showContextualMenu(selectionPath, nodePoint);
            }
          }
        }
      }
    });
    
    branchesTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // Show context menu
        branchesTree.requestFocus();
        if(e.isPopupTrigger()) {
          Point nodePoint = e.getPoint();
          TreePath pathForLocation = branchesTree.getPathForLocation(nodePoint.x, nodePoint.y);
          showContextualMenu(pathForLocation, nodePoint);
        }
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        mousePressed(e);
      }
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!e.isConsumed()
            && !e.isPopupTrigger()
            && e.getClickCount() == 2) {
          // Checkout branch on double click
          e.consume();
          Point nodePoint = e.getPoint();
          TreePath pathForLocation = branchesTree.getPathForLocation(nodePoint.x, nodePoint.y);
          if (pathForLocation != null) {
            AbstractAction checkoutAction = branchesTreeActionProvider
                .getCheckoutAction((GitTreeNode) pathForLocation.getLastPathComponent());
            if (checkoutAction != null) {
              checkoutAction.actionPerformed(null);
            }
          }
        }
      }
    });
  }    
  
  /**
   * Gets a list of actions for the selected node from the BranchTreeActionProvider.
   * 
   * @param treePath Tree path.
   * 
   * @return The list of actions.
   */
  private List<AbstractAction> getActionsList4SelectedNode(TreePath treePath) {
    return treePath == null ? Collections.emptyList()
        : branchesTreeActionProvider.getActionsForNode((GitTreeNode) treePath.getLastPathComponent());
  }
  
  /**
   * Creates the contextual menu and populates it with action for the selected node.
   * 
   * @param selectionPath The path of the selection.
   * @param point         Point where to show the menu.
   */
  private void showContextualMenu(TreePath  selectionPath, Point point) {
    branchesTree.setSelectionPath(selectionPath);
    
    List<AbstractAction> actionsForSelectedNode = getActionsList4SelectedNode(selectionPath);
    if (!actionsForSelectedNode.isEmpty()) {
      JPopupMenu popupMenu = new JPopupMenu();
      popupMenu.addPopupMenuListener(new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
          isContextMenuShowing = true;
        }
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
          isContextMenuShowing = false;
          popupMenu.removePopupMenuListener(this);
        }
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
          isContextMenuShowing = false;
        }
      });
      actionsForSelectedNode.forEach(popupMenu::add);
      popupMenu.show(branchesTree, point.x, point.y);
    }
  }

  /**
   * Creates the tree for the branches in the current repository.
   */
  private void createBranchesTree() {
    branchesTree = new Tree(new BranchManagementTreeModel(null, allBranches));
    branchesTree.setCellRenderer(new BranchesTreeCellRenderer(() -> isContextMenuShowing, () -> currentBranchName));
    branchesTree.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    branchesTree.setDragEnabled(false);
    branchesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    branchesTree.setLargeModel(true);
    branchesTree.setVisible(false);
    
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
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, INSET_3_PX, 0, 0);
    add(searchField, gbc);
    
    createRefreshButton();
    gbc.gridx ++;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(0, INSET_3_PX, 0, INSET_3_PX);
    add(refreshButton, gbc);
    
    createBranchesTree();
    JScrollPane branchesTreeScrollPane = new JScrollPane(branchesTree);
    gbc.insets = new Insets(INSET_3_PX, INSET_3_PX, INSET_3_PX, INSET_3_PX);
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;
    add(branchesTreeScrollPane, gbc);

    setMinimumSize(new Dimension(UIConstants.MIN_PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
  }

  /**
   * Create refresh button.
   */
  private void createRefreshButton() {
    Action refreshAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(BranchManagementPanel.this::showBranches);
      }
    };
    refreshAction.putValue(Action.SMALL_ICON, Icons.getIcon(Icons.REFRESH_ICON));
    refreshAction.putValue(Action.SHORT_DESCRIPTION, Translator.getInstance().getTranslation(Tags.REFRESH));
    refreshButton = new ToolbarButton(refreshAction, false);
  }

  /**
   * Updates a tree structure with the given branches.
   * 
   * @param branchList The branches used to generate the nodes.
   */
  private void updateTreeView(List<String> branchList) {
    if (branchesTree != null) {
      Enumeration<TreePath> expandedPaths = TreeUtil.getLastExpandedPaths(branchesTree);
      TreePath selectionPath = branchesTree.getSelectionPath();

      // Create the tree with the new model
      BranchManagementTreeModel newModel = new BranchManagementTreeModel(GitAccess.getInstance().getWorkingCopyName(), branchList);
      branchesTree.setModel(newModel);

      // restore last expanded paths after refresh
      TreeUtil.restoreLastExpandedPaths(expandedPaths, branchesTree);
      
      // EXM-46684 Restore previous selection only if it still present in the model.
      if (selectionPath != null 
          && branchList.contains(((GitTreeNode) selectionPath.getLastPathComponent()).getUserObject())) {
        branchesTree.setSelectionPath(selectionPath);
      }
    }
  }

  /**
   * Creates the search bar for the branches in the current repository.
   */
  private void createSearchBar() {
    @SuppressWarnings("java:S110")
    FilterTextField filterTemp = new FilterTextField(
        Translator.getInstance().getTranslation(Tags.FILTER_HINT)) { 
      @Override
      public void filterChanged(String text) {
        filterTree(text);
      }
    };
    filterTemp.setToolTipText(translator.getTranslation(Tags.SEARCH_BAR_TOOL_TIP));
    searchField = filterTemp;
  }

  /**
   * Filters the tree by keeping only the nodes that match to the given text.
   * 
   * @param filterText The string to find.
   */
  protected void filterTree(String filterText) {
    List<String> remainingBranches = new ArrayList<>();
    if (filterText.equals(translator.getTranslation(Tags.FILTER_HINT)) || filterText.isEmpty()) {
      remainingBranches.addAll(allBranches);
    } else {
      for (String branch : allBranches) {
        String[] path = branch.split("/");
        // Sees if the leaf node/branch contains the given text
        if (path[path.length - 1].contains(filterText)) {
          remainingBranches.add(branch);
        }
      }
    }
    SwingUtilities.invokeLater(() -> {
      updateTreeView(remainingBranches);
      TreeUtil.expandAllNodes(branchesTree, 0, branchesTree.getRowCount());
    });
  }

  /**
   * Shows the branch panel with all its components.
   */
  public void showBranches() {
    refreshBranches();
    branchesTree.setVisible(true);
  }
  
  /**
   * @return All branches.
   */
  private static List<String> getAllBranches(){
    try {
      return BranchesUtil.getAllBranches();
    } catch (NoRepositorySelected e) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
    }
    return Collections.emptyList();
  }

  /**
   * Refresh branches.
   */
  public void refreshBranches() {
    currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    allBranches = getAllBranches();
    filterTree(searchField.getText());
  }
  
  /**
   * Returns the tree that contains all the branches for the current repository.
   * 
   * @return the branches tree.
   */
  public JTree getTree() {
    return branchesTree;
  }

}
