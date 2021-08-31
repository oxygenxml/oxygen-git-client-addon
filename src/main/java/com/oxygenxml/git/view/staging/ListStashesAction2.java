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
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
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

import com.jidesoft.swing.JideSplitPane;
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
public class ListStashesAction2 extends JDialog {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(ListStashesAction2.class.getName());

  /**
   * The default width for table.
   */
  private static final int FILES_LIST_DEFAULT_WIDTH = 225;

  /**
   * The default width for table.
   */
  private static final int TABLE_DEFAULT_WIDTH =  800;

  /**
   * The default height for table.
   */
  private static final int TABLE_NO_CELLS_DISPLAYED = 11;
  
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
   * The size of column id.
   */
  private static final int COLUMN_ID_SIZE = 25;

  /**
   * When is selected, if apply a stash, it will be deleted after that.
   */
  private JCheckBox deleteAfterApplingCheckBox;

  /**
   * The view button.
   */
  private Button viewButton;

  /**
   * The clear button.
   */
  private Button clearButton;


  /**
   * Constructor
   */
  public ListStashesAction2 () {

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

    JPanel stashesPanel = new JPanel(new GridBagLayout()) {
      @Override
      public void paint(Graphics g) {
        super.paint(g); 
        updateStashTableWidths();
      }
    };
    GridBagConstraints constrains = new GridBagConstraints();

    stashesTable = (Table)createStashesTable();

    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.weighty = 1;
    constrains.gridheight = 1;
    constrains.insets = new Insets(0, 10, 0, 10);
    constrains.weightx = 1;
    constrains.gridwidth = 1;
    constrains.fill = GridBagConstraints.BOTH;

    JScrollPane tableStashesScrollPane = new JScrollPane();
    tableStashesScrollPane.setBorder(null);
    tableStashesScrollPane.setPreferredSize(new Dimension(TABLE_DEFAULT_WIDTH, 
        TABLE_NO_CELLS_DISPLAYED * stashesTable.getRowHeight()));
    tableStashesScrollPane.setViewportView(stashesTable);
    tableStashesScrollPane.setBackground(stashesTable.getBackground());
    tableStashesScrollPane.setForeground(stashesTable.getForeground());
    tableStashesScrollPane.setFont(stashesTable.getFont());

    stashesPanel.setBackground(stashesTable.getBackground());
    stashesPanel.setForeground(stashesTable.getForeground());
    stashesPanel.setFont(stashesTable.getFont());
    stashesPanel.add(tableStashesScrollPane,constrains);
    
    constrains.gridx++;
    constrains.fill = GridBagConstraints.NONE;
    constrains.gridwidth = 1;
    constrains.weightx = 0;
    constrains.weighty = 0;
    constrains.anchor = GridBagConstraints.NORTH;
    stashesPanel.add(createButtonsPanel(), constrains);
    
    constrains.gridy++;
    constrains.gridx = 0;
    constrains.fill = GridBagConstraints.NONE;
    constrains.gridwidth = 1;
    constrains.weightx = 0;
    constrains.weighty = 0;
    constrains.anchor = GridBagConstraints.WEST;
    constrains.insets = new Insets(7, 10, 0, 10);
    deleteAfterApplingCheckBox = createDeleteAfterApplyingCheckBox();
    stashesPanel.add(deleteAfterApplingCheckBox, constrains);
    
    constrains.gridx++;
    constrains.gridy++;
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.insets = new Insets(5, 10, 7, 10);
    Button closeButton = new Button(Translator.getInstance().getTranslation(Tags.CLOSE));
    closeButton.addActionListener(e-> this.dispose());
    stashesPanel.add(closeButton, constrains);
    
    return stashesPanel;
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
   * Create the panel with the buttons.
   * 
   * @return The created panel.
   */
  private JPanel createButtonsPanel() {
    JPanel buttonsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints constrains = new GridBagConstraints();
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.anchor = GridBagConstraints.NORTH;
    constrains.insets = new Insets(0, 0, 10, 0);
    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.gridwidth = 1;
    constrains.gridheight = 1;
    constrains.weightx = 1;
    constrains.weighty = 0;

    applyButton = createApplyButton();
    buttonsPanel.add(applyButton, constrains);

    constrains.gridy++;
    viewButton = createViewButton();
    buttonsPanel.add(viewButton, constrains);
    
    constrains.gridy++;
    deleteButton = createDeleteButton();
    buttonsPanel.add(deleteButton, constrains);
    
    constrains.gridy++;
    clearButton = createClearButton();
    buttonsPanel.add(clearButton, constrains);

    buttonsPanel.setBackground(stashesTable.getBackground());
    buttonsPanel.setForeground(stashesTable.getForeground());
    buttonsPanel.setFont(stashesTable.getFont());

    changeStatusAllButtons(false);

    return buttonsPanel;
  }


  private Button createViewButton() {
    Button localViewButton = new Button("View");
    localViewButton.setToolTipText("View all affected files");
    localViewButton.addActionListener(new AbstractAction() {

      @Override
      public void actionPerformed(ActionEvent e) {
       JDialog affectedFilesDialog = new stashDetailsDialog();
       affectedFilesDialog.setVisible(true);   
      }
    }); 
 
    return localViewButton;
  }
  
  
  private Button createClearButton() {
    Button clearAllButton = new Button("Clear");
    clearAllButton.addActionListener( e-> {
      int answer = FileStatusDialog.showQuestionMessage(
          Translator.getInstance().getTranslation(Tags.STASH),
          Translator.getInstance().getTranslation("Are you sure you want to delete all stashes ? This cannot be undone."),
          Translator.getInstance().getTranslation(Tags.YES),
          Translator.getInstance().getTranslation(Tags.CANCEL));
      if (OKCancelDialog.RESULT_OK == answer) {
        while (stashesTable.getRowCount() != 0 ) {
          deleteRow(stashesTable.getRowCount() - 1);
         }
         changeStatusAllButtons(false);
      }
    });
    clearAllButton.setToolTipText("Delete all stashes from the repository");
    return clearAllButton;
  }
  
  /**
   * Creates the apply button.
   * 
   * @return the created button.
   */
  private Button createApplyButton() {
    Button button = new Button(Translator.getInstance().getTranslation(Tags.APPLY));

    button.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
      if (!stashes.isEmpty() && selectedRow >= 0 && selectedRow < stashesTable.getRowCount()) {
        try {
          ApplyStashStatus applyStashStatus = GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
          if((applyStashStatus == ApplyStashStatus.APPLIED_WITH_GENERATED_CONFLICTS ||
              applyStashStatus == ApplyStashStatus.SUCCESSFULLY)
              && deleteAfterApplingCheckBox.isSelected()) {
            deleteRow(selectedRow);
          }
        } catch (GitAPIException e1) {
          LOGGER.error(e1, e1);
        }
      }
    });

