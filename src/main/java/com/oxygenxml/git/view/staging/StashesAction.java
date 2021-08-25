package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.history.HistoryPanel;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Used for create the dialog that shows the Stashes of the repository.
 * 
 * @author Alex_Smarandache
 *
 */
public class StashesAction extends JDialog {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(StashesAction.class.getName());
  
  /**
   * The table with the stashes.
   */
  private JTable stashesTable;
 
  
  /**
   * Constructor
   */
  public StashesAction (){

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
   * Create GUI
   */
  private void createGUI() {
    this.add(createStashesPanel());
    pack();
  }
  
  
  /**
   * Creates a JPanel with the
   * 
   * @return a JPanel for the tags
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
    
    createTable();
      
    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.weighty = 1;
    constrains.gridheight = 1;
    constrains.insets = new Insets(0, 10, 0, 10);
    constrains.weightx = 1;
    constrains.gridwidth = 1;
    constrains.fill = GridBagConstraints.BOTH;
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setBorder(null);
    scrollPane.setPreferredSize(new Dimension(400, 200));
    scrollPane.setViewportView(stashesTable);
    scrollPane.setBackground(stashesTable.getBackground());
    scrollPane.setForeground(stashesTable.getForeground());
    scrollPane.setFont(stashesTable.getFont());
  
    stashesPanel.setBackground(stashesTable.getBackground());
    stashesPanel.setForeground(stashesTable.getForeground());
    stashesPanel.setFont(stashesTable.getFont());
    stashesPanel.add(scrollPane,constrains);
    
    constrains.gridy++;
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.weightx = 1;
    constrains.weighty = 0;
    constrains.anchor = GridBagConstraints.EAST;
    constrains.insets = new Insets(5, 10, 0, 10);
    stashesPanel.add(createButtonsPanel(), constrains);
    
    return stashesPanel;
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
   
    
    Button pushButton = new Button("Apply");
    pushButton.setEnabled(true);
    pushButton.addActionListener(e -> {
      int selectedRow = stashesTable.getSelectedRow();
      List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStash());
      if(!stashes.isEmpty()) {
        try {
          GitAccess.getInstance().applyStash(stashes.get(selectedRow).getName());
        } catch (GitAPIException e1) {
          LOGGER.error(e1, e1);
        }
      }
    });
    
    buttonsPanel.add(pushButton, constrains);    
    Button deleteButton = new Button("Delete");
    pushButton.setEnabled(true);
    deleteButton.addActionListener(e -> {
        int selectedRow = stashesTable.getSelectedRow();
        GitAccess.getInstance().stashDrop(selectedRow);
        TableModel model = stashesTable.getModel();
        for (int row = selectedRow + 1; row <  stashesTable.getRowCount(); row++) {
            model.setValueAt((int)model.getValueAt(row, 0) - 1, row, 0);
            ((DefaultTableModel)stashesTable.getModel()).fireTableCellUpdated(row, 0);
        }
        ((DefaultTableModel)stashesTable.getModel()).removeRow(selectedRow);     
    });
    constrains.gridx ++;
    buttonsPanel.add(deleteButton, constrains);
    
    buttonsPanel.setBackground(stashesTable.getBackground());
    buttonsPanel.setForeground(stashesTable.getForeground());
    buttonsPanel.setFont(stashesTable.getFont());
    
    return buttonsPanel;
  }
  
  
  /**
   * Create a table with all stashes.
   * 
   * @return a JTable with the tags and the messages of every stash
   *
   */
  private void createTable() {
    List<RevCommit> stashes = new ArrayList<>(GitAccess.getInstance().listStash());
    String[] columnNames = {"Id","Description"};
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
     }
    };
    
    for (int i = 0; i < stashes.size(); i++){
      Object[] row = {i, stashes.get(i).getFullMessage()};
      model.addRow(row);
  }
    stashesTable = OxygenUIComponentsFactory.createTable(model);
    stashesTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN);
    stashesTable.setFillsViewportHeight(true);
    
    stashesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
}


