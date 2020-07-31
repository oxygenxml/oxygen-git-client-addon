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
import com.oxygenxml.git.watcher.RemoteTrackingAction;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

/**
 * Plugin option page extension.
 */
public class OxygenGitOptionPagePluginExtension extends OptionPagePluginExtension{
  /**
   * CheckBox for the option to notify the user as soon as there are new commits.
   */
  private JRadioButton alwaysCheckBox;
  
  /**
   * CheckBox for the option to notify the user when there is a change in the opened files.
   */
  private JRadioButton onChangeCheckBox;
  
  /**
   * CheckBock for the option to never notify the user when there are new commits.
   */
  private JRadioButton neverCheckBox;
  
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
    String warnOnUpstream = RemoteTrackingAction.WARN_UPSTREAM_NEVER;
    if (alwaysCheckBox.isSelected()) {
      warnOnUpstream = RemoteTrackingAction.WARN_UPSTREAM_ALWAYS;
    } else if (onChangeCheckBox.isSelected()) {
      warnOnUpstream = RemoteTrackingAction.WARN_UPSTREAM_ON_CHANGE;
    } 
    
    optionsManager.setWarnOnUpstreamChange(warnOnUpstream);
    optionsManager.saveOptions();
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    neverCheckBox.setSelected(true);
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
    
    //Create a ButtonGroup so that there will be always only one option selected.
    ButtonGroup group = new ButtonGroup();

    //Add the alwayCheckBox option that notifies us as soon as there are any remote changes.
    c.gridy ++;
    c.insets = new Insets(0, 0, 0, 0);
    alwaysCheckBox = new JRadioButton(translator.getTranslation(Tags.ALWAYS_NOTIFY_ON_NEW_COMMITS));
    group.add(alwaysCheckBox);
    panel.add(alwaysCheckBox, c);
    
    //Add the onChangeCheckBox option that looks for remote changes in the files opened in the editing areas.
    c.gridy ++;
    onChangeCheckBox = new JRadioButton(translator.getTranslation(Tags.NOTIFY_ON_POSSIBLE_CONFLICTS));
    group.add(onChangeCheckBox);
    panel.add(onChangeCheckBox, c);
    
    //Add the neverCheckBox option that never looks for remote changes.
    c.gridy ++;
    neverCheckBox = new JRadioButton(translator.getTranslation(Tags.NEVER_NOTIFY_ON_NEW_COMMITS));
    group.add(neverCheckBox);
    panel.add(neverCheckBox, c);
    
    c.gridx = 0;
    c.gridy ++;
    c.gridwidth = 3;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    panel.add(new JPanel(), c);

    //Set the initial state of the option.
    String warnOnUpstreamChange = optionsManager.getWarnOnUpstreamChange();

    if (RemoteTrackingAction.WARN_UPSTREAM_ALWAYS.equals(warnOnUpstreamChange)) {
      alwaysCheckBox.setSelected(true);
    } else if (RemoteTrackingAction.WARN_UPSTREAM_ON_CHANGE.equals(warnOnUpstreamChange)) {
      onChangeCheckBox.setSelected(true);
    } else {
      neverCheckBox.setSelected(true);
    }
    
    return panel;
  }
}
