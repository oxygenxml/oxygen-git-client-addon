package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Shows information regarding files status.
 * 
 * @author Beniamin Savu
 *
 */
public class FileStatusDialog extends OKCancelDialog {
  
  /**
   * Constructor.
   * 
   * @param title Dialog title.
   * @param targetFiles Files that relate to the message.
   * @param message The message.
   * @param questionMessage A question message connected to the presented information.
   * @param okButtonName Text to be written on the button in case the answer to the question is affirmative
   * @param cancelButtonName Text to be written on the button in case the answer to the question is negative
   */
  private FileStatusDialog(String title, List<String> targetFiles, String message, String questionMessage,
      String okButtonName, String cancelButtonName) {
		super(
		    PluginWorkspaceProvider.getPluginWorkspace() != null ? 
		        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
		    title,
		    true);
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel iconLabel = new JLabel();
    iconLabel.setIcon(Icons.getIcon(Icons.WARNING_ICON));
		gbc.insets = new Insets(
		    UIConstants.COMPONENT_TOP_PADDING, 
		    UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, 
				UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		panel.add(iconLabel, gbc);
		
    if (message != null) {
      JTextArea label = UIUtil.createMessageArea(message);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(label, gbc);
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
      JScrollPane scollPane = new JScrollPane(filesInConflictList);
      scollPane.setPreferredSize(new Dimension(scollPane.getPreferredSize().width, 50));
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
      JTextArea questionLabel = UIUtil.createMessageArea(questionMessage);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(questionLabel, gbc);
      
      setOkButtonText(okButtonName);
      setCancelButtonText(cancelButtonName);
    }
    
    getContentPane().add(panel);
		this.setResizable(true);
		this.setMinimumSize(new Dimension(500, 80));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		pack();
		
		if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
		  this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
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
    FileStatusDialog dialog = new FileStatusDialog(title, conflictFiles, message, null, null, null);
    dialog.setVisible(true);
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
    FileStatusDialog dialog = new FileStatusDialog(title, conflictFiles, message, questionMessage, okButtonName, cancelButtonName);
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
    FileStatusDialog dialog = new FileStatusDialog(title, null, null, questionMessage, okButtonName, cancelButtonName);
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
}
