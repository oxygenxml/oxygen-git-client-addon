package com.oxygenxml.git;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.ui.Icons;

/**
 * Plugin extension - workspace access extension.
 */
public class CustomWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CustomWorkspaceAccessPluginExtension.class);

	/**
	 * ID of the view.
	 */
	private final String GIT_STAGING_VIEW = "GitStagingView";

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */

	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {

		try {
			//PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null);

			CustomAuthenticator.install();

			Translator translator = new TranslatorExtensionImpl();
			StageController stageController = new StageController(GitAccess.getInstance());
			final Refresh refresh = new PanelRefresh(translator);
			final StagingPanel stagingPanel = new StagingPanel(translator, refresh, stageController);
			refresh.setPanel(stagingPanel);

			createProjectViewContextualMenu(pluginWorkspaceAccess, translator, stageController, stagingPanel);

			pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
				/**
				 * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
				 */
				public void customizeView(ViewInfo viewInfo) {

					if (
					// The view ID defined in the "plugin.xml"
					GIT_STAGING_VIEW.equals(viewInfo.getViewID())) {

						viewInfo.setComponent(stagingPanel);
						viewInfo.setIcon(Icons.getIcon(ImageConstants.GIT_ICON));
						viewInfo.setTitle("Git Staging");
					}
				}
			});

			final JFrame parentFrame = (JFrame) pluginWorkspaceAccess.getParentFrame();

			// Present the view to the user if it is the first run of the plugin
			parentFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					// parentFrame.removeComponentListener(this);
					String key = "view.presented.on.first.run";
					String firstRun = pluginWorkspaceAccess.getOptionsStorage().getOption(key, null);
					if (firstRun == null) {
						// This is the first run of the plugin.
						pluginWorkspaceAccess.showView(GIT_STAGING_VIEW, false);
						pluginWorkspaceAccess.getOptionsStorage().setOption(key, "true");
					}
				}

			});

			// Call the refresh command when the Oxygen window is activated
			parentFrame.addWindowListener(new WindowAdapter() {

				private boolean toRefresh = false;

				@Override
				public void windowActivated(WindowEvent e) {
					super.windowActivated(e);
					if (toRefresh) {
						refresh.call();
						toRefresh = false;
					}
				}

				@Override
				public void windowDeactivated(WindowEvent e) {
					super.windowDeactivated(e);
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							Object focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
							if (focusedWindow == null) {
								toRefresh = true;
							}

						}
					});
				}
			});
		} catch (Throwable t) {
			// Runtime exceptions shouldn't affect Oxygen.
			pluginWorkspaceAccess.showErrorMessage(t.getMessage());
			logger.fatal(t, t);
		}
	}

	/**
	 * Creates the contextual menu in the project view
	 * 
	 * @param pluginWorkspaceAccess
	 * @param translator
	 *          - the translator used to translate the menu items
	 * @param stageController
	 *          - used to stage, unstage and commit the files
	 * @param stagingPanel
	 *          - the main view
	 */
	private void createProjectViewContextualMenu(final StandalonePluginWorkspace pluginWorkspaceAccess,
			final Translator translator, final StageController stageController, final StagingPanel stagingPanel) {

		JMenu git = new JMenu(translator.getTraslation(Tags.PROJECT_VIEW_GIT_CONTEXTUAL_MENU_ITEM));
		JMenuItem gitDiff = new JMenuItem(translator.getTraslation(Tags.PROJECT_VIEW_GIT_DIFF_CONTEXTUAL_MENU_ITEM));

		// THE DIFF MENU ITEM
		gitDiff.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				File[] selectedFiles = ProjectManagerEditor.getSelectedFiles(pluginWorkspaceAccess);
				// the diff action is enabled only for one file
				File repository = new File(selectedFiles[0].getAbsolutePath());

				// We try and find the
				while (repository.getParent() != null) {
					if (FileHelper.isGitRepository(repository.getAbsolutePath())) {
						break;
					}
					repository = repository.getParentFile();
				}
				try {
					String previousRepository = OptionsManager.getInstance().getSelectedRepository();
					GitAccess.getInstance().setRepository(repository.getAbsolutePath());
					OptionsManager.getInstance().saveSelectedRepository(repository.getAbsolutePath());
					List<FileStatus> gitFiles = GitAccess.getInstance().getUnstagedFiles();
					gitFiles.addAll(GitAccess.getInstance().getStagedFile());
					String selectedFilePath = selectedFiles[0].getAbsolutePath().replace("\\", "/");
					for (FileStatus fileStatus : gitFiles) {
						if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
							DiffPresenter diff = new DiffPresenter(fileStatus, stageController, translator);
							diff.showDiff();
							break;
						}
					}
					OptionsManager.getInstance().saveSelectedRepository(previousRepository);
				} catch (Exception e1) {
					if (logger.isDebugEnabled()) {
						logger.debug(e1, e1);
					}
				}
			}
		});
		JMenuItem commit = new JMenuItem(translator.getTraslation(Tags.PROJECT_VIEW_COMMIT_CONTEXTUAL_MENU_ITEM));
		commit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				pluginWorkspaceAccess.showView(GIT_STAGING_VIEW, true);
				boolean filesStaged = false;
				File[] selectedFiles = ProjectManagerEditor.getSelectedFiles(pluginWorkspaceAccess);
				File repository = new File(selectedFiles[0].getAbsolutePath());
				while (repository.getParent() != null) {
					if (FileHelper.isGitRepository(repository.getAbsolutePath())) {
						break;
					}
					repository = repository.getParentFile();
				}
				String previousRepository = OptionsManager.getInstance().getSelectedRepository();
				try {
					GitAccess.getInstance().setRepository(repository.getAbsolutePath());
					List<FileStatus> gitFiles = GitAccess.getInstance().getUnstagedFiles();
					Set<String> allFiles = ProjectManagerEditor.getAllFiles(pluginWorkspaceAccess);
					for (FileStatus fileStatus : gitFiles) {
						if (allFiles.contains(repository.getAbsolutePath().replace("\\", "/") + "/" + fileStatus.getFileLocation())
								&& fileStatus.getChangeType() != GitChangeType.CONFLICT) {
							filesStaged = true;
							GitAccess.getInstance().add(fileStatus);
						}
					}

					if (filesStaged) {
						if (OptionsManager.getInstance().getRepositoryEntries().contains(repository.getAbsolutePath())) {
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
									.setSelectedItem(repository.getAbsolutePath());
						} else {
							OptionsManager.getInstance().addRepository(repository.getAbsolutePath());
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
									.addItem(repository.getAbsolutePath());
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
									.setSelectedItem(repository.getAbsolutePath());
						}
						return;
					}

					GitAccess.getInstance().setRepository(previousRepository);
				} catch (Exception e1) {
					if (logger.isDebugEnabled()) {
						logger.debug(e1, e1);
					}
				}
			}
		});
		git.add(commit);
		git.add(gitDiff);

		ProjectManagerEditor.addPopUpMenuCustomizer(pluginWorkspaceAccess, git);
	}

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */

	public boolean applicationClosing() {
		// You can reject the application closing here
		OptionsManager.getInstance().saveOptions();
		GitAccess.getInstance().close();
		return true;
	}

}