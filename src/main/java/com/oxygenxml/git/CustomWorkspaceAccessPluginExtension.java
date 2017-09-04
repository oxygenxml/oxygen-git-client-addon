package com.oxygenxml.git;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.Authenticator.RequestorType;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.mozilla.javascript.ast.ParenthesizedExpression;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.utils.StagingPanelRefresh;
import com.oxygenxml.git.view.StagingPanel;

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
          int count = 1;

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
                //beacuse of the Authorization-requierd refs dialog
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
      

			//PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("MY_PLUGIN_OPTIONS", "");
			Translator translator = new TranslatorExtensionImpl();
			final Refresh refresh = new StagingPanelRefresh(translator);
			final StagingPanel stagingPanel = new StagingPanel(translator, refresh);
			refresh.setPanel(stagingPanel);
			// refresh.call();

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

				// Call the refresh command when the Oxygen window is activated

			});

			// refresh activates when the the oxygen window gains again focus
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
							if(focusedWindow == null){
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
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */

	public boolean applicationClosing() {
		// You can reject the application closing here
		return true;
	}

}