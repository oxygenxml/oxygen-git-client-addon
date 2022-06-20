package com.oxygenxml.git.view.dialog.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Collections;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Represents a dialog that presents a message and some details.
 * 
 * @author alex_smarandache
 *
 */
public class MessageDialog extends OKCancelDialog {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDialog.class.getName());
  
  /**
   * The preferred width of the scroll pane for the files list.
   */
  public static final int FILES_SCROLLPANE_PREFERRED_WIDTH = 300;
  
  /**
   * The preferred eight of the scroll pane for the files list.
   */
  public static final int FILES_SCROLLPANE_PREFERRED_HEIGHT = 100;
  
  /**
   * Minimum width.
   */
  public static final int WARN_MESSAGE_DLG_MINIMUM_WIDTH = 300;
  
  /**
   * Minimum height.
   */
  public static final int WARN_MESSAGE_DLG_MINIMUM_HEIGHT = 150;
  
  /**
   * String value for this class.
   */
  private final DialogInfo info;
  
  /**
   * Document with custom wrapping.
   */
  public static class CustomWrapDocument extends DefaultStyledDocument {
    /**
     * Maximum number of characters without a newline.
     */
    private static final int MAX_NO_OF_CHARS_PER_LINE = 100;

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      StringBuilder sb = new StringBuilder();
      int charsSinceLastNewline = 0;
      char[] charArray = str.toCharArray();
      for (char ch : charArray) {
        if (charsSinceLastNewline >= MAX_NO_OF_CHARS_PER_LINE) {
          if (Character.isWhitespace(ch)) {
            sb.append('\n');
            charsSinceLastNewline = 0;
          } else {
            sb.append(ch);
          }
        } else {
          if (ch == '\n') {
            charsSinceLastNewline = 0;
          }
          sb.append(ch);
        }
        charsSinceLastNewline++;
      }
      super.insertString(offs, sb.toString(), a);
    }
  }
  
 
  /**
   * Constructor.
   * 
   * @param info The dialog informations. 
   */
  protected MessageDialog(@NonNull final DialogInfo info) {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? 
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
            info.title,
        true);
    this.info = info;
    createUI();
  }

  /**
   * Creates UI for this dialog.
   * 
   * @param info The dialog informations. 
   */
  private void createUI() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    // Icon
    JLabel iconLabel = new JLabel();
    if(info.iconPath != null) {
      final Icon infoIcon = Icons.getIcon(info.iconPath);
      if (infoIcon != null) {
        iconLabel.setIcon(infoIcon);
      }
    }
   
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        10);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 1;
    panel.add(iconLabel, gbc);
    
    // Message
    if (info.message != null) {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(info.message);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);
      
      gbc.gridy++;
    }
    
    // Files
    if (info.targetFiles != null) {
      Collections.sort(info.targetFiles, String.CASE_INSENSITIVE_ORDER);
      DefaultListModel<String> model = new DefaultListModel<>();
      for (String listElement : info.targetFiles) {
        model.addElement(listElement);
      }
      
      JList<String> filesList = new JList<>(model);
      filesList.setCellRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          try {
            File workingCopyAbsolute = GitAccess.getInstance().getWorkingCopy().getAbsoluteFile();
            File absoluteFile = new File(workingCopyAbsolute, (String) value); // NOSONAR: no vulnerability here
            setToolTipText(absoluteFile.toString());
          } catch (NoRepositorySelected e) {
            LOGGER.error(e.getMessage(), e);
          }
          return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
      });
      
      JScrollPane scollPane = new JScrollPane(filesList);
      scollPane.setPreferredSize(new Dimension(FILES_SCROLLPANE_PREFERRED_WIDTH, FILES_SCROLLPANE_PREFERRED_HEIGHT));
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(scollPane, gbc);
      
      gbc.gridy++;
    }
    
    if(info.questionMessage != null) {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(info.questionMessage);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);
    }
    
    getCancelButton().setVisible(info.showCancelButton);
    getOkButton().setVisible(info.showOkButton);
   
    if(info.showOkButton && info.okButtonName != null && !info.okButtonName.isEmpty()) {
      setOkButtonText(info.okButtonName);
    }
    if(info.showCancelButton && info.cancelButtonName != null && !info.cancelButtonName.isEmpty()) {
      setCancelButtonText(info.cancelButtonName);
    }      
    
    getContentPane().add(panel);
    setResizable(info.targetFiles != null && info.targetFiles.isEmpty());
    setMinimumSize(new Dimension(MessageDialog.WARN_MESSAGE_DLG_MINIMUM_WIDTH, MessageDialog.WARN_MESSAGE_DLG_MINIMUM_HEIGHT));
    pack();
    
    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }  
  }
 
  @Override
  public String toString() {
    return info.toString();
  }
  
}
