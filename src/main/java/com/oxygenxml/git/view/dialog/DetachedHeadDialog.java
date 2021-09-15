package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Detached head dialog.
 */
public class DetachedHeadDialog extends OKCancelDialog {

  /**
   * Bottom inset for the info message.
   */
  private static final int INFO_MESSAGE_BOTTOM_INSET = 11;

  /**
   * Icon right padding.
   */
  private static final int ICON_RIGHT_PADDING = 10;

  /**
   * The preferred width of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_WIDTH = 320;

  /**
   * The preferred eight of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_HEIGHT = 75;

  /**
   * Minimum width.
   */
  private static final int DLG_MINIMUM_WIDTH = 350;

  /**
   * Minimum height.
   */
  private static final int DLG_MINIMUM_HEIGHT = 250;
  
  /**
   * Right padding for commit details labels.
   */
  private static final int COMMIT_DETAILS_LABELS_RIGHT_PADDING = 5;
  
  /**
   * i18n
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * The checked out commit.
   */
  private RevCommit commit;

  /**
   * Constructor.
   * 
   * @param commit The commit that is checked out.
   * 
   */
  public DetachedHeadDialog(RevCommit commit) {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? 
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        TRANSLATOR.getTranslation(Tags.DETACHED_HEAD),
        true);
    this.commit = commit;
    
    JPanel mainPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    // Icon
    JLabel iconLabel = new JLabel();
    Icon infoIcon = Icons.getIcon(Icons.INFO_ICON);
    if (infoIcon != null) {
      iconLabel.setIcon(infoIcon);
    }
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        ICON_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    mainPanel.add(iconLabel, gbc);
    
    // Info message ("You are on a detached HEAD...").
    JTextArea infoMessageArea = new JTextArea();
    infoMessageArea.setWrapStyleWord(true);
    infoMessageArea.setLineWrap(true);
    infoMessageArea.setEditable(false); 
    infoMessageArea.setOpaque(false);
    infoMessageArea.setText(TRANSLATOR.getTranslation(Tags.DETACHED_HEAD_MESSAGE));
    gbc.gridx++;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.gridheight = 1;
    gbc.insets.bottom = INFO_MESSAGE_BOTTOM_INSET;
    mainPanel.add(infoMessageArea, gbc);
    
    // Commit details (ID, author, date, message).
    JPanel commitDetailsPanel = createCommitDetailsPanel();
    gbc.gridy++;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridheight = 1;
    gbc.insets.bottom = 0;
    mainPanel.add(commitDetailsPanel, gbc);
    
    getCancelButton().setVisible(false);
    setMinimumSize(new Dimension(DLG_MINIMUM_WIDTH, DLG_MINIMUM_HEIGHT));
    getContentPane().add(mainPanel);
    setResizable(true);
    pack();
    
    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  }

  /**
   * @return the commit details panel.
   */
  private JPanel createCommitDetailsPanel() {
    JPanel commitDetailsPanel = new JPanel(new GridBagLayout());
    
    // Commit ID
    JLabel commitIDLabel = new JLabel(TRANSLATOR.getTranslation(Tags.COMMITID) + ":");
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 1;
    gbc.insets.bottom = UIConstants.COMPONENT_BOTTOM_PADDING;
    gbc.insets.right = COMMIT_DETAILS_LABELS_RIGHT_PADDING;
    commitDetailsPanel.add(commitIDLabel, gbc);
    
    JLabel commitIDValueLabel = new JLabel(commit.getId().getName());
    gbc.gridx++;
    commitDetailsPanel.add(commitIDValueLabel, gbc);
    
    // Author
    JLabel authorLabel = new JLabel(TRANSLATOR.getTranslation(Tags.AUTHOR) + ":");
    gbc.gridx = 0;
    gbc.gridy++;
    commitDetailsPanel.add(authorLabel, gbc);
    
    PersonIdent authorIdentity = commit.getAuthorIdent();
    JLabel authorValue = new JLabel(authorIdentity.getName() + " <" + authorIdentity.getEmailAddress() + ">");
    gbc.gridx++;
    commitDetailsPanel.add(authorValue, gbc);
    
    // Date
    JLabel dateLabel = new JLabel(TRANSLATOR.getTranslation(Tags.DATE) + ":");
    gbc.gridx = 0;
    gbc.gridy++;
    commitDetailsPanel.add(dateLabel, gbc);
    
    SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
    JLabel dateValue = new JLabel(dateFormat.format(authorIdentity.getWhen()));
    gbc.gridx++;
    commitDetailsPanel.add(dateValue, gbc);
    
    // Commit message
    JLabel commitMessageLabel = new JLabel(TRANSLATOR.getTranslation(Tags.COMMIT_MESSAGE_LABEL) + ":");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    commitDetailsPanel.add(commitMessageLabel, gbc);
    
    JTextArea commitMessageArea = new JTextArea();
    commitMessageArea.setText(commit.getFullMessage());
    commitMessageArea.setWrapStyleWord(true);
    commitMessageArea.setLineWrap(true);
    commitMessageArea.setEditable(false);             
    JScrollPane scollPane = new JScrollPane(commitMessageArea);
    scollPane.setPreferredSize(new Dimension(MESSAGE_SCROLLPANE_PREFERRED_WIDTH, MESSAGE_SCROLLPANE_PREFERRED_HEIGHT));
    gbc.gridy++;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;
    commitDetailsPanel.add(scollPane, gbc);
    
    return commitDetailsPanel;
  }

}
