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
import javax.swing.WindowConstants;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Show pull status and corresponding files.
 * 
 * @author Beniamin Savu
 *
 */
public class FileStatusDialog extends OKCancelDialog {
  
	private FileStatusDialog(String title, List<String> conflictFiles, String message, String questionMessage) {
		super(
		    PluginWorkspaceProvider.getPluginWorkspace() != null ? 
		        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
		    title,
		    true);
		JLabel label = new JLabel("<html>" + message + "</html>");

		// populating the JList with the conflict files
		DefaultListModel<String> model = new DefaultListModel<>();
		Collections.sort(conflictFiles);
		for(String listElement : conflictFiles) {
		  model.addElement(listElement);
		}
		JList<String> filesInConflictList = new JList<>(model);
		JScrollPane scollPane = new JScrollPane(filesInConflictList);
		scollPane.setPreferredSize(new Dimension(scollPane.getPreferredSize().width, 50));
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel iconLabel = new JLabel();
        iconLabel.setIcon(Icons.getIcon(Icons.WARNING_ICON));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		panel.add(iconLabel, gbc);
		
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		panel.add(label, gbc);
		
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		panel.add(scollPane, gbc);
		
		JLabel questionLabel = new JLabel("<html>" + questionMessage + "</html>");
    gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridheight = 1;
    panel.add(questionLabel, gbc);
	
    if (questionMessage == null) {
      getCancelButton().setVisible(false);
      questionLabel.setVisible(false);
    } else {
      setOkButtonText(Translator.getInstance().getTranslation(Tags.YES));
      setCancelButtonText(Translator.getInstance().getTranslation(Tags.NO));
    }
    getContentPane().add(panel);
		this.setResizable(true);
		this.setMinimumSize(new Dimension(420, 220));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
		  this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		}
	}

  /**
   * Shows pull status and corresponding files.
   * 
   * @param title         Title of the Dialog
   * @param conflictFiles Files with conflicts
   * @param message       Message shown
   */
  public static void showMessage(String title, List<String> conflictFiles, String message) {

    FileStatusDialog pullStatusAndFilesDialog = new FileStatusDialog(title, conflictFiles, message,
        null);
    
    pullStatusAndFilesDialog.pack();
    pullStatusAndFilesDialog.setVisible(true);
  }
  
  /**
   * Shows pull status and conflicting files and asks the user a question.
   * 
   * @param title           Title of the Dialog
   * @param conflictFiles   Files with conflicts
   * @param message         Message shown
   * @param questionMessage Question for the user
   * @return The option chosen by the user.
   */
  public static int showMessage(String title, List<String> conflictFiles, String message,
      String questionMessage) {
    FileStatusDialog pullStatusAndFilesDialog = new FileStatusDialog(title, conflictFiles, message,
        questionMessage);

    pullStatusAndFilesDialog.pack();
    pullStatusAndFilesDialog.setVisible(true);

    return pullStatusAndFilesDialog.getResult();
  }
}
