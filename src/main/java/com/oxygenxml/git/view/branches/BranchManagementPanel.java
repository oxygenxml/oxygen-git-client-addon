package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TreeUtil;
import com.oxygenxml.git.view.CoalescedEventUpdater;
import com.oxygenxml.git.view.dialog.UIUtil;

import ro.sync.exml.workspace.api.standalone.ui.Tree;

/**
 * Branch management panel. Contains the branches tree (local + remote
 * branches).
 */
public class BranchManagementPanel extends JPanel {

  /**
   * Git API access.
   */
  private static final GitAccess gitAccess = GitAccess.getInstance();

  /**
   * A field for searching branches in the current repository.
   */
  private JTextField searchBar;

  /**
   * Logger for this class.
   */
  private Logger LOGGER = Logger.getLogger(BranchManagementPanel.class);
  
  /**
   * Translator for translation.
   */
  private Translator translator = Translator.getInstance();

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
   * Public constructor
   */
  public BranchManagementPanel() {
    createGUI();
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
        SwingUtilities.invokeLater(BranchManagementPanel.this::showBranches);
      }
      @Override
      public void branchChanged(String oldBranch, String newBranch) {
        currentBranchName = newBranch;
        SwingUtilities.invokeLater(BranchManagementPanel.this::showBranches);
      }
    });
    //TODO add and modify the focus listener such that the branch panel will gain focus on right click
    //installFocusListener(this, createFocusListener());
    addTreeListeners();
  }
