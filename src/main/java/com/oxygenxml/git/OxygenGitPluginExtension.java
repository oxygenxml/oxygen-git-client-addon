package com.oxygenxml.git;

import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Plugin extension - workspace access extension.
 * 
 * @author Beniamin Savu
 */
public class OxygenGitPluginExtension implements WorkspaceAccessPluginExtension {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(OxygenGitPluginExtension.class);

	/**
	 * ID of the Git staging view. Defined in plugin.xml.
	 */
	static final String GIT_STAGING_VIEW = "GitStagingView";

	/**
	 * Refresh support.
	 */
	final PanelRefresh gitRefreshSupport = new PanelRefresh();

	/**
	 * Staging panel.
	 */
  private StagingPanel stagingPanel;
  /**
   * Plugin workspace access.
   */
  private StandalonePluginWorkspace pluginWorkspaceAccess;
	
	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
	  this.pluginWorkspaceAccess = pluginWorkspaceAccess;
		try {
		  // Uncomment this to start with fresh options. For testing purposes
//			PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null);

		  AuthenticationInterceptor.install();

			StageController stageController = new StageController();
			
			ProjectViewManager.addPopUpMenuCustomizer(
			    pluginWorkspaceAccess,
			    new GitMenuActionsProvider(pluginWorkspaceAccess, stageController));

			pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
				/**
				 * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
				 */
			  @Override
			  public void customizeView(ViewInfo viewInfo) {
			    // The constant's value is defined in plugin.xml
			    if (GIT_STAGING_VIEW.equals(viewInfo.getViewID())) {
			      stagingPanel = new StagingPanel(gitRefreshSupport, stageController);
			      gitRefreshSupport.setPanel(stagingPanel);
			      
			      viewInfo.setComponent(stagingPanel);
			      
			      // Start the thread that populates the view.
			      gitRefreshSupport.call();
			      
			      ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
			      URL resource = getClass().getResource(ImageConstants.GIT_ICON);
			      if (resource != null) {
			        ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
			        viewInfo.setIcon(icon);
			      }
			      viewInfo.setTitle("Git Staging");
					}
				}
			});

			final JFrame parentFrame = (JFrame) pluginWorkspaceAccess.getParentFrame();
			
			// Present the view to the user if it is the first run of the plugin
			parentFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
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

			  /**
			   * <code>true</code> to refresh.
			   */
				private boolean refresh = false;

				@Override
				public void windowActivated(WindowEvent e) {
					super.windowActivated(e);
					if (refresh && stagingPanel != null && stagingPanel.isShowing()) {
						gitRefreshSupport.call();
					}
					refresh = false;
				}

				@Override
				public void windowDeactivated(WindowEvent e) {
					super.windowDeactivated(e);
					SwingUtilities.invokeLater(new Runnable() {
					  @Override
						public void run() {
							Object focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
							if (focusedWindow == null) {
								refresh = true;
							}
						}
					});
				}
			});
			
			// Redirect logging to the Log4J instance.
			com.jcraft.jsch.JSch.setLogger(new com.jcraft.jsch.Logger() {
			  @Override
        public void log(int level, String message) {
          if (logger.isDebugEnabled()) {
            logger.debug(message);
          }
        }
			  @Override
        public boolean isEnabled(int level) {
          return logger.isDebugEnabled();
        }
      });
			
		} catch (Throwable t) {
			// Catch Throwable - Runtime exceptions shouldn't affect Oxygen.
			pluginWorkspaceAccess.showErrorMessage(t.getMessage());
			logger.fatal(t, t);
		}
	}

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */
	@Override
	public boolean applicationClosing() {
		OptionsManager.getInstance().saveOptions();
		
		if (stagingPanel != null) {
		  // Only if the view was actually created.
		  try {
		    stagingPanel.shutdown();
		  } catch (IllegalStateException e) {
		    pluginWorkspaceAccess.showView(GIT_STAGING_VIEW, true);
		    pluginWorkspaceAccess.showWarningMessage(e.getMessage());

		    // Cancel the closing.
		    return false;
		  }
		}
		// EXM-42867: wait for the refresh to execute
		gitRefreshSupport.shutdown();
		
		GitAccess.getInstance().close();
		
		// Close application.
		return true;
	}

}