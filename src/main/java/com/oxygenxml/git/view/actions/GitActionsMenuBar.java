package com.oxygenxml.git.view.actions;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Menu bar for Git.
 * 
 * @author Alex_Smarandache
 *
 */
public class GitActionsMenuBar implements MenuBarCustomizer {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The Git menu.
	 */
	private final JMenu gitMenu;

	/**
	 * Git actions manager.
	 */
	private final GitActionsManager gitActionsManager;

	/**
	 * Constructor.
	 * 
	 * @param gitActionsManager The manager for git actions.
	 */
	public GitActionsMenuBar(final GitActionsManager gitActionsManager) {

		this.gitMenu = OxygenUIComponentsFactory.createMenu("Git");
		this.gitActionsManager = gitActionsManager;

		// Add clone repository item
		final JMenuItem cloneRepositoryMenuItem = new JMenuItem(gitActionsManager.getCloneRepositoryAction());
		cloneRepositoryMenuItem.setIcon(Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON));
		cloneRepositoryMenuItem
				.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON)).getDisabledIcon());
		gitMenu.add(cloneRepositoryMenuItem);

		// Add push menu item
		final JMenuItem pushMenuItem = new JMenuItem(gitActionsManager.getPushAction());
		gitActionsManager.getPushAction().setEnabled(false);
		pushMenuItem.setIcon(Icons.getIcon(Icons.GIT_PUSH_ICON));
		pushMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_PUSH_ICON)).getDisabledIcon());
		gitMenu.add(pushMenuItem);

		// Add pull options
		final JMenu pullMenuItem = new JMenu(TRANSLATOR.getTranslation("Pull"));
		pullMenuItem.setIcon(Icons.getIcon(Icons.GIT_PULL_ICON));
		pullMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_PULL_ICON)).getDisabledIcon());
		pullMenuItem.add(new JMenuItem(gitActionsManager.getPullMergeAction()));
		pullMenuItem.add(new JMenuItem(gitActionsManager.getPullRebaseAction()));
		gitMenu.add(pullMenuItem);

		// Add show branches item
		gitMenu.addSeparator();
		final JMenuItem showBranchesMenuItem = new JMenuItem(gitActionsManager.getShowBranchesAction());
		showBranchesMenuItem.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
		showBranchesMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_BRANCH_ICON)).getDisabledIcon());
		gitMenu.add(showBranchesMenuItem);

		// Add show tags item
		final JMenuItem showTagsMenuItem = new JMenuItem(gitActionsManager.getShowTagsAction());
		showTagsMenuItem.setIcon(Icons.getIcon(Icons.TAG));
		showTagsMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.TAG)).getDisabledIcon());
		gitMenu.add(showTagsMenuItem);

		// Add show history item
		final JMenuItem showHistoryMenuItem = new JMenuItem(gitActionsManager.getShowHistoryAction());
		showHistoryMenuItem.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
		showHistoryMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_HISTORY)).getDisabledIcon());
		gitMenu.add(showHistoryMenuItem);

		// Add submodules item
		final JMenuItem submodulesMenuItem = new JMenuItem(gitActionsManager.getSubmoduleAction());
		submodulesMenuItem.setIcon(Icons.getIcon(Icons.GIT_SUBMODULE_ICON));
		submodulesMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.GIT_SUBMODULE_ICON)).getDisabledIcon());
		gitMenu.add(submodulesMenuItem);

		// Add stash actions
		gitMenu.addSeparator();
		gitMenu.add(gitActionsManager.getStashChangesAction());
		final JMenuItem listStashesMenuItem = new JMenuItem(gitActionsManager.getListStashesAction());
		listStashesMenuItem.setIcon(Icons.getIcon(Icons.STASH_ICON));
		listStashesMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.STASH_ICON)).getDisabledIcon());
		gitMenu.add(listStashesMenuItem);

		// Add remote actions
		gitMenu.addSeparator();
		final JMenuItem manageRemoteMenuItem = new JMenuItem(gitActionsManager.getManageRemoteRepositoriesAction());
		manageRemoteMenuItem.setIcon(Icons.getIcon(Icons.REMOTE));
		manageRemoteMenuItem.setDisabledIcon(new Button(Icons.getIcon(Icons.REMOTE)).getDisabledIcon());
		gitMenu.add(manageRemoteMenuItem);
		gitMenu.add(gitActionsManager.getTrackRemoteBranchAction());
		gitMenu.add(gitActionsManager.getEditConfigAction());

	}

	@Override
	public void customizeMainMenu(JMenuBar mainMenu) {
		for (int i = 0; i < mainMenu.getMenuCount(); i++) {
			if (TRANSLATOR.getTranslation(Tags.TOOLS).equals(mainMenu.getMenu(i).getText())) {
				mainMenu.add(gitMenu, i + 1);
				break;
			}
		}

	}

}
