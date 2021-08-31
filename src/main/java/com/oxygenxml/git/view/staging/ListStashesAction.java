package com.oxygenxml.git.view.staging;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.ApplyStashStatus;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.history.HistoryPanel;
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
public class ListStashesAction extends JDialog {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(ListStashesAction.class.getName());
  
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
  private static final int TABLE_DEFAULT_HEIGHT = 250;
  
  /**
   * Extra width for column icon.
   */
  private static final int RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH = 3;

  /**
   * The table with the stashes.
   */
  private Table stashesTable;

  /**
   * The apply button.
   */
  private Button applyButton;

  /**
   * The delete Button.
   */
  private Button deleteButton;

  /**
   * The model for the table.
   */
  private DefaultTableModel tableModel;

  /**
   * Show diff action.
   */
  private Action showDiff;

  /**
   * The files table.
   */
  private Table affectedFilesTable;
  
  /**
   * The size of column id.
   */
  private static final int COLUMN_ID_SIZE = 25;

  /**
   * When is selected, if apply a stash, it will be deleted after that.
   */
  private JCheckBox deleteAfterApplingCheckBox;

  /**
   * The clear button.
   */
  private Button clearButton;


  /**
   * Constructor
   */
  public ListStashesAction () {

    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        Translator.getInstance().getTranslation(Tags.STASHES),
        false);

    JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;

    if (parentFrame != null) {
      setIconImage(parentFrame.getIconImage());
    }

