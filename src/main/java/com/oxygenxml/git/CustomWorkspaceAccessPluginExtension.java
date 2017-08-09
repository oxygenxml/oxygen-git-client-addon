package com.oxygenxml.git;

import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.StagingPanel;

import ro.sync.exml.options.Options;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.util.editorvars.EditorVariables;

/**
 * Plugin extension - workspace access extension.
 */
public class CustomWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {

  /**
   * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
   */

  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
  	
  	String projectViewPath = EditorVariables.expandEditorVariables("${pd}", null);
  	if(FileHelper.isGitRepository(projectViewPath)){
  		OptionsManager.getInstance().addRepository(projectViewPath);
  	}
	  pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
		  /**
		   * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
		   */
		  public void customizeView(ViewInfo viewInfo) {
			  if(
					  //The view ID defined in the "plugin.xml"
					  "GitStagingView".equals(viewInfo.getViewID())) {

				  viewInfo.setComponent(new StagingPanel());
				//  viewInfo.setComponent(new JScrollPane(customMessagesArea));
				  //viewInfo.setTitle("Custom Messages");
				  //You can have images located inside the JAR library and use them...
				 viewInfo.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_ICON)));
				 
				 viewInfo.setTitle("Git Staging");
			  } 
		  }
	  }); 
  }

  /**
   * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
   */

  public boolean applicationClosing() {
	  //You can reject the application closing here
    return true;
  }
}