//  /**
//   * Adds a focus listener on the component and its descendents.
//   * 
//   * @param c
//   *          The component.
//   * @param focusListener
//   *          Focus Listener.
//   */
//  private void installFocusListener(Component c, FocusListener focusListener) {
//    c.addFocusListener(focusListener);
//
//    if (c instanceof Container) {
//      Container container = (Container) c;
//      int componentCount = container.getComponentCount();
//      for (int i = 0; i < componentCount; i++) {
//        Component child = container.getComponent(i);
//        installFocusListener(child, focusListener);
//      }
//    }
//  }
//  /**
//   * @return The focus listener.
//   */
//  private FocusAdapter createFocusListener() {
//    return new FocusAdapter() {
//      boolean inTheView = false;
//
//      @Override
//      public void focusGained(final FocusEvent e) {
//        // The focus is somewhere in the view.
//        if (!inTheView) {
//          // EXM-40880: Invoke later so that the focus event gets processed.
//          //SwingUtilities.invokeLater(() -> refreshSupport.call());
//        }
//        inTheView = true;
//      }
//
//      @Override
//      public void focusLost(FocusEvent e) {
//        
//        // The focus might still be somewhere in the view.
//        if(e.getOppositeComponent() != null){
//          Window windowAncestor = SwingUtilities.getWindowAncestor(e.getOppositeComponent());
//          if (windowAncestor != null) {
//            boolean contains = windowAncestor.toString().contains("MainFrame");
//            if(contains && !SwingUtilities.isDescendingFrom(e.getOppositeComponent(), BranchManagementPanel.this)){
//              inTheView = false;
//            } else {
//              inTheView = true;
//            }
//          }
//        } else {
//          inTheView = true;
//        }
//      }
//    };
//  }
  /**
   * 
   * @return
   */
  public boolean isContextMenuShowing() {
    return isContextMenuShowing;
  }
  
  /**
   * Adds the tree listeners.
   */
  private void addTreeListeners() {
    
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
    
    branchesTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if(e.isPopupTrigger()) {
          showContextualMenu(e);
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
          e.consume();
          List<AbstractAction> actionsForSelectedNode = getActionsList4SelectedNode(e);
          if(!actionsForSelectedNode.isEmpty()) {
            actionsForSelectedNode.get(0).actionPerformed(null);
          }
        }
      }
    });
  }    
  
  /**
   * Gets a list of actions for the selected node from the BranchTreeActionProvider.
   * @param e Mouse event.
   * @return The list of actions.
   */
  private List<AbstractAction> getActionsList4SelectedNode(MouseEvent e) {
    Point nodePoint = e.getPoint();
    TreePath pathForLocation = branchesTree.getPathForLocation(nodePoint.x, nodePoint.y);
    if (pathForLocation != null) {
      branchesTree.setSelectionPath(pathForLocation);
      BranchTreeMenuActionsProvider branchTreeActions = new BranchTreeMenuActionsProvider(branchesTree);
      return branchTreeActions.getActionsForBranchNode();
    }
    return Collections.emptyList();
  }
  
  boolean isContextMenuShowing;
  
  /**
   * Creates the contextual menu and populates it with action for the selected node.
   * @param e The Mouse Event.
   */
  private void showContextualMenu(MouseEvent e) {
    List<AbstractAction> actionsForSelectedNode = getActionsList4SelectedNode(e);
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
      popupMenu.show(branchesTree, e.getPoint().x, e.getPoint().y);
    }
  }

  /**
   * Creates the tree for the branches in the current repository.
   */
  private void createBranchesTree() {
    allBranches = getBranches();

    branchesTree = new Tree(new BranchManagementTreeModel(null, allBranches));
    branchesTree.setCellRenderer(new BranchesTreeCellRenderer(() -> isContextMenuShowing, () -> currentBranchName));
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

    setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
    setVisible(false);
  }

  /**
   * Creates a list with all branches, local and remote, for the current
   * repository.
   * 
   * @return The list of all branches. Never <code>null</code>.
   */
  public List<String> getBranches() {
    List<String> branchList = new ArrayList<>();
    Repository repository = getCurrentRepository();
    if (repository != null) {
      List<Ref> branches = new ArrayList<>();
      branches.addAll(gitAccess.getLocalBranchList());
      branches.addAll(gitAccess.getRemoteBrachListForCurrentRepo());
      branchList = branches.stream().map(branch -> branch.getName()).collect(Collectors.toList());
    }
    return branchList;
  }

  /**
   * Updates a tree structure with the given branches.
   * 
   * @param branchList The branches used to generate the nodes.
   */
  private void updateTreeView(List<String> branchList) {
    if (branchesTree != null) {
      Enumeration<TreePath> expandedPaths = TreeUtil.getLastExpandedPaths(branchesTree);
      TreePath[] selectionPaths = branchesTree.getSelectionPaths();

      // Create the tree with the new model
      branchesTree.setModel(new BranchManagementTreeModel(GitAccess.getInstance().getWorkingCopyName(), branchList));

      // restore last expanded paths after refresh
      TreeUtil.restoreLastExpandedPaths(expandedPaths, branchesTree);
      branchesTree.setSelectionPaths(selectionPaths);
    }
  }

  /**
   * @return the current repository or <code>null</code> if there's no repository
   *         selected.
   */
  private Repository getCurrentRepository() {
    Repository repo = null;
    try {
      repo = GitAccess.getInstance().getRepository();
    } catch (NoRepositorySelected e) {
      // TODO: this shows an error on loading... We should take care of the loading
      // part,
      // because in other cases we should show this error
//      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
    }
    return repo;
  }

  /**
   * Creates the search bar for the branches in the current repository.
   */
  private void createSearchBar() {
    searchBar = UIUtil.createTextField();
    searchBar.setText(translator.getTranslation(Tags.FILTER_HINT));
    searchBar.setForeground(Color.GRAY);
    searchBar.setToolTipText(translator.getTranslation(Tags.SEARCH_BAR_TOOL_TIP));
    searchBar.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        if (searchBar.getText().contentEquals(translator.getTranslation(Tags.FILTER_HINT))) {
          searchBar.setText("");
        } else {
          searchBar.selectAll();
        }
        searchBar.setForeground(Color.BLACK);
      }
      @Override
      public void focusLost(FocusEvent e) {
        if (searchBar.getText().isEmpty()) {
          searchBar.setText(translator.getTranslation(Tags.FILTER_HINT));
          searchBar.setForeground(Color.GRAY);
        }
      }
    });
    
    CoalescedEventUpdater updater = new CoalescedEventUpdater(500, () -> searchInTree(searchBar.getText()));
    searchBar.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent evt) {
        updater.update();
      }
    });
  }

  /**
   * Searches in tree for the branches that contains a string of characters and
   * updates the tree with those branches.
   * 
   * @param text The string to find.
   */
  private void searchInTree(String text) {
    List<String> remainingBranches = new ArrayList<>();
    for(String branch : allBranches) {
      String[] path = branch.split("/");
      // Sees if the leaf node/branch contains the given text
      if(path[path.length - 1].contains(text))
        remainingBranches.add(branch);
    }
    updateTreeView(remainingBranches);
    TreeUtil.expandAllNodes(branchesTree, 0, branchesTree.getRowCount());
  }

  /**
   * Shows the branch panel with all its components.
   */
  public void showBranches() {
    allBranches = getBranches();
    updateTreeView(allBranches);
    TreeUtil.expandAllNodes(branchesTree, 0, branchesTree.getRowCount());
    setVisible(true);
  }
  /**
   * Returns the tree that contains all the branches for the current repository
   * @return the branches tree.
   */
  public JTree getTree() {
    return branchesTree;
  }

}