    createGUI();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    if (parentFrame != null) {
      setLocationRelativeTo(parentFrame);
    }

  }

  /**
   * Create GUI.
   */
  private void createGUI() {
    this.add(createStashesPanel());
    pack();
  }

  /**
   * Creates the main panel.
   * 
   * @return a JPanel for the stashes list.
   */
  private JPanel createStashesPanel() {

    //a panel with the header and table
    JPanel stashesPanel = new JPanel(new GridBagLayout()) {
      @Override
      public void paint(Graphics g) {
        super.paint(g); 
        updateStashTableWidths();
      }
    };
    GridBagConstraints constraints = new GridBagConstraints();

    stashesTable = (Table)createStashesTable();

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(7, 10, 5, 0);
    constraints.gridwidth = 1;
    constraints.weighty = 0;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    
    JLabel stashesLabel = new JLabel("Stashes:");
    stashesPanel.add(stashesLabel, constraints);
    
    constraints.gridx++;
    constraints.weightx = 0;
    constraints.insets = new Insets(7, 10, 5, 10);
    JLabel tableTitleLabel = new JLabel("Affected files:");
    stashesPanel.add(tableTitleLabel, constraints);
    
    constraints.weighty = 1;
    constraints.weightx = 1;
    constraints.gridy++;
    constraints.gridx = 0;
    constraints.insets = new Insets(0, 10, 0, 10);
    constraints.fill = GridBagConstraints.BOTH;

    JScrollPane tableStashesScrollPane = new JScrollPane();
    
    tableStashesScrollPane.setPreferredSize(new Dimension(TABLE_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    tableStashesScrollPane.setViewportView(stashesTable);
    tableStashesScrollPane.setBackground(stashesTable.getBackground());
    tableStashesScrollPane.setForeground(stashesTable.getForeground());
    tableStashesScrollPane.setFont(stashesTable.getFont());

    stashesLabel.setBackground(stashesTable.getBackground());
    stashesLabel.setForeground(stashesTable.getForeground());
    stashesLabel.setFont(stashesTable.getFont());
    tableTitleLabel.setBackground(stashesTable.getBackground());
    tableTitleLabel.setForeground(stashesTable.getForeground());
    tableTitleLabel.setFont(stashesTable.getFont());
    
    stashesPanel.setBackground(stashesTable.getBackground());
    stashesPanel.setForeground(stashesTable.getForeground());
    stashesPanel.setFont(stashesTable.getFont());
    stashesPanel.add(tableStashesScrollPane,constraints);

    constraints.insets = new Insets(0, 10, 0, 10);
    constraints.gridx++;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.weightx = 1;
    showDiff = createShowDiffAction();
    affectedFilesTable = createAffectedFilesTable();
    JScrollPane changesOfStashScrollPane = new JScrollPane(affectedFilesTable);
    
    changesOfStashScrollPane.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    stashesPanel.add(changesOfStashScrollPane, constraints);

    constraints.gridy++;
    constraints.gridx = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    constraints.weighty = 0;
    constraints.anchor = GridBagConstraints.WEST;
    JPanel stashesTableButtons = createButtonsStashesPanel();
    constraints.insets = new Insets(3, 10, 0, 10);
    stashesPanel.add(stashesTableButtons, constraints);

    constraints.gridy++;
    constraints.insets = new Insets(0, 0, 0, 0);
    JPanel emptyPanel = new JPanel();
    emptyPanel.setBackground(stashesTable.getBackground());
    emptyPanel.setForeground(stashesTable.getForeground());
    emptyPanel.setFont(stashesTable.getFont());
    stashesPanel.add(emptyPanel, constraints);

    constraints.gridx++;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.insets = new Insets(5, 0, 10, 10);
    Button closeButton = new Button(TRANSLATOR.getTranslation(Tags.CLOSE));
    closeButton.addActionListener(e-> this.dispose());
    stashesPanel.add(closeButton, constraints);

    return stashesPanel;
  }


  /**
   * Creates the clear button.
   *
   * @return the created button.
   */
  private Button createClearButton() {
    Button clearAllButton = new Button(Translator.getInstance().getTranslation(Tags.CLEAR));
    clearAllButton.addActionListener( e-> {
      int answer = FileStatusDialog.showWarningMessageWithConfirmation(
          TRANSLATOR.getTranslation(Tags.CLEAR_STASHES),
          TRANSLATOR.getTranslation(Tags.CONFIRMATION_CLEAR_STASHES_MESSAGE),
          TRANSLATOR.getTranslation(Tags.YES),
          TRANSLATOR.getTranslation(Tags.NO));
      if (OKCancelDialog.RESULT_OK == answer) {
        while (stashesTable.getRowCount() != 0 ) {
          deleteRow(stashesTable.getRowCount() - 1);
        }
        changeStatusAllButtons(false);
      }
    });
    clearAllButton.setToolTipText(TRANSLATOR.getTranslation(Tags.CLEAR_STASHES__BUTTON_TOOLTIP));
    return clearAllButton;
  }

  
 
  /**
   * Creates table for affected files by stash.
   * 
   * @return The created table.
   */
  private Table createAffectedFilesTable() {
    String[] columnName = {"", ""};
    tableModel = new DefaultTableModel(columnName, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    Table filesTable = new Table(tableModel) {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
    };
    filesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    filesTable.setFillsViewportHeight(true);
    filesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked (MouseEvent evt) {
        int selectedRow = stashesTable.getSelectedRow();
        if(selectedRow >= 0 && evt.getClickCount() == 2) {
          showDiff.actionPerformed(null);
        }
      }
    });
    
    JPopupMenu contextualActions  = new JPopupMenu();
    JMenuItem  menuItemShowDiff   = new JMenuItem(Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION));
    menuItemShowDiff.setAction(showDiff);
    contextualActions.add(menuItemShowDiff);
    filesTable.setComponentPopupMenu(contextualActions);
    filesTable.addKeyListener(new KeyListener() {

      @Override
      public void keyTyped(KeyEvent e) {
        // Nothing
      }

      @Override
      public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER 
            && filesTable.getSelectedRow() >= 0 
            && filesTable.getSelectedRow() < filesTable.getRowCount()) {     
          showDiff.actionPerformed(null);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        // Nothing
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


  private JPanel createButtonsStashesPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 0, 0, 0);
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0;
    constraints.weighty = 0;
    deleteAfterApplingCheckBox = createDeleteAfterApplyingCheckBox();
    deleteAfterApplingCheckBox.setBackground(stashesTable.getBackground());
    deleteAfterApplingCheckBox.setForeground(stashesTable.getForeground());
    deleteAfterApplingCheckBox.setFont(stashesTable.getFont());
    panel.add(deleteAfterApplingCheckBox, constraints);

    constraints.gridx++;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    JPanel emptyPanel = new JPanel();
    emptyPanel.setBackground(stashesTable.getBackground());
    emptyPanel.setForeground(stashesTable.getForeground());
    emptyPanel.setFont(stashesTable.getFont());
    panel.add(emptyPanel, constraints);

    constraints.insets =new Insets(4, 0, 0, 0);
    constraints.gridx++;
    constraints.weightx = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    JPanel buttonsPanel = createButtonsPanel();
    buttonsPanel.setBackground(stashesTable.getBackground());
    buttonsPanel.setForeground(stashesTable.getForeground());
    buttonsPanel.setFont(stashesTable.getFont());
    panel.add(buttonsPanel, constraints);
    
    panel.setBackground(stashesTable.getBackground());
    panel.setForeground(stashesTable.getForeground());
    panel.setFont(stashesTable.getFont());
    
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
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 3, 7, 0);
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0;
    constraints.weighty = 0;

    applyButton = createApplyButton();
    buttonsPanel.add(applyButton, constraints);

    constraints.gridx ++;
    deleteButton = createDeleteButton();
    buttonsPanel.add(deleteButton, constraints);

    constraints.gridx++;
    clearButton = createClearButton();
    buttonsPanel.add(clearButton, constraints);

    buttonsPanel.setBackground(stashesTable.getBackground());
    buttonsPanel.setForeground(stashesTable.getForeground());
    buttonsPanel.setFont(stashesTable.getFont());

    changeStatusAllButtons(false);

    return buttonsPanel;
  }


  /**
   * Creates the apply button.
   * 
   * @return the created button.
   */
  private Button createApplyButton() {
    Button button = new Button(Translator.getInstance().getTranslation(Tags.APPLY));

    button.setToolTipText(TRANSLATOR.getTranslation(Tags.APPLY_STASH__BUTTON_TOOLTIP));
    
    button.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      int noOfRows = stashesTable.getRowCount();
      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
      if (!stashes.isEmpty() && selectedRow >= 0 && selectedRow < noOfRows) {
        try {
          if(deleteAfterApplingCheckBox.isSelected()) {
            ApplyStashStatus applyStashStatus =
                GitAccess.getInstance().popStash(stashes.get(selectedRow).getName());
            if(applyStashStatus == ApplyStashStatus.SUCCESSFULLY) {
              deleteRow(selectedRow);
              selectNextRow(stashesTable, selectedRow, noOfRows);
            }
          } else {
            GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
          }
        } catch (GitAPIException e1) {
          LOGGER.error(e1, e1);
        }
      }
    });

    return button;
  }

  
  /**
   * Removes files from table.
   */
  private void depopulateFilesTable() {
    while(affectedFilesTable.getRowCount() > 0) {
      ((DefaultTableModel)affectedFilesTable.getModel()).removeRow(tableModel.getRowCount() - 1);
    }
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
  
  /**
   * Creates the delete button.
   * 
   * @return the created button.
   */
  private Button createDeleteButton() {
    Button button = new Button(Translator.getInstance().getTranslation(Tags.DELETE));

    button.setToolTipText(TRANSLATOR.getTranslation(Tags.DELETE_STASH__BUTTON_TOOLTIP));
    
    button.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      int noOfRows = stashesTable.getRowCount();
      if (selectedRow >= 0 && selectedRow < noOfRows) {
        int answer = FileStatusDialog.showWarningMessageWithConfirmation(
            TRANSLATOR.getTranslation(Tags.DELETE_STASH),
            TRANSLATOR.getTranslation(Tags.STASH_DELETE_CONFIRMATION),
            TRANSLATOR.getTranslation(Tags.YES),
            TRANSLATOR.getTranslation(Tags.NO));
        if (OKCancelDialog.RESULT_OK == answer) {
          deleteRow(selectedRow);
          depopulateFilesTable();
          if(noOfRows > 1) {
            if (selectedRow == noOfRows - 1) {
              stashesTable.setRowSelectionInterval(noOfRows - 2,  noOfRows - 2);
            } else {
              stashesTable.setRowSelectionInterval(selectedRow, selectedRow);
            }
          }
        } 
      }
    });

    return button;
  }

  
  /**
   * Delete a row from stash table.
   * 
   * @param toDeleteRow row to delete.
   */
  private void deleteRow(int toDeleteRow) {
    GitAccess.getInstance().dropStash(toDeleteRow);
    TableModel model = stashesTable.getModel();
    for (int row = toDeleteRow + 1; row <  stashesTable.getRowCount(); row++) {
      model.setValueAt((int)model.getValueAt(row, 0) - 1, row, 0);
      ((DefaultTableModel)stashesTable.getModel()).fireTableCellUpdated(row, 0);
    }
    ((DefaultTableModel)stashesTable.getModel()).removeRow(toDeleteRow); 

    if (stashesTable.getRowCount() == 0) {
      changeStatusAllButtons(false);
    }
  }
  
  
  private JCheckBox createDeleteAfterApplyingCheckBox() {
    JCheckBox checkBox = new JCheckBox(TRANSLATOR.getTranslation(Tags.DELETE_STASH_AFTER_APPLIED));
    checkBox.setSelected(false);
    checkBox.setBackground(stashesTable.getBackground());
    checkBox.setForeground(stashesTable.getForeground());
    checkBox.setFont(stashesTable.getFont());
    return checkBox;
  }

  /**
   * Creates the action to show difference for stashed changes.
   * 
   * @return The action created
   */
  private AbstractAction createShowDiffAction() {
    return new AbstractAction(Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selectedRow = stashesTable.getSelectedRow();
        int selectedFilesIndex = affectedFilesTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < stashesTable.getRowCount()
            && selectedFilesIndex >= 0 && selectedFilesIndex < affectedFilesTable.getRowCount()
            ) {
          FileStatus selectedFile = null;
          List<RevCommit> stashes = null;
          try {
            stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
            selectedFile = ((FileStatus)affectedFilesTable.getValueAt(selectedFilesIndex, 1));
            String filePath = selectedFile.getFileLocation();
            DiffPresenter.showTwoWayDiffWithLocal(filePath, stashes.get(selectedRow).getId().getName());
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
   * Enable or disable all buttons with the new status.
   * 
   * @param newStatus the new status for enabled all buttons.
   */
  private void changeStatusAllButtons(boolean newStatus) {
    SwingUtilities.invokeLater(
        () -> {
          deleteButton.setEnabled(newStatus);
          applyButton.setEnabled(newStatus);
          clearButton.setEnabled(newStatus);
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
    String[] columnNames = {
        Translator.getInstance().getTranslation(Tags.ID),
        Translator.getInstance().getTranslation(Tags.DESCRIPTION)
        };
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    for (int i = 0; i < stashes.size(); i++) {
      Object[] row = {i, stashes.get(i).getFullMessage()};
      model.addRow(row);
    }

    JTable tableOfStashes = new Table(model) {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
    };
    tableOfStashes.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    tableOfStashes.setFillsViewportHeight(true);
    tableOfStashes.getColumnModel().getColumn(1).setCellRenderer(new StringRender());

    JPopupMenu contextualActions = createContextualActionsForStashedTable(tableOfStashes, stashes, model);
    
    tableOfStashes.setComponentPopupMenu(contextualActions);
    
    tableOfStashes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    tableOfStashes.getSelectionModel().addListSelectionListener(e -> {
      changeStatusAllButtons(true);
      int selectedRow = tableOfStashes.getSelectedRow();
      if (selectedRow >= 0) {
        List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
        try {
          List<FileStatus> listOfChangedFiles = RevCommitUtil.
              getChangedFiles(stashesList.get(selectedRow).getName());
          while (tableModel.getRowCount() != 0) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
          }
          for (FileStatus file : listOfChangedFiles) {
            Object[] row = {file.getChangeType(), file};
            tableModel.addRow(row);
          }
        } catch (IOException | GitAPIException exc) {
          LOGGER.debug(exc, exc);
        } 
      }
    });

    tableOfStashes.getTableHeader().setResizingAllowed(false);
    
    model.fireTableDataChanged();
    
    SwingUtilities.invokeLater(
        () -> tableOfStashes.setRowSelectionInterval(0, 0)
        );
    
    return tableOfStashes;
  }


  /**
   * Creates the contextual actions menu for a stash.
   * 
   * @param stashes  List of stahses.
   * @param model    The table model.
   * 
   * @return a JPopupMenu with actions created.
   */
  private JPopupMenu createContextualActionsForStashedTable(JTable tableOfStashes, List<RevCommit> stashes, DefaultTableModel model) {
    JPopupMenu contextualActions = new JPopupMenu();
    JMenuItem menuItemApply      = new JMenuItem(Translator.getInstance().getTranslation(Tags.APPLY));
    JMenuItem menuItemDelete     = new JMenuItem(Translator.getInstance().getTranslation(Tags.DELETE));

    menuItemApply.addActionListener(e -> {
      ActionListener[] actionsApply = applyButton.getActionListeners();
      for(ActionListener action : actionsApply) {
        action.actionPerformed(null);
      }
    });
    
    menuItemDelete.addActionListener(e -> {
      ActionListener[] actionsDelete = deleteButton.getActionListeners();
      for(ActionListener action : actionsDelete) {
        action.actionPerformed(null);
      }
    });

    contextualActions.add(menuItemApply);
    contextualActions.addSeparator();
    contextualActions.add(menuItemDelete);

    addClickRightSelection((Table)tableOfStashes, contextualActions); 
    
    return contextualActions;
  }
  
  
  /**
   * Distribute widths to the columns according to their content.
   */
  private void updateStashTableWidths() {
    int idColWidth = HistoryPanel.scaleColumnsWidth(COLUMN_ID_SIZE);
    TableColumnModel tcm = stashesTable.getColumnModel();
    TableColumn column = tcm.getColumn(1);
    column.setPreferredWidth(stashesTable.getWidth() - idColWidth);
    column = tcm.getColumn(0);
    column.setPreferredWidth(COLUMN_ID_SIZE);
  }


  /**
   * A custom render for String.
   *
   * @author Alex_Smarandache
   */
  private static class StringRender extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {  
      
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      
      setText((String)value);
      setToolTipText((String)value);
      
      return this;
    }
    
    @Override
    public JToolTip createToolTip() {
      return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
    }
  }

}


