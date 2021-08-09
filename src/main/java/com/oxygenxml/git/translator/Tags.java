package com.oxygenxml.git.translator;

/**
 * Constants used for translation
 * 
 * @author Beniamin Savu
 *
 */
public class Tags {
  /**
   * Private constructor.
   */
  private Tags() {
    // Nothing.
  }
  /**
   * Last commit details.
   */
  public static final String LAST_COMMIT_DETAILS = "Last_Commit_Details";
  /**
   * Staged files.
   */
  public static final String STAGED_FILES = "Staged_files";
  /**
   * Unstaged files.
   */
  public static final String UNSTAGED_FILES = "Unstaged_files";
  /**
   * Keep conflict.
   */
  public static final String KEEP_CONFLICT = "Keep_conflict";
  /**
   * Resolve anyway.
   */
  public static final String RESOLVE_ANYWAY = "Resolve_anyway";
  /**
   * Description for the short_branch_name editor variable.
   */
  public static final String SHORT_BRANCH_NAME_DESCRIPTION = "Short_branch_name_description";
  /**
   * Description for the full_branch_name editor variable.
   */
  public static final String FULL_BRANCH_NAME_DESCRIPTION = "Full_branch_name_description";
  /**
   * Description for the working_copy_name editor variable.
   */
  public static final String WORKING_COPY_NAME_DESCRIPTION = "Working_copy_name_description";
  /**
   * Description for the working_copy_path editor variable.
   */
  public static final String WORKING_COPY_PATH_DESCRIPTION = "Working_copy_path_description";
  /**
   * Description for the working_copy_url editor variable.
   */
  public static final String WORKING_COPY_URL_DESCRIPTION = "Working_copy_url_description";
  /**
   * Uncommitted changes.
   */
  public static final String UNCOMMITTED_CHANGES = "Uncommitted_changes";
  /**
   * Displays the message "File".
   */
  public static final String FILE = "File";
  /**
   * Displays the message "Repository".
   */
  public static final String REPOSITORY = "Repository";
  /**
   * Displays the message "Checkout".
   */
  public static final String CHECKOUT = "Checkout";
  /**
   * Displays the message "Delete".
   */
  public static final String DELETE = "Delete";
  /**
   * Displays information about what a soft reset of a branch does.
   */
  public static final String SOFT_RESET_INFO = "Soft_reset_info";
  /**
   * Displays information about what a mixed reset of a branch does.
   */
  public static final String MIXED_RESET_INFO = "Mixed_reset_info";
  /**
   * Displays information about what a hard reset of a branch does.
   */
  public static final String HARD_RESET_INFO = "Hard_reset_info";
  /**
   * The text displayed in a label when asking for the reset type.
   */
  public static final String RESET_MODE = "Reset_mode";
  /**
   * Dialog title.
   */
  public static final String RESET_BRANCH_TO_COMMIT = "Reset_branch_to_commit";
  /**
   * Action name in history.
   */
  public static final String RESET_BRANCH_TO_THIS_COMMIT = "Reset_branch_to_this_commit";
  /**
   * The "reset" message.
   */
  public static final String RESET = "Reset";
  /**
   * The warning message when trying to create a branch with the same name as another local branch.
   */
  public static final String LOCAL_BRANCH_ALREADY_EXISTS = "Local_branch_already_exists";
  /**
   * The confirmation message when trying to delete a branch.
   */
  public static final String CONFIRMATION_MESSAGE_DELETE_BRANCH = "Confirmation_message_delete_branch";
  /**
   * Delete branch.
   */
  public static final String DELETE_BRANCH = "Delete_branch";
  /**
   * The tool tip for the button that opens the branch manager side view.
   */
  public static final String BRANCH_MANAGER_BUTTON_TOOL_TIP = "Branch_manager_button_tool_tip";
  /**
   * The filter hint for search bar.
   */
  public static final String FILTER_HINT = "Filter_hint";
  /**
   * The tool tip for the filter of the braches tree.
   */
  public static final String SEARCH_BAR_TOOL_TIP = "Search_bar_tool_tip";
  /**
   * Action in the dialog that notifies the user that there are changes in the remote.
   */
  public static final String CLOSE = "Close";
  /**
   * Action in the dialog that notifies the user that there are changes in the remote.
   */
  public static final String PULL_CHANGES = "Pull_changes";
  
