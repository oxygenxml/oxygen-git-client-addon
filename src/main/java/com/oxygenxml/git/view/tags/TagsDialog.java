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

import javax.swing.Action;
import javax.swing.JButton;
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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.history.actions.CheckoutCommitAction;

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

  /**
   * The percents of the columns
   */
  private static final float[] COLUMN_WIDTH_PER = {0.2f, 0.8f};
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(TagsDialog.class.getName());

  /**
   * Translator
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * The table with the tags
   */
  private JTable tagsTable;
  /**
   * The button used to push a local tag
   */
  private JButton pushButton;

  /**
   * The button used to delete a local tag
   */
  private JButton deleteButton;
  
  /**
   * The button used to checkout a tag.
   */
  private JButton checkoutButton;
  

  /**
   * Constructor
   * 
   * @throws GitAPIException
   * @throws IOException
   * @throws NoRepositorySelected
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

    //add a panel with buttons
    JPanel buttonsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints buttonsGridBagConstraints = new GridBagConstraints();

    buttonsGridBagConstraints.gridx = 0;
    buttonsGridBagConstraints.gridy = 0;
    buttonsGridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
    buttonsGridBagConstraints.fill = GridBagConstraints.NONE;
    buttonsGridBagConstraints.insets = new Insets(UIConstants.INDENT_5PX, 0, UIConstants.INDENT_5PX, UIConstants.INDENT_5PX);
    checkoutButton = new JButton(TRANSLATOR.getTranslation(Tags.CHECKOUT) + "...");
    checkoutButton.addActionListener(e -> {
    	 int selectedRow = (tagsTable.getSelectedRow());
		  if(selectedRow >= 0) {
			  GitTag tag = ((TagsTableModel)tagsTable.getModel()).getItemAt(selectedRow);
			  String tagID = tag.getTagID();
			  Action action = new CheckoutCommitAction(tagID);
			  action.actionPerformed(e);
		  }
	});
    
    checkoutButton.setEnabled(false);
    buttonsPanel.add(checkoutButton, buttonsGridBagConstraints);

    pushButton = new JButton(TRANSLATOR.getTranslation(Tags.PUSH));
    pushButton.addActionListener(createPushListener());
    pushButton.setEnabled(false);
    buttonsGridBagConstraints.gridx ++;
    buttonsPanel.add(pushButton, buttonsGridBagConstraints);
    
    deleteButton = new JButton(TRANSLATOR.getTranslation(Tags.DELETE));
    deleteButton.addActionListener(createDeleteListener());
    deleteButton.setEnabled(false);

    buttonsGridBagConstraints.gridx ++;
    buttonsGridBagConstraints.insets = new Insets(UIConstants.INDENT_5PX, 0, UIConstants.INDENT_5PX, 0);
    buttonsPanel.add(deleteButton, buttonsGridBagConstraints);  
    
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    gbc.weighty = 0;
    gbc.weightx = 0;
    gbc.insets = new Insets(0, 0, UIConstants.LAST_LINE_COMPONENT_BOTTOM_PADDING, 0);
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    tagsPanel.add(buttonsPanel, gbc);

    getOkButton().setVisible(false);
    getCancelButton().setText(TRANSLATOR.getTranslation(Tags.CLOSE));

    return tagsPanel;
  }

  /**
   * Create a table with the tags
   * 
   * @throws GitAPIException
   * @throws NoRepositorySelected
   * @throws IOException
   */
  private void createTagsTable() throws GitAPIException, NoRepositorySelected, IOException {
    List<GitTag> localTagsList = GitTagsManager.getLocalTags();

    String[] columnNames = {TRANSLATOR.getTranslation(Tags.TAGS_DIALOG_NAME_COLUMN),
        TRANSLATOR.getTranslation(Tags.MESSAGE_LABEL)};  
    TagsTableModel model = new TagsTableModel(columnNames);
    model.setGitTags(localTagsList);

    tagsTable = OxygenUIComponentsFactory.createTable(model);
    //Add the listener for selecting a row
    tagsTable.getSelectionModel().addListSelectionListener(e -> {
      int selectedRow = (tagsTable.getSelectedRow());
      boolean isSelectionValid = selectedRow >= 0;
      checkoutButton.setEnabled(isSelectionValid);
      if(isSelectionValid) { 	
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
   * @return a JPopUpMenu for the tags table
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
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { /*//Not necessary to be implemented*/ } 

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) { /*//Not necessary to be implemented*/ }
    });
    return contextualActions;
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

      if (!tag.isPushed()) {
        try {
          GitAccess.getInstance().pushTag(tag.getName());
          pushButton.setEnabled(false);
          deleteButton.setEnabled(false);
          tag.setPushed(true);
        } catch (GitAPIException ex) {
          LOGGER.debug(ex.getMessage(), ex);
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
      
      int result = MessagePresenterProvider.getPresenter().showQuestionMessage(
          TRANSLATOR.getTranslation(Tags.DELETE_TAG_DIALOG_TITLE),
          TRANSLATOR.getTranslation(Tags.DELETE_TAG_DIALOG_MESSAGE),
          TRANSLATOR.getTranslation(Tags.YES),
          TRANSLATOR.getTranslation(Tags.NO));
      
      if ( result == OKCancelDialog.RESULT_OK) {
        deleteButton.setEnabled(false);
        pushButton.setEnabled(false);
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
    };
  }
  
  /**
   * Table with the tags
   * 
   * @return a JTable
   */
  public JTable getTagsTable() {
    return tagsTable;
  }
  
  /**
   * Push Button
   * 
   * @return the Button used to push a tag
   */
  public JButton getPushButton() {
    return pushButton;
  }
  
  /**
   * Delete Button
   *    
   * @return the Button used to delete a tag
   */
  public JButton getDeleteButton() {
    return deleteButton;
  }
  
}
