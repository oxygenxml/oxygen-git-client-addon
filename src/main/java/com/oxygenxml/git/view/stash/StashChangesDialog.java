package com.oxygenxml.git.view.stash;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;

/**
 * Dialog shown when stashing changes.
 * 
 * @author Alex Smarandache
 */
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
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_WITH_COMMA_PATTERN);
  
  /**
   * When is selected, the stash will include the untracked files.
   */
  private final JCheckBox includeUntrackedCheckBox = new JCheckBox(TRANSLATOR.getTranslation(Tags.INCLUDE_UNTRACKED));

  /**
   * The tag option for include untracked files.
   */
  // TODO: This should be moved in a class where all Tags will reside
  private static final String OPTION_TAG_INCLUDE_UNTRACKED = "Stash.should.include.untracked.files";

  /**
   * Public constructor.
   */
  public StashChangesDialog() {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null,
        TRANSLATOR.getTranslation(Tags.STASH_CHANGES),
        true);
    
    setOkButtonText(TRANSLATOR.getTranslation(Tags.STASH));

    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel);
    add(panel);
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
  }

  
  /**
   * Adds the elements to the user interface/
   * 
   * @param panel         The panel in which the components are added.
   */
  private void createGUI(JPanel panel) {
    
    // Informative label message.
    JLabel informativeLabel = new JLabel(TextFormatUtil.toHTML(TRANSLATOR.getTranslation(Tags.STASH_INFORMATIVE_MESSAGE)));
    GridBagConstraints constrains = new GridBagConstraints();
    constrains.gridx = 0;
    constrains.gridy = 0;
    constrains.weightx = 0;
    constrains.weighty = 0;
    constrains.fill = GridBagConstraints.NONE;
    constrains.anchor = GridBagConstraints.WEST;
    constrains.insets = new Insets(0, 0, 3, 0);
    panel.add(informativeLabel, constrains);
    
    JLabel addDescriptionMessage = new JLabel(TextFormatUtil.toHTML(TRANSLATOR.getTranslation(Tags.STASH_ADD_DESCRIPTION)));
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
    constrains.insets = new Insets(3, 0, 0, 0);
    panel.add(stashDescriptionField, constrains);

    addDescriptionMessage.setLabelFor(stashDescriptionField);

    constrains.gridy++;
    constrains.weightx = 0;
    constrains.fill = GridBagConstraints.NONE;
    constrains.insets = new Insets(5, 0, 0, 0);
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    includeUntrackedCheckBox.setSelected(
        Boolean.parseBoolean(optionsStorage.getOption(OPTION_TAG_INCLUDE_UNTRACKED, Boolean.toString(true))));
    includeUntrackedCheckBox.addItemListener(
        e -> optionsStorage.setOption(
            OPTION_TAG_INCLUDE_UNTRACKED,
            Boolean.toString(includeUntrackedCheckBox.isSelected())));
    panel.add(includeUntrackedCheckBox, constrains);
    
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


  /**
   * @return <code>true</code> if the stash should include the untracked files.
   */
  public boolean shouldIncludeUntracked() {
    return includeUntrackedCheckBox.isSelected();
  }

}