  /**
   * The title for the Branch Manager side view.
   */
  public static final String BRANCH_MANAGER_TITLE = "Branch_manager_title";
  /**
   * The message when there are changes in the remote repository.
   */
  public static final String REMOTE_CHANGES_LABEL = "Remote_changes_label";
  /**
   * Recommendation to pull remote changes.
   */
  public static final String PULL_REMOTE_CHANGED_RECOMMENDATION = "Pull_remote_changed_recommendation";
  /**
   * The message when the user has the choice to pull new commits.
   */
  public static final String WANT_TO_PULL_QUESTION = "Want_to_pull_question";
  /**
   * Option text for notifying when there are new commits upstream.
   */
  public static final String NOTIFY_ON_NEW_COMMITS = "Notify_on_new_commits";
	/**
	 * The message displayed to the user when conflict markers are detected.
	 */
	public static final String CONFLICT_MARKERS_MESSAGE = "Conflict_markers_message";
	/**
   * There is a new commit in the remote repository that has not been pulled.
   */
  public static final String NEW_COMMIT_UPSTREAM = "New_commit_upstream";
  /**
   * A file is not present at a specific revision.
   */
  public static final String FILE_WAS_REMOVED_IN_REVISION = "File_was_removed_in_revision";
  /**
   * A file is not present at a specific revision.
   */
  public static final String FILE_NOT_PRESENT_IN_REVISION = "File_not_present_in_revision";
  /**
   * Action in the history view to compare two revisions of the same file.
   */
  public static final String COMPARE_WITH_EACH_OTHER = "Compare_with_each_other";
  /**
   * Message shown when invoking the diff on an unchanged file.
   */
  public static final String NO_CHANGES = "No_changes";
  /**
   * There are no changes to commit. 
   */
  public static final String NOTHING_TO_COMMIT = "Nothing_to_commit";
  /**
   * Message shown when trying to clone from an invalid remote.
   * 
   * en: Submodule_load_fail
   */
  public static final String SUBMODULE_LOAD_FAIL = "Submodule_load_fail";
  /**
   * Message shown when trying to clone from an invalid remote.
   * 
   * en: Invalid remote
   */
  public static final String INVALID_REMOTE = "Invalid_remote";
  /**
   * Label in the repository cloning dialog.
   * 
   * en: Checkout branch
   */
  public static final String CHECKOUT_BRANCH = "Checkout_branch";
  /**
   * en: Yes
   */
  public static final String YES = "Yes";
  /**
   * en: No
   */
  public static final String NO = "No";
  
  /**
   * Message shown when a previous SSH passphrase was invalid.
   * 
   * en: The previous passphrase is invalid.
   */
  public static final String PREVIOUS_PASS_PHRASE_INVALID = "Previous_passphrase_invalid";
  /**
   * Label.
   * 
   * en: Pull status
   */
  public static final String PULL_STATUS = "Pull_status";
  /**
   * Message asking for the SSH passphrase.
   * 
   * en: Please enter your SSH passphrase.
   */
  public static final String ENTER_SSH_PASS_PHRASE = "Enter_ssh_passphrase";
  /**
   * Shown when a command is aborted.
   * 
   * en: Command aborted.
   */
  public static final String COMMAND_ABORTED = "Command_aborted";

	/**
	 * Label displayed on the left of the working copy combo box
	 */
	public static final String WORKING_COPY_LABEL = "Working_Copy_Label";

	/**
	 * The tooltip for the push button
	 */
	public static final String PUSH_BUTTON_TOOLTIP = "Push_Button_ToolTip";

	/**
	 * The tooltip for the pull button
	 */
	public static final String PULL_BUTTON_TOOLTIP = "Pull_Button_ToolTip";

	/**
	 * The tooltip for the browse button
	 */
	public static final String BROWSE_BUTTON_TOOLTIP = "Browse_Button_ToolTip";

	/**
	 * The text displayed on the StageAll button
	 */
	public static final String STAGE_ALL_BUTTON_TEXT = "Stage_All_Button_Text";

	/**
	 * The text displayed on the UnstageAll button
	 */
	public static final String UNSTAGE_ALL_BUTTON_TEXT = "Unstage_All_Button_Text";

	/**
	 * The text displayed on the Stage Selected button
	 */
	public static final String STAGE_SELECTED_BUTTON_TEXT = "Stage_Selected_Button_Text";

	/**
	 * The text displayed on the Unstage Selected button
	 */
	public static final String UNSTAGE_SELECTED_BUTTON_TEXT = "Unstage_Selected_Button_Text";

	/**
	 * The tooltip for the ChangeView button
	 */
	public static final String CHANGE_VIEW_BUTTON_TOOLTIP = "Change_View_Button_ToolTip";

	/**
	 * The massage displayed above the previously committed messages combo box
	 */
	public static final String COMMIT_MESSAGE_LABEL = "Commit_Message_Label";

	/**
	 * The massage displayed on the combo box containing the previouslt commited
	 * messages
	 */
	public static final String COMMIT_COMBOBOX_DISPLAY_MESSAGE = "Commit_ComboBox_Display_Message";

	/**
	 * The massage displayed on the commit button
	 */
	public static final String COMMIT_BUTTON_TEXT = "Commit_Button_Text";

	/**
	 * The tooltip for the "+" icon that appears on the left side of the file
	 */
	public static final String ADD_ICON_TOOLTIP = "Add_Icon_ToolTip";

	/**
	 * The tooltip for the "*" icon that appears on the left side of the file
	 */
	public static final String MODIFIED_ICON_TOOLTIP = "Modified_Icon_ToolTip";

	/**
	 * The tooltip for the "-" icon that appears on the left side of the file
	 */
	public static final String DELETE_ICON_TOOLTIP = "Delete_Icon_ToolTip";

	/**
	 * The tooltip for the "!" icon that appears on the left side of the file
	 */
	public static final String CONFLICT_ICON_TOOLTIP = "Conflict_Icon_ToolTip";

