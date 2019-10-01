package com.oxygenxml.git.constants;

import java.net.URL;

import javax.swing.Icon;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * All image locations that are used in this plugin
 * 
 * @author Beniamin Savu 
 *
 */
public class ImageConstants {
  
  private ImageConstants() {
    // Nothing.
  }

	public static final String GIT_ADD_ICON = "/images/GitAdd10.png";
	public static final String GIT_DELETE_ICON = "/images/GitRemoved10.png";
	public static final String GIT_MODIFIED_ICON = "/images/GitModified10.png";
	public static final String GIT_CONFLICT_ICON = "/images/GitWarning10.png";
	public static final String GIT_PUSH_ICON = "/images/GitPush16.png";
	public static final String GIT_PULL_ICON = "/images/GitPull16.png";
	public static final String GIT_BRANCH_ICON = "/images/GitBranch16.png";
	public static final String GIT_SUBMODULE_ICON = "/images/GitSubmodule16.png";
	public static final String GIT_SUBMODULE_FILE_ICON = "/images/GitStorage12.png";
	public static final String GIT_ICON = "/images/DockableFrameGitStaging16.png";
	public static final String GIT_CLONE_REPOSITORY_ICON = "/images/Add16.png";
	public static final String FILE_CHOOSER_ICON = "/images/Open16.png";
	public static final String TREE_VIEW = "/images/GitTreeMode16.png";
	public static final String TABLE_VIEW = "/images/GitFlatMode16.png";
	public static final String FOLDER_TREE_ICON = "/images/FolderTree10.png";
	public static final String VALIDATION_ERROR = "/images/ValidationError12.png";
	public static final String WARNING_ICON = "/images/Warning32@2x.png";
	public static final String LOADING_ICON = "/images/Loading16.gif";
	// TODO change icon for Git history
	public static final String GIT_HISTORY = "/images/History16.png";
	public static final String REFRESH_ICON = "/images/Refresh16.png";
	

  /**
   * Get icon.
   *  
   * @param imgKey The image key.
   * 
   * @return the icon.
   */
  public static Icon getIcon(String imgKey) {
    Icon toReturn = null;
    URL resource = ImageConstants.class.getResource(imgKey);
    if (resource != null) {
      toReturn = (Icon) PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities().loadIcon(resource);
    }
    return toReturn;
  }
}
