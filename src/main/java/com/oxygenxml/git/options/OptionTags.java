package com.oxygenxml.git.options;

/**
 * Contains all of the option Tags
 * 
 * @author alex_jitianu
 * @author gabriel_nedianu
 *
 */
public interface OptionTags {
  
  /**
   * <code>true</code> to automatically push to remote when committing.
   */
  public static final String AUTO_PUSH_WHEN_COMMITTING = "git.auto.push.when.committing";
  
  /**
   * Stores the option selected to notify or not if there are new changes in the remote
   */
  public static final String NOTIFY_ABOUT_NEW_REMOTE_COMMITS = "git.notify.about.new.remote.commits";

  /**
   * <code>true</code> to automatically checkout a newly created local branch.
   */
  public static final String CHECKOUT_NEWLY_CREATED_LOCAL_BRANCH = "git.checkout.newly.created.local.branch";
  
  /**
   * The current loaded repository in the staging view.
   */
  public static final String SELECTED_REPOSITORY = "git.selected.repository";
  
  /**
   * Wrapper for a list with the repository locations
   */
  public static final String REPOSITORY_LOCATIONS = "git.repository.locations";

  /**
   * Wrapper for a list of previously selected destination paths
   */
  public static final String DESTINATION_PATHS = "git.destination.paths";

  /**
   * The default pull type: with merge or rebase.
   */
  public static final String DEFAULT_PULL_TYPE = "git.default.pull.type";

  /**
   * The view mode for the unstaged resources: tree or table.
   */
  public static final String UNSTAGED_RES_VIEW_MODE = "git.unstaged.res.view.mode";
  
  /**
   * The view mode for the staged resources: tree or table.
   */
  public static final String STAGED_RES_VIEW_MODE = "git.staged.res.view.mode";

  /**
   * Wrapper for a list of project.xpr that were tested if they want to be a git
   * repository
   */
  public static final String PROJECTS_TESTED_FOR_GIT = "git.projects.tested.for.git";

  /**
   * A list of user credentials containing the username, password and the host.
   * Only one credential per host can be stored
   */
  public static final String USER_CREDENTIALS_LIST = "git.projects.tested.for.git";

  /**
   * Wrapper for a list of commit messages
   */
  public static final String COMMIT_MESSAGES = "git.commit.messages";
  
  /**
   * The passphrase for the SSH
   */
  public static final String PASSPHRASE = "git.passphrase";
  
  /**
   * Option about what to do when opening a prject in Oxygen and detecting a Git repository.
   */
  public static final String WHEN_REPO_DETECTED_IN_PROJECT = "git.when.repo.detected.in.project";
  
  /**
   * Option used to update submodules on pull when <code>true</code>.
   */
  public static final String UPDATE_SUBMODULES_ON_PULL = "git.update.submodules.on.pull";
  
  /**
   * The id from the last commit fetched.
   */
  public static final String WARN_ON_CHANGE_COMMIT_ID = "git.warn.on.change.commit";

  /**
   * A cache for the SSH questions and the user answer.
   */
  public static final String SSH_PROMPT_ANSWERS = "git.ssh.prompt.answers";

  /**
   * A list of personal access token + host entries. Only one personal access token per host.
   */
  public static final String PERSONAL_ACCES_TOKENS_LIST = "git.personal.acces.tokens.list";


}