	/**
	 * The massage displayed when a commit is successful
	 */
	public static final String COMMIT_SUCCESS = "Commit_Success";

	/**
	 * The massage displayed when you have conflicts
	 */
	public static final String COMMIT_WITH_CONFLICTS = "Commit_With_Conflicts";

	/**
	 * The massage displayed when you push with conflicts
	 */
	public static final String RESOLVE_CONFLICTS_FIRST = "Resolve_conflicts_first";

	/**
	 * The massage displayed when you push but your repository is not up to date
	 */
	public static final String BRANCH_BEHIND = "Branch_Behind";

	/**
	 * The massage displayed when you push successful
	 */
	public static final String PUSH_SUCCESSFUL = "Push_Successful";

	/**
	 * The massage displayed when your push fails
	 */
	public static final String PUSH_FAILED_UNKNOWN = "Push_Failed_Unknown";
    
	/**
	 * The message displayed when your push/pull fails and it is because of a transport exception.
	 */
	public static final String UNABLE_TO_ACCESS_REPO = "Unable_to_access_repo";
	
	/**
	 * The massage displayed when you push with no changes
	 */
	public static final String PUSH_UP_TO_DATE = "Push_Up_To_Date";

	/**
	 * The massage displayed when your push is in progress
	 */
	public static final String PUSH_IN_PROGRESS = "Push_In_Progress";

	/**
	 * The massage displayed when you pull with uncommitted files
	 */
	public static final String PULL_WITH_UNCOMMITED_CHANGES = "Pull_With_Uncommitted_Changes";

	/**
	 * The massage displayed when your repository is up to date
	 */
	public static final String PULL_UP_TO_DATE = "Pull_Up_To_Date";

	/**
	 * The massage displayed when your pull is successful
	 */
	public static final String PULL_SUCCESSFUL = "Pull_Successful";

	/**
	 * The massage displayed when your pull is in progress
	 */
	public static final String PULL_IN_PROGRESS = "Pull_In_Progress";

	/**
	 * The massage displayed when you pull while having conflicts
	 */
	public static final String PULL_WHEN_REPO_IN_CONFLICT = "Pull_when_repo_in_conflict";

	/**
	 * The massage displayed when your pull is successful but has conflicts
	 */
	public static final String PULL_SUCCESSFUL_CONFLICTS = "Pull_Successful_Conflicts";

	/**
	 * The text displayed for the "Open in compare editor" contextual menu item
	 */
	public static final String OPEN_IN_COMPARE = "Open_In_Compare";

	/**
	 * The text displayed for the "Open" contextual menu item
	 */
	public static final String OPEN = "Open";

	/**
	 * The text displayed for the "Stage" contextual menu item
	 */
	public static final String STAGE = "Stage";

	/**
	 * The text displayed for the "Unstage" contextual menu item
	 */
	public static final String UNSTAGE = "Unstage";

	/**
	 * The text displayed for "Resolve_Conflict".
	 */
	public static final String RESOLVE_CONFLICT = "Resolve_Conflict";

	/**
	 * The text displayed for the "Discard" contextual menu item
	 */
	public static final String DISCARD = "Discard";

	/**
	 * The text displayed when you click on the "Discard" contextual menu item
	 */
	public static final String DISCARD_CONFIRMATION_MESSAGE = "Discard_Confirmation_Message";

	/**
	 * The text displayed for " Resolve Using "Mine" ".
	 */
	public static final String RESOLVE_USING_MINE = "Resolve_Using_Mine";

	/**
	 * The text displayed for " Resolve Using "Theirs" ".
	 */
	public static final String RESOLVE_USING_THEIRS = "Resolve_Using_Theirs";

	/**
	 * The text displayed for the "Restart Merge " contextual menu item
	 */
	public static final String RESTART_MERGE = "Restart_Merge";

	/**
	 * The text displayed for the "Mark Resolved" contextual menu item
	 */
	public static final String MARK_RESOLVED = "Mark_Resolved";

	/**
	 * The text displayed when the user selects a repository from the working copy
	 * selector and it doesn't exists anymore
	 */
	public static final String WORKINGCOPY_REPOSITORY_NOT_FOUND = "Workingcopy_Repository_Not_Found";

	/**
	 * The text displayed when the user selects a non git folder(It doesn't
	 * contain the ".git" folder)
	 */
	public static final String WORKINGCOPY_NOT_GIT_DIRECTORY = "Workingcopy_Not_Git_Directory";

	/**
	 * The text displayed when the application starts and the last selected
	 * repository doesn't exists
	 */
	public static final String WORKINGCOPY_LAST_SELECTED_REPOSITORY_DELETED = "Workingcopy_Last_Selected_Repository_Deleted";

	/**
	 * The text displayed after exiting the diff for a conflict file and you don't
	 * modify anithing
	 */
	public static final String CHECK_IF_CONFLICT_RESOLVED = "Check_If_Conflict_Resolved";

	/**
	 * The text displayed in the title for the dialog that appears after exiting
	 * the diff for a conflict file and you don't modify anithing
	 */
	public static final String CHECK_IF_CONFLICT_RESOLVED_TITLE = "Title_Check_If_Conflict_Resolved";

