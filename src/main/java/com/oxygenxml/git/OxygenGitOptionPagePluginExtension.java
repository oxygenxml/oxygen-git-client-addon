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

import com.oxygenxml.git.options.OptionTags;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

/**
 * Plugin option page extension.
 */
public class OxygenGitOptionPagePluginExtension extends OptionPagePluginExtension {
  
  /**
   * Inset value for nested/subordinated options. 
   */
  private static final int NESTED_OPTION_INSET = 15;

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
   * If is selected, the files will be validated before commit.
   */
  private JCheckBox validateBeforeCommit;
  
  /**
   * If is selected, the commit will be rejected if validation problems occurs.
   */
  private JCheckBox rejectCommitOnValidationProblems;
  
  /**
   * Update submodules on pull.
   */
  private JCheckBox updateSubmodulesOnPull;
  
  /**
   * CheckBox for the option to notify the user about new commits in the remote.
   */
  private JCheckBox notifyAboutRemoteCommitsCheckBox;
  
  /**
   * The OptionsManager instance
   */
  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();
  
  /**
   * The Translator instance
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
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
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.anchor = GridBagConstraints.LINE_START;
    mainPanel.add(createRepoDetectedInProjectSettingsPanel(), constraints);
    
    // Option that notifies us when commits are detected in the remote branch
    constraints.gridx = 0;
    constraints.gridy ++;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.insets = new Insets(NESTED_OPTION_INSET, 0, 0, 0);
    notifyAboutRemoteCommitsCheckBox = new JCheckBox(
        TRANSLATOR.getTranslation(Tags.NOTIFY_ON_NEW_COMMITS));
    mainPanel.add(notifyAboutRemoteCommitsCheckBox, constraints);
    
    // Option that notifies us when commits are detected in the remote branch
    constraints.gridy ++;
    updateSubmodulesOnPull = new JCheckBox(TRANSLATOR.getTranslation(
        Tags.UPDATE_SUBMODULES_ON_PULL));
    mainPanel.add(updateSubmodulesOnPull, constraints);

    // Option to validate files before commit
    constraints.gridy ++;
    validateBeforeCommit = new JCheckBox(
        TRANSLATOR.getTranslation(Tags.VALIDATE_BEFORE_COMMIT));
    mainPanel.add(validateBeforeCommit, constraints);
    
    // Option to reject commit when problems occurs
    constraints.insets = new Insets(0, NESTED_OPTION_INSET, 0, 0);
    constraints.gridy ++;
    rejectCommitOnValidationProblems = new JCheckBox(
        TRANSLATOR.getTranslation(Tags.REJECT_COMMIT_ON_PROBLEMS));
    mainPanel.add(rejectCommitOnValidationProblems, constraints);
    validateBeforeCommit.addItemListener(event -> {
      rejectCommitOnValidationProblems.setEnabled(validateBeforeCommit.isSelected());
    });
    
    // Empty panel to take up the rest of the space
    constraints.gridx = 0;
    constraints.gridy ++;
    constraints.gridwidth = 3; // NOSONAR checkstyle:MagicNumberCheckSyncro
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.fill = GridBagConstraints.BOTH;
    mainPanel.add(new JPanel(), constraints);

    //Set the initial state of the option.
    setOptionsInitialStates();
    
    return mainPanel;
  }

  /**
   * Set the initial states of the options.
   */
  private void setOptionsInitialStates() {
    boolean notifyOnNewRemoteCommits = OPTIONS_MANAGER.isNotifyAboutNewRemoteCommits();
    notifyAboutRemoteCommitsCheckBox.setSelected(notifyOnNewRemoteCommits);
    
    boolean updateSubmodules = OPTIONS_MANAGER.getUpdateSubmodulesOnPull();
    updateSubmodulesOnPull.setSelected(updateSubmodules);

    WhenRepoDetectedInProject whatToDo = OPTIONS_MANAGER.getWhenRepoDetectedInProject();
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
    
    JLabel label = new JLabel(TRANSLATOR.getTranslation(Tags.WHEN_DETECTING_REPO_IN_PROJECT));
    repoInProjectSettingsPanel.add(label);
    
    ButtonGroup buttonGroup = new ButtonGroup();
    autoSwitchToWCRadio = new JRadioButton(TRANSLATOR.getTranslation(Tags.ALWAYS_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(autoSwitchToWCRadio);
    repoInProjectSettingsPanel.add(autoSwitchToWCRadio);
    
    doNothingRadio = new JRadioButton(TRANSLATOR.getTranslation(Tags.NEVER_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(doNothingRadio);
    repoInProjectSettingsPanel.add(doNothingRadio);
    
    askToSwitchToWCRadio = new JRadioButton(TRANSLATOR.getTranslation(Tags.ASK_SWITCH_TO_DETECTED_WORKING_COPY));
    buttonGroup.add(askToSwitchToWCRadio);
    repoInProjectSettingsPanel.add(askToSwitchToWCRadio);
    
    return repoInProjectSettingsPanel;
  }
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    OPTIONS_MANAGER.setNotifyAboutNewRemoteCommits(notifyAboutRemoteCommitsCheckBox.isSelected());
    OPTIONS_MANAGER.setUpdateSubmodulesOnPull(updateSubmodulesOnPull.isSelected());

    WhenRepoDetectedInProject whatToDo = WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC;
    if (autoSwitchToWCRadio.isSelected()) {
      whatToDo = WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC;
    } else if (doNothingRadio.isSelected()) {
      whatToDo = WhenRepoDetectedInProject.DO_NOTHING;
    }
    OPTIONS_MANAGER.setWhenRepoDetectedInProject(whatToDo);
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    notifyAboutRemoteCommitsCheckBox.setSelected(false);
    updateSubmodulesOnPull.setSelected(true);
    askToSwitchToWCRadio.setSelected(true);
    validateBeforeCommit.setSelected(true);
    rejectCommitOnValidationProblems.setSelected(false);
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return TRANSLATOR.getTranslation(Tags.GIT_CLIENT);
  }
  
  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getKey()
   */
  @Override
  public String getKey() {
    return KEY;
  }
  /**
   * The options that will be saved inside the project file when this page is switched to project level
   * inside the preferences dialog.
   * 
   * @return The options presented in this page.
   * 
   * @since 24.0
   */
  public String[] getProjectLevelOptionKeys() {
    return new String[] {
        OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS,
        OptionTags.WHEN_REPO_DETECTED_IN_PROJECT,
        OptionTags.UPDATE_SUBMODULES_ON_PULL,
    };
  }
}
