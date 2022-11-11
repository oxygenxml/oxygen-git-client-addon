package com.oxygenxml.git;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.oxygenxml.git.auth.sshagent.SSHAgent;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
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
   * Page key.
   */
  public static final String KEY = "Git_client_plugin_ssh_support_page";

  /**
   * The OptionsManager instance
   */
  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  /**
   * When selected, use the SSH support.
   */
  private JCheckBox useSshSupport;
  
  /**
   * Radio button for Pageant SSH agent.
   */
  private JRadioButton usePageantSshAgent;
  
  /**
   * Radio button for Win32 SSH Agent.
   */
  private JRadioButton useWin32SshAgent;
  
  /**
   * <code>true</code> if the page is for Windows.
   */
  private boolean isForWin = false;

  /**
   * The buttons group.
   */
  private ButtonGroup buttonsGroup;


  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints constraints = new GridBagConstraints();

    addUseSshSupportOption(mainPanel, constraints);
    isForWin = PlatformDetectionUtil.isWin();
    if(isForWin) {
      addDefaultSshAgentSelector(mainPanel, constraints);
    }
    addEmptySpace(mainPanel, constraints);

    setInitialStates();

    return mainPanel;
  }

  /**
   * Initialize all components.
   */
  private void setInitialStates() {
    useSshSupport.setSelected(OPTIONS_MANAGER.getUseSshAgent());
    final String sshAgentName = OPTIONS_MANAGER.getDefaultSshAgent();
    final SSHAgent sshAgent = SSHAgent.getByName(sshAgentName);
    if(isForWin) {
      if(SSHAgent.isForWin(sshAgent)) {
        buttonsGroup.setSelected(sshAgent == SSHAgent.WIN_PAGEANT ? usePageantSshAgent.getModel() : useWin32SshAgent.getModel(), true);
      } else {
        buttonsGroup.setSelected(useWin32SshAgent.getModel(), true);
      }
    } 
  }

  /**
   * Add and create the combo for SSH agent selection.
   * 
   * @param mainPanel    The main panel.
   * @param constraints  The constraints.
   */
  private void addDefaultSshAgentSelector(JPanel mainPanel, GridBagConstraints constraints) {
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridheight = 1;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.weightx = 0.2;
    constraints.weighty = 0;
    constraints.insets = new Insets(0, NESTED_OPTION_INSET, 0, 0);
    
    usePageantSshAgent = new JRadioButton(SSHAgent.WIN_PAGEANT.getName());
    useWin32SshAgent   = new JRadioButton(SSHAgent.WIN_WIN32_OPENSSH.getName());
    
    usePageantSshAgent.addItemListener(l -> {
      if(usePageantSshAgent.isSelected() && !useSshSupport.isSelected()) {
        useSshSupport.setSelected(true);
      }
    });
    
    useWin32SshAgent.addItemListener(l -> {
      if(useWin32SshAgent.isSelected() && !useSshSupport.isSelected()) {
        useSshSupport.setSelected(true);
      }
    });
    
    buttonsGroup = new ButtonGroup();
    buttonsGroup.add(usePageantSshAgent);
    buttonsGroup.add(useWin32SshAgent);
      
    mainPanel.add(usePageantSshAgent, constraints);
    constraints.gridy++;
    mainPanel.add(useWin32SshAgent, constraints);
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

    useSshSupport = new JCheckBox(Translator.getInstance().getTranslation(Tags.USE_SSH_SUPPORT));
    useSshSupport.addItemListener(event -> updateDefaultSshAgentUsed());

    mainPanel.add(useSshSupport, constraints);

  }

  /**
   * Update the selection after use SSH support check box selection change.
   */
  private void updateDefaultSshAgentUsed() {
    if(isForWin && usePageantSshAgent != null && useWin32SshAgent != null) {
      if(!useSshSupport.isSelected()) {
        buttonsGroup.clearSelection();
      } else {
        if(usePageantSshAgent.isSelected() || useWin32SshAgent.isSelected()) {
          return;
        }
        final String defaultSshAgent = OPTIONS_MANAGER.getDefaultSshAgent();
        if(SSHAgent.isForWin(SSHAgent.getByName(defaultSshAgent))) {
          buttonsGroup.setSelected(SSHAgent.WIN_PAGEANT.getName().equals(defaultSshAgent) ? usePageantSshAgent.getModel() : useWin32SshAgent.getModel(), true);
        } else {
          buttonsGroup.setSelected(useWin32SshAgent.getModel(), true);
        }
      }
    }
  }


  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    final boolean useSshSupportSelected = useSshSupport.isSelected();
    OPTIONS_MANAGER.setUseSshAgent(useSshSupportSelected);
    if(useSshSupportSelected) {
      if(isForWin) {
        OPTIONS_MANAGER.setDefaultSshAgent(usePageantSshAgent.isSelected() ? SSHAgent.WIN_PAGEANT.getName() : SSHAgent.WIN_WIN32_OPENSSH.getName());
      } else {
        OPTIONS_MANAGER.setDefaultSshAgent(SSHAgent.UNIX_DEFAULT_SSH_AGENT.getName());
      }
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    useSshSupport.setSelected(true);
    if(isForWin) {
      usePageantSshAgent.setSelected(false);
      useWin32SshAgent.setSelected(true);
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return Translator.getInstance().getTranslation(Tags.SSH_CONNECTIONS);
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
