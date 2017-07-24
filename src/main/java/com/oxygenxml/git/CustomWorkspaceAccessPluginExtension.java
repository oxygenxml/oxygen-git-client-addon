package com.oxygenxml.git;

import javax.swing.ImageIcon;

import com.oxygenxml.git.constants.ImageConstants;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Plugin extension - workspace access extension.
 */
public class CustomWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {

  /**
   * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
   */

  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
  	
  	
	  pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
		  /**
		   * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
		   */
		  public void customizeView(ViewInfo viewInfo) {
			  if(
					  //The view ID defined in the "plugin.xml"
					  "GitStagingView".equals(viewInfo.getViewID())) {


			  	Application application = new Application();
				  application.start();
				  
				  // TODO THE StagingPanel is enough. NO need to create the application.
				  viewInfo.setComponent(application.getGitWindow());
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