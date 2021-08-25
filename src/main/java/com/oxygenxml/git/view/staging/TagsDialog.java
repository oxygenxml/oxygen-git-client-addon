package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;

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
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(TagsDialog.class.getName());
  
  /**
   * A list with the remote tags
   */
  private List<String> remoteTagStrings;
  
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
   * The local tags titles
   */
  private List<String> localTags;
  
  /**
   * The local tags messages
   */
  private List<String> localTagsMessages;
  
  /**
   * Constructor
   */
  TagsDialog (){

    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        "Tags",
        false);
    
    try {
      createRemoteTagList();
    } catch (GitAPIException  e) {
      e.printStackTrace();
    }
    
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
    setSize(new Dimension(475, getPreferredSize().height));
    setResizable(true);
    
    
  }
  
  private void createRemoteTagList() throws GitAPIException {
    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
    Collection <Ref> refs = GitAccess.getInstance().getGit().lsRemote().setCredentialsProvider(credentialsProvider).setTags(true).call();
    remoteTagStrings = new ArrayList<>();
    for (Ref ref : refs) {
    remoteTagStrings.add(Repository.shortenRefName(ref.getName()));
    }
  }
  
  /**
   * Create GUI
   */
  private void createGUI() {
    
    JPanel mainPanel;
    try {
      mainPanel = createTagsPanel();
      getContentPane().add(mainPanel);
    } catch (GitAPIException | NoRepositorySelected | IOException e) {
      e.printStackTrace();
    }
    
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
    
    
    int topInset = UIConstants.COMPONENT_TOP_PADDING;
    int bottomInset = UIConstants.COMPONENT_BOTTOM_PADDING;
    int leftInset = UIConstants.COMPONENT_LEFT_PADDING;
    int rightInset = UIConstants.COMPONENT_RIGHT_PADDING;
    
    //add the header and table
    JPanel tagsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    createTagsTable();
    JTableHeader header = tagsTable.getTableHeader();
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);
    tagsPanel.add(header,gbc);
    
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    tagsPanel.add(new JScrollPane(tagsTable),gbc);
    
    //add a panel with buttons
    JPanel buttonsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints buttonsGridBagConstraints = new GridBagConstraints();
    
    buttonsGridBagConstraints.gridx=0;
    buttonsGridBagConstraints.gridy=0;
    buttonsGridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
    buttonsGridBagConstraints.fill = GridBagConstraints.NONE;
    buttonsGridBagConstraints.insets = new Insets(topInset, 0, bottomInset, rightInset);
    
    pushButton = new JButton("Push");
    pushButton.addActionListener(createPushListener());
    buttonsPanel.add(pushButton, buttonsGridBagConstraints);
    
    deleteButton = new JButton("Delete");
    deleteButton.addActionListener(createDeleteListener());
    
    buttonsGridBagConstraints.gridx ++;
    buttonsPanel.add(deleteButton, buttonsGridBagConstraints);
    
    
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.insets = new Insets(topInset, leftInset, bottomInset, rightInset);
    tagsPanel.add(pushButton, gbc);
    gbc.gridx++;
    tagsPanel.add(deleteButton,gbc);
    
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
      String tag = (String) tagsTable.getValueAt(selectedRow, 0);
      if (!remoteTagStrings.contains(tag)) {
        try {
          GitAccess.getInstance().pushTag(tag);
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
      int selectedRow = (tagsTable.getSelectedRow());
      String tag = (String) tagsTable.getValueAt(selectedRow, 0);
      try {
        GitAccess.getInstance().deleteTag(tag);
      } catch (GitAPIException ex) {
        LOGGER.debug(ex);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
      localTags.remove(selectedRow);
      localTagsMessages.remove(selectedRow);
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
  private void createTagsTable() throws GitAPIException, NoRepositorySelected, IOException{
    List<Ref> call = GitAccess.getInstance().getGit().tagList().call();
    localTags = new ArrayList<>();
    localTagsMessages = new ArrayList<>();
    Repository repository = GitAccess.getInstance().getRepository();
    RevWalk walk = new RevWalk(repository);

    for (Ref ref : call) {
      ObjectId objectIdOfTag = ref.getObjectId();
      RevObject object = walk.parseAny(objectIdOfTag);
      if (object instanceof RevTag) {
        RevTag tag = (RevTag) object;
        localTags.add( tag.getTagName() );
        localTagsMessages.add(tag.getFullMessage());
        
      } else if (object instanceof RevCommit) {
        RevCommit lightTag = (RevCommit) object;
        localTags.add( Repository.shortenRefName(lightTag.getName()) );
        localTagsMessages.add("");
      } 
    }
    
    walk.close();
    
    String[] columnNames = {"Tag name","Message"};
    DefaultTableModel model = new DefaultTableModel(columnNames, 0);
    
    for (int i = 0; i < localTags.size(); i++){
      Object[] row = { localTags.get(i), localTagsMessages.get(i) };
      model.addRow(row);
  }
    tagsTable = OxygenUIComponentsFactory.createTable(model);
    tagsTable.getSelectionModel().addListSelectionListener(e -> {
      int selectedRow = (tagsTable.getSelectedRow());
      String tag = (String) tagsTable.getValueAt(selectedRow, 0);
      if (!remoteTagStrings.contains(tag)) {
        pushButton.setEnabled(true);
        deleteButton.setEnabled(true);
      } else {
        pushButton.setEnabled(false);
        deleteButton.setEnabled(false);
      }
    });
  }
}