	/**
	 * The text displayed in the title for the dialog that appears when you select
	 * your current branch
	 */
	public static final String BRANCH_SELECTION_DIALOG_TITLE = "Branch_Selection_Dialog_Title";

	/**
	 * The the message displayed when branch selection fails because of checkout conflict.
	 */
	public static final String BRANCH_SWITCH_CHECKOUT_CONFLICT_ERROR_MSG = "Branch_switch_checkout_conflict_error_msg";
	
	/**
	 * The the message displayed when branch selection fails because the repo is in conflict.
	 */
	public static final String BRANCH_SWITCH_WHEN_REPO_IN_CONFLICT_ERROR_MSG = "Branch_switch_when_repo_in_conflict_error_msg";

	/**
	 * The text displayed in the title for the dialog that appears when you push
	 * while having no remote set
	 */
	public static final String ADD_REMOTE_DIALOG_TITLE = "Add_Remote_Dialog_Title";

	/**
	 * The text displayed in the dialog that appears near the text field for
	 * remote name
	 */
	public static final String ADD_REMOTE_DIALOG_ADD_REMOTE_NAME_LABEL = "Add_Remote_Dialog_Add_Remote_Name_Label";

	/**
	 * The text displayed in the dialog that appears near the text field for
	 * remote repo
	 */
	public static final String REMOTE_REPO_URL = "Add_Remote_Dialog_Add_Remote_Repo_Label";

	/**
	 * The text displayed in the dialog that appears at the top of the dialog
	 */
	public static final String ADD_REMOTE_DIALOG_INFO_LABEL = "Add_Remote_Dialog_Info_Label";

	/**
	 * The text displayed in the title for the dialog that appears if the
	 * project.xpr is not a git repository and has no got repositories
	 */
	public static final String CHECK_PROJECTXPR_IS_GIT_TITLE = "Check_ProjcetXPR_Is_Git_Title";

	/**
	 * The text displayed in the dialog that appears if the project.xpr is not a
	 * git repository and has no got repositories
	 * 
	 * en: Do you want your current project ("{0}") to be a git project?
	 */
	public static final String CHECK_PROJECTXPR_IS_GIT = "Check_ProjcetXPR_Is_Git";
	
	/**
	 * Confirmation message for changing the current working copy to the project folder,
	 * which is also a Git repository.
	 */
	public static final String CHANGE_TO_PROJECT_REPO_CONFIRM_MESSAGE = "Change_to_project_repo_confirm_message";
	
	/**
	 * "Change working copy"
	 */
	public static final String CHANGE_WORKING_COPY = "Change_working_copy";

	/**
	 * The tooltip for the select submodule button
	 */
	public static final String SELECT_SUBMODULE_BUTTON_TOOLTIP = "Select_Submodule_Button_Tooltip";

	/**
	 * The text displayed in the dialog that appears near the combo box
	 */
	public static final String SUBMODULE_DIALOG_SUBMODULE_SELECTION_LABEL = "Submodule_Dialog_Submodule_Selection_Label";

	/**
	 * The text displayed in the title for the dialog that appears when you select
	 * a submodule
	 */
	public static final String SUBMODULE_DIALOG_TITLE = "Submodule_Dialog_Title";

	/**
	 * The tooltip for the submodule icon that appears on the left side of the
	 * file
	 */
	public static final String SUBMODULE = "Submodule";
	
	/**
   * The tooltip for a renamed file.
   */
  public static final String RENAMED_ICON_TOOLTIP = "Renamed_Icon_Tooltip";

	/**
	 * The text displayed on the label for the username
	 */
	public static final String LOGIN_DIALOG_USERNAME_LABEL = "Login_Dilaog_Username_Label";

	/**
	 * The text displayed on the label for the password
	 */
	public static final String LOGIN_DIALOG_PASS_WORD_LABEL = "Login_Dilaog_Password_Label";

	/**
	 * The text displayed on the title for the login dialog
	 */
	public static final String LOGIN_DIALOG_TITLE = "Login_Dialog_Title";

	/**
	 * The text displayed above the text fields
	 */
	public static final String LOGIN_DIALOG_MAIN_LABEL = "Login_Dialog_Main_Label";
	
	/**
	 * Authenticate.
	 */
	public static final String AUTHENTICATE = "Authenticate";
	
	/**
   * Basic authentication.
   */
  public static final String BASIC_AUTHENTICATION = "Basic_authentication";
  
  /**
   * Personal access token.
   */
  public static final String PERSONAL_ACCESS_TOKEN = "Personal_access_token";

	/**
	 * The text displayed on the first row of the login dialog if there are no
	 * credentials stored
	 */
	public static final String NO_CREDENTIALS_FOUND = "No_credentials_found";

	/**
	 * The text displayed on the first row of the login dialog if the credentials
	 * are invalid
	 */
	public static final String CHECK_CREDENTIALS = "Check_credentials";
	
	/**
	 * "Invalid token value."
	 */
	public static final String CHECK_TOKEN_VALUE_AND_PERMISSIONS = "Check_token_value_and_permissions";
	
