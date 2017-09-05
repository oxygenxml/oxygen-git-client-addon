package com.oxygenxml.git;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.Authenticator.RequestorType;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.mozilla.javascript.ast.ParenthesizedExpression;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.actions.MenusAndToolbarsContributorCustomizer;
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
			//PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("MY_PLUGIN_OPTIONS", "");

			final Authenticator[] oldAuth = new Authenticator[1];
			try {

				final Field requestingHost = Authenticator.class.getDeclaredField("requestingHost");
				final Field requestingSite = Authenticator.class.getDeclaredField("requestingSite");
				final Field requestingPort = Authenticator.class.getDeclaredField("requestingPort");
				final Field requestingProtocol = Authenticator.class.getDeclaredField("requestingProtocol");
				final Field requestingPrompt = Authenticator.class.getDeclaredField("requestingPrompt");
				final Field requestingScheme = Authenticator.class.getDeclaredField("requestingScheme");
				final Field requestingURL = Authenticator.class.getDeclaredField("requestingURL");
				final Field requestingAuthType = Authenticator.class.getDeclaredField("requestingAuthType");

				requestingHost.setAccessible(true);
				requestingSite.setAccessible(true);
				requestingPort.setAccessible(true);
				requestingProtocol.setAccessible(true);
				requestingPrompt.setAccessible(true);
				requestingScheme.setAccessible(true);
				requestingURL.setAccessible(true);
				requestingAuthType.setAccessible(true);

				Field declaredField = Authenticator.class.getDeclaredField("theAuthenticator");
				declaredField.setAccessible(true);
				oldAuth[0] = (Authenticator) declaredField.get(null);

				Authenticator.setDefault(new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {

						try {
							final String oldRequestingHost = (String) requestingHost.get(this);
							final InetAddress oldRequestingSite = (InetAddress) requestingSite.get(this);
							final int oldRequestingPort = (Integer) requestingPort.get(this);
							final String oldRequestingProtocol = (String) requestingProtocol.get(this);
							final String oldRequestingPrompt = (String) requestingPrompt.get(this);
							final String oldRequestingScheme = (String) requestingScheme.get(this);
							final URL oldRequestingURL = (URL) requestingURL.get(this);
							final RequestorType oldRequestingAuthType = (RequestorType) requestingAuthType.get(this);

							if (GitAccess.getInstance().getHostName().equals(getRequestingHost())) {
								// we need to return null to let our own authentication dialog
								// (LoginDialog)
								// appear for git related hosts. Thus preventing the Oxygen's
								// dialog appear
								return null;
							} else {
								Method reset = Authenticator.class.getDeclaredMethod("reset");
								reset.setAccessible(true);
								reset.invoke(oldAuth[0]);
								requestingHost.set(oldAuth[0], oldRequestingHost);
								requestingSite.set(oldAuth[0], oldRequestingSite);
								requestingPort.set(oldAuth[0], oldRequestingPort);
								requestingProtocol.set(oldAuth[0], oldRequestingProtocol);
								requestingPrompt.set(oldAuth[0], oldRequestingPrompt);
								requestingScheme.set(oldAuth[0], oldRequestingScheme);
								requestingURL.set(oldAuth[0], oldRequestingURL);
								requestingAuthType.set(oldAuth[0], oldRequestingAuthType);

								Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
								getPasswordAuthentication.setAccessible(true);
								return (PasswordAuthentication) getPasswordAuthentication.invoke(oldAuth[0]);
							}
						} catch (Exception e) {
							logger.error(e, e);
						}
						return null;
					}
				});

			} catch (Throwable e) {
				e.printStackTrace();
			}

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

				private boolean toRefresh = true;

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

	private void createProjectViewContextualMenu(final StandalonePluginWorkspace pluginWorkspaceAccess,
			final Translator translator, final StageController stageController, final StagingPanel stagingPanel) {

		JMenu git = new JMenu("Git");
		JMenuItem gitDiff = new JMenuItem("Git Diff");
		gitDiff.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				File[] selectedFiles = ProjectManagerEditor.getSelectedFiles(pluginWorkspaceAccess);
				// in the diff action is enabled only for one file
				File repository = new File(selectedFiles[0].getAbsolutePath());
				while (repository.getParent() != null) {
					if (FileHelper.isGitRepository(repository.getAbsolutePath())) {
						break;
					}
					repository = repository.getParentFile();
				}
				try {
					String previousRepository = OptionsManager.getInstance().getSelectedRepository();
					GitAccess.getInstance().setRepository(repository.getAbsolutePath());
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
					GitAccess.getInstance().setRepository(previousRepository);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		JMenuItem commit = new JMenuItem("Commit");
		commit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
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
					
					if(filesStaged){
						pluginWorkspaceAccess.showView(GIT_STAGING_VIEW, true);
						if(OptionsManager.getInstance().getRepositoryEntries().contains(repository.getAbsolutePath())){
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(repository.getAbsolutePath());
						} else {
							OptionsManager.getInstance().addRepository(repository.getAbsolutePath());
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().addItem(repository.getAbsolutePath());
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(repository.getAbsolutePath());
						}
						return;
					}
					
					GitAccess.getInstance().setRepository(previousRepository);
				} catch (Exception e1) {
					e1.printStackTrace();
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
		return true;
	}

}