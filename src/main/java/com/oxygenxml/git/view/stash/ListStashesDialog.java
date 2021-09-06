package com.oxygenxml.git.view.stash;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.staging.StagingResourcesTableCellRenderer;
import com.oxygenxml.git.view.staging.StagingResourcesTableModel;
import com.oxygenxml.git.view.util.HiDPIUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.Table;


/**
 * Used for create the dialog that shows the stashes of the repository.
 * 
 * @author Alex_Smarandache
 *
 */
public class ListStashesDialog extends JDialog {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(ListStashesDialog.class.getName());
  
  /**
   * The translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * The default width for table.
   */
  private static final int FILES_LIST_DEFAULT_WIDTH = 300;

  /**
   * The default width for table.
   */
  private static final int TABLE_DEFAULT_WIDTH = 600;

  /**
   * The default height for table.
   */
  private static final int TABLE_DEFAULT_HEIGHT = 275;
  
  /**
   * The minimum dialog width.
   */
  private static final int DIALOG_MINIMUM_WIDTH = 750;
  
  /**
   * The minimum dialog height.
   */
  private static final int DIALOG_MINIMUM_HEIGHT = 250;
  
  /**
   * Extra width for column icon.
   */
  private static final int RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH = 3;
  
  /**
   * The size of column id.
   */
  private static final int COLUMN_ID_SIZE = 25;

  /**
   * The table with the stashes.
   */
  private Table stashesTable;
  
  /**
   * The files table.
   */
  private Table affectedFilesTable;

  /**
   * The model for the files table.
   */
  private FilesTableModel affectedFilesTableModel;

  /**
   * The model for the stashes table.
   */
  private StashesTableModel stashesTableModel;
  
  /**
   * The apply button.
   */
  private Button applyButton;

  /**
   * The delete Button.
   */
  private Button deleteSelectedButton;
  
  /**
   * The clear button.
   */
  private Button deleteAllButton;

  /**
   * Show diff action.
   */
  private Action compareWithWorkingCopyAction;

  /**
   * Action to apply the selected stash.
   */
  private Action applySelectedStashAction;

  /**
   * Action to delete the selected stash.
   */
  private Action deleteSelectedStashAction;
  
  /**
   * When is selected, if apply a stash, it will be deleted after that.
   */
  private JCheckBox deleteAfterApplyingCheckBox;

  /**
   * The actions provider.
   */
  private final StashActionsProvider stashActionsProvider = new StashActionsProvider();


  /**
   * Constructor
   */
  public ListStashesDialog () {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        Translator.getInstance().getTranslation(Tags.STASHES),
        false);

    this.add(createStashesPanel());
    pack();

    JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
    if (parentFrame != null) {
      setIconImage(parentFrame.getIconImage());
      setLocationRelativeTo(parentFrame);
    }
    
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(DIALOG_MINIMUM_WIDTH, DIALOG_MINIMUM_HEIGHT));
  }
  

  /**
   * Creates the main panel.
   * 
   * @return a JPanel for the stashes list.
   */
  private JPanel createStashesPanel() {
    JPanel stashesPanel = new JPanel(new GridBagLayout());

    createAllActions();

    JLabel stashesLabel = new JLabel(TRANSLATOR.getTranslation(Tags.STASHES) + ":");
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(
        UIConstants.INSETS_3PX, 
        UIConstants.INSETS_11PX, 
        UIConstants.INSETS_5PX, 
        0);
    constraints.weightx = 1;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    stashesPanel.add(stashesLabel, constraints);
    
    JLabel tableTitleLabel = new JLabel(TRANSLATOR.getTranslation(Tags.AFFECTED_FILES) + ":");
    constraints.gridx++;
    constraints.insets = new Insets(UIConstants.INSETS_3PX, UIConstants.INSETS_5PX, UIConstants.INSETS_5PX, 0);
    stashesPanel.add(tableTitleLabel, constraints);
    
    stashesTable = (Table) createStashesTable();
    JScrollPane tableStashesScrollPane = new JScrollPane(stashesTable);
    tableStashesScrollPane.setPreferredSize(new Dimension(TABLE_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    tableStashesScrollPane.setMinimumSize(tableStashesScrollPane.getPreferredSize());
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.insets = new Insets(0, UIConstants.INSETS_11PX, 0, UIConstants.INSETS_11PX);
    constraints.fill = GridBagConstraints.BOTH;
    stashesPanel.add(tableStashesScrollPane, constraints);

    affectedFilesTable = createAffectedFilesTable();
    JScrollPane changesOfStashScrollPane = new JScrollPane(affectedFilesTable);
    changesOfStashScrollPane.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    changesOfStashScrollPane.setMinimumSize(changesOfStashScrollPane.getPreferredSize());
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.insets = new Insets(0, UIConstants.INSETS_5PX, 0, UIConstants.INSETS_11PX);
    stashesPanel.add(changesOfStashScrollPane, constraints);

    JPanel stashesTableButtons = createUnderStashesPanel();
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.weightx = 1;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(UIConstants.INSETS_3PX, UIConstants.INSETS_11PX, 0, UIConstants.INSETS_11PX);
    stashesPanel.add(stashesTableButtons, constraints);

    JPanel emptyPanel = new JPanel();
    constraints.gridx++;
    constraints.insets = new Insets(0, 0, 0, 0);
    stashesPanel.add(emptyPanel, constraints);

    Button closeButton = new Button(TRANSLATOR.getTranslation(Tags.CLOSE));
    closeButton.addActionListener(e -> this.dispose());
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.gridwidth = 2;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(UIConstants.INSETS_5PX, 0, UIConstants.INSETS_7PX, UIConstants.INSETS_11PX);
    stashesPanel.add(closeButton, constraints);
    
    return stashesPanel;
  }


  /**
   * Creates the "Delete all" button.
   *
   * @return the created button.
   */
  private Button createDeleteAllButton() {
    Button deleteAllstashesButton = new Button(Translator.getInstance().getTranslation(Tags.DELETE_ALL));
    deleteAllstashesButton.addActionListener( e-> {
      int answer = FileStatusDialog.showWarningMessageWithConfirmation(
          TRANSLATOR.getTranslation(Tags.DELETE_ALL_STASHES),
          TRANSLATOR.getTranslation(Tags.CONFIRMATION_CLEAR_STASHES_MESSAGE),
          TRANSLATOR.getTranslation(Tags.YES),
          TRANSLATOR.getTranslation(Tags.NO));
      if (OKCancelDialog.RESULT_OK == answer) {
        stashesTableModel.clear();
        affectedFilesTableModel.clear();
        deleteAfterApplyingCheckBox.setEnabled(false);
        setStashTableButtonsEnabled(false);
      }
    });
    deleteAllstashesButton.setToolTipText(TRANSLATOR.getTranslation(Tags.DELETE_ALL_STASHES_BUTTON_TOOLTIP));
    return deleteAllstashesButton;
  }


  /**
   * Creates table for affected files by stash.
   * 
   * @return The created table.
   */
  private Table createAffectedFilesTable() {

    affectedFilesTableModel = new FilesTableModel();

    Table filesTable = new Table(affectedFilesTableModel) {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
    };
    filesTable.setFillsViewportHeight(true);

    filesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked (MouseEvent evt) {
        int selectedRow = stashesTable.getSelectedRow();
        if(selectedRow >= 0 && evt.getClickCount() == 2) {
          compareWithWorkingCopyAction.actionPerformed(null);
        }
      }
    });
    
    JPopupMenu contextualActions  = new JPopupMenu();
    JMenuItem  menuItemShowDiff   = new JMenuItem(Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION));
    menuItemShowDiff.setAction(compareWithWorkingCopyAction);
    contextualActions.add(menuItemShowDiff);
    filesTable.setComponentPopupMenu(contextualActions);
    filesTable.addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER 
            && filesTable.getSelectedRow() >= 0 
            && filesTable.getSelectedRow() < filesTable.getRowCount()) {     
          compareWithWorkingCopyAction.actionPerformed(null);
        }
      }

    });

    addClickRightSelection(filesTable, contextualActions);

    filesTable.getColumnModel().setColumnMargin(0);
    filesTable.setTableHeader(null);
    filesTable.setShowGrid(false);

    Icon icon = Icons.getIcon(Icons.GIT_ADD_ICON);
    int iconWidth = icon.getIconWidth();
    int colWidth = iconWidth + RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH;
    TableColumn statusCol = filesTable.getColumnModel().getColumn(StagingResourcesTableModel.FILE_STATUS_COLUMN);
    statusCol.setMinWidth(colWidth);
    statusCol.setPreferredWidth(colWidth);
    statusCol.setMaxWidth(colWidth);

    filesTable.setDefaultRenderer(Object.class, new StagingResourcesTableCellRenderer(() -> false));

    return filesTable;
  }


  /**
   * Initialize all actions.
   */
  private void createAllActions() {
     applySelectedStashAction     = stashActionsProvider.createApplySelectedStashAction();
     deleteSelectedStashAction    = stashActionsProvider.createDeleteSelectedStashAction();
     compareWithWorkingCopyAction = stashActionsProvider.createCompareWithWCAction();
  }


  /**
   * Add selection on click-right for JPopupMenu actions.
   *
   * @param table             the table.
   * @param contextualActions the actionsMenu
   */
  private void addClickRightSelection(Table table, JPopupMenu contextualActions) {
    contextualActions.addPopupMenuListener(new PopupMenuListener() {

      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        SwingUtilities.invokeLater(() -> {
          int rowAtPoint = table.rowAtPoint(SwingUtilities.convertPoint(contextualActions,
                  new Point(0, 0),
                  table));
          if (rowAtPoint >= 0) {
            table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
          }
        });
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // Nothing
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
       // Nothing
      }
    });
  }


  /**
   * Creates the panel under stahses.
   *
   * @return the created panel
   */
  private JPanel createUnderStashesPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();

    deleteAfterApplyingCheckBox = new JCheckBox(TRANSLATOR.getTranslation(Tags.DELETE_STASH_AFTER_APPLIED));
    deleteAfterApplyingCheckBox.setSelected(false);
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(deleteAfterApplyingCheckBox, constraints);

    JPanel buttonsPanel = createButtonsPanel();
    constraints.insets = new Insets(UIConstants.INSETS_5PX, 0, 0, 0);
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(buttonsPanel, constraints);
    
    return panel;
  }


  /**
   * Create the panel with the buttons.
   * 
   * @return The created panel.
   */
  private JPanel createButtonsPanel() {
    JPanel buttonsPanel = new JPanel(new GridBagLayout());
    
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, UIConstants.INSETS_3PX, UIConstants.INSETS_7PX, 0);

    applyButton = createApplyButton();
    buttonsPanel.add(applyButton, constraints);

    constraints.gridx ++;
    deleteSelectedButton = createDeleteButton();
    buttonsPanel.add(deleteSelectedButton, constraints);

    constraints.gridx++;
    deleteAllButton = createDeleteAllButton();
    buttonsPanel.add(deleteAllButton, constraints);

    setStashTableButtonsEnabled(false);

    return buttonsPanel;
  }


  /**
   * Creates the apply button.
   * 
   * @return the created button.
   */
  private Button createApplyButton() {

    Button button = new Button(Translator.getInstance().getTranslation(Tags.APPLY));

    button.setToolTipText(TRANSLATOR.getTranslation(Tags.APPLY_STASH_BUTTON_TOOLTIP));

    button.addActionListener(applySelectedStashAction);

    return button;
  }


  /**
   * Creates the delete button.
   * 
   * @return the created button.
   */
  private Button createDeleteButton() {

    Button button = new Button(Translator.getInstance().getTranslation(Tags.DELETE));

    button.setToolTipText(TRANSLATOR.getTranslation(Tags.DELETE_STASH_BUTTON_TOOLTIP));

    button.addActionListener(deleteSelectedStashAction);

    return button;
  }


  /**
   * Enable or disable all buttons with the new status.
   * 
   * @param newStatus the new status for enabled all buttons.
   */
  private void setStashTableButtonsEnabled(boolean newStatus) {
    SwingUtilities.invokeLater(() -> {
      applyButton.setEnabled(newStatus);
      deleteSelectedButton.setEnabled(newStatus);
      deleteAllButton.setEnabled(newStatus);
    });
  }


  /**
   * Create a table with all stashes.
   * 
   * @return a JTable with the tags and the messages of every stash.
   *
   */
  private JTable createStashesTable() {
    List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());

    stashesTableModel = new StashesTableModel(stashes);

    JTable tableOfStashes = new Table(stashesTableModel) {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
      
      @Override
      public void setRowSelectionInterval(int a, int b) {
        super.setRowSelectionInterval(a, b);
        if(affectedFilesTableModel != null) {
          affectedFilesTableModel.updateTable(a);
        }  
      }
      
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        // TODO: move this somewhere else
        updateStashTableWidths();
      }
      
    };

    tableOfStashes.setFillsViewportHeight(true);
    tableOfStashes.getColumnModel().getColumn(1).setCellRenderer(new StashMessageRender());
    tableOfStashes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableOfStashes.getTableHeader().setReorderingAllowed(false);

    JPopupMenu contextualActions = createContextualActionsForStashedTable(tableOfStashes);
    tableOfStashes.setComponentPopupMenu(contextualActions);
    
    tableOfStashes.getSelectionModel().addListSelectionListener(e -> {
      setStashTableButtonsEnabled(true);
      int selectedRow = tableOfStashes.getSelectedRow();
      affectedFilesTableModel.updateTable(selectedRow);
    });

    stashesTableModel.fireTableDataChanged();
    
    SwingUtilities.invokeLater(() -> tableOfStashes.setRowSelectionInterval(0, 0));

    tableOfStashes.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

    return tableOfStashes;
  }


  /**
   * Creates the contextual actions menu for a stash.
   * 
   * @param tableOfStashes The stashes table.
   * 
   * @return a JPopupMenu with actions created.
   */
  private JPopupMenu createContextualActionsForStashedTable(JTable tableOfStashes) {
    JPopupMenu contextualActions = new JPopupMenu();
    JMenuItem menuItemApply      = new JMenuItem(Translator.getInstance().getTranslation(Tags.APPLY));
    JMenuItem menuItemDelete     = new JMenuItem(Translator.getInstance().getTranslation(Tags.DELETE));

    menuItemApply.addActionListener(applySelectedStashAction);
    
    menuItemDelete.addActionListener(deleteSelectedStashAction);

    contextualActions.add(menuItemApply);
    contextualActions.addSeparator();
    contextualActions.add(menuItemDelete);

    addClickRightSelection((Table) tableOfStashes, contextualActions); 
    
    return contextualActions;
  }
  
  
  /**
   * Distribute widths to the columns according to their content.
   */
  private void updateStashTableWidths() {
    int idColWidth = HiDPIUtil.scaleWidth(COLUMN_ID_SIZE);
    TableColumnModel tcm = stashesTable.getColumnModel();
    TableColumn idCol = tcm.getColumn(0);
    idCol.setPreferredWidth(COLUMN_ID_SIZE);
    TableColumn msgCol = tcm.getColumn(1);
    msgCol.setPreferredWidth(stashesTable.getWidth() - idColWidth);
  }


  /**
   * A custom render for String.
   *
   * @author Alex_Smarandache
   */
  private static class StashMessageRender extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {  
      
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      
      setText((String) value);
      setToolTipText((String) value);
      
      return this;
    }
    
    @Override
    public JToolTip createToolTip() {
      return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
    }
  }


  /**
   * Creates the actions for buttons.
   *
   * @author Alex_Smarandache
   */
  private class StashActionsProvider {


    /**
     * Creates the action to show difference for stashed changes.
     *
     * @return The action created
     */
    public AbstractAction createCompareWithWCAction() {
      return new AbstractAction(Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          int selectedRow = stashesTable.getSelectedRow();
          int selectedFilesIndex = affectedFilesTable.getSelectedRow();

          if (selectedRow >= 0 && selectedRow < stashesTable.getRowCount()
                  && selectedFilesIndex >= 0 && selectedFilesIndex < affectedFilesTable.getRowCount()) {
            FileStatus selectedFile = null;
            List<RevCommit> stashes = null;

            try {
              stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
              selectedFile = ((FileStatus) affectedFilesTable.getValueAt(selectedFilesIndex, 1));
              String filePath = selectedFile.getFileLocation();

              if(selectedFile.getChangeType() == GitChangeType.UNTRACKED) {
                RevCommit[] parents = RevCommitUtil.getParents(GitAccess.getInstance().getRepository(),
                        stashes.get(selectedRow).getId().getName());
                DiffPresenter.showTwoWayDiffOnlyGitFile(filePath, parents[RevCommitUtil.PARENT_COMMIT_UNTRACKED].getId().getName());
              }
              else {
                DiffPresenter.showTwoWayDiffWithLocal(filePath, stashes.get(selectedRow).getId().getName());
              }
            } catch (FileNotFoundException e1) {
              try {
                DiffPresenter.showTwoWayDiffOnlyGitFile(selectedFile.getFileLocation(), stashes.get(selectedRow).getId().getName());
              } catch (IOException e2) {
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(Tags.UNABLE_TO_COMPARE + e2.getMessage());
                LOGGER.error(e2, e2);
              }
            } catch (NoRepositorySelected | IOException | GitAPIException e1) {
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(Tags.UNABLE_TO_COMPARE + e1.getMessage());
              LOGGER.error(e1, e1);
            }

          }
        }
      };
    }


    /**
     * Creates the delete selected stash action.
     *
     * @return the created action.
     */
    private Action createDeleteSelectedStashAction() {
      return new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          int selectedRow = stashesTable.getSelectedRow();
          int noOfRows = stashesTable.getRowCount();
          if (selectedRow >= 0 && selectedRow < noOfRows) {
            int answer = FileStatusDialog.showWarningMessageWithConfirmation(
                    TRANSLATOR.getTranslation(Tags.DELETE_STASH),
                    TRANSLATOR.getTranslation(Tags.STASH_DELETE_CONFIRMATION),
                    TRANSLATOR.getTranslation(Tags.YES),
                    TRANSLATOR.getTranslation(Tags.NO));
            if (OKCancelDialog.RESULT_OK == answer) {
              stashesTableModel.removeRow(selectedRow);
              if(stashesTableModel.getRowCount() == 0) {
                setStashTableButtonsEnabled(false);
                affectedFilesTableModel.clear();
              } else {
                selectNextRow(stashesTable, selectedRow, noOfRows);
              }
            }
          }
        }
      };
    }


    /**
     * Creates the apply selected stash action.
     *
     * @return the created action.
     */
    private Action createApplySelectedStashAction() {
      return new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          int selectedRow = stashesTable.getSelectedRow();
          int noOfRows = stashesTable.getRowCount();
          List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
          if (!stashes.isEmpty() && selectedRow >= 0 && selectedRow < noOfRows) {
            try {
              if(deleteAfterApplyingCheckBox.isSelected()) {
                StashApplyStatus applyStashStatus =
                        GitAccess.getInstance().popStash(stashes.get(selectedRow).getName());
                if(applyStashStatus == StashApplyStatus.APPLIED_SUCCESSFULLY) {
                  stashesTableModel.removeRow(selectedRow);
                  if(stashesTableModel.getRowCount() == 0) {
                    setStashTableButtonsEnabled(false);
                  }
                  selectNextRow(stashesTable, selectedRow, noOfRows);
                }
              } else {
                GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
              }
            } catch (GitAPIException e1) {
              LOGGER.error(e1, e1);
            }
          }
        }
      };
    }
    
    
    /**
     * Select next row in the table.
     * 
     * @param table         The table.
     * @param selectedRow   The deleted row
     * @param noOfRows      The initial number of rows
     */
     private void selectNextRow(Table table, int selectedRow, int noOfRows) {
       if(noOfRows > 1) {
         if (selectedRow == noOfRows - 1) {
           table.setRowSelectionInterval(noOfRows - 2,  noOfRows - 2);
         } else {
           table.setRowSelectionInterval(selectedRow, selectedRow);
         }
       }
     }
     
     
  }

}


