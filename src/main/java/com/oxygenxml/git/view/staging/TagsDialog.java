package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;

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
   * The preferred width of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_WIDTH = 320;

  /**
   * The preferred eight of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_HEIGHT = 75;

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
    
    buttonsGridBagConstraints.gridx=0;
    buttonsGridBagConstraints.gridy=0;
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
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
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
      GitTag tag = localTagsList.get(selectedRow);
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
      String tag = (String) tagsTable.getValueAt(selectedRow, 0);
      GitAccess.getInstance().deleteTag(tag);
      
      localTagsList.remove(selectedRow);

      DefaultTableModel tagsTableModel = (DefaultTableModel) tagsTable.getModel();
      tagsTableModel.removeRow(selectedRow);
      tagsTableModel.fireTableStructureChanged();
      tagsTable.repaint();
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
    DefaultTableModel model = new DefaultTableModel(columnNames, 0);
    
    for (int i = 0; i < localTagsList.size(); i++){
      Object[] row = { localTagsList.get(i).getName(), localTagsList.get(i).getMessage() };
      model.addRow(row);
  }
    tagsTable = OxygenUIComponentsFactory.createTable(model);
    tagsTable.getSelectionModel().addListSelectionListener(e -> {
      int selectedRow = (tagsTable.getSelectedRow());
      if(selectedRow >= 0) {
        GitTag tag = localTagsList.get(selectedRow);
        if (!tag.isPushed()) {
          pushButton.setEnabled(true);
          deleteButton.setEnabled(true);
        } else {
          pushButton.setEnabled(false);
          deleteButton.setEnabled(false);
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
    JMenuItem menuItemDetails = new JMenuItem("See details");
    
    menuItemDetails.addActionListener(e -> {
      JDialog dialog;
      try {
        dialog = createSeeDetailsDialog();
        dialog.setVisible(true);
      } catch (NoRepositorySelected | IOException ex) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
    
    contextualActions.add(menuItemDetails);
    
    new JDialog();
    return contextualActions;
  }
  
  private JDialog createSeeDetailsDialog() throws NoRepositorySelected, IOException {
    
    JDialog detailsDialog = new JDialog(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        "Tag Details",
        false);
    
    int selectedRow = tagsTable.getSelectedRow();
    GitTag tag = localTagsList.get(selectedRow);
    
    //The panel with the Tag Details, Commit Details and a cancel button
    JPanel mainPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JLabel tagNameLabel = new JLabel();
    tagNameLabel.setText("Tag name: ");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);
    mainPanel.add(tagNameLabel, gbc);
    
    JLabel tagNameValueLabel = new JLabel();
    tagNameValueLabel.setText(tag.getName());
    gbc.gridx++;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);
    mainPanel.add(tagNameValueLabel, gbc);
    
    JLabel taggerDetailsLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    taggerDetailsLabel.setText("Tagger name: ");
    mainPanel.add(taggerDetailsLabel, gbc);
    
    JLabel taggerDetailsValueLabel = new JLabel();
    gbc.gridx ++;
    taggerDetailsValueLabel.setText(tag.getTaggerName() + " <" + tag.getTaggerEmail() + ">");
    mainPanel.add(taggerDetailsValueLabel, gbc);
    
    JLabel tagDateLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    tagDateLabel.setText("Date: ");
    mainPanel.add(tagDateLabel, gbc);
    
    SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
    JLabel tagDateValueLabel = new JLabel();
    gbc.gridx++;
    tagDateValueLabel.setText(dateFormat.format( tag.getTaggingDate() ));
    mainPanel.add(tagDateValueLabel, gbc);
    
    JLabel tagMessageLabel = new JLabel();
    tagMessageLabel.setText("Tag message:");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.gridwidth = 2;
    mainPanel.add(tagMessageLabel, gbc);
    
    JTextArea tagMessageArea = new JTextArea();
    gbc.gridy++;
    gbc.fill = GridBagConstraints.BOTH;
    tagMessageArea.setText(tag.getMessage());
    tagMessageArea.setBorder(OxygenUIComponentsFactory.createTextField().getBorder());
    tagMessageArea.setPreferredSize(new Dimension(tagMessageArea.getPreferredSize().width, 2* tagMessageArea.getPreferredSize().height));
    tagMessageArea.setWrapStyleWord(true);
    tagMessageArea.setLineWrap(true);
    tagMessageArea.setEditable(false);             
    JScrollPane tagMesagePane = new JScrollPane(tagMessageArea);
    tagMesagePane.setPreferredSize(new Dimension(MESSAGE_SCROLLPANE_PREFERRED_WIDTH, MESSAGE_SCROLLPANE_PREFERRED_HEIGHT));
    mainPanel.add(tagMesagePane, gbc);
    
    JSeparator separator = new JSeparator();
    gbc.gridy++;
    gbc.insets = new Insets(3* topInset, leftInset, 3 * bottomInset, rightInset);
    mainPanel.add(separator, gbc);
    
    JLabel commitIDLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    gbc.gridwidth = 1;
    commitIDLabel.setText("Commit: ");
    mainPanel.add(commitIDLabel, gbc);
    
    JLabel commitIDValueLabel = new JLabel();
    gbc.gridx++;
    gbc.weightx = 0;
    commitIDValueLabel.setText(tag.getCommitID() );
    mainPanel.add(commitIDValueLabel, gbc);
    
    RevCommit commit = RevCommitUtil.getCommit(tag.getCommitID());
    
    JLabel authorDetailsLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    authorDetailsLabel.setText("Author: ");
    mainPanel.add(authorDetailsLabel, gbc);
    
    JLabel authorDetailsValueLabel = new JLabel();
    gbc.gridx++;
    authorDetailsValueLabel.setText(commit.getAuthorIdent().getName() + " <" + commit.getAuthorIdent().getEmailAddress() + ">");
    mainPanel.add(authorDetailsValueLabel, gbc);
    
    JLabel commitDateLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    commitDateLabel.setText("Date: ");
    mainPanel.add(commitDateLabel, gbc);
    
    JLabel commitDateValueLabel = new JLabel();
    gbc.gridx++;
    commitDateValueLabel.setText(dateFormat.format( commit.getAuthorIdent().getWhen() ));
    mainPanel.add(commitDateValueLabel, gbc);
    
    JLabel commitMessageLabel = new JLabel();
    commitMessageLabel.setText("Commit message:");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.gridwidth = 2;
    mainPanel.add(commitMessageLabel, gbc);
    
    JTextArea commitMessageArea = new JTextArea();
    commitMessageArea.setEditable(false);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy++;
    commitMessageArea.setText(commit.getFullMessage());
    commitMessageArea.setBorder(OxygenUIComponentsFactory.createTextField().getBorder());
    commitMessageArea.setWrapStyleWord(true);
    commitMessageArea.setLineWrap(true);
    commitMessageArea.setEditable(false);             
    JScrollPane scollPane = new JScrollPane(commitMessageArea);
    scollPane.setPreferredSize(new Dimension(MESSAGE_SCROLLPANE_PREFERRED_WIDTH, MESSAGE_SCROLLPANE_PREFERRED_HEIGHT));
    mainPanel.add(scollPane, gbc);
    
    JButton closeButton = new JButton(TRANSLATOR.getTranslation(Tags.CLOSE));
    gbc.gridy++;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.insets = new Insets(2 * topInset, leftInset, UIConstants.LAST_LINE_COMPONENT_BOTTOM_PADDING, rightInset);
    closeButton.addActionListener(e -> {
      detailsDialog.dispose();
    });
    mainPanel.add(closeButton, gbc);
    
    detailsDialog.getContentPane().add(mainPanel);
    detailsDialog.pack();
    detailsDialog.setResizable(false);
    return detailsDialog;
  }
}
