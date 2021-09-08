package com.oxygenxml.git.view.tags;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Used for making a dialog that shows the Tags of the repository
 * 
 * @author gabriel_nedianu
 *
 */
public class TagsDialog extends JDialog {
  
  /**
   * Preferred width
   */
  private static final int PREFERRED_WIDTH = 475;

  private static final float[] columnWidthPer = {0.2f, 0.8f};
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(TagsDialog.class.getName());
  
  /**
   * A list with the remote tags titles
   */
  private List<String> remoteTagsTitle;
  
  /**
   * The button used to push a local tag
   */
  private JButton pushButton;
  
  /**
   * The button used to delete a local tag
   */
  private JButton deleteButton;
  
  /**
   * The table with the tags
   */
  private JTable tagsTable;
  
  /**
   * A list with all GitTags 
   */
  private List<GitTag> localTagsList;
  
  
  private int topInset = UIConstants.COMPONENT_TOP_PADDING;
  private int bottomInset = UIConstants.COMPONENT_BOTTOM_PADDING;
  private int leftInset = UIConstants.INDENT_5PX;
  private int rightInset = UIConstants.INDENT_5PX;
  
  /**
   * Translator
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Constructor
   */
  public TagsDialog() throws GitAPIException, IOException, NoRepositorySelected {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        TRANSLATOR.getTranslation(Tags.TAGS_DIALOG),
        false);
    
    remoteTagsTitle = GitTagsManager.getRemoteTagsTitle();
    localTagsList = GitTagsManager.getLocalTags();
    
    createGUI();
    
    JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
    if (parentFrame != null) {
      setLocationRelativeTo(parentFrame);
      setIconImage(parentFrame.getIconImage());
    } 

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(PREFERRED_WIDTH, getPreferredSize().height));
    setResizable(true);
  }
  
  
  /**
   * Create GUI
   */
  private void createGUI() {

    JPanel mainPanel = createTagsPanel();
    getContentPane().add(mainPanel);

    pack();
  }
  
  /**
   * Creates a JPanel with the
   * 
   * @return a JPanel for the tags
   * 
   * @throws GitAPIException
   * @throws NoRepositorySelected
   * @throws IOException
   */
  private JPanel createTagsPanel() {
    
    
    //add the table
    JPanel tagsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    createTagsTable();
    tagsTable.getTableHeader().setReorderingAllowed(false);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1;
    gbc.weightx = 1;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);

    tagsPanel.add(new JScrollPane(tagsTable),gbc);
    
    //add a panel with buttons
    JPanel buttonsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints buttonsGridBagConstraints = new GridBagConstraints();
    
    buttonsGridBagConstraints.gridx = 0;
    buttonsGridBagConstraints.gridy = 0;
    buttonsGridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
    buttonsGridBagConstraints.fill = GridBagConstraints.NONE;
    buttonsGridBagConstraints.insets = new Insets(topInset, 0, bottomInset, rightInset);
    
    pushButton = new JButton(TRANSLATOR.getTranslation(Tags.PUSH));
    pushButton.addActionListener(createPushListener());
    pushButton.setEnabled(false);
    buttonsPanel.add(pushButton, buttonsGridBagConstraints);
    
    deleteButton = new JButton(TRANSLATOR.getTranslation(Tags.DELETE));
    deleteButton.addActionListener(createDeleteListener());
    deleteButton.setEnabled(false);
    
    buttonsGridBagConstraints.gridx ++;
    buttonsPanel.add(deleteButton, buttonsGridBagConstraints);
    
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    gbc.weighty = 0;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.insets = new Insets(topInset, leftInset, bottomInset, rightInset);
    tagsPanel.add(buttonsPanel, gbc);
    
    return tagsPanel;
  }
  
  /**
   * Create the Listener for the Push Button
   * 
   * @return an ActionListener
   */
  private ActionListener createPushListener() {

    return e -> {
      int selectedRow = tagsTable.getSelectedRow();
      TagsTableModel model = (TagsTableModel) tagsTable.getModel();
      GitTag tag = model.getItemAt(selectedRow);
      
      if (!remoteTagsTitle.contains(tag.getName())) {
        try {
          GitAccess.getInstance().pushTag(tag.getName());
          tag.setPushed(true);
          pushButton.setEnabled(false);
          deleteButton.setEnabled(false);
        } catch (GitAPIException ex) {
          LOGGER.debug(ex);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
        }
      }
    };
  }
  
  /**
   * Create the Listener for the Delete Button
   * 
   * @return an ActionListener
   */
  private ActionListener createDeleteListener() {

    return e -> {
      deleteButton.setEnabled(false);
      pushButton.setEnabled(false);
      int selectedRow = (tagsTable.getSelectedRow());
      String tagName = (String) tagsTable.getValueAt(selectedRow, 0);

      try {
        GitAccess.getInstance().deleteTag(tagName);
        localTagsList.remove(selectedRow);

        TagsTableModel model = (TagsTableModel) tagsTable.getModel();
        GitTag tag = model.getItemAt(selectedRow);
        model.remove(tag);
        model.fireTableStructureChanged();
        tagsTable.repaint();
      } catch (GitAPIException ex) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }

    };
  }
  
  /**
   * Create a table with the tags
   * 
   * @return a JTable with the tags and the messages of every tag
   * 
   * @throws GitAPIException
   * @throws NoRepositorySelected
   * @throws IOException
   */
  private void createTagsTable() {
    String[] columnNames = {TRANSLATOR.getTranslation(Tags.TAGS_DIALOG_NAME_COLUMN),
                            TRANSLATOR.getTranslation(Tags.TAGS_DIALOG_MESSAGE_COLUMN)};  
    TagsTableModel model = new TagsTableModel(columnNames);
    model.setGitTags(localTagsList);
    
    tagsTable = OxygenUIComponentsFactory.createTable(model);
    //Add the listener for selecting a row
    tagsTable.getSelectionModel().addListSelectionListener(e -> {
      int selectedRow = (tagsTable.getSelectedRow());
      if(selectedRow >= 0) {
        GitTag tag = model.getItemAt(selectedRow);
        if (!tag.isPushed()) {
          pushButton.setEnabled(true);
          deleteButton.setEnabled(true);
        } else {
          pushButton.setEnabled(false);
          deleteButton.setEnabled(false);
        }
      }
    });
    
    //Add the listener for double clicked
    tagsTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent mouseEvent) {
        tagsTable =(JTable) mouseEvent.getSource();
        if (mouseEvent.getClickCount() == 2 && tagsTable.getSelectedRow() != -1) {
          int selectedRow = tagsTable.getSelectedRow();
          GitTag tag = model.getItemAt(selectedRow);
          TagDetailsDialog dialog;
          try {
            dialog = new TagDetailsDialog(tag);
            dialog.setVisible(true);
          } catch (NoRepositorySelected | IOException ex) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
          }
        }
    }
    });
    
    //Resize the table
    TableColumnModel tagsTableColumnModel = tagsTable.getColumnModel();
    int tableWidth = tagsTableColumnModel.getTotalColumnWidth();
    
    TableColumn column;
    int cantCols = tagsTableColumnModel.getColumnCount();
    for (int i = 0; i < cantCols; i++) {
        column = tagsTableColumnModel.getColumn(i);
        int pWidth = Math.round(columnWidthPer[i] * tableWidth);
        column.setPreferredWidth(pWidth);
    }
    
    tagsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tagsTable.setComponentPopupMenu(createTableComponentMenu());  
  }
  
  /**
   * Create the popup menu for the tagsTable
   * 
   * @return
   */
  private JPopupMenu createTableComponentMenu() {
    JPopupMenu contextualActions = new JPopupMenu();
    JMenuItem menuItemDetails = new JMenuItem(TRANSLATOR.getTranslation(Tags.TAGS_DIALOG_POPUP_MENU_DETAILS));
    
    menuItemDetails.addActionListener(e -> {
      try {
        int selectedRow = tagsTable.getSelectedRow();
        TagsTableModel model = (TagsTableModel) tagsTable.getModel();
        GitTag tag = model.getItemAt(selectedRow);
        TagDetailsDialog dialog = new TagDetailsDialog(tag);
        dialog.setVisible(true);
      } catch (NoRepositorySelected | IOException ex) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
    
    contextualActions.add(menuItemDetails);
    
    contextualActions.addPopupMenuListener(new PopupMenuListener() {

      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
          SwingUtilities.invokeLater(() -> {
              int rowAtPoint = tagsTable.rowAtPoint(SwingUtilities.convertPoint(contextualActions, new Point(0, 0), tagsTable));
              if (rowAtPoint > -1) {
                tagsTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
              }
          });
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { } 

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) { }
  });
    return contextualActions;
  }
 }
