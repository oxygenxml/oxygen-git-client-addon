package com.oxygenxml.git;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.oxygenxml.git.auth.sshagent.SSHAgent;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PlatformDetectionUtil;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

/**
 * A preferences page for SSH support configurations.
 * 
 * @author alex_smarandache
 *
 */
public class SSHSupportOptionPage extends OptionPagePluginExtension {

  /**
   * Inset value for nested/subordinated options. 
   */
  private static final int NESTED_OPTION_INSET = 15;

  /**
   * The left inset of a combo box.
   */
  private static final int COMBO_LEFT_INSET = new JCheckBox().getInsets().left; 

  /**
   * Page key.
   */
  public static final String KEY = "Git_client_plugin_ssh_support_page";

  /**
   * The OptionsManager instance
   */
  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  /**
   * The Translator instance
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * When selected, use the SSH support.
   */
  private JCheckBox useSshSupport;

  /**
   * When SSH support is used, the selected item is also used for SSH agent. 
   */
  private JComboBox<SSHAgent> defaultAgent;


  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints constraints = new GridBagConstraints();

    addUseSshSupportOption(mainPanel, constraints);
    addDefaultSshAgentCombo(mainPanel, constraints);
    addEmptySpace(mainPanel, constraints);

    setInitialStates();

    return mainPanel;
  }

  /**
   * Initialize all components.
   */
  private void setInitialStates() {
    useSshSupport.setSelected(OPTIONS_MANAGER.getUseSshAgent());
    defaultAgent.setEnabled(useSshSupport.isSelected());
    final String sshAgentName = OPTIONS_MANAGER.getDefaultSshAgent();
    final SSHAgent sshAgent = SSHAgent.getByName(sshAgentName);
    if(PlatformDetectionUtil.isWin()) {
      defaultAgent.setSelectedItem(SSHAgent.isForWin(sshAgent) ? sshAgent : SSHAgent.WIN_WIN32_OPENSSH);
    } else {
      defaultAgent.setSelectedItem(SSHAgent.UNIX_DEFAULT_SSH_AGENT);
    }
  }

  /**
   * Add and create the combo for SSH agent selection.
   * 
   * @param mainPanel    The main panel.
   * @param constraints  The constraints.
   */
  private void addDefaultSshAgentCombo(JPanel mainPanel, GridBagConstraints constraints) {
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridheight = 1;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.weightx = 0.2;
    constraints.weighty = 0;
    constraints.insets = new Insets(0, NESTED_OPTION_INSET + COMBO_LEFT_INSET, 0, 0);

    defaultAgent = new JComboBox<>();
    final SSHAgent[] agents = PlatformDetectionUtil.isWin() ? SSHAgent.getWindowsSSHAgents() : SSHAgent.getUnixSSHAgents();
    for(final SSHAgent agent : agents) {
      defaultAgent.addItem(agent);
    }
    
    defaultAgent.setMinimumSize(defaultAgent.getPreferredSize());
  
    mainPanel.add(defaultAgent, constraints);
  }

  /**
   * Add empty space for a better page organization.
   * 
   * @param mainPanel    The main panel.
   * @param constraints  The constraints.
   */
  private void addEmptySpace(JPanel mainPanel, GridBagConstraints constraints) {
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridheight = 3;
    constraints.gridwidth = 3;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.insets = new Insets(0, 0, 0, 0);

    mainPanel.add(new JPanel(), constraints);
  }


  /**
   * Add option to use or not the SSH support.
   * 
   * @param mainPanel    The main panel.
   * @param constraints  The constraints.
   */
  private void addUseSshSupportOption(JPanel mainPanel, GridBagConstraints constraints) {
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridheight = 1;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.insets = new Insets(0, 0, 0, 0);

    useSshSupport = new JCheckBox("Use SSH Agent");
    useSshSupport.addItemListener(event -> defaultAgent.setEnabled(useSshSupport.isSelected()));

    mainPanel.add(useSshSupport, constraints);

  }


  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    OPTIONS_MANAGER.setDefaultSshAgent(defaultAgent.getSelectedItem().toString());
    OPTIONS_MANAGER.setUseSshAgent(useSshSupport.isSelected());
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    useSshSupport.setSelected(true);
    defaultAgent.setSelectedItem(PlatformDetectionUtil.isWin() ? SSHAgent.WIN_WIN32_OPENSSH : SSHAgent.UNIX_DEFAULT_SSH_AGENT);  
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return "SSH Agent";
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
  @Override
  public String[] getProjectLevelOptionKeys() {
    return new String[0];
  }
}