	/**
	 * "Authentication failed"
	 */
	public static final String AUTHENTICATION_FAILED = "Authentication_failed";

	/**
	 * Local branch
	 */
	public static final String LOCAL_BRANCH = "Local_branch";

	/**
	 * Remote branch
	 */
	public static final String REMOTE_BRANCH = "Remote_branch";

	/**
	 * Upstream branch
	 */
	public static final String UPSTREAM_BRANCH = "Upstream_branch";
	
	/**
   * No upstream branch
   */
  public static final String NO_UPSTREAM_BRANCH = "No_upstream_branch";

	/**
	 * The text displayed on the right side of the toolbar buttons if the
	 * repository is one commit behind
	 */
	public static final String ONE_COMMIT_BEHIND = "One_commit_behind";

	/**
	 * The text displayed on the right side of the toolbar buttons if the
	 * repository is 2 or more commits behind
	 */
	public static final String COMMITS_BEHIND = "Commits_behind";

	/**
	 * The text displayed on the right side of the toolbar buttons if the
	 * repository is up to date
	 */
	public static final String TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE = "Toolbar_Panel_Information_Status_Up_To_Date";

	/**
	 * The text displayed on the right side of the toolbar buttons if the
	 * repository has a detached head
	 */
	public static final String TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD = "Toolbar_Panel_Information_Status_Detached_Head";

	/**
	 * The text displayed for the "Git" contextual menu item in the project view
	 */
	public static final String GIT = "Git";

	/**
	 * The text displayed for the "Git Diff" contextual menu item in the project
	 * view
	 */
	public static final String GIT_DIFF = "Git_Diff";

	/**
	 * The text displayed for the "Commit" contextual menu item in the project
	 * view
	 */
	public static final String COMMIT= "Commit";
	
	/**
	 * Committing.
	 */
	public static final String COMMITTING= "Committing";

	/**
	 * The text displayed when you push but don't have rights for that repository
	 */
	public static final String NO_RIGHTS_TO_PUSH_MESSAGE = "No_Right_To_Push_Message";

	/**
	 * The text displayed on the first row of the login dialog if the user entered
	 * doesn't have rights for that repository
	 */
	public static final String LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS = "Login_Dialog_Credentials_Doesnt_Have_Rights";

	/**
	 * The tooltip for the ChangeView button when the icon shows the tree view
	 * icon
	 */
	public static final String CHANGE_TREE_VIEW_BUTTON_TOOLTIP = "Change_Tree_View_Button_ToolTip";

	/**
	 * The tooltip for the ChangeView button when the icon shows the flat view
	 * icon
	 */
	public static final String CHANGE_TO_LIST_VIEW = "Change_To_List_View";

	/**
	 * The text displayed on the first row of the login dialog if the repository
	 * is private
	 */
	public static final String LOGIN_DIALOG_PRIVATE_REPOSITORY_MESSAGE = "Login_Dialog_Private_Repository_Message";

	/**
	 * The tooltip for the clone repository button
	 */
	public static final String CLONE_REPOSITORY_BUTTON_TOOLTIP = "Clone_Repository_Button_Tooltip";

	/**
	 * The text displayed in the title for the dialog that appears when you clone
	 * a new repository
	 */
	public static final String CLONE_REPOSITORY_DIALOG_TITLE = "Clone_Repository_Dialog_Title";

	/**
	 * The text displayed for the "URL" label in the clone repository dialog
	 */
	public static final String CLONE_REPOSITORY_DIALOG_URL_LABEL = "Clone_Repository_Dialog_Url_Label";

	/**
	 * The text displayed for the "Destination Path" label in the clone repository
	 * dialog
	 */
	public static final String CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_LABEL = "Clone_Repository_Dialog_Destination_Path_Label";

	/**
	 * The text displayed if the destination path is invalid
	 */
	public static final String CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH = "Clone_Repository_Dialog_Invalid_Destination_Path";

	/**
	 * The text displayed if an error occured during cloning
	 */
	public static final String CLONE_REPOSITORY_DIALOG_CLONE_ERROR = "Clone_Repository_Dialog_Clone_Error";

	/**
	 * The text displayed if the chosen destionation path is not an empty folder
	 */
	public static final String CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY = "Clone_Repository_Dialog_Destination_Path_Not_Empty";

	/**
	 * The text displayed if the URL doesn't point to a remote repository
	 */
	public static final String CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY = "Clone_Repository_Dialog_Url_Is_Not_A_Repository";

	/**
	 * The text displayed in the login dialog if you are not authorized to clone
	 * the repository
	 */
	public static final String CLONE_REPOSITORY_DIALOG_LOGIN_MESSAGE = "Clone_Repository_Dialog_Login_Message";

	/**
	 * The text in the title of the cloning progress dialog
	 */
	public static final String CLONE_PROGRESS_DIALOG_TITLE = "Cloning_Progress_Dialog_TItle";

	/**
	 * The text is displayed in the bottom left corner in the commit panel when
	 * the host is down
	 */
	public static final String CANNOT_REACH_HOST = "Cannot_Reach_Host";

