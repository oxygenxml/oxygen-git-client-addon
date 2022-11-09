package com.oxygenxml.git.options;

/**
 * Contains all of the option Tags
 * 
 * @author alex_jitianu
 * @author gabriel_nedianu
 *
 */
public class OptionTags {
  
  /**
    * Constructor.
    *
    * @throws UnsupportedOperationException when invoked.
    */
  private OptionTags() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }
  
  /**
   * <code>true</code> to automatically push to remote when committing.
   */
  public static final String AUTO_PUSH_WHEN_COMMITTING = "com.oxygenxml.git.auto.push.when.committing";
  
  /**
   * Stores the option selected to notify or not if there are new changes in the remote
   */
  public static final String NOTIFY_ABOUT_NEW_REMOTE_COMMITS = "com.oxygenxml.git.notify.about.new.remote.commits";

  /**
   * <code>true</code> to automatically checkout a newly created local branch.
   */
  public static final String CHECKOUT_NEWLY_CREATED_LOCAL_BRANCH = "com.oxygenxml.git.checkout.newly.created.local.branch";
  
  /**
   * The current loaded repository in the staging view.
   */
  public static final String SELECTED_REPOSITORY = "com.oxygenxml.git.selected.repository";
  
  /**
   * Wrapper for a list with the repository locations
   */
  public static final String REPOSITORY_LOCATIONS = "com.oxygenxml.git.repository.locations";

  /**
   * Wrapper for a list of previously selected destination paths
   */
  public static final String DESTINATION_PATHS = "com.oxygenxml.git.destination.paths";

  /**
   * The default pull type: with merge or rebase.
   */
  public static final String DEFAULT_PULL_TYPE = "com.oxygenxml.git.default.pull.type";

  /**
   * The view mode for the unstaged resources: tree or table.
   */
  public static final String UNSTAGED_RES_VIEW_MODE = "com.oxygenxml.git.unstaged.res.view.mode";
  
  /**
   * The view mode for the staged resources: tree or table.
   */
  public static final String STAGED_RES_VIEW_MODE = "com.oxygenxml.git.staged.res.view.mode";

  /**
   * Wrapper for a list of project.xpr that were tested if they want to be a git
   * repository
   */
  public static final String PROJECTS_TESTED_FOR_GIT = "com.oxygenxml.git.projects.tested.for.git";

  /**
   * A list of user credentials containing the username, password and the host.
   * Only one credential per host can be stored
   */
  public static final String USER_CREDENTIALS_LIST = "com.oxygenxml.user.credentials.list";

  /**
   * Wrapper for a list of commit messages
   */
  public static final String COMMIT_MESSAGES = "com.oxygenxml.git.commit.messages";
  
  /**
   * The passphrase for the SSH
   */
  public static final String PASSPHRASE = "com.oxygenxml.git.passphrase";
  
  /**
   * Option about what to do when opening a prject in Oxygen and detecting a Git repository.
   */
  public static final String WHEN_REPO_DETECTED_IN_PROJECT = "com.oxygenxml.git.when.repo.detected.in.project";
  
  /**
   * Option used to update submodules on pull when <code>true</code>.
   */
  public static final String UPDATE_SUBMODULES_ON_PULL = "com.oxygenxml.git.update.submodules.on.pull";
  
  /**
   * The id from the last commit fetched.
   */
  public static final String WARN_ON_CHANGE_COMMIT_ID = "com.oxygenxml.git.warn.on.change.commit";

  /**
   * A cache for the SSH questions and the user answer.
   */
  public static final String SSH_PROMPT_ANSWERS = "com.oxygenxml.git.ssh.prompt.answers";

  /**
   * A list of personal access token + host entries. Only one personal access token per host.
   */
  public static final String PERSONAL_ACCES_TOKENS_LIST = "com.oxygenxml.git.personal.acces.tokens.list";

  /**
   * The tag option for including the untracked files in the stash.
   */
  public static final String STASH_INCLUDE_UNTRACKED = "com.oxygenxml.git.stash.include.untracked";
  
  /**
   * <code>true</code> if the create new branch option is selected.
   */
  public static final String CHECKOUT_COMMIT_SELECT_NEW_BRANCH = "com.oxygenxml.git.checkout.commit.select.new.branch";
  
  /**
   * The tag option for history strategy to present commits history.
   */
  public static final String HISTORY_STRATEGY = "com.oxygenxml.git.history.strategy";

  /**
   * The tag option for validate files before commit.
   */
  public static final String VALIDATE_FILES_BEFORE_COMMIT = "com.oxygenxml.git.validator.validate";
  
  /**
   * The tag option for reject commit on validation problems.
   */
  public static final String REJECT_COMMIT_ON_VALIDATION_PROBLEMS = "com.oxygenxml.git.validator.reject";
  
  /**
   * The tag option for validate main files before push.
   */
  public static final String VALIDATE_MAIN_FILES_BEFORE_PUSH = "com.oxygenxml.git.validator.push.validate";
  
  /**
   * The tag option for reject push on validation problems.
   */
  public static final String REJECT_PUSH_ON_VALIDATION_PROBLEMS = "com.oxygenxml.git.validator.push.reject";
  
  /**
   * The tag option for "Detect and open xpr files from opened working copies".
   */
  public static final String DETECT_AND_OPEN_XPR_FILES = "com.oxygenxml.git.detect.and.open.xpr.files";
  
  /**
   * The tag option for use SSH agent.
   */
  public static final String USE_SSH_AGENT = "com.oxygenxml.git.use.ssh.agent";
  
  /**
   * The tag option for default SSH agent.
   */
  public static final String DEFAULT_SSH_AGENT = "com.oxygenxml.git.default.ssh.agent";

}
