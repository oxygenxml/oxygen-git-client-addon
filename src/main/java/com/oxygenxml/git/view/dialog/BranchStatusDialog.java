package com.oxygenxml.git.view.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
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

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

public class BranchStatusDialog extends OKOtherAndCancelDialog {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(FileStatusDialog.class.getName());

  /**
   * The preferred width of the scroll pane for the files list.
   */
  private static final int FILES_SCROLLPANE_PREFERRED_WIDTH = 300;

  /**
   * The preferred eight of the scroll pane for the files list.
   */
  private static final int FILES_SCROLLPANE_PREFERRED_HEIGHT = 100;

  /**
   * Minimum width.
   */
  private static final int WARN_MESSAGE_DLG_MINIMUM_WIDTH = 300;

  /**
   * Minimum height.
   */
  private static final int WARN_MESSAGE_DLG_MINIMUM_HEIGHT = 150;

  /**
   * Document with custom wrapping.
   */
  private static class CustomWrapDocument extends DefaultStyledDocument {

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
   * @param iconPath          Icon path.
   * @param title             Dialog title.
   * @param targetFiles       Files that relate to the message. May be <code>null</code>.
   * @param message           An information message. May be <code>null</code>.
   * @param questionMessage   A question message connected to the presented information. May be <code>null</code>.
   * @param option1ButtonName Text to be written on the button in case the answer to the question is option 1
   * @param option2ButtonName Text to be written on the button in case the answer to the question is option 2
   * @param cancelButtonName  Text to be written on the button in case the answer to the question is negative
   */
  private BranchStatusDialog(
          String iconPath,
          String title,
          List<String> targetFiles,
          String message,
          String questionMessage,
          String option1ButtonName,
          String option2ButtonName,
          String cancelButtonName) {
    super(
            PluginWorkspaceProvider.getPluginWorkspace() != null ?
                    (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
            title,
            true);

    JPanel panel = getContentPanel();
    GridBagConstraints gbc = new GridBagConstraints();

    // Icon
    JLabel iconLabel = new JLabel();
    Icon infoIcon = Icons.getIcon(iconPath);
    if (infoIcon != null) {
      iconLabel.setIcon(infoIcon);
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
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    panel.add(iconLabel, gbc);

    // Message
    if (message != null) {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new FileStatusDialog.CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(message);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      gbc.gridwidth = 1;
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
      gbc.gridwidth = 1;
      gbc.gridheight = 1;
      panel.add(scollPane, gbc);

      gbc.gridy++;
    }

    if (questionMessage == null) {
      // No question message. Hide Cancel button.
      getCancelButton().setVisible(false);
    } else {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new FileStatusDialog.CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(questionMessage);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridwidth = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);

      this.setButtonText(getOKButton(), option1ButtonName);
      this.setButtonText(getOtherButton(), option2ButtonName);
      this.setButtonText(getCancelButton(), cancelButtonName);
    }

    setResizable(false);
    pack();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  }

  
  /**
   * Shows a warning and asks the user a question.
   *
   * @param title             Dialog title.
   * @param questionMessage   The warning and question message to be presented.
   * @param option1ButtonName The name given to the button for answering first option to the question.
   * @param option2ButtonName The name given to the button for answering second option to the question.
   * @param cancelButtonName  The name given to the button for answering negative to the question.
   *
   * @return The option chosen by the user.
   */
  public static int showQuestionMessage(
          String title,
          String questionMessage,
          String option1ButtonName,
          String option2ButtonName,
          String cancelButtonName) {
    BranchStatusDialog dialog = new BranchStatusDialog(Icons.QUESTION_ICON, title,
            null, null, questionMessage, option1ButtonName, option2ButtonName, cancelButtonName);

    dialog.setVisible(true);

    return dialog.getResult();
  }

}