    return button;
  }


  /**
   * Creates the delete button.
   * 
   * @return the created button.
   */
  private Button createDeleteButton() {
    Button button = new Button(Translator.getInstance().getTranslation(Tags.DELETE));

    button.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      if (selectedRow >= 0 && selectedRow < stashesTable.getRowCount()) {
        int answer = FileStatusDialog.showQuestionMessage(
            Translator.getInstance().getTranslation(Tags.STASH),
            Translator.getInstance().getTranslation(Tags.STASH_DELETE_CONFIRMATION),
            Translator.getInstance().getTranslation(Tags.YES),
            Translator.getInstance().getTranslation(Tags.CANCEL));
        if (OKCancelDialog.RESULT_OK == answer) {
          deleteRow(selectedRow);
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
    JCheckBox checkBox = new JCheckBox("Pop stash");
    checkBox.setToolTipText("If selected the stash is deleted after it is applied");
    checkBox.setSelected(false);
    checkBox.setBackground(stashesTable.getBackground());
    checkBox.setForeground(stashesTable.getForeground());
    checkBox.setFont(stashesTable.getFont());
    return checkBox;
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
          viewButton.setEnabled(newStatus);
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
    // Creates list of current stashes.
    List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());

    String[] columnNames = {
        Translator.getInstance().getTranslation(Tags.ID),
        Translator.getInstance().getTranslation(Tags.DESCRIPTION)
        };

    // Creates the table model.
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    // Fills the model with data.
    for (int i = 0; i < stashes.size(); i++) {
      Object[] row = {i, stashes.get(i).getFullMessage()};
      model.addRow(row);
    }

    // Creates the table from model.
    JTable tableOfStashes = new Table(model) {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
    };
    tableOfStashes.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    tableOfStashes.setFillsViewportHeight(true);
    tableOfStashes.getColumnModel().getColumn(1).setCellRenderer(new StringRender());

    // Adds the contextual actions' for current selected stash.
    JPopupMenu contextualActions = createContextualActionsForStashedTable(tableOfStashes);
    tableOfStashes.setComponentPopupMenu(contextualActions);
    
    tableOfStashes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    tableOfStashes.getSelectionModel().addListSelectionListener(e -> changeStatusAllButtons(true));

    tableOfStashes.getTableHeader().setResizingAllowed(false);
    
    tableOfStashes.addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER 
            && tableOfStashes.getSelectedRow() >= 0 
            && tableOfStashes.getSelectedRow() < tableOfStashes.getRowCount()) {     
          ActionListener[] actionsView = viewButton.getActionListeners();
          for(ActionListener action : actionsView) {
            action.actionPerformed(null);
          }
        }
      }

    });
    
    tableOfStashes.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked (MouseEvent evt) {
        int selectedRow = stashesTable.getSelectedRow();
        if(selectedRow >= 0 && evt.getClickCount() == 2) {
          ActionListener[] actionsView = viewButton.getActionListeners();
          for(ActionListener action : actionsView) {
            action.actionPerformed(null);
          }
        }
       
      }
    });
    
    model.fireTableDataChanged();
    
    SwingUtilities.invokeLater(
        () -> tableOfStashes.setRowSelectionInterval(0, 0)
        );
    
    return tableOfStashes;
  }


  /**
   * Creates the contextual actions' menu for current selected stash.
   * 
   * @return a JPopupMenu with actions created.
   */
  private JPopupMenu createContextualActionsForStashedTable(JTable tableOfStashes) {
    JPopupMenu contextualActions = new JPopupMenu();
    JMenuItem menuItemApply      = new JMenuItem(Translator.getInstance().getTranslation(Tags.APPLY));
    JMenuItem menuItemView       = new JMenuItem(Translator.getInstance().getTranslation("View"));
    JMenuItem menuItemDelete     = new JMenuItem(Translator.getInstance().getTranslation(Tags.DELETE));
    
    
    menuItemApply.addActionListener(e -> {
      ActionListener[] actionsApply = applyButton.getActionListeners();
      for(ActionListener action : actionsApply) {
        action.actionPerformed(null);
      }
    });
    
    menuItemView.addActionListener(e -> {
          ActionListener[] actionsView = viewButton.getActionListeners();
          for(ActionListener action : actionsView) {
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
    contextualActions.add(menuItemView);
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


  /**
   * The dialog for stash details.
   */
  private class stashDetailsDialog extends JDialog {
    /**
     * Action for show differences between git file version and current version from WC.
     */
    private Action showDiff;
    
    /**
     * The files table.
     */
    private Table affectedFilesTable;

    JLabel messageLabel;
    private JScrollPane messageStashScrollPane;

    private JTextArea messageStashTextArea;


    /**
     * The public constructor.
     */
    public stashDetailsDialog() {
      super(PluginWorkspaceProvider.getPluginWorkspace() != null ?
                      (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
              Translator.getInstance().getTranslation("Stash Details"),
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
      this.add(createStashDetailsPanel());
      pack();
    }


    /**
     * Create a panel with details for current selected stash.
     *
     * @return the created panel
     */
    private JPanel createStashDetailsPanel() {
      JPanel stashDetailsPanel = new JPanel(new GridBagLayout()) {
        @Override
        public void paint(Graphics g) {

          messageLabel.setLocation(messageLabel.getX(), 
              messageStashScrollPane.getLocation().y + 25);
          super.paint(g);
        }
      };

      GridBagConstraints constrains = new GridBagConstraints();

      constrains.gridwidth = 2;
      constrains.gridx = 0;
      constrains.gridy = 0;
      constrains.gridheight = 1;
      constrains.fill = GridBagConstraints.HORIZONTAL;
      constrains.anchor = GridBagConstraints.WEST;
      constrains.insets = new Insets(0, 10, 0, 10);
      JLabel stashIdLabel = new JLabel("Stash ID: " + stashesTable.getSelectedRow());
      stashIdLabel.setLabelFor(affectedFilesTable);
      stashIdLabel.setBackground(stashesTable.getBackground());
      stashIdLabel.setForeground(stashesTable.getForeground());
      stashIdLabel.setFont(stashesTable.getFont());
      stashDetailsPanel.add(stashIdLabel, constrains);
      
      constrains.gridy++;  
      constrains.gridheight = 1;
      constrains.gridwidth = 1;
      constrains.insets = new Insets(10, 10, 0, 10); 
      constrains.fill = GridBagConstraints.NONE;
      constrains.anchor = GridBagConstraints.NORTH;
      JLabel tableLabel = new JLabel("Affected files:");
      tableLabel.setLabelFor(affectedFilesTable);
      tableLabel.setBackground(stashesTable.getBackground());
      tableLabel.setForeground(stashesTable.getForeground());
      tableLabel.setFont(stashesTable.getFont());
      stashDetailsPanel.add(tableLabel, constrains);

/*      constrains.gridx++;
      constrains.weightx = 1;
      constrains.weighty = 1;
      constrains.fill = GridBagConstraints.BOTH;
      showDiff = createShowDiffAction();
      affectedFilesTable = createAffectedFilesTable();
      populateAffectedFilesTable();
      JScrollPane affectedFilesScrollPane = new JScrollPane(affectedFilesTable);
      affectedFilesScrollPane.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH + 100, 255));
      stashDetailsPanel.add(affectedFilesScrollPane, constrains);
 */
      constrains.gridx++;
      constrains.fill = GridBagConstraints.BOTH;
      constrains.anchor = GridBagConstraints.WEST;
      constrains.weightx = 1;
      constrains.gridheight = 2;
      constrains.weighty = 1;

      showDiff = createShowDiffAction();
      affectedFilesTable = createAffectedFilesTable();
      populateAffectedFilesTable();
      JScrollPane affectedFilesScrollPane = new JScrollPane(affectedFilesTable);
      affectedFilesScrollPane.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH + 100, 255));

      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStashes());
      messageStashTextArea = new JTextArea();
      messageStashTextArea.setText(stashes.get(stashesTable.getSelectedRow()).getFullMessage());
      messageStashTextArea.setWrapStyleWord(true);
      messageStashTextArea.setLineWrap(true);
      messageStashTextArea.setEditable(false);
      messageStashTextArea.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH , 100));
      messageStashScrollPane = new JScrollPane(messageStashTextArea);
      messageStashScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      messageStashScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      messageStashTextArea.setCaretPosition(0);

      JideSplitPane centerSplitPane = createSplitPane(JideSplitPane.VERTICAL_SPLIT, affectedFilesScrollPane,
              messageStashScrollPane);
      centerSplitPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
      stashDetailsPanel.add(centerSplitPane, constrains);
      
      constrains.gridy++; 
      constrains.gridx = 0;
      constrains.fill = GridBagConstraints.NONE;
      constrains.anchor = GridBagConstraints.WEST;
      constrains.weightx = 0;
      constrains.weighty = 0;
      constrains.gridheight = 1;
      messageLabel = new JLabel("Description:");
      messageLabel.setBackground(stashesTable.getBackground());
      messageLabel.setForeground(stashesTable.getForeground());
      messageLabel.setFont(stashesTable.getFont());
      messageLabel.setLabelFor(messageStashScrollPane);
      

      stashDetailsPanel.add(messageLabel, constrains);


      
 /*     constrains.gridx++;
      constrains.anchor = GridBagConstraints.WEST;
      constrains.fill = GridBagConstraints.BOTH;
      constrains.weightx = 1;
      constrains.weighty = 1;
      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStash());
      JTextArea messageStashTextArea = new JTextArea();
      messageStashTextArea.setText(stashes.get(stashesTable.getSelectedRow()).getFullMessage());
      messageStashTextArea.setWrapStyleWord(true);
      messageStashTextArea.setLineWrap(true);
      messageStashTextArea.setEditable(false);
      messageStashTextArea.setPreferredSize(new Dimension(FILES_LIST_DEFAULT_WIDTH , 100));   
      JScrollPane messageStashScrollPane = new JScrollPane(messageStashTextArea);
      messageStashScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      messageStashScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      messageStashTextArea.setCaretPosition(0);
      stashDetailsPanel.add(messageStashScrollPane, constrains);

  */
      
      constrains.gridy++;
      constrains.gridx++;
      constrains.anchor = GridBagConstraints.EAST;
      constrains.fill = GridBagConstraints.NONE;
      constrains.weightx = 0;
      constrains.weighty = 0;
      constrains.insets = new Insets(10, 10, 10, 10);
      Button closeButton = new Button(Translator.getInstance().getTranslation(Tags.CLOSE));
      closeButton.addActionListener(e-> this.dispose());
      stashDetailsPanel.add(closeButton, constrains);
      
      stashDetailsPanel.setBackground(stashesTable.getBackground());
      stashDetailsPanel.setForeground(stashesTable.getForeground());
      stashDetailsPanel.setFont(stashesTable.getFont());
      
  
      return  stashDetailsPanel;
    }


    /**
     * Populate table with affected files by current stash.
     */
    private void populateAffectedFilesTable() {
      int selectedRow = stashesTable.getSelectedRow();
      if (selectedRow >= 0) {
        while(tableModel.getRowCount() > 0) {
          tableModel.removeRow(tableModel.getRowCount() - 1);
        }
        List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
        try {
          List<FileStatus> listOfChangedFiles = RevCommitUtil.
              getChangedFiles(stashesList.get(selectedRow).getName());
          for (FileStatus file : listOfChangedFiles) {
            Object[] row = {file.getChangeType(), file};
            tableModel.addRow(row);
          }
        } catch (IOException | GitAPIException exc) {
          LOGGER.debug(exc, exc);
        } 
      }
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
     filesTable.addKeyListener(new KeyAdapter() {

       @Override
       public void keyPressed(KeyEvent e) {
         if(e.getKeyCode() == KeyEvent.VK_ENTER 
             && filesTable.getSelectedRow() >= 0 
             && filesTable.getSelectedRow() < filesTable.getRowCount()) {     
           showDiff.actionPerformed(null);
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
    * Creates a split pane and puts the two components in it.
    * 
    * @param splitType       {@link JideSplitPane#HORIZONTAL_SPLIT} or
    *                        {@link JideSplitPane#VERTICAL_SPLIT}
    * @param firstComponent  Fist component to add.
    * @param secondComponent Second component to add.
    * 
    * @return The split pane.
    */
   private JideSplitPane createSplitPane(int splitType, 
       JComponent firstComponent, 
       JComponent secondComponent) {
     JideSplitPane splitPane = new JideSplitPane(splitType);

     splitPane.add(firstComponent);
     splitPane.add(secondComponent);

     splitPane.setDividerSize(5);
     splitPane.setContinuousLayout(true);
     splitPane.setOneTouchExpandable(false);
     splitPane.setBorder(null);

     return splitPane;
   }

   
  }



}


