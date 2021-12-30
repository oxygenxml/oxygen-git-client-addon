package com.oxygenxml.git.view.actions;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.refresh.IRefreshable;

import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Menu bar for Git.
 * 
 * @author Alex_Smarandache
 *
 */
public class GitActionsMenuBar implements MenuBarCustomizer, IRefreshable {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The Git menu.
	 */
	private final JMenu gitMenu;

	/**
	 * Used to generate the disabled icons for menu actions.
	 */
	private final JButton iconFactory;

	/**
	 * The git actions manager.
	 */
	private GitActionsManager gitActionsManager;

	/**
	 * The pull menu item.
	 */
	private JMenu pullMenuItem;
	
	/**
	 * Settings menu. Used in tests
	 */
	private JMenu settingsMenu;



	/**
	 * Hidden constructor.
	 */
	private GitActionsMenuBar() {
		gitMenu = OxygenUIComponentsFactory.createMenu(TRANSLATOR.getTranslation(Tags.GIT));
		iconFactory = new Button("");
	}


	/**
	 * Helper class for singleton pattern.
	 * 
	 * @author alex_smarandache
	 *
	 */
	private static class SingletonHelper {

		/**
		 * The unique instance of Git menu bae.
		 */
		static final GitActionsMenuBar INSTANCE = new GitActionsMenuBar();

	}



	/**
	 * @return The singleton instance.
	 */
	public static GitActionsMenuBar getInstance() {
		return SingletonHelper.INSTANCE;
	}


	/**
	 * Populate menu with actions from git actions manager. 
	 * <br><br>
	 * This method will remove any previous component from this menu.
	 * 
	 * @param gitActionsManager The manager for git actions.
	 */
	public void populateMenu(final GitActionsManager gitActionsManager) {

		if(this.gitActionsManager != null) {
			this.gitActionsManager.removeRefreshable(this);
		}

		gitMenu.removeAll();

		gitActionsManager.addRefreshable(this);
		this.gitActionsManager = gitActionsManager;

		// Add clone repository item
		final JMenuItem cloneRepositoryMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getCloneRepositoryAction());
		cloneRepositoryMenuItem.setIcon(Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON));
		cloneRepositoryMenuItem
		.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON)));
		gitMenu.add(cloneRepositoryMenuItem);

		// Add open repository action.
		gitMenu.add(gitActionsManager.getOpenRepositoryAction());

		// Add push menu item
		final JMenuItem pushMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getPushAction());
		gitActionsManager.getPushAction().setEnabled(false);
		pushMenuItem.setIcon(Icons.getIcon(Icons.GIT_PUSH_ICON));
		pushMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_PUSH_ICON)));
		gitMenu.add(pushMenuItem);

		// Add pull options
		pullMenuItem = OxygenUIComponentsFactory.createMenu(
				TRANSLATOR.getTranslation(Tags.PULL));
		pullMenuItem.setIcon(Icons.getIcon(Icons.GIT_PULL_ICON));
		pullMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_PULL_ICON)));
		pullMenuItem.add(OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getPullMergeAction()));
		pullMenuItem.add(OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getPullRebaseAction()));
		pullMenuItem.setEnabled(gitActionsManager.getPullMergeAction().isEnabled() 
				|| gitActionsManager.getPullRebaseAction().isEnabled());

		gitMenu.add(pullMenuItem);

		// Add show staging item
		gitMenu.addSeparator();
		final JMenuItem showStagingMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getShowStagingAction());
		showStagingMenuItem.setIcon(Icons.getIcon(ro.sync.ui.Icons.GIT_PLUGIN_ICON_MENU));
		showStagingMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(ro.sync.ui.Icons.GIT_PLUGIN_ICON_MENU)));
		gitMenu.add(showStagingMenuItem);

		// Add show branches item
		final JMenuItem showBranchesMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getShowBranchesAction());
		showBranchesMenuItem.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
		showBranchesMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON)));
		gitMenu.add(showBranchesMenuItem);

		// Add show tags item
		final JMenuItem showTagsMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getShowTagsAction());
		showTagsMenuItem.setIcon(Icons.getIcon(Icons.TAG));
		showTagsMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.TAG)));
		gitMenu.add(showTagsMenuItem);

		// Add show history item
		final JMenuItem showHistoryMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getShowHistoryAction());
		showHistoryMenuItem.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
		showHistoryMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_HISTORY)));
		gitMenu.add(showHistoryMenuItem);

		// Add submodules item
		final JMenuItem submodulesMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getSubmoduleAction());
		submodulesMenuItem.setIcon(Icons.getIcon(Icons.GIT_SUBMODULE_ICON));
		submodulesMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.GIT_SUBMODULE_ICON)));
		gitMenu.add(submodulesMenuItem);

		// Add stash actions
		gitMenu.addSeparator();	
		final JMenuItem stashChangesMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getStashChangesAction());
		stashChangesMenuItem.setIcon(Icons.getIcon(Icons.STASH_ICON));
		stashChangesMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.STASH_ICON)));
		gitMenu.add(stashChangesMenuItem);
		gitMenu.add(gitActionsManager.getListStashesAction());

		// Add remote actions
		gitMenu.addSeparator();
		final JMenuItem manageRemoteMenuItem = OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getManageRemoteRepositoriesAction());
		manageRemoteMenuItem.setIcon(Icons.getIcon(Icons.REMOTE));
		manageRemoteMenuItem.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.REMOTE)));
		gitMenu.add(manageRemoteMenuItem);
		gitMenu.add(gitActionsManager.getTrackRemoteBranchAction());
		gitMenu.add(gitActionsManager.getEditConfigAction());

		// Add settings actions
		gitMenu.addSeparator();
		settingsMenu = OxygenUIComponentsFactory.createMenu(
				TRANSLATOR.getTranslation(Tags.SETTINGS));
		settingsMenu.setIcon(Icons.getIcon(Icons.SETTINGS));
		settingsMenu.setDisabledIcon(getDisabledIcon(Icons.getIcon(Icons.SETTINGS)));
		settingsMenu.add(OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getResetAllCredentialsAction()));
		settingsMenu.add(OxygenUIComponentsFactory.createMenuItem(
				gitActionsManager.getOpenPreferencesAction()));

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


	/**
	 * Used to generate a disable icon for given icon as argument.
	 * 
	 * @param icon The given icon.
	 * 
	 * @return The disabled icon for given icon.
	 */
	private Icon getDisabledIcon(Icon icon) {
		iconFactory.setIcon(icon);
		return iconFactory.getDisabledIcon();
	}


	/**
	 * @param isVisible <code>true</code> if the menu should be visible.
	 */
	public void setVisible(boolean isVisible) {
		gitMenu.setVisible(isVisible);
	}


	/**
	 * @return <code>true</code> if the menu is visible.
	 */
	public boolean isVisible() {
		return gitMenu.isVisible();
	}


	@Override
	public void refresh() {
		if(gitActionsManager != null) {
			pullMenuItem.setEnabled(gitActionsManager.getPullMergeAction().isEnabled() 
					|| gitActionsManager.getPullRebaseAction().isEnabled());
		}
	}


	/**
	 * @return The current Git Actions Manager.
	 */
	public GitActionsManager getGitActionsManager() {
		return gitActionsManager;
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
