package com.oxygenxml.git;

import java.io.File;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.ui.Icons;

/**
 * InvocationHandler for ProjectPopupMenuCustomizer
 * 
 * @author Beniamin Savu
 *
 */
public class ProjectPopupMenuCustomizerInvocationHandler implements java.lang.reflect.InvocationHandler {
	/**
	 * Object giving access to the workspace plug-ins API.
	 */
	private StandalonePluginWorkspace pluginWorkspaceAccess;
	
	/**
	 * Translator.
	 */
  private Translator translator;
  
  /**
   * Object providing the Git-specific actions.
   */
  private GitActionsProvider gitActionsProvider;

	/**
	 * Constructor.
	 * 
	 * @param pluginWorkspaceAccess	    Object giving access to the workspace plug-ins API.
	 * @param translator                Translator.
	 * @param gitActionsProvider        Object providing the Git-specific actions.
	 */
	public ProjectPopupMenuCustomizerInvocationHandler(
			StandalonePluginWorkspace pluginWorkspaceAccess,
			Translator translator, GitActionsProvider gitActionsProvider) {
		this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    this.translator = translator;
    this.gitActionsProvider = gitActionsProvider;
	}

	/**
	 * @see java.lang.reflect.InvocationHandler.invoke(Object, Method, Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	  Object result = null;
	  // Customize pop-up menu
	  if (method.getName().equals("customizePopUpMenu")) {
	    // The contextual menu of the project view, where we should add the Git sub-menu.
	    JPopupMenu projectContextMenu = (JPopupMenu) args[0];

	    // Proceed only if the selected file pertain to a Git repository
	    if (areSelectedFilesFromGitRepo()) {
	      projectContextMenu.addSeparator();
	      JMenu gitMenu = new JMenu(translator.getTranslation(Tags.PROJECT_VIEW_GIT_CONTEXTUAL_MENU_ITEM));
	      gitMenu.setIcon(Icons.getIcon(ImageConstants.GIT_ICON));
	      for (AbstractAction action : gitActionsProvider.getActionsForProjectViewSelection()) {
          gitMenu.add(action);
        }
	      projectContextMenu.add(gitMenu);
	    }
	  }

	  return result;
	}

	/**
	 * Check if the selected files from the Project view pertain to a Git repository.
	 * 
	 * @return <code>true</code> if the selected files pertain to a Git repository,
	 *  <code>false</code> otherwise.
	 */
  private boolean areSelectedFilesFromGitRepo() {
	  boolean isGit = false;
    File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
    for (int i = 0; i < selectedFiles.length; i++) {
    	isGit = false;
    	File temp = new File(selectedFiles[i].getAbsolutePath());
    	while (temp.getParent() != null && !isGit) {
    		if (FileHelper.isGitRepository(temp.getPath())) {
    			isGit = true;
    		}
    		temp = temp.getParentFile();
    	}
    	if (!isGit) {
    		// If one of the selected project files does not pertain to a Git repository,
    		// it means that the project is not a Git one
    		break;
    	}
    }
    return isGit;
  }
}