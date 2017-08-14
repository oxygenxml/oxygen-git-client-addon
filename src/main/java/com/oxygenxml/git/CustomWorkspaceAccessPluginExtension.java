package com.oxygenxml.git;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.utils.StagingPanelRefresh;
import com.oxygenxml.git.view.StagingPanel;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.util.editorvars.EditorVariables;

/**
 * Plugin extension - workspace access extension.
 */
public class CustomWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {
	/**
	 * ID of the view.
	 */
	private final String GIT_STAGING_VIEW = "GitStagingView";

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */

	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {

		Translator translator = new TranslatorExtensionImpl();
		final Refresh refresh = new StagingPanelRefresh();
		final StagingPanel stagingPanel = new StagingPanel(translator, refresh);
		refresh.call(stagingPanel);
		

		pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
			/**
			 * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
			 */
			public void customizeView(ViewInfo viewInfo) {

				if (
				// The view ID defined in the "plugin.xml"
				GIT_STAGING_VIEW.equals(viewInfo.getViewID())) {

					viewInfo.setComponent(stagingPanel);
					viewInfo.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_ICON)));
					viewInfo.setTitle("Git Staging");
				}
			}
		});

		final JFrame parentFrame = (JFrame) pluginWorkspaceAccess.getParentFrame();

		// Present the view to the user if it is the first run of the plugin
		parentFrame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				parentFrame.removeComponentListener(this);
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
			@Override
			public void windowActivated(WindowEvent e) {
				super.windowActivated(e);
				refresh.call(stagingPanel);
			}
		});

	}

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */

	public boolean applicationClosing() {
		// You can reject the application closing here
		return true;
	}

}