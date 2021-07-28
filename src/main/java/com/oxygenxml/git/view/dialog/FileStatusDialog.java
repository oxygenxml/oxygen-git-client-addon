package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.ListModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Shows information regarding files status.
 * 
 * @author Beniamin Savu
 *
 */
@SuppressWarnings("java:S110")
public class FileStatusDialog extends OKCancelDialog {
  
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
   * @param iconPath         Icont path.
   * @param title            Dialog title.
   * @param targetFiles      Files that relate to the message. May be <code>null</code>.
   * @param message          An information message. May be <code>null</code>.
   * @param questionMessage  A question message connected to the presented information. May be <code>null</code>.
   * @param okButtonName     Text to be written on the button in case the answer to the question is affirmative
   * @param cancelButtonName Text to be written on the button in case the answer to the question is negative
   */
  private FileStatusDialog(
      String iconPath,
      String title,
      List<String> targetFiles,
      String message,
      String questionMessage,
      String okButtonName,
      String cancelButtonName) {
		super(
		    PluginWorkspaceProvider.getPluginWorkspace() != null ? 
		        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
		    title,
		    true);
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel iconLabel = new JLabel();
    URL iconURL =  PluginWorkspaceProvider.class.getResource(iconPath);
    if (iconURL != null) {
      ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
      Icon icon = (Icon) imageUtilities.loadIcon(iconURL);
      iconLabel.setIcon(icon);
    }
		
		gbc.insets = new Insets(
		    UIConstants.COMPONENT_TOP_PADDING, 
		    UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, 
				10);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		panel.add(iconLabel, gbc);
		
    if (message != null) {
      JTextArea textArea = UIUtil.createMessageArea("");
      textArea.setDocument(new CustomWrapDocument());
      textArea.setLineWrap(false);
      textArea.setText(message);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(textArea, gbc);
      gbc.gridy++;
    }
    
    if (targetFiles != null) {
      // populating the JList with the conflict files
      Collections.sort(targetFiles, String.CASE_INSENSITIVE_ORDER);
      DefaultListModel<String> model = new DefaultListModel<>();
      for (String listElement : targetFiles) {
        model.addElement(listElement);
      }
      JList<String> filesInConflictList = new JList<>(model);
      
      ListModel<String> filesInConflictModel = filesInConflictList.getModel();

      for(int i=0; i < filesInConflictModel.getSize(); i++){
          Object textAtIndexI  = filesInConflictModel.getElementAt(i); 
          JLabel textLabelWithTheError = new JLabel();
          textLabelWithTheError.setText(textAtIndexI.toString());
          JToolTip toolTipForFiles = new JToolTip();
          toolTipForFiles.setTipText(textLabelWithTheError.getText());
          textLabelWithTheError.add(toolTipForFiles);
          
      }
      
      filesInConflictList.setPreferredSize(new Dimension(250, 50));
      JScrollPane scollPane = new JScrollPane(filesInConflictList);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(scollPane, gbc);
      
      gbc.gridy++;
    }
    
    if (questionMessage == null) {
      // No question message. Hide Cancel button.
      getCancelButton().setVisible(false);
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
      
      setOkButtonText(okButtonName);
      setCancelButtonText(cancelButtonName);
    }
    
    getContentPane().add(panel);
    pack();
		setResizable(false);
		
		if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
		  setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		}
	}
  
  /**
   * Presents a warning to the user about the files' status.
   * 
   * @param title         Title of the Dialog
   * @param conflictFiles Files that relate to the message / Files with conflicts.
   * @param message       The message.
   */
  public static void showWarningMessage(String title, List<String> conflictFiles, String message) {
	  FileStatusDialog dialog = new FileStatusDialog(Icons.WARNING_ICON,title, conflictFiles, message, null, null, null);
	  dialog.setResizable(true);
	  dialog.setVisible(true);
  }
  
  /**
   * Presents a warning message where the user also has to confirm something.
   * 
   * @param title             Title of the Dialog
   * @param message           The message.
   * @param okButtonLabel     The label of the OK button.
   * @param cancelButtonLabel The label of the cancel dialog.
   * 
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public static int showWarningMessageWithConfirmation(
      String title,
      String message,
      String okButtonLabel,
      String cancelButtonLabel) {
    FileStatusDialog dialog = 
        new FileStatusDialog(Icons.WARNING_ICON,title, null, null, message, okButtonLabel, cancelButtonLabel);
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
  /**
   * Shows pull status and conflicting files and asks the user a question.
   * 
   * @param title            Dialog title.
   * @param conflictFiles    Files that relate to the message.
   * @param message          Message shown
   * @param questionMessage  A question message connected to the presented information.
   * @param okButtonName     The name given to the button for answering affirmative to the question.
   * @param cancelButtonName The name given to the button for answering negative to the question.
   * 
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public static int showQuestionMessage(
      String title, 
      List<String> conflictFiles, 
      String message,
      String questionMessage,
      String okButtonName,
      String cancelButtonName) {
    FileStatusDialog dialog = new FileStatusDialog(Icons.QUESTION_ICON, title, conflictFiles, message, questionMessage, okButtonName, cancelButtonName);
    dialog.setVisible(true);  
    return dialog.getResult();
  }

  /**
   * Shows a warning and asks the user a question.
   * 
   * @param title            Dialog title.
   * @param questionMessage  The warning and question message to be presented.
   * @param okButtonName     The name given to the button for answering affirmative to the question.
   * @param cancelButtonName The name given to the button for answering negative to the question.
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public static int showQuestionMessage(
      String title,
      String questionMessage,
      String okButtonName,
      String cancelButtonName) {
    FileStatusDialog dialog = new FileStatusDialog(Icons.QUESTION_ICON, title, null, null, questionMessage, okButtonName, cancelButtonName);
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
  /**
   * Shows an information and gives the user 2 options.
   * 
   * @param title              Dialog title.
   * @param informationMessage The information message to be presented.
   * @param okButtonName       The name given to the button for answering
   *                           affirmative to the question.
   * @param cancelButtonName   The name given to the button for answering negative
   *                           to the question.
   * @return The option chosen by the user. {@link #RESULT_OK} or
   *         {@link #RESULT_CANCEL}
   */
  public static int showInformationMessage(
      String title, 
      String informationMessage, 
      String okButtonName, 
      String cancelButtonName) {
    FileStatusDialog dialog = new FileStatusDialog(Icons.INFO_ICON, title, null, null, informationMessage, okButtonName, cancelButtonName);
    dialog.setVisible(true);
    return dialog.getResult();
  }
}