	/**
	 * The text is displayed when your repository is on a detached head
	 */
	public static final String DETACHED_HEAD_MESSAGE = "Detached_Head_Message";

	/**
	 * The text is displayed you fix all conflicts and you try to pull without commiting the merge.
	 */
	public static final String CONCLUDE_MERGE_MESSAGE = "Conclude_Merge_Message";
	
	 /**
   * The text is displayed you fix all conflicts and you need to commit.
   * It appears as the commit message.
   */
  public static final String COMMIT_TO_MERGE = "Commit_to_merge";
  
  /**
   * Text for the toggle used to automatically push when committing.
   */
  public static final String PUSH_WHEN_COMMITTING = "Push_when_committing";

	/**
	 * The text is displayed when you pull but it fails because there are uncommitted changes.
	 */
	public static final String PULL_WOULD_OVERWRITE_UNCOMMITTED_CHANGES = "Pull_would_overwrite_uncommitted_changes";
	
	/**
	 * Lock failed when pulling.
	 */
  public static final String LOCK_FAILED = "Lock_failed";
  
  /**
   * Lock failed explanation.
   */
  public static final String LOCK_FAILED_EXPLANATION = "Lock_failed_explanation";
  
  /**
   * Cannot lock ref {0}.
   */
  public static final String CANNOT_LOCK_REF = "Cannot_lock_ref";
  
  /**
   * Unable to create {0}.
   */
  public static final String UNABLE_TO_CREATE_FILE = "Unable_to_create_file";
  
  /**
   * File exists.
   */
  public static final String FILE_EXISTS = "File_exists";
  
  /**
   * Error.
   */
  public static final String ERROR = "Error";
  
  /**
   * Remote.
   */
  public static final String REMOTE = "Remote";
  
  /**
   * Push to.
   */
  public static final String PUSH_TO = "Push_to";
  
  /**
   * Pull from.
   */
  public static final String PULL_FROM = "Pull_from";
  
  /**
   * Pull rebase.
   */
  public static final String PULL_REBASE = "Pull_rebase";
  
  /**
   * Pull rebase from {0}.
   */
  public static final String PULL_REBASE_FROM = "Pull_rebase_from";
  
  /**
   * Pull merge.
   */
  public static final String PULL_MERGE = "Pull_merge";
  
  /**
   * Pull merge.
   */
  public static final String PULL_MERGE_FROM = "Pull_merge_from";
  
  /**
   * Pull failed.
   */
  public static final String PULL_FAILED = "Pull_failed";
  
  /**
   * Push failed.
   */
  public static final String PUSH_FAILED = "Push_failed";
  
  /**
   * Rebase in progress.
   */
  public static final String REBASE_IN_PROGRESS = "Rebase_in_progress";
  
  /**
   * Something like: "Cannot continue rebasing. You have conflicts in the working copy that need to be resolved."
   */
  public static final String CANNOT_CONTINUE_REBASE_BECAUSE_OF_CONFLICTS = "Cannot_continue_rebase_because_of_conflicts";
  
  /**
   * Something like: "It seems you have a rebase in progress that was probably interrupted
   *  because of a conflict. You should first resolve the conflicts.
   *  If you have already resolved them, choose whether to continue or abort the rebase."
   */
  public static final String INTERRUPTED_REBASE = "Interrupted_rebase";
  
  /**
   * Mine.
   */
  public static final String MINE = "Mine";
  
  /**
   * Theirs.
   */
  public static final String THEIRS = "Theirs";
  
  /**
   * Message shown when invoking "Resolve using Mine" or "Resolve using Theirs" for a conflict generated when trying to rebase.
   */
  public static final String CONTINUE_RESOLVING_REBASE_CONFLICT_USING_MINE_OR_THEIRS = "Continue_resolving_rebase_conflict_using_mine_or_theirs";
  
  /**
   * The working branch.
   */
  public static final String THE_WORKING_BRANCH = "The_working_branch";
  
  /**
   * The upstream branch.
   */
  public static final String THE_UPSTREAM_BRANCH = "The_upstream_branch";
  
  /**
   * Confirmation message for "Restart merge".
   */
  public static final String RESTART_MERGE_CONFIRMATION = "Restart_merge_confirmation";
  
  /**
   * Abort merge
   */
  public static final String ABORT_MERGE = "Abort_merge";
  
  /**
   * Abort rebase.
   */
  public static final String ABORT_REBASE = "Abort_rebase";
  
  /**
   * Continue rebase.
   */
  public static final String CONTINUE_REBASE = "Continue_rebase";
  
  /**
   * Message shown when pull (rebase) failed because of uncommitted files.
   */
  public static final String PULL_REBASE_FAILED_BECAUSE_UNCOMMITTED = "Pull_rebase_failed_because_uncommitted";
  
  /**
   * Message shown when pull (rebase) failed because of conflicting paths.
   */
  public static final String PULL_REBASE_FAILED_BECAUSE_CONFLICTING_PATHS = "Pull_rebase_failed_because_conflicting_paths";

  /**
   * Show current branch history.
   */
  public static final String SHOW_CURRENT_BRANCH_HISTORY = "Show_current_branch_history";
  
  /**
   * Git History.
   */
  public static final String GIT_HISTORY = "Git_history";
  
  /**
   * Git Staging.
   */
  public static final String GIT_STAGING = "Git_staging";
  
  /**
   * Action in the history panel.
   */
  public static final String REFRESH = "Refresh";
  /**
   * Message presented on a tooltip when the local branch is not connected to any upstream branch.
   */
  public static final String NO_REMOTE_BRANCH = "No_remote_branch";
  /**
   * Action name to compare a file with its previous version.
   */
  public static final String COMPARE_WITH_PREVIOUS_VERSION = "Compare_with_previous_version";
  /**
   * Action name to compare a file at a given revision with the version from the working tree/copy.
   */
  public static final String COMPARE_WITH_WORKING_TREE_VERSION = "Compare_with_working_tree_version";

  /**
   * Action name that opens a file and presents the file name as well.
   * 
   * en: Open {0}
   */
  public static final String OPEN_FILE = "Open_file";
  /**
   * Action name to compare a file with its previous version.
   * 
   * en: Compare {0} with previous version
   */
  public static final String COMPARE_FILE_WITH_PREVIOUS_VERSION = "Compare_file_with_previous_version";
  /**
   * Action name to compare a file at a given revision with the version from the working tree/copy.
   * 
   * en: Compare {0} with working copy version
   */
  public static final String COMPARE_FILE_WITH_WORKING_TREE_VERSION = "Compare_file_with_working_tree_version";
  /**
   * Contextual action in the staging panel.
   * 
   * en: Show history
   */
  public static final String SHOW_HISTORY = "Show_history";
  /**
   * Contextual action in the staging panel.
   * 
   * en: Show blame
   */
  public static final String SHOW_BLAME = "Show_blame";
  /**
   * History table column name. The person that made that commit.
   */
  public static final String PARENTS = "Parents";
  /**
   * 
   */
  public static final String AUTHOR = "Author";
  /**
   * History table column name.
   */
  public static final String DATE = "Date";
  /**
   * History table column name.
   */
  public static final String COMMITTER = "Comitter";
  /**
   * Cancel. Taken from oXygen's "translation.xml".
   */
  public static final String CANCEL = "Cancel";
  
  /**
   * Clear history.
   */
  public static final String CLEAR_HISTORY = "Clear_history";
  /**
   * Clear history confirmation (something like: "Are you sure...?")
   */
  public static final String CLEAR_HISTORY_CONFIRMATION = "Clear_history_confirmation";
  
  /**
   * The text displayed for the "Open previous version" contextual menu item.
   */
  public static final String OPEN_PREVIOUS_VERSION = "Open_previous_version";
  
  /**
   * Nothing to show for new files.
   */
  public static final String NOTHING_TO_SHOW_FOR_NEW_FILES = "Nothing_to_show_for_new_files";
  
  /**
   * This operation requires saving {0}.
   */
  public static final String THIS_OPERATION_REQUIRES_SAVING = "This_operation_requires_saving";
  
  /**
   * Warning shown when trying to amend pushed commit.
   */
  public static final String AMEND_PUSHED_COMMIT_WARNING = "Amend_pushed_commit_warning";
  
  /**
   * Amend last commit.
   */
  public static final String AMEND_LAST_COMMIT = "Amend_last_commit";
  
  /**
   * The text displayed after exiting the diff for a rebase conflict file and you didn't modify anything.
   */
  public static final String KEEP_RESOLVED_VERSION_FOR_REBASE_CONFLICT = "Keep_resolved_version_for_rebase_conflict";
  
  /**
   * Nothing to push.
   */
  public static final String NOTHING_TO_PUSH = "Nothing_to_push";
  
  /**
   * 1 commit ahead.
   */
  public static final String ONE_COMMIT_AHEAD = "One_commit_ahead";
  
  /**
   * X commits ahead.
   */
  public static final String COMMITS_AHEAD = "Commits_ahead";
  
  /**
   * Cannot pull.
   */
  public static final String CANNOT_PULL = "Cannot_pull";
  
  /**
   * Push to create and track remote branch {0}.
   */
  public static final String PUSH_TO_CREATE_AND_TRACK_REMOTE_BRANCH = "Push_to_create_and_track_remote_branch";
  
  /**
   * Push to track existing remote branch {0}.
   */
  public static final String PUSH_TO_TRACK_REMOTE_BRANCH = "Push_to_track_remote_branch";
  
  /**
   * Reset all credentials.
   */
  public static final String RESET_ALL_CREDENTIALS = "Reset_all_credentials";
  
  /**
   * Confirmation message shown when invoking "Reset credentials".
   */
  public static final String RESET_CREDENTIALS_CONFIRM_MESAGE = "Reset_credentials_confirm_mesage";
  
  /**
   * Title for no message when commit
   */
  public static final String NO_COMMIT_MESSAGE_TITLE = "No_commit_message_provided";
  
  /**
   * Informations when about commit without a message   
   */
  public static final String NO_COMMIT_MESSAGE_DIALOG = "The_commit_message_is_empty";
  
  /**
   * Amended successfully.
   */
  public static final String AMENDED_SUCCESSFULLY = "Amended_successfully";
  
