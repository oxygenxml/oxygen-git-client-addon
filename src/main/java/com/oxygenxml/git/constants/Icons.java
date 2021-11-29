package com.oxygenxml.git.constants;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Image utility class.
 */
public class Icons {
  
  /**
   * Hidden constructor.
   */
  private Icons() {
    // Nothing.
  }

  /**
   * Icon for a renamed resource.
   */
  public static final String GIT_RENAME_ICON = "/images/GitRenameFile12.png";
  /**
   * Icon for an added resource.
   */
	public static final String GIT_ADD_ICON = "/images/GitAdd10.png";
	/**
	 * Icon for a deleted icon.
	 */
	public static final String GIT_DELETE_ICON = "/images/GitRemoved10.png";
	/**
	 * Icon for a modified resource.
	 */
	public static final String GIT_MODIFIED_ICON = "/images/GitModified10.png";
	/**
	 * Icon for a resource that is in conflict state.
	 */
	public static final String GIT_CONFLICT_ICON = "/images/GitWarning10.png";
	/**
	 * Icon for the push button.
	 */
	public static final String GIT_PUSH_ICON = "/images/GitPush16.png";
	/**
	 * Icon for the pull icon.
	 */
	public static final String GIT_PULL_ICON = "/images/GitPull16.png";
	/**
	 * Branch icon.
	 */
	public static final String GIT_BRANCH_ICON = "/images/GitBranch16.png";
	/**
	 * Submodule button icon.
	 */
	public static final String GIT_SUBMODULE_ICON = "/images/GitSubmodule16.png";
	/**
	 * Submodule root icon in the staging area.
	 */
	public static final String GIT_SUBMODULE_FILE_ICON = "/images/GitStorage12.png";
	/**
	 * Git icon.
	 */
	public static final String GIT_ICON = "/images/DockableFrameGitStaging16.png";
	/**
	 * Clone icon (a plus symbol).
	 */
	public static final String GIT_CLONE_REPOSITORY_ICON = "/images/Add16.png";
	/**
	 * File chooser (open) icon.
	 */
	public static final String FILE_CHOOSER_ICON = "/images/Open16.png";
	/**
	 * Tree mode icon.
	 */
	public static final String TREE_VIEW = "/images/GitTreeMode16.png";
	/**
	 * Flat/table mode icon.
	 */
	public static final String LIST_VIEW = "/images/GitFlatMode16.png";
	/**
	 * Folder icon in tree.
	 */
	public static final String FOLDER_TREE_ICON = "/images/FolderTree10.png";
	/**
	 * Validation error.
	 */
	public static final String VALIDATION_ERROR = "/images/ValidationError12.png";
	/**
	 * Loading (in progress) icon.
	 */
	public static final String LOADING_ICON = "/images/Loading16.gif";
	/**
	 * Icon for the Git History button.
	 */
	public static final String GIT_HISTORY = "/images/History16.png";
	/**
	 * Refresh icon.
	 */
	public static final String REFRESH_ICON = "/images/Refresh16.png";
	/**
	 * Icon for auto push toggle.
	 */
	public static final String AUTO_PUSH_ON_COMMIT = "/images/AutoPush16.png";
	/**
	 * Icon for the previous commits menu button.
	 */
	public static final String PREV_COMMIT_MESSAGES = "/images/PreviousCommits16.png";
	/**
	 * Icon for the amend commit toggle.
	 */
  public static final String AMEND_COMMIT = "/images/AmendCommit16.png";
  /**
   * Settings icon.
   */
  public static final String SETTINGS = "/images/SettingsToolbar16.png";
  /**
   * Stage selected.
   */
  public static final String STAGE_SELECTED = "/images/StageSelected16.png";
  /**
   * Stage all.
   */
  public static final String STAGE_ALL = "/images/StageAll16.png";
  /**
   * Unstage selected.
   */
  public static final String UNSTAGE_SELECTED = "/images/UnstageSelected16.png";
  /**
   * Unstage all.
   */
  public static final String UNSTAGE_ALL = "/images/UnstageAll16.png";
  
  //=============== These icons are loaded from Oxygen =================
  /**
   * Tag icon.
   */
  public static final String TAG = "/images/GitTag16.png";
  /**
   * Info icon.
   */
  public static final String INFO_ICON = "/images/Info32.png";
  /**
   * Question icon.
   */
  public static final String QUESTION_ICON = "/images/Help32.png";
  /**
   * Warning icon small size.
   */
  public static final String SMALL_WARNING_ICON = "/images/Warning16.png";
  /**
   * Warning icon.
   */
  public static final String WARNING_ICON = "/images/Warning32.png";
  /**
   * Error icon.
   */
  public static final String ERROR_ICON = "/images/Error32.png";
  // ====================================================================

  /**
   * Local repo icon for Git Branch Manager.
   */
  public static final String LOCAL_REPO = "/images/LocalRepo16.png";
  /**
   * Icon for the local branches category in Git Branch Manager.
   */
  public static final String LOCAL = "/images/Local16.png";
  /**
   * Icon for the remote branches category in Git Branch Manager.
   */
  public static final String REMOTE = "/images/Remote16.png";
  /**
   * Stash icon.
   */
  public static final String STASH_ICON = "/images/GitStash16.png";
  
  /**
   * Get icon.
   *  
   * @param imgKey The image key.
   * 
   * @return the icon.
   */
  public static Icon getIcon(String imgKey) {
    Icon toReturn = null;
    URL resource = Icons.class.getResource(imgKey);
    if (resource != null) {
      if (PluginWorkspaceProvider.getPluginWorkspace() != null && PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities() != null) {
        toReturn = (Icon) PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities().loadIcon(resource);
      } else {
        // Probably in a unit test context.
        toReturn = new ImageIcon(resource);
      }
    }
    return toReturn;
  }
}
