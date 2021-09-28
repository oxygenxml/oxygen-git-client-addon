package com.oxygenxml.git.view.staging;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Action provider for the Settings menu button.
 */
final class SettingsMenuBuilder {
  /**
   * i18n.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Builds the Settings menu button.
   * 
   * @param refreshSupport     Refresh support. Needed by some actions.
   * 
   * @return A button with the settings actions.
   */
  public static SplitMenuButton build(GitRefreshSupport refreshSupport) {
    SplitMenuButton settingsMenuButton = new SplitMenuButton(
        null,
        Icons.getIcon(Icons.SETTINGS),
        false,
        false,
        true,
        false);
    
    settingsMenuButton.addActionToMenu(createResetCredentialsAction(refreshSupport), false);
    settingsMenuButton.addSeparator();
    settingsMenuButton.addActionToMenu(createGoToPreferencesAction(), false);
    
    return settingsMenuButton;
  }
  
  /**
   * Create "Reset all credentials".
   * 
   * @param refreshSupport Needed to perform a refresh after resetting the credentials.
   * 
   * @return the "Reset all credentials" action.
   */
  private static AbstractAction createResetCredentialsAction(GitRefreshSupport refreshSupport) {
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
          
          refreshSupport.call();
        }
      }
    };
  }
  
  /**
   * @return the "Preferences" action.
   */
  private static AbstractAction createGoToPreferencesAction() {
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
