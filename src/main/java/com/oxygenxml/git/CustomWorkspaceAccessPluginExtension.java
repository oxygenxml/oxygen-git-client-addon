package com.oxygenxml.git;

import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
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
	 * ID of the Git staging view.
	 */
	final static String GIT_STAGING_VIEW = "GitStagingView";

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */

	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {

		try {
			//PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null);

		  AuthenticationInterceptor.install();

			Translator translator = new TranslatorExtensionImpl();
			StageController stageController = new StageController(GitAccess.getInstance());
			final GitRefreshSupport refresh = new PanelRefresh(translator);
			final StagingPanel stagingPanel = new StagingPanel(translator, refresh, stageController);
			refresh.setPanel(stagingPanel);

			ProjectViewManager.addPopUpMenuCustomizer(
			    pluginWorkspaceAccess,
			    translator,
			    // TODO Sorin: I feel like passing the staging panel is not a great idea...
			    new GitActionsProvider(pluginWorkspaceAccess, translator, stagingPanel));

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
			
			// Redirect logging to the Log4J instance.
			com.jcraft.jsch.JSch.setLogger(new com.jcraft.jsch.Logger() {
        public void log(int level, String message) {
          if (logger.isDebugEnabled()) {
            logger.debug(message);
          }
        }
        public boolean isEnabled(int level) {
          return logger.isDebugEnabled();
        }
      });
			
		} catch (Throwable t) {
			// Runtime exceptions shouldn't affect Oxygen.
			pluginWorkspaceAccess.showErrorMessage(t.getMessage());
			logger.fatal(t, t);
		}
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