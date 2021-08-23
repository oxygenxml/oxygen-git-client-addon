package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

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
   * Constructor
   */
  TagsDialog (){

    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        "Tags",
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
    setSize(new Dimension(475, getPreferredSize().height));
    setResizable(false);
    
  
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
    
    //a panel with the header and table
    JPanel tagsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JTable tagsTable = createTagsTable();    
    JTableHeader header = tagsTable.getTableHeader();
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1;
    gbc.gridwidth = 1;
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
    buttonsGridBagConstraints.anchor = GridBagConstraints.EAST;
    buttonsGridBagConstraints.fill = GridBagConstraints.NONE;
    buttonsGridBagConstraints.insets = new Insets(topInset, 0, bottomInset, 0);
    
    JButton pushButton = new JButton("Push");
    buttonsPanel.add(pushButton, buttonsGridBagConstraints);
    
    JButton deleteButton = new JButton("Delete");
    buttonsGridBagConstraints.gridx ++;
    buttonsPanel.add(deleteButton, buttonsGridBagConstraints);
    
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.insets = new Insets(topInset, 0, 0, rightInset);
    tagsPanel.add(buttonsPanel,gbc);
    
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
  private JTable createTagsTable() throws GitAPIException, NoRepositorySelected, IOException{
    List<Ref> call = GitAccess.getInstance().getGit().tagList().call();
    List<String> tagsList = new ArrayList<>();
    List<String> tagsMessages = new ArrayList<>();
    Repository repository = GitAccess.getInstance().getRepository();
    RevWalk walk = new RevWalk(repository);

    for (Ref ref : call) {
      ObjectId objectIdOfTag = ref.getObjectId();
      RevObject object = walk.parseAny(objectIdOfTag);
      if (object instanceof RevTag) {
        RevTag tag = (RevTag) object;
        tagsList.add( tag.getTagName() );
        tagsMessages.add(tag.getFullMessage());
        
      } else if (object instanceof RevCommit) {
        RevCommit lightTag = (RevCommit) object;
        tagsList.add( Repository.shortenRefName(lightTag.getName()) );
        tagsMessages.add("");
      } 
    }
    
    walk.close();
    
    String[] columnNames = {"Tag name","Message"};
    DefaultTableModel model = new DefaultTableModel(columnNames, 0);
    
    for (int i = 0; i < tagsList.size(); i++){
      Object[] row = { tagsList.get(i), tagsMessages.get(i) };
      model.addRow(row);
  }
    
    return OxygenUIComponentsFactory.createTable(model);
  }
}
