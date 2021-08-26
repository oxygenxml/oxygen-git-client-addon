package com.oxygenxml.git.view.staging;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.util.HiDPIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;
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
   * The default width for table.
   */
  private static final int FILES_LIST_DEFAULT_WIDTH = 200;

  /**
   * The default width for table.
   */
  private static final int TABLE_DEFAULT_WIDTH = 400;

  /**
   * The default height for table.
   */
  private static final int TABLE_DEFAULT_HEIGHT = 200;

  /**
   * The table with the stashes.
   */
  private JTable stashesTable;

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
   * Constructor
   */
  public ListStashesAction () {

    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        "Stashes",
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
    GridBagConstraints constrains = new GridBagConstraints();

    createStashesTable();

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
    tableStashesScrollPane.setPreferredSize(HiDPIUtil.getHiDPIDimension(TABLE_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    tableStashesScrollPane.setViewportView(stashesTable);
    tableStashesScrollPane.setBackground(stashesTable.getBackground());
    tableStashesScrollPane.setForeground(stashesTable.getForeground());
    tableStashesScrollPane.setFont(stashesTable.getFont());

    stashesPanel.setBackground(stashesTable.getBackground());
    stashesPanel.setForeground(stashesTable.getForeground());
    stashesPanel.setFont(stashesTable.getFont());
    stashesPanel.add(tableStashesScrollPane,constrains);

    constrains.gridx++;
    constrains.weightx = 0.5;
    showDiff = createShowDiffAction();
    JScrollPane changesOfStashScrollPane = new JScrollPane(createAffectedFilesTable());
    changesOfStashScrollPane.setPreferredSize(HiDPIUtil.getHiDPIDimension(FILES_LIST_DEFAULT_WIDTH, TABLE_DEFAULT_HEIGHT));
    stashesPanel.add(changesOfStashScrollPane, constrains);

    constrains.gridy++;
    constrains.gridx = 0;
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.weightx = 1;
    constrains.gridwidth = 1;
    constrains.weighty = 0;
    constrains.anchor = GridBagConstraints.EAST;
    constrains.insets = new Insets(5, 10, 0, 10);
    stashesPanel.add(createButtonsPanel(), constrains);

    return stashesPanel;
  }
  

  /**
   * Creates table for affected files by stash.
   * 
   * @return The created table.
   */
  private Table createAffectedFilesTable() {
    String[] columnName = {"Affected files:"};
    tableModel = new DefaultTableModel(columnName, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    
    Table filesTable = OxygenUIComponentsFactory.createTable(tableModel);
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
    filesTable.getColumnModel().getColumn(0).setCellRenderer(new FileStatusRender());
    JPopupMenu contextualActions  = new JPopupMenu();
    JMenuItem  menuItemShowDiff   = new JMenuItem(Translator.getInstance().getTranslation("Show Diff"));
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
    
    return filesTable;
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
    constrains.anchor = GridBagConstraints.WEST;
    constrains.insets = new Insets(0, 0, 0, 0);
    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.gridwidth = 1;
    constrains.gridheight = 1;
    constrains.weightx = 1;
    constrains.weighty = 0;
    JPanel emptyPanel = new JPanel();
    emptyPanel.setBackground(stashesTable.getBackground());
    emptyPanel.setForeground(stashesTable.getForeground());
    emptyPanel.setFont(stashesTable.getFont());

    buttonsPanel.add(emptyPanel, constrains);   

    constrains.fill = GridBagConstraints.NONE;
    constrains.anchor = GridBagConstraints.WEST;
    constrains.insets = new Insets(0, 3, 7, 0);
    constrains.gridx++;
    constrains.gridy = 0;
    constrains.gridwidth = 1;
    constrains.gridheight = 1;
    constrains.weightx = 0;
    constrains.weighty = 0;

    applyButton    = new Button(Translator.getInstance().getTranslation(Tags.APPLY));
    deleteButton   = new Button(Translator.getInstance().getTranslation(Tags.DELETE));

    applyButton.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStash());
      if(!stashes.isEmpty() && selectedRow >= 0 && selectedRow < stashesTable.getRowCount()) {
        try {
          GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
        } catch (GitAPIException e1) {
          LOGGER.error(e1, e1);
        }
      }
    });
    buttonsPanel.add(applyButton, constrains); 

    deleteButton.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      if (selectedRow >= 0 && selectedRow < stashesTable.getRowCount()) {
        int answer = FileStatusDialog.showQuestionMessage(
            Translator.getInstance().getTranslation(Tags.STASH),
            Translator.getInstance().getTranslation(Tags.STASH_DELETE_CONFIRMATION),
            Translator.getInstance().getTranslation(Tags.YES),
            Translator.getInstance().getTranslation(Tags.CANCEL));
        if(OKCancelDialog.RESULT_OK == answer) {
          GitAccess.getInstance().stashDrop(selectedRow);
          TableModel model = stashesTable.getModel();
          for (int row = selectedRow + 1; row <  stashesTable.getRowCount(); row++) {
            model.setValueAt((int)model.getValueAt(row, 0) - 1, row, 0);
            ((DefaultTableModel)stashesTable.getModel()).fireTableCellUpdated(row, 0);
          }
          ((DefaultTableModel)stashesTable.getModel()).removeRow(selectedRow); 
          if(stashesTable.getRowCount() == 0) {
            changeStatusAllButtons(false);
          }
        } 
      }
    });
    constrains.gridx ++;
    buttonsPanel.add(deleteButton, constrains);

    buttonsPanel.setBackground(stashesTable.getBackground());
    buttonsPanel.setForeground(stashesTable.getForeground());
    buttonsPanel.setFont(stashesTable.getFont());

    stashesTable.getSelectionModel().addListSelectionListener(e -> {
      changeStatusAllButtons(true);

      while(tableModel.getRowCount() != 0) {
        tableModel.removeRow(tableModel.getRowCount() - 1);
      }
      List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStash());
      try {
        List<FileStatus> listOfChangedFiles = RevCommitUtil.
            getChangedFiles(stashesList.get(stashesTable.getSelectedRow()).getName());
        for(FileStatus file : listOfChangedFiles) {
          Object[] row = {file};
          tableModel.addRow(row);
        }
      } catch (IOException | GitAPIException ex) {
        LOGGER.debug(ex, ex);
      } 

    });

    changeStatusAllButtons(false);

    return buttonsPanel;
  }

  
 /**
  * Creates the action to show difference for stashed changes.
  * 
  * @return The action created
  */
  private AbstractAction createShowDiffAction() {
    return new AbstractAction("Show Diff") {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO
        System.out.println("Show Diff was Pressed");
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
        });
  }   


  /**
   * Create a table with all stashes.
   * 
   * @return a JTable with the tags and the messages of every stash
   *
   */
  private void createStashesTable() {
    List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStash());
    String[] columnNames = {"Id","Description"};
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

    stashesTable = OxygenUIComponentsFactory.createTable(model);
    stashesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    stashesTable.setFillsViewportHeight(true);

    JPopupMenu contextualActions = new JPopupMenu();
    JMenuItem menuItemStash      = new JMenuItem(Translator.getInstance().getTranslation(Tags.APPLY));
    JMenuItem menuItemRemove     = new JMenuItem(Translator.getInstance().getTranslation(Tags.DELETE));

    menuItemStash.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      try {
        GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
      } catch (GitAPIException e1) {
        LOGGER.error(e1, e1);
      }
    });

    menuItemRemove.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      if(selectedRow >= 0 && selectedRow < stashesTable.getRowCount()) {
        int answer = FileStatusDialog.showQuestionMessage(
            Translator.getInstance().getTranslation(Tags.STASH),
            Translator.getInstance().getTranslation(Tags.STASH_DELETE_CONFIRMATION),
            Translator.getInstance().getTranslation(Tags.YES),
            Translator.getInstance().getTranslation(Tags.CANCEL));
        if(OKCancelDialog.RESULT_OK == answer) {
          GitAccess.getInstance().stashDrop(selectedRow);
          for (int row = selectedRow + 1; row <  stashesTable.getRowCount(); row++) {
            model.setValueAt((int)model.getValueAt(row, 0) - 1, row, 0);
            model.fireTableCellUpdated(row, 0);
          }
          model.removeRow(selectedRow);   
          if(stashesTable.getRowCount() == 0) {
            changeStatusAllButtons(false);
          }
        }
      }
    });

    contextualActions.add(menuItemStash);
    contextualActions.addSeparator();
    contextualActions.add(menuItemRemove);

    stashesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    stashesTable.setComponentPopupMenu(contextualActions);

    model.fireTableDataChanged();
    updateStashTableWidths();
  }


  /**
   * Distribute widths to the columns according to their content.
   */
  private void updateStashTableWidths() {
    int idColWidth = HistoryPanel.scaleColumnsWidth(20);
    TableColumnModel tcm = stashesTable.getColumnModel();
    TableColumn column = tcm.getColumn(1);
    column.setPreferredWidth(stashesTable.getWidth() - idColWidth);
  }


  /**
   * A custom render for FileStatus.
   *
   * @author Alex_Smarandache
   */
  static class FileStatusRender extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      String path = ((FileStatus)value).getFileLocation();
      setToolTipText(path);
      setValue(path.substring(path.lastIndexOf('/') + 1));
      return this;
    }
  }

}


