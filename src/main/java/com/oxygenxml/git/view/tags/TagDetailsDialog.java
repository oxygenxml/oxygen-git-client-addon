package com.oxygenxml.git.view.tags;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Used to create a dialog for tag details
 * 
 * @author gabriel_nedianu
 *
 */
public class TagDetailsDialog extends OKCancelDialog{
  

  /**
   * i18n
   */
  private static final Translator translator = Translator.getInstance(); // NOSONAR checkstyle:ConstantNameCheckSyncro
  
  /**
   * The preferred width of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_WIDTH = 320;

  /**
   * The multiplier used for separator
   */
  private static final int SEPARATOR_MULTIPLIER = 3;
  
  /**
   * The preferred eight of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_HEIGHT = 75;
  
  private int topInset = UIConstants.COMPONENT_TOP_PADDING;
  private int bottomInset = UIConstants.COMPONENT_BOTTOM_PADDING;
  private int leftInset = UIConstants.INDENT_5PX;
  private int rightInset = UIConstants.INDENT_5PX;

  /**
   * The constructor
   * 
   * @param tag a GitTag
   * 
   * @throws IOException 
   * @throws NoRepositorySelected 
   */
  public TagDetailsDialog(GitTag tag) throws NoRepositorySelected, IOException {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? 
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        translator.getTranslation(Tags.TAG_DETAILS_DIALOG_TITLE),
        false);
    
    this.getContentPane().add(createMainPanel(tag));
    
    getOkButton().setVisible(true);
    getOkButton().setText(translator.getTranslation(Tags.CLOSE));
    getCancelButton().setVisible(false);
    setResizable(false);
    pack();
  }
  
  /**
   * Create the main Panel for the dialog 
   * 
   * @param tag, a GitTag needed to generate the panel
   * 
   * @return a panel with all components added
   * 
   * @throws NoRepositorySelected
   * @throws IOException
   */
  private JPanel createMainPanel(GitTag tag) throws NoRepositorySelected, IOException {
    
    //The panel with the Tag Details, Commit Details and a cancel button
    JPanel mainPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JLabel tagNameLabel = new JLabel();
    tagNameLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_TAG_NAME) + ": ");
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
    taggerDetailsLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_TAGGER_DETAILS) + ": ");
    mainPanel.add(taggerDetailsLabel, gbc);
    
    JLabel taggerDetailsValueLabel = new JLabel();
    gbc.gridx ++;
    taggerDetailsValueLabel.setText(tag.getTaggerName() + " <" + tag.getTaggerEmail() + ">");
    mainPanel.add(taggerDetailsValueLabel, gbc);
    
    JLabel tagDateLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    tagDateLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_DATE) +": ");
    mainPanel.add(tagDateLabel, gbc);
    
    SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
    JLabel tagDateValueLabel = new JLabel();
    gbc.gridx++;
    tagDateValueLabel.setText(dateFormat.format( tag.getTaggingDate() ));
    mainPanel.add(tagDateValueLabel, gbc);
    
    JLabel tagMessageLabel = new JLabel();
    tagMessageLabel.setText(translator.getTranslation(Tags.MESSAGE_LABEL) + ":");
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
    gbc.insets = new Insets(SEPARATOR_MULTIPLIER* topInset, leftInset, SEPARATOR_MULTIPLIER * bottomInset, rightInset);
    mainPanel.add(separator, gbc);
    
    JLabel commitIDLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.insets = new Insets(topInset, leftInset, 0, rightInset);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    gbc.gridwidth = 1;
    commitIDLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_COMMIT) + ": ");
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
    authorDetailsLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_COMMIT_AUTHOR) + ": ");
    mainPanel.add(authorDetailsLabel, gbc);
    
    JLabel authorDetailsValueLabel = new JLabel();
    gbc.gridx++;
    authorDetailsValueLabel.setText(commit.getAuthorIdent().getName() + " <" + commit.getAuthorIdent().getEmailAddress() + ">");
    mainPanel.add(authorDetailsValueLabel, gbc);
    
    JLabel commitDateLabel = new JLabel();
    gbc.gridx = 0;
    gbc.gridy++;
    commitDateLabel.setText(translator.getTranslation(Tags.TAG_DETAILS_DIALOG_DATE) +": ");
    mainPanel.add(commitDateLabel, gbc);
    
    JLabel commitDateValueLabel = new JLabel();
    gbc.gridx++;
    commitDateValueLabel.setText(dateFormat.format( commit.getAuthorIdent().getWhen() ));
    mainPanel.add(commitDateValueLabel, gbc);
    
    JLabel commitMessageLabel = new JLabel();
    commitMessageLabel.setText(translator.getTranslation(Tags.MESSAGE_LABEL) + ":");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 1;
    gbc.gridwidth = 2;
    mainPanel.add(commitMessageLabel, gbc);
    
    JTextArea commitMessageArea = new JTextArea();
    commitMessageArea.setEditable(false);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy++;
    gbc.insets = new Insets(topInset, leftInset, UIConstants.LAST_LINE_COMPONENT_BOTTOM_PADDING, rightInset);
    commitMessageArea.setText(commit.getFullMessage());
    commitMessageArea.setBorder(OxygenUIComponentsFactory.createTextField().getBorder());
    commitMessageArea.setWrapStyleWord(true);
    commitMessageArea.setLineWrap(true);
    commitMessageArea.setEditable(false);             
    JScrollPane scollPane = new JScrollPane(commitMessageArea);
    scollPane.setPreferredSize(new Dimension(MESSAGE_SCROLLPANE_PREFERRED_WIDTH, MESSAGE_SCROLLPANE_PREFERRED_HEIGHT));
    mainPanel.add(scollPane, gbc);
    
    return mainPanel;
  }
}
