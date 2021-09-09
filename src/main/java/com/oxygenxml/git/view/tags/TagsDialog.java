package com.oxygenxml.git.view.tags;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

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

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Used for making a dialog that shows the Tags of the repository
 * 
 * @author gabriel_nedianu
 *
 */
public class TagsDialog extends OKCancelDialog {
  
  /**
   * Preferred width
   */
  private static final int PREFERRED_WIDTH = 475;

  private static final float[] COLUMN_WIDTH_PER = {0.2f, 0.8f};
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(TagsDialog.class.getName());
  
  /**
   * The table with the tags
   */
  private JTable tagsTable;
  
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
   * 
   * @throws IOException 
   * @throws NoRepositorySelected 
   * @throws GitAPIException 
   */
  private void createGUI() throws GitAPIException, NoRepositorySelected, IOException {

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
  private JPanel createTagsPanel() throws GitAPIException, NoRepositorySelected, IOException {
    
    
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
    gbc.insets = new Insets(0, 0, 0, 0);

    tagsPanel.add(new JScrollPane(tagsTable),gbc);
    
    getOkButton().setText(TRANSLATOR.getTranslation(Tags.PUSH));
    getOkButton().setEnabled(false);
    
    getCancelButton().setText(TRANSLATOR.getTranslation(Tags.DELETE));
    getCancelButton().setEnabled(false);
    
    return tagsPanel;
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
  private void createTagsTable() throws GitAPIException, NoRepositorySelected, IOException {
    List<GitTag> localTagsList = GitTagsManager.getLocalTags();
    
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
          getOkButton().setEnabled(true);
          getCancelButton().setEnabled(true);
        } else {
          getOkButton().setEnabled(false);
          getCancelButton().setEnabled(false);
        }
      }
    });
    
    //Add the listener for double clicked
    tagsTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        tagsTable =(JTable) mouseEvent.getSource();
        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2 && tagsTable.getSelectedRow() != -1 ) {
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
        int pWidth = Math.round(COLUMN_WIDTH_PER[i] * tableWidth);
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
  
  /**
   * Push button pressed.
   */
  @Override
  protected void doOK() {

    int selectedRow = tagsTable.getSelectedRow();
    TagsTableModel model = (TagsTableModel) tagsTable.getModel();
    GitTag tag = model.getItemAt(selectedRow);
    
    if (!tag.isPushed()) {
      try {
        GitAccess.getInstance().pushTag(tag.getName());
        getOkButton().setEnabled(false);
        getCancelButton().setEnabled(false);
        tag.setPushed(true);
      } catch (GitAPIException ex) {
        LOGGER.debug(ex);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    }
  }
  
  /**
   * Delete button pressed.
   */
  @Override
  protected void doCancel() {
    getOkButton().setEnabled(false);
    getCancelButton().setEnabled(false);
    int selectedRow = (tagsTable.getSelectedRow());
    String tagName = (String) tagsTable.getValueAt(selectedRow, 0);

    try {
      GitAccess.getInstance().deleteTag(tagName);

      TagsTableModel model = (TagsTableModel) tagsTable.getModel();
      GitTag tag = model.getItemAt(selectedRow);
      model.remove(tag);
      model.fireTableRowsDeleted(selectedRow,selectedRow);
    } catch (GitAPIException ex) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
    }
  }
 }
