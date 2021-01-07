  package com.oxygenxml.git;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.xml.bind.annotation.XmlEnum;

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
   * What to do when detecting a repository inside a newly opened project.
   */
  @XmlEnum
  public enum WhenRepoDetectedInProject {
    /**
     * Automatically switch to the new working copy.
     */
    AUTO_SWITCH_TO_WC,
    /**
     * Ask if Oxygen should switch to the new working copy.
     */
    ASK_TO_SWITCH_TO_WC,
    /**
     * Do nothing
     */
    DO_NOTHING
  }
  /**
   * Page key.
   */
  public static final String KEY = "Git_client_plugin_preferences_page";
  /**
   * CheckBox for the option to notify the user about new commits in the remote.
   */
  private JCheckBox notifyAboutRemoteCommitsCheckBox;
  /**
   * The OptionsManager instance
   */
  private static OptionsManager optionsManager = OptionsManager.getInstance();
  /**
   * The Translator instance
   */
  private static Translator translator = Translator.getInstance();
  /**
   * Automatically switch to the new working copy when detecting a repo inside a project.
   */
  private JRadioButton autoSwitchToWCRadio;
  /**
   * Ask if Oxygen should switch to the new working copy when detecting a repo inside a project.
   */
  private JRadioButton askToSwitchToWCRadio;
  /**
   * Do nothing when detecting a repo inside a project.
   */
  private JRadioButton doNothingRadio;
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    JPanel mainPanel = new JPanel(new GridBagLayout());
    
    // Repo detected in project settings
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.weighty = 0;
    c.anchor = GridBagConstraints.LINE_START;
    mainPanel.add(createRepoDetectedInProjectSettingsPanel(), c);
    
    // Option that notifies us when commits are detected in the remote branch
    c.gridx = 0;
    c.gridy ++;
    c.weightx = 0;
    c.weighty = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.insets = new Insets(15, 0, 0, 0);
    notifyAboutRemoteCommitsCheckBox = new JCheckBox(translator.getTranslation(Tags.NOTIFY_ON_NEW_COMMITS));
    mainPanel.add(notifyAboutRemoteCommitsCheckBox, c);
    
    // Empty panel to take up the rest of the space
    c.gridx = 0;
    c.gridy ++;
    c.gridwidth = 3;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    mainPanel.add(new JPanel(), c);

    //Set the initial state of the option.
    setOptionsInitialStates();
    
    return mainPanel;
  }

  /**
   * Set the initial states of the options.
   */
  private void setOptionsInitialStates() {
    boolean notifyOnNewRemoteCommits = optionsManager.getNotifyAboutNewRemoteCommits();
    notifyAboutRemoteCommitsCheckBox.setSelected(notifyOnNewRemoteCommits);
    
    WhenRepoDetectedInProject whatToDo = optionsManager.getWhenRepoDetectedInProject();
    switch (whatToDo) {
      case ASK_TO_SWITCH_TO_WC:
        askToSwitchToWCRadio.setSelected(true);
        break;
      case AUTO_SWITCH_TO_WC:
        autoSwitchToWCRadio.setSelected(true);
        break;
      case DO_NOTHING:
        doNothingRadio.setSelected(true);
        break;
      default:
        break;
    }
  }

  /**
   * Create the panel that contains the settings related to what should happen
   * when a repository is detected at project-loading time.
   * 
   * @return the panel, of course.
   */
  private JPanel createRepoDetectedInProjectSettingsPanel() {
    JPanel repoInProjectSettingsPanel = new JPanel(new GridLayout(4, 1));
    
    JLabel label = new JLabel(translator.getTranslation(Tags.WHEN_DETECTING_REPO_IN_PROJECT));
    repoInProjectSettingsPanel.add(label);
    
    ButtonGroup buttonGroup = new ButtonGroup();
    autoSwitchToWCRadio = new JRadioButton(translator.getTranslation(Tags.ALWAYS_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(autoSwitchToWCRadio);
    repoInProjectSettingsPanel.add(autoSwitchToWCRadio);
    
    doNothingRadio = new JRadioButton(translator.getTranslation(Tags.NEVER_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(doNothingRadio);
    repoInProjectSettingsPanel.add(doNothingRadio);
    
    askToSwitchToWCRadio = new JRadioButton(translator.getTranslation(Tags.ASK_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(askToSwitchToWCRadio);
    repoInProjectSettingsPanel.add(askToSwitchToWCRadio);
    
    return repoInProjectSettingsPanel;
  }
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    optionsManager.setNotifyAboutNewRemoteCommits(notifyAboutRemoteCommitsCheckBox.isSelected());
    
    WhenRepoDetectedInProject whatToDo = WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC;
    if (autoSwitchToWCRadio.isSelected()) {
      whatToDo = WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC;
    } else if (doNothingRadio.isSelected()) {
      whatToDo = WhenRepoDetectedInProject.DO_NOTHING;
    }
    optionsManager.setWhenRepoDetectedInProject(whatToDo);
    
    optionsManager.saveOptions();
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    notifyAboutRemoteCommitsCheckBox.setSelected(false);
    askToSwitchToWCRadio.setSelected(true);
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return translator.getTranslation(Tags.GIT_CLIENT);
  }
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getKey()
   */
  @Override
  public String getKey() {
    return KEY;
  }
}