  /**
   * Upstream branch {0} does not exist.
   */
  public static final String UPSTREAM_BRANCH_DOES_NOT_EXIST = "Upstream_branch_does_not_exist";
  
  /**
   * Create branch
   */
  public static final String CREATE_BRANCH = "Create_branch";
  
  /**
   * Branch name
   */
  public static final String BRANCH_NAME = "Branch_name";
  
  /**
   * Message shown when cannot checkout newly created branch because of uncommitted changes.
   */
  public static final String CANNOT_CHECKOUT_NEW_BRANCH_BECAUSE_UNCOMMITTED_CHANGES = 
      "Cannot_checkout_new_branch_because_uncommitted_changes";
  
  /**
   * Message shown when cannot checkout newly created branch because of conflicts.
   */
  public static final String CANNOT_CHECKOUT_NEW_BRANCH_WHEN_HAVING_CONFLICTS = 
      "Cannot_checkout_new_branch_when_having_conflicts";
  
  /**
   * Message shown when cannot checkout newly created branch for an uknown reason.
   */
  public static final String CANNOT_CHECKOUT_NEW_BRANCH = "Cannot_checkout_new_branch";
  
  /**
   * Commit anyway.
   */
  public static final String COMMIT_ANYWAY = "Commit_anyway";
  
  /**
   * No details available.
   */
  public static final String NO_DETAILS_AVAILABLE = "No_details_available";
  
  /**
   * Details.
   */
  public static final String DETAILS = "Details";
  
  /**
   * A custom message for the Git "pre-receive hook declined" message.
   */
  public static final String PRE_RECEIVE_HOOK_DECLINED_CUSTOM_MESSAGE = "Pre_receive_hook_declined_custom_message";
  /**
   * "Branch"
   */
  public static final String BRANCH = "Branch";
  /**
   * Preferences. Translation loaded from Oxygen.
   */
  public static final String PREFERENCES = "Preferences";
  /**
   * Label in preferences.
   */
  public static final String WHEN_DETECTING_REPO_IN_PROJECT = "When_detecting_repo_in_project";
  /**
   * Label in preferences.
   */
  public static final String ALWAYS_SWITCH_TO_DETECTED_WORKING_COPY = "Always_switch_to_detected_working_copy";
  /**
   * Label in preferences.
   */
  public static final String ASK_SWITCH_TO_DETECTED_WORKING_COPY = "Ask_switch_to_detected_working_copy";
  /**
   * Label in preferences.
   */
  public static final String NEVER_SWITCH_TO_DETECTED_WORKING_COPY = "Never_switch_to_detected_working_copy";
  /**
   * Message for when the passphrase for the SSH key is required.
   */
  public static final String SSH_KEY_PASSPHRASE_REQUIRED = "SSH_key_passphrase_required";
  /**
   * Message for when the HTTPS user name and password are required.
   */
  public static final String USERNAME_AND_PASSWORD_REQUIRED = "Username_and_password_required";
  /**
   * Hover for details.
   */
  public static final String HOVER_FOR_DETAILS = "Hover_for_details";
  /**
   * Git Client. Translation loaded from Oxygen.
   */
  public static final String GIT_CLIENT = "Git_client";
  /**
   * Use filter to serach 
   */
  public static final String USE_FILTER_TO_SEARCH = "Use_filter_to_search";
  /**
   * Type text to filter 
   */
  public static final String TYPE_TEXT_TO_FILTER = "Type_text_to_filter";
  /**
   * "Create". Loaded from Oxygen.
   */
  public static final String CREATE = "Create";
  /**
   * Update submodules after a pull action.
   */
  public static final String UPDATE_SUBMODULES_ON_PULL = "Update_submodules_on_pull";
  /**
   * The new commit tracked by a submodule.
   */
  public static final String SUBMODULE_NEW_TRACKED_COMMIT = "Submodule_new_tracked_commit";
  /**
   * The previous commit tracked by a submodule.
   */
  public static final String SUBMODULE_PREVIOUS_TRACKED_COMMIT = "Submodule_previous_tracked_commit";
  /**
   * The tile of the question dialog shown when you try to switch branches and you have uncommitted changes. 
   */
  public static final String SWITCH_BRANCH = "Switch_branch";
  /**
   * The message in the question dialog shown when you try to switch branches and you have uncommitted changes. 
   */
  public static final String UNCOMMITTED_CHANGES_WHEN_SWITCHING_BRANCHES = "Uncommitted_changes_when_switching_branches";
  /**
   * The message on the OK button of the question dialog shown when you try to switch branches and you have uncommitted changes.
   */
  public static final String MOVE_CHANGES = "Move_changes";
  /**
   * The message on the action that reverts a selected commit.
   */
  public static final String REVERT_COMMIT = "Revert_Commit";
  /**
   * The message that warns about a revert.
   */
  public static final String REVERT_COMMIT_WARNING = "Revert_Commit_Warning";
  /**
   * Stash warning title.
   */
  public static final String STASH = "Stash";
  /**
   * Stash apply failed.
   */
  public static final String STASH_APPLY_FAILED = "Stash_Apply_Failed";
  
}
