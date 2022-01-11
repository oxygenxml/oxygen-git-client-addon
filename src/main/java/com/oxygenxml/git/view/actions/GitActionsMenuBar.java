package com.oxygenxml.git.view.actions;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Menu bar for Git.
 * 
 * @author Alex_Smarandache
 */
public class GitActionsMenuBar implements MenuBarCustomizer, UpdateActionsStatesListener {
  
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The Git menu.
	 */
	private final JMenu gitMenu = OxygenUIComponentsFactory.createMenu(TRANSLATOR.getTranslation(Tags.GIT));

	/**
	 * The git actions manager.
	 */
	private final GitActionsManager actionsManager;

	/**
	 * The pull menu item.
	 */
	private JMenu pullMenuItem;

	/**
	 * Settings menu. Used in tests
	 */
	private JMenu settingsMenu;

	/**
	 * Private constructor to avoid instantiation.
	 */
	public GitActionsMenuBar(@NonNull final GitActionsManager gitActionsManager) {
		this.actionsManager = gitActionsManager;

		gitMenu.setMnemonic(KeyEvent.VK_G);
		
		// Add clone repository item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getCloneRepositoryAction()));

		// Add open repository action.
		gitMenu.add(actionsManager.getOpenRepositoryAction());

		// Add push menu item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getPushAction()));

		// Add pull options
		pullMenuItem = OxygenUIComponentsFactory.createMenu(TRANSLATOR.getTranslation(Tags.PULL));
		pullMenuItem.setIcon(Icons.getIcon(Icons.GIT_PULL_ICON));
		pullMenuItem.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getPullMergeAction()));
		pullMenuItem.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getPullRebaseAction()));
		pullMenuItem.setEnabled(isPullButtonEnabled());
		gitMenu.add(pullMenuItem);

		// Add show staging item
		gitMenu.addSeparator();
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getShowStagingAction()));

		// Add show branches item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getShowBranchesAction()));

		// Add show tags item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getShowTagsAction()));

		// Add show history item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getShowHistoryAction()));

		// Add submodules item
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getSubmoduleAction()));

		// Add stash actions
		gitMenu.addSeparator();	
		JMenuItem stashChangesMenuItem = OxygenUIComponentsFactory.createMenuItem(actionsManager.getStashChangesAction());
		stashChangesMenuItem.setIcon(Icons.getIcon(Icons.STASH_ICON));
		gitMenu.add(stashChangesMenuItem);
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getListStashesAction()));

		// Add remote actions
		gitMenu.addSeparator();
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getManageRemoteRepositoriesAction()));
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getTrackRemoteBranchAction()));
		gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getEditConfigAction()));

		// Add settings actions
		gitMenu.addSeparator();
		settingsMenu = OxygenUIComponentsFactory.createMenu(TRANSLATOR.getTranslation(Tags.SETTINGS));
		settingsMenu.setIcon(Icons.getIcon(Icons.SETTINGS));
		settingsMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getResetAllCredentialsAction()));
		settingsMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getOpenPreferencesAction()));

		gitMenu.add(settingsMenu);
	}


	/**
	 * Add git menu after "Tools" menu.
	 */
	@Override
	public void customizeMainMenu(JMenuBar mainMenu) {
		for (int i = 0; i < mainMenu.getMenuCount(); i++) {
			if (TRANSLATOR.getTranslation(Tags.TOOLS).equals(mainMenu.getMenu(i).getText())) {
				mainMenu.add(gitMenu, i + 1);
				break;
			}
		}
	}

	@Override
	public void updateButtonStates() {
	  SwingUtilities.invokeLater(() -> pullMenuItem.setEnabled(isPullButtonEnabled()));
	}

	/**
	 * @return The git actions manager.
	 */
	public GitActionsManager getGitActionsManager() {
		return actionsManager;
	}
	
  /**
   * @return <code>true</code> is the pull button is enabled. 
   * Any of the inner actions of the button are enabled. 
   */
  private boolean isPullButtonEnabled() {
    return actionsManager.getPullMergeAction().isEnabled() || actionsManager.getPullRebaseAction().isEnabled();
  }
	
	/**
	 * !!! Used for tests. !!!
	 * 
	 * @return The settings menu.
	 */
	public JMenu getSettingsMenu() {
		return settingsMenu;
	}

}
