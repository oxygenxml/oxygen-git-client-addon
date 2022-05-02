package com.oxygenxml.git.view.dialog.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Collections;
import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

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
   * Icon path for dialog.
   */
  private String iconPath;
  
  /**
   * Files that relate to the message.
   */
  private List<String> targetFiles;
  
  /**
   * The dialog message.
   */
  private String message;
  
  /**
   * The question message.
   */
  private String questionMessage;
  
  /**
   * Text for "Ok" button.
   */
  private String okButtonName;
  
  /**
   * Text for "Cancel" button.
   */
  private String cancelButtonName;
  
  /**
   * <code>True</code> if "Ok" button should be visible.
   */
  private boolean isOkButtonVisible = true;
  
  /**
   * <code>True</code> if "Cancel" button should be visible.
   */
  private boolean isCancelButtonVisible = true;
  
  
  /**
   * Constructor.
   *
   * @param title Dialog title.
   */
  public MessageDialog(final String title) {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? 
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        title,
        true);
  }

  /**
   * Build UI for current dialog.
   * !!! Call this method after setting the values of all attributes you need in the current dialog.
   * 
   * @return The builded dialog.
   */
  public MessageDialog build() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    // Icon
    JLabel iconLabel = new JLabel();
    if(iconPath != null) {
      final Icon infoIcon = Icons.getIcon(iconPath);
      if (infoIcon != null) {
        iconLabel.setIcon(infoIcon);
      }
    }
   
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        10);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    panel.add(iconLabel, gbc);
    
    // Message
    if (message != null) {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(message);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);
      
      gbc.gridy++;
    }
    
    // Files
    if (targetFiles != null) {
      Collections.sort(targetFiles, String.CASE_INSENSITIVE_ORDER);
      DefaultListModel<String> model = new DefaultListModel<>();
      for (String listElement : targetFiles) {
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
    
    if (!isCancelButtonVisible || !isOkButtonVisible) {
      // No question message. Hide Cancel button.
      getCancelButton().setVisible(isCancelButtonVisible);
      getOkButton().setVisible(isOkButtonVisible);
    } else {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(questionMessage);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);
      
      if(okButtonName != null && !okButtonName.isEmpty()) {
        setOkButtonText(okButtonName);
      }
      if(cancelButtonName != null && !cancelButtonName.isEmpty()) {
        setCancelButtonText(cancelButtonName);
      }
      
    }
    
    getContentPane().add(panel);
    setResizable(false);
    pack();
    
    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
    
    return this;
  }
  
  /**
   * Set path for the desired icon of this dialog.
   * 
   * @param iconPath The icon path.
   * 
   * @return This dialog.
   */
  public MessageDialog setIconPath(final String iconPath) {
    this.iconPath = iconPath;
    System.out.println("this: " + this.iconPath + ", local: " + iconPath);
    return this;
  }
  
  /**
   * Set message for this dialog.
   * 
   * @param message The dialog message.
   * 
   * @return This dialog.
   */
  public MessageDialog setMessage(final String message) {
    this.message = message;
    return this;
  }

  /**
   * Set the new target files to be presented.
   * 
   * @param targetFiles The new files.
   * 
   * @return This dialog.
   */
  public MessageDialog setTargetFiles(final List<String> targetFiles) {
    this.targetFiles = targetFiles;
    return this;
  }

  /**
   * Set the question message.
   * 
   * @param questionMessage The new question message.
   *
   * @return This dialog.
   */
  public MessageDialog setQuestionMessage(String questionMessage) {
    this.questionMessage = questionMessage;
    return this;
  }

  /**
   * Set name for ok button.
   * 
   * @param okButtonName The new name for ok button.
   * 
   * @return This dialog.
   */
  public MessageDialog setOkButtonName(final String okButtonName) {
    this.okButtonName = okButtonName;
    return this;
  }

  /**
   * Set name for cancel button.
   * 
   * @param cancelButtonName The new name for cancel button.
   * 
   * @return This dialog.
   */
  public MessageDialog setCancelButtonName(final String cancelButtonName) {
    this.cancelButtonName = cancelButtonName;
    return this;
  }

  /**
   * By default the value is <code>true</code>.
   * 
   * @param isOkButtonVisible <code>true</code> if the ok button should be visible.
   * 
   * @return This dialog.
   */
  public MessageDialog setOkButtonVisible(final boolean isOkButtonVisible) {
    this.isOkButtonVisible = isOkButtonVisible;
    return this;
  }

  /**
   * By default the value is <code>true</code>.
   * 
   * @param isCancelButtonVisible <code>true</code> if the cancel button should be visible.
   * 
   * @return This dialog.
   */
  public MessageDialog setCancelButtonVisible(final boolean isCancelButtonVisible) {
    this.isCancelButtonVisible = isCancelButtonVisible;
    return this;
  }
  
  
}
