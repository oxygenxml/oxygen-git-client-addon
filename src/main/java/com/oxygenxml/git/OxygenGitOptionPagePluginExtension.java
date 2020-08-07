  package com.oxygenxml.git;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

/**
 * Plugin option page extension.
 */
public class OxygenGitOptionPagePluginExtension extends OptionPagePluginExtension{
  /**
   * CheckBox for the option to notify the user about new commits in the remote.
   */
  private JRadioButton warnOnNewCommitsCheckBox;
  
  /**
   * The OptionsManager instance
   */
  private final OptionsManager optionsManager = OptionsManager.getInstance();
  
  /**
   * The Translator instance
   */
  private final Translator translator = Translator.getInstance();
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    boolean warnOnUpstream = warnOnNewCommitsCheckBox.isSelected();
    
    optionsManager.setWarnOnUpstreamChange(warnOnUpstream);
    optionsManager.saveOptions();
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    warnOnNewCommitsCheckBox.setSelected(false);
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return "Git Client";
  }
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    GridBagConstraints c = new GridBagConstraints();
    JPanel panel = new JPanel(new GridBagLayout());
    
    //Add a label that asks us when do we want to see if there are remote changes for the repository.
    JLabel whatcherTypeLbl = new JLabel(translator.getTranslation(Tags.WHEN_TO_NOTIFY_ON_NEW_COMMITS));
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.weighty = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.insets = new Insets(0, 0, 5, 0);
    panel.add(whatcherTypeLbl, c);
    
    //Add the warnOnNewCommitsCheckBox option that notifies us as soon as there are any remote changes.
    c.gridy ++;
    c.insets = new Insets(0, 0, 0, 0);
    warnOnNewCommitsCheckBox = new JRadioButton(translator.getTranslation(Tags.NOTIFY_ON_NEW_COMMITS));
    panel.add(warnOnNewCommitsCheckBox, c);
    
    c.gridx = 0;
    c.gridy ++;
    c.gridwidth = 3;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    panel.add(new JPanel(), c);

    //Set the initial state of the option.
    boolean warnOnUpstreamChange = optionsManager.getWarnOnUpstreamChange();
    warnOnNewCommitsCheckBox.setSelected(warnOnUpstreamChange);
    
    return panel;
  }
}
