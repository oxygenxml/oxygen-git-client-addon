package com.oxygenxml.git.view.stash;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;

public class StashChangesDialog extends OKCancelDialog {

  /**
   * Translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * A text field for the tag title.
   */
  private JTextField stashDescriptionField;
  
  /**
   * The date format.
   */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm");
 
  
  /**
   * Public constructor.
   * 
   * @param title            The title of the dialog.
   */
  public StashChangesDialog(
      String title) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, title, true);
    
    setOkButtonText(TRANSLATOR.getTranslation(Tags.STASH));

    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel);
    this.add(panel);
    setResizable(true);
    pack();
    
    stashDescriptionField.requestFocus();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  
    setMinimumSize(getPreferredSize());
    Dimension maximumSize = getMaximumSize();
    maximumSize.height = getPreferredSize().height;
    setMaximumSize(maximumSize);
    pack();
    setVisible(true);
    
    
    
  }

  
  /**
   * Adds the elements to the user interface/
   * 
   * @param panel         The panel in which the components are added.
   */
  private void createGUI(JPanel panel) {
    
    // Informative label message.
    JLabel informativeLabel = new JLabel("<html>" + TRANSLATOR.getTranslation(Tags.STASH_INFORMATIVE_MESSAGE) + "</html>");
    GridBagConstraints constrains = new GridBagConstraints();
    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.weightx = 1;
    constrains.weighty = 0;
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.anchor = GridBagConstraints.WEST;
    constrains.insets = new Insets(0, 0, 3, 0);
    panel.add(informativeLabel, constrains);
    
    JLabel addDescriptionMessage = new JLabel("<html>" + TRANSLATOR.getTranslation(Tags.STASH_ADD_DESCRIPTION) + ": </html>");
    constrains.gridy++;
    panel.add(addDescriptionMessage, constrains);

    // Stash description field.
    stashDescriptionField = new TextField();
    String description = "WIP on " 
        + GitAccess.getInstance().getBranchInfo().getBranchName() 
        + " [" 
        + dateFormat.format(new Date())
        + "]";
    stashDescriptionField.setText(description);
    stashDescriptionField.selectAll();
    stashDescriptionField.setPreferredSize(new Dimension(200, stashDescriptionField.getPreferredSize().height));
    constrains.gridy ++;
    constrains.weightx = 1;
    constrains.fill = GridBagConstraints.HORIZONTAL;
    constrains.insets = new Insets(2, 0, 0, 0);
    panel.add(stashDescriptionField, constrains);

    addDescriptionMessage.setLabelFor(stashDescriptionField);
    
    constrains.insets = new Insets(0, 0, 0, 0);
    constrains.fill = GridBagConstraints.BOTH;
    constrains.weighty = 1;
    constrains.gridy ++;
    panel.add(new JPanel(), constrains);
  }

  
  /**
   * Gets the stash message to be set for the new stashed changes.
   * 
   * @return The name for the tag.
   */
  public String getStashMessage() {
    return stashDescriptionField.getText();
  }
  
}


