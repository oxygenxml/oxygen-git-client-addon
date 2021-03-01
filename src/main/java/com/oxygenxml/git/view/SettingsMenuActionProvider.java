package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitRefreshSupport;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Action provider for the Settings menu button.
 */
final class SettingsMenuActionProvider {
  /**
   * i18n.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Refresh support.
   */
  private GitRefreshSupport refreshSupport;

  /**
   * Constructor.
   * 
   * @param refreshSupport Refresh support.
   */
  public SettingsMenuActionProvider(GitRefreshSupport refreshSupport) {
    this.refreshSupport = refreshSupport;
  }
  
  /**
   * @return the "Reset all credentials" action;
   */
  public AbstractAction createAndGetResetCredentialsAction() {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.RESET_ALL_CREDENTIALS)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        int result = PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
            TRANSLATOR.getTranslation(Tags.RESET_ALL_CREDENTIALS),
            TRANSLATOR.getTranslation(Tags.RESET_CREDENTIALS_CONFIRM_MESAGE),
            new String[] {
                "   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
                "   " + TRANSLATOR.getTranslation(Tags.NO) + "   " },
            new int[] {1, 0});
        if (result == 1) {
          OptionsManager optManager = OptionsManager.getInstance();
          optManager.saveSshPassphare(null);
          optManager.saveGitCredentials(null);
          optManager.saveOptions();
          
          refreshSupport.call();
        }
      }
    };
  }
  
  /**
   * @return the "Preferences" action.
   */
  public AbstractAction createAndGetGoToPreferencesAction() {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.PREFERENCES)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        PluginWorkspaceProvider.getPluginWorkspace().showPreferencesPages(
            new String[] {OxygenGitOptionPagePluginExtension.KEY},
            OxygenGitOptionPagePluginExtension.KEY,
            true);
      }
    };
  }
  
}
