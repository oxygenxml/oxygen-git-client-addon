package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.errors.NoMergeBaseException.MergeBaseFailureReason;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import com.oxygenxml.git.auth.AuthExceptionMessagePresenter;
import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.BranchGitEventInfo;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Implements some basic git functionality like commit, push, pull, retrieve
 * File status(staged, unstaged)
 * 
 * @author Beniamin Savu
 */
public class GitAccess {
  /**
	 * A synthetic object representing the uncommitted changes.
	 */
  public static final CommitCharacteristics UNCOMMITED_CHANGES = new CommitCharacteristics(
      Translator.getInstance().getTranslation(Tags.UNCOMMITTED_CHANGES), null, "*", "*", "*", null, null);
  /**
	 * Logger for logging.
	 */
	private static final Logger logger = Logger.getLogger(GitAccess.class);
	/**
	 * The GIT repository.
	 */
	private Git git;
	/**
	 * Singleton instance.
	 */
	private static GitAccess instance;
	/**
	 * Translation support.
	 */
	private static final Translator translator = Translator.getInstance();
	/**
	 * Receive notifications when things change.
	 */
	private HashSet<GitEventListener> gitEventListeners = new LinkedHashSet<>();

	 /**
   * Singleton instance.
   */
	private GitAccess() {}

	 /**
   * Singleton instance.
   */
	public static GitAccess getInstance() {
		if (instance == null) {
			instance = new GitAccess();
		}
		return instance;
	}

	/**
	 * Creates a local clone of the given repository and loads it.
	 * 
	 * @param url Remote repository to clone.
	 * @param directory Local directory in which to create the clone.
	 * @param progressDialog Progress support.
	 * @param branchName     The name of the branch to clone and checkout. Must be
	 *                       specified as full ref names (e.g.
	 *                       "refs/heads/hotfixes/17.0").
	 * 
	 * @throws GitAPIException
	 */
	public void clone(URIish url, File directory, final ProgressDialog progressDialog, String branchName)
			throws GitAPIException {
	  closeRepo();
	  
		// Intercept all authentication requests.
    String host = url.getHost();
    AuthenticationInterceptor.bind(host);
		UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(host);
		String username = gitCredentials.getUsername();
		String password = gitCredentials.getPassword();

		ProgressMonitor p = new ProgressMonitor() {
			String taskTitle;
			float totalWork;
			float currentWork = 0;

			@Override
			public void update(int completed) {
			  currentWork += completed;
			  if (progressDialog != null) {
			    String text = "";
			    if (totalWork != 0) {
			      float percentFloat = currentWork / totalWork * 100;
			      int percent = (int) percentFloat;
			      text = taskTitle + " " + percent + "% completed";
			    } else {
			      text = taskTitle + "100% completed";
			    }
			    progressDialog.setNote(text);
			  }
			}

			@Override
      public void start(int totalTasks) {
				currentWork = 0;
			}

			@Override
			public boolean isCancelled() {
			  boolean isCanceled = false;
			  if (progressDialog != null) {
			    if (progressDialog.isCanceled()) {
			      progressDialog.setNote("Canceling...");
			    }
			    isCanceled = progressDialog.isCanceled();
			  }
			  return isCanceled;
			}

			@Override
      public void endTask() {
				currentWork = 0;
			}

			@Override
      public void beginTask(String title, int totalWork) {
				currentWork = 0;
				this.taskTitle = title;
				this.totalWork = totalWork;
			}
		};
		
		if (progressDialog != null) {
		  progressDialog.setNote("Initializing...");
		}
		
		String pass = OptionsManager.getInstance().getSshPassphrase();
		CloneCommand cloneCommand = Git.cloneRepository().setURI(url.toString()).setDirectory(directory)
		    .setCredentialsProvider(new SSHCapableUserCredentialsProvider(username, password, pass, url.getHost()))
		    .setProgressMonitor(p);
		if (branchName != null) {
			git = cloneCommand.setBranchesToClone(Arrays.asList(branchName)).setBranch(branchName).call();
		} else {
		  git = cloneCommand.call();
		}
		
		fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, directory));
	}
	
	/**
   * Sets the Git repository asynchronously, on a new thread, and at the end updates the
   * GUI on AWT. The repository file path must exist.
   * 
   * @param path A string that specifies the Git repository folder.
   */
	public void setRepositoryAsync(String path) {
	  GitOperationScheduler.getInstance().schedule(() -> {
	    try {
	      openRepository(path);
	    } catch (IOException e) {
	      //  openRepository() already notified the listeners.
	      logger.debug(e, e);
	    }
	  });
	}
  
  /**
   * Sets the Git repository on the current thread. The repository file path must exist.
   * 
   * @param path A string that specifies the Git repository folder.
   */
  public void setRepositorySynchronously(String path) throws IOException {
    openRepository(path);
  }

  /**
   * Check if the given repository is the current one.
   * 
   * @param repo Repository.
   * 
   * @return <code>true</code> if the given repository is the current one.
   */
  private boolean isCurrentRepo(final File repo) {
    return git != null && repo.equals(git.getRepository().getDirectory());
  }

  /**
   * Open repo.
   * 
   * @param currentRepo The repo to open.
   * 
   * @return a {@link org.eclipse.jgit.api.Git} object for the existing git repository.
   * 
   * @throws IOException
   */
  private void openRepository(String path) throws IOException {
    final File repo = new File(path + "/.git");
    if (!isCurrentRepo(repo) ) {
      File workingCopy = repo.getParentFile();
      fireOperationAboutToStart(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, workingCopy));
      closeRepo();
      try {
        git = Git.open(repo);
        repositoryOpened(workingCopy);
      } catch (IOException e) {
        fireOperationFailed(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, workingCopy), e);
        throw e;
      }
    }
  }

  /**
   * Actions to do after a repository was opened.
   * 
   * @param workingCopy The WC.
   */
  private void repositoryOpened(File workingCopy) {
    // Start intercepting authentication requests.
    AuthenticationInterceptor.bind(getHostName());

    if (logger.isDebugEnabled()) {
      logSshKeyLoadingData();
    }

    fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, workingCopy));
  }

	/**
	 * Log SSH key loading location data.
	 * 
	 * @param path The path to the Git repository folder.
	 */
  private void logSshKeyLoadingData() {
    // Debug data for the SSH key load location.
    logger.debug("Java env user home: " + System.getProperty("user.home"));
    try {
      Repository repository = getRepository();
      logger.debug("Load repository " + repository.getDirectory());
      
      FS fs = repository.getFS();
      if (fs != null) {
        File userHome = fs.userHome();
        logger.debug("User home " + userHome);

        File sshDir = new File(userHome, ".ssh");

        boolean exists = sshDir.exists();
        logger.debug("SSH dir exists " + exists);
        if (exists) {
          File[] listFiles = sshDir.listFiles();
          for (int i = 0; i < listFiles.length; i++) {
            logger.debug("SSH resource path " + listFiles[i]);
          }
        }
      } else {
        logger.debug("Null FS");
      }
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
  }
  
  /**
   * Fire operation about to start.
   * 
   * @param info event info.
   */
  private void fireOperationAboutToStart(GitEventInfo info) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation about to start: " + info);
    }
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationAboutToStart(info);
    }
  }
  
  /**
   * Fire operation successfully ended.
   * 
   * @param info event info.
   */
  private void fireOperationSuccessfullyEnded(GitEventInfo info) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation successfully ended: " + info);
    }
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationSuccessfullyEnded(info);
    }
  }
  
  /**
   * Fire operation failed.
   * 
   * @param info event info.
   * @param t related exception/error. May be <code>null</code>.
   */
  private void fireOperationFailed(GitEventInfo info, Throwable t) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation failed: " + info + ". Reason: " + t.getMessage());
    }
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationFailed(info, t);
    }
  }
  
  /**
   * Add a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to add.
   */
  @SuppressWarnings("unchecked")
	public void addGitListener(GitEventListener listener) {
	  HashSet<GitEventListener> clone = (HashSet<GitEventListener>) gitEventListeners.clone();
	  clone.add(listener);
	  
	  gitEventListeners = clone;
  }
	
  /**
	 * Get repository.
	 * 
	 * @return the Git repository.
	 * 
	 * @throws NoRepositorySelected
	 */
	public Repository getRepository() throws NoRepositorySelected {
		if (git == null) {
			throw new NoRepositorySelected("Repository is empty");
		}
		return git.getRepository();
	}
	
	/**
	 * Gets the Working copy location.
	 * 
	 * @return The working copy location.
	 * 
	 * @throws NoRepositorySelected
	 */
	public File getWorkingCopy() throws NoRepositorySelected {
	  return getRepository().getWorkTree();
	}

	/**
	 * Creates a blank new Repository.
	 * 
	 * @param path - A string that specifies the git Repository folder
	 */
	public void createNewRepository(String path) throws GitAPIException {
	  File wc = new File(path);
	  fireOperationAboutToStart(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, wc));
    closeRepo();
    try {
      git = Git.init().setBare(false).setDirectory(wc).call();
      fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, wc));
    } catch (GitAPIException e) {
      fireOperationFailed(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, wc), e);
      throw e;
    }
	}
	
	/**
	 * @return A status of the Working Copy, with the unstaged and staged files.
	 */
	public GitStatus getStatus() {
	  GitStatus gitStatus = null;
	  if (git != null) {
	    try {
	      logger.debug("-- Compute our GitStatus -> getStatus() --");
	      Status status = git.status().call();
	      logger.debug("-- Get JGit status -> git.status().call() --");
	      gitStatus = new GitStatus(getUnstagedFiles(status), getStagedFiles(status));
	    } catch (GitAPIException e) {
	      logger.error(e, e);
	    }
	  }
    return gitStatus != null ? gitStatus 
        : new GitStatus(Collections.emptyList(),Collections.emptyList());
  }
	
	/**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.
   * 
   * @return - A list with all unstaged files
   */
  public List<FileStatus> getUnstagedFiles() {
    return getUnstagedFiles(Collections.<String>emptyList());
  }
  
  /**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.
   * 
   * @param paths A subset of interest.
   * 
	 * @return - A list with the files from the given set that are un-staged as well
	 *         as their states.
   */
  public List<FileStatus> getUnstagedFiles(Collection<String> paths) {
    if (git != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("PUBLIC - GET UNSTAGED FILES");
        logger.debug("Prepare fot JGit status, in paths " + paths);
      }
      
      StatusCommand statusCmd = git.status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        statusCmd.addPath(iterator.next());
      }
      try {
        Status status = statusCmd.call();
        logger.debug("JGit Status computed: " + status);
        return getUnstagedFiles(status);
      } catch (GitAPIException e) {
        logger.error(e, e);
      }
    }
    
    return Collections.emptyList();
  }

	/**
	 * Makes a diff between the files from the last commit and the files from the
	 * working directory. If there are diffs, they will be saved and returned.
	 * 
	 * @param status The repository's status.
	 * 
	 * @return The unstaged files and their states.
	 */
	private List<FileStatus> getUnstagedFiles(Status status) {
	  logger.debug("PRIVATE - GET UNSTAGE FOR GIVEN STATUS " + status);
		List<FileStatus> unstagedFiles = new ArrayList<>();
		if (git != null) {
			try {
				Set<String> submodules = getSubmodules();
        addSubmodulesToUnstaged(unstagedFiles, submodules);
				addUntrackedFilesToUnstaged(status, unstagedFiles, submodules);
        addModifiedFilesToUnstaged(status, unstagedFiles, submodules);
        addMissingFilesToUnstaged(status, unstagedFiles, submodules);
				addConflictingFilesToUnstaged(status, unstagedFiles);
			} catch (NoWorkTreeException | GitAPIException e1) {
			  logger.error(e1, e1);
			}
		}
		return unstagedFiles;
	}

	/**
	 * Add conflicting files to the list of resources that are not staged.
	 * 
	 * @param status        The repository's status.
	 * @param unstagedFiles The list of unstaged (not in the INDEX) files.
	 */
  private void addConflictingFilesToUnstaged(Status status, List<FileStatus> unstagedFiles) {
    if (logger.isDebugEnabled()) {
      logger.debug("addConflictingFilesToUnstaged: " + status.getConflicting());
    }
    for (String fileName : status.getConflicting()) {
      unstagedFiles.add(new FileStatus(GitChangeType.CONFLICT, fileName));
    }
  }

  /**
	 * Add missing files to the list of resources that are not staged (not in the
	 * INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addMissingFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (logger.isDebugEnabled()) {
      logger.debug("addMissingFilesToUnstaged: " + status.getMissing());
    }
    for (String string : status.getMissing()) {
    	if (!submodules.contains(string)) {
    		unstagedFiles.add(new FileStatus(GitChangeType.MISSING, string));
    	}
    }
  }

  /**
	 * Add modified files to the list of resources that are not staged (not in the
	 * INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addModifiedFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (logger.isDebugEnabled()) {
      logger.debug("addModifiedFilesToUnstaged " + status.getModified());
    }
    for (String string : status.getModified()) {
      // A file that was modified compared to the one from INDEX.
    	if (!submodules.contains(string)) {
    		unstagedFiles.add(new FileStatus(GitChangeType.MODIFIED, string));
    	}
    }
  }

  /**
	 * Add untracked files (i.e. newly created files) to the list of resources that
	 * are not staged (not in the INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addUntrackedFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (logger.isDebugEnabled()) {
      logger.debug("addUntrackedFilesToUnstaged " + status.getUntracked());
    }
    for (String string : status.getUntracked()) {
    	if (!submodules.contains(string)) {
    		unstagedFiles.add(new FileStatus(GitChangeType.UNTRACKED, string));
    	}
    }
  }

  /**
   * Add submodules to the list of resources that are not staged.
   * 
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   * 
	 * @throws GitAPIException When an error occurs when trying to check the
	 *                         submodules status.
   */
  private void addSubmodulesToUnstaged(List<FileStatus> unstagedFiles, Set<String> submodules) throws GitAPIException {
    if (logger.isDebugEnabled()) {
      logger.debug("addSubmodulesToUnstaged " + submodules);
    }
    for (String string : submodules) {
    	SubmoduleStatus submoduleStatus = git.submoduleStatus().call().get(string);
			if (submoduleStatus != null && submoduleStatus.getHeadId() != null
    	    && !submoduleStatus.getHeadId().equals(submoduleStatus.getIndexId())) {
    		unstagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, string));
    	}
    }
  }

	/**
	 * Returns for the given submodule the SHA-1 commit id for the Index if the
	 * given index boolean is <code>true</code> or the SHA-1 commit id for the HEAD
	 * if the given index boolean is <code>false</code>
	 * 
	 * @param submodulePath - the path to get the submodule
	 * @param index         - boolean to determine what commit id to return
	 * @return the SHA-1 id
	 */
	public ObjectId submoduleCompare(String submodulePath, boolean index) {
		try {
			SubmoduleStatus submoduleStatus = git.submoduleStatus().addPath(submodulePath).call().get(submodulePath);
			if (submoduleStatus != null) {
			  if (index) {
			    return submoduleStatus.getIndexId();
			  } else {
			    return submoduleStatus.getHeadId();
			  }
			}
		} catch (GitAPIException e) {
		  logger.error(e, e);
		}
		return null;
	}

	/**
	 * Returns a list with all the submodules name for the current repository
	 * 
	 * @return a list containing all the submodules
	 */
	public Set<String> getSubmodules() {
		try {
			if (git != null) {
				return git.submoduleStatus().call().keySet();
			}
		} catch (GitAPIException e) {
		  logger.error(e, e);
		}
		return new HashSet<>();
	}

	/**
	 * Sets the given submodule as the current repository
	 * 
	 * @param submodule - the name of the submodule
	 * @throws IOException Failed to load the submodule.
	 * @throws GitAPIException Failed to load the submodule.
	 */
	public void setSubmodule(String submodule) throws IOException, GitAPIException {
		Repository parentRepository = git.getRepository();
		Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		
		if (submoduleRepository == null) {
		  // The submodule wasn't updated.
		  git.submoduleInit().call();
		  git.submoduleUpdate().call();
		  
		  submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		}
		
		git = Git.wrap(submoduleRepository);
		
		fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, submoduleRepository.getDirectory()));
	}
	
	 /**
   * Commits a single file locally
   * 
   * @param file    - File to be committed
   * @param message - Message for the commit
   * 
   * All JGit exceptions have a common ancestor, but sub classes offer different API for getting extra information
   * about the cause of the exception.
   * 
   * @throws AbortedByHookException The commit failed because it a hook rejected it.
   * @throws ConcurrentRefUpdateException Exception thrown when a command wants to update a ref but failed because
   * another process is accessing (or even also updating) the ref.
   * @throws NoHeadException Exception thrown when a command expected the {@code HEAD} reference to exist
   * but couldn't find such a reference
   * @throws NoMessageException A commit was called without explicitly specifying a commit message
   * @throws UnmergedPathsException Thrown when branch deletion fails due to unmerged data
   * @throws WrongRepositoryStateException Exception thrown when the state of the repository doesn't allow the execution
   * of a certain command. E.g. when a CommitCommand should be executed on a repository with unresolved conflicts this exception will be thrown.
   * @throws GitAPIException Other unexpected exceptions.
   */
  public void commit(String message) throws GitAPIException, NoHeadException, //NOSONAR See doc above.
      NoMessageException, UnmergedPathsException, //NOSONAR See doc above.
      ConcurrentRefUpdateException, WrongRepositoryStateException, //NOSONAR See doc above.
      AbortedByHookException { //NOSONAR See doc above.
    commit(message, false);
  }

	/**
	 * Commits a single file locally
	 * 
	 * @param file    - File to be committed
	 * @param message - Message for the commit
	 * 
	 * All JGit exceptions have a common ancestor, but sub classes offer different API for getting extra information
	 * about the cause of the exception.
	 * 
	 * @throws AbortedByHookException The commit failed because it a hook rejected it.
	 * @throws ConcurrentRefUpdateException Exception thrown when a command wants to update a ref but failed because
	 * another process is accessing (or even also updating) the ref.
	 * @throws NoHeadException Exception thrown when a command expected the {@code HEAD} reference to exist
	 * but couldn't find such a reference
	 * @throws NoMessageException A commit was called without explicitly specifying a commit message
	 * @throws UnmergedPathsException Thrown when branch deletion fails due to unmerged data
	 * @throws WrongRepositoryStateException Exception thrown when the state of the repository doesn't allow the execution
	 * of a certain command. E.g. when a CommitCommand should be executed on a repository with unresolved conflicts this exception will be thrown.
	 * @throws GitAPIException Other unexpected exceptions.
	 */
	public void commit(String message, boolean isAmendLastCommit) throws GitAPIException, NoHeadException, //NOSONAR See doc above.
      NoMessageException, UnmergedPathsException, //NOSONAR See doc above.
      ConcurrentRefUpdateException, WrongRepositoryStateException, //NOSONAR See doc above.
      AbortedByHookException { //NOSONAR See doc above.
	  List<FileStatus> files = getStagedFiles();
	  Collection<String> filePaths = getFilePaths(files);
		try {
		  fireOperationAboutToStart(new FileGitEventInfo(GitOperation.COMMIT, filePaths));
		  git.commit().setMessage(message).setAmend(isAmendLastCommit).call();
		  fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.COMMIT, filePaths));
		} catch (GitAPIException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.COMMIT, filePaths), e);
		  logger.error(e, e);
		  // Re throw the exception so that the user sees a proper error message, depending on its type.
		  throw e;
		}
	}

	/**
	 * Get file paths for the given file statuses.
	 * 
	 * @param files The file statuses.
	 * 
	 * @return the paths.
	 */
	private Collection<String> getFilePaths(List<FileStatus> files) {
	  List<String> paths = new LinkedList<>();
	  for (Iterator<FileStatus> iterator = files.iterator(); iterator.hasNext();) {
      FileStatus fileStatus = iterator.next();
      paths.add(fileStatus.getFileLocation());
    }
	  return paths;
  }

  /**
	 * Frees resources associated with the git instance.
	 */
	public void closeRepo() {
		if (git != null) {
		  AuthenticationInterceptor.unbind(getHostName());
			git.close();
			git = null;
		}
	}

	/**
	 * Gets all the local branches.
	 * 
	 * @return All the local branches from the repository or an empty list.
	 */
	public List<Ref> getLocalBranchList() {
		List<Ref> branches = Collections.emptyList();
		try {
			branches = git.branchList().call();
		} catch (GitAPIException e) {
		  logger.error(e, e);
		}
		return branches;
	}
	
	 /**
   * Gets all the remote branches for the current repository.
   * 
   * @return All the remote branches from the repository or an empty list.
   */
  public List<Ref> getRemoteBrachListForCurrentRepo() {
    List<Ref> branches = Collections.emptyList();
    try {
      branches = git.branchList().setListMode(ListMode.REMOTE).call();
    } catch (GitAPIException e) {
      logger.error(e, e);
    }
    return branches;
  }
  
  /**
   * List the remote branches for the given repository URL.
   * 
   * @param urlString         The repository URL.
   * @param excMessPresenter  Exception message presenter.
   * 
   * @return the collection of remote branches or an empty set.
   */
  public Collection<Ref> listRemoteBranchesForURL(
      URIish sourceURL,
      AuthExceptionMessagePresenter excMessPresenter) {
    AuthenticationInterceptor.bind(sourceURL.getHost());
    return doListRemoteBranchesInternal(sourceURL, excMessPresenter);
  }

  /**
   * Do list remote branches internal.
   * 
   * @param repoURL The repository URL.
   * @param excMessPresenter  Exception message presenter.
   * 
   * @return the remote branches or an empty list.
   */
  private Collection<Ref> doListRemoteBranchesInternal(
      URIish repoURL,
      AuthExceptionMessagePresenter excMessPresenter) {
    Collection<Ref> remoteRefs = Collections.emptySet();
    String host = repoURL.getHost();
    boolean shouldStopTryingLogin = false;
    if (logger.isDebugEnabled()) {
      logger.debug("START LISTING REMOTE BRANCHES FOR: " + repoURL);
    }
    do {
      final UserCredentials[] gitCredentials = 
          new UserCredentials[] {OptionsManager.getInstance().getGitCredentials(host)};
      String username = gitCredentials[0].getUsername();
      String password = gitCredentials[0].getPassword();
      if (logger.isDebugEnabled()) {
        logger.debug("Try login with user: " + username 
            + " and a password that I won't tell you.");
      }
      SSHCapableUserCredentialsProvider credentialsProvider = new SSHCapableUserCredentialsProvider(
          username,
          password,
          OptionsManager.getInstance().getSshPassphrase(),
          host);
      try {
        logger.debug("Now do list the remote branches...");
        remoteRefs = Git.lsRemoteRepository()
            .setHeads(true)
            .setRemote(repoURL.toString())
            .setCredentialsProvider(credentialsProvider)
            .call();
        if (logger.isDebugEnabled()) {
          logger.debug("BRANCHES: " + remoteRefs);
        }
        shouldStopTryingLogin = true;
      } catch (TransportException ex) {
        
        logger.debug(ex, ex);
        
        boolean retryLogin = AuthUtil.handleAuthException(
            ex,
            host,
            new UserCredentials(
                credentialsProvider.getUsername(),
                credentialsProvider.getPassword(),
                host),
            excMessPresenter,
            !credentialsProvider.wasResetCalled());
        if (!retryLogin || credentialsProvider.shouldCancelLogin()) {
          logger.debug("STOP TRYING TO LOGIN!");
          shouldStopTryingLogin = true;
        }
      } catch (GitAPIException e) {
        logger.error(e, e);
      }
    } while (!shouldStopTryingLogin);
    
    return remoteRefs;
  }

	/**
	 * Creates a new branch in the repository
	 * 
	 * @param branchName - Name for the new branch
	 */
	public void createBranch(String branchName) {
		try {
		  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, branchName));
			git.branchCreate().setName(branchName).call();
			fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, branchName));
		} catch (GitAPIException e) {
		  fireOperationFailed(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, branchName), e);
		  PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
		}

	}
	/**
	 * Creates a new local branch in the current repository, starting from another local branch.
	 * 
	 * @param newBranchName The name for the new branch.
	 * @param sourceBranch The full path for the local branch from which to create the new branch.
	 */
	public void createBranchFromLocalBranch(String newBranchName, String sourceBranch) {
	  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName));
	  try {
      git.branchCreate()
        .setName(newBranchName)
        .setStartPoint(sourceBranch)
        .call();
      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName));
    } catch (GitAPIException e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName), e);
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
    }
	}
	
	/**
	 * Deletes from the current repository a local branch with a specified name.
	 * @param branchName The name of the branch to be deleted.
	 */
	public void deleteBranch(String branchName) {
	  DeleteBranchCommand command = git.branchDelete();
	  command.setBranchNames(branchName);
	  command.setForce(true);
	  try {
	    fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.DELETE_BRANCH, branchName));
	      command.call();
	      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.DELETE_BRANCH, branchName));
	  } catch(GitAPIException e) {
	    fireOperationFailed(new BranchGitEventInfo(GitOperation.DELETE_BRANCH, branchName), e);
	    PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
	  }
	}

	/**
	 * Pushes all the commits from the local repository to the remote repository
	 * 
	 * @param username - Git username
	 * @param password - Git password
	 *          
	 * @throws GitAPIException
	 */
	public PushResponse push(final String username, final String password)
	    throws GitAPIException {

	  AuthenticationInterceptor.install();
	  PushResponse response = new PushResponse();

	  Repository repo = git.getRepository();
    RepositoryState repositoryState = repo.getRepositoryState();
	  if (repositoryState == RepositoryState.MERGING) {
	    response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
	    response.setMessage(translator.getTranslation(Tags.PUSH_WITH_CONFLICTS));
	    return response;
	  }
	  
	  if (getPullsBehind() > 0) {
	    response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD);
	    response.setMessage(translator.getTranslation(Tags.BRANCH_BEHIND));
	    return response;
	  }
	  
    try {
      if (getPushesAhead() == 0) {
        response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE);
        response.setMessage(translator.getTranslation(Tags.PUSH_UP_TO_DATE));
        return response;
      }
    } catch (RepoNotInitializedException e) {
      logger.debug(e, e);
    }
	  
	  String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
	  Iterable<PushResult> call = git.push().setCredentialsProvider(
	      new SSHCapableUserCredentialsProvider(username, password, sshPassphrase, getHostName())).call();
	  logger.debug("Push Ended");
	  
	  Iterator<PushResult> results = call.iterator();
	  while (results.hasNext()) {
	    PushResult result = results.next();
	    for (RemoteRefUpdate info : result.getRemoteUpdates()) {
	      try {
	        String localBranchName = getBranchInfo().getBranchName();
          if (getRemoteFromConfig(localBranchName) == null) {
            repo.getConfig().setString(
                ConfigConstants.CONFIG_BRANCH_SECTION,
                getBranchInfo().getBranchName(),
                ConfigConstants.CONFIG_KEY_REMOTE,
                Constants.DEFAULT_REMOTE_NAME);
            repo.getConfig().setString(
                ConfigConstants.CONFIG_BRANCH_SECTION,
                getBranchInfo().getBranchName(),
                ConfigConstants.CONFIG_KEY_MERGE,
                info.getRemoteName());
            repo.getConfig().save();
          }
        } catch (NoRepositorySelected | IOException ex) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
        } 
	      
	      response.setStatus(info.getStatus());
	      response.setMessage(info.getMessage());
	      
	      return response; // NOSONAR
	    }
	  }

	  response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
	  response.setMessage(translator.getTranslation(Tags.PUSH_FAILED_UNKNOWN));
	  return response;
	}

	/**
	 * Pulls the files that are not on the local repository from the remote
	 * repository
	 * 
	 * @param username Git username
	 * @param password Git password
   * @param pullType One of ff, no-ff, ff-only, rebase.
	 * 
	 * @return The result, if successful.
	 *  
   * @throws CheckoutConflictException There is a conflict between the local repository and the remote one.
   *  The same file that is in conflict is changed inside the working copy so operation is aborted.
   * @throws GitAPIException other errors.
   */
  public PullResponse pull(String username, String password) throws GitAPIException {
    return pull(username, password, PullType.MERGE_FF);
  }

	/**
	 * Pulls the files that are not on the local repository from the remote
	 * repository
	 * 
	 * @param username Git username
	 * @param password Git password
	 * @param pullType One of ff, no-ff, ff-only, rebase.
	 * 
	 * @return The result, if successful.
	 *  
	 * @throws CheckoutConflictException There is a conflict between the local
	 *                                   repository and the remote one. The same
	 *                                   file that is in conflict is changed inside
	 *                                   the working copy so operation is aborted.
	 * @throws GitAPIException other errors.
	 */
  public PullResponse pull(String username, String password, PullType pullType) throws GitAPIException {
	  PullResponse pullResponseToReturn = new PullResponse(PullStatus.OK, new HashSet<>());
	  AuthenticationInterceptor.install();

		if (!getConflictingFiles().isEmpty()) {
			pullResponseToReturn.setStatus(PullStatus.REPOSITORY_HAS_CONFLICTS);
		} else {
		  git.reset().call();

		  // Call "Pull"
		  Repository repository = git.getRepository();
		  ObjectId oldHead = resolveHead(repository);
		  String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
		  SSHCapableUserCredentialsProvider credentialsProvider = 
		      new SSHCapableUserCredentialsProvider(username, password, sshPassphrase, getHostName());
      PullCommand pullCmd = git.pull()
          .setRebase(PullType.REBASE == pullType)
          .setCredentialsProvider(credentialsProvider);
      PullResult pullCommandResult = pullCmd.call();

		  // Get fetch result
		  Collection<TrackingRefUpdate> trackingRefUpdates = pullCommandResult.getFetchResult().getTrackingRefUpdates();
		  String lockFailureMessage = createLockFailureMessageIfNeeded(trackingRefUpdates);
		  if (!lockFailureMessage.isEmpty()) {
		    // Lock failure
		    ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
            .showErrorMessage(translator.getTranslation(lockFailureMessage));
		    pullResponseToReturn.setStatus(PullStatus.LOCK_FAILED);
		  } else {
		    ObjectId head = resolveHead(repository);
		    if (oldHead != null && head != null) {
		      refreshProject(repository, oldHead, head);
		    }

		    RebaseResult rebaseResult = pullCommandResult.getRebaseResult();
		    if (rebaseResult != null) {
		      treatRebaseResult(pullResponseToReturn, rebaseResult);
		    } else { 
		    treatMergeResult(pullResponseToReturn, pullCommandResult.getMergeResult());
		  }
		}
		}
		
		return pullResponseToReturn;

	}

	/**
   * Treat rebase result.
   * 
   * @param pullResponseToReturn The pull response to alter.
   * @param rebaseResult         The result of the rebase operation.
   * 
   * @throws CheckoutConflictException when the command can't succeed because of unresolved conflicts.
   * @throws RebaseUncommittedChangesException 
   */
  private void treatRebaseResult(PullResponse pullResponseToReturn, RebaseResult rebaseResult) throws CheckoutConflictException, RebaseUncommittedChangesException {
    RebaseResult.Status status = rebaseResult.getStatus();
    if (logger.isDebugEnabled()) {
      logger.debug("Rebase result status: " + status);
    }
    
    switch (status) {
      case UP_TO_DATE:
        pullResponseToReturn.setStatus(PullStatus.UP_TO_DATE);
        break;
      case CONFLICTS:
        // There are uncommitted and incoming changes in the same resource.
        // If there are changes in different resources, the pull will be successful (fast-forward).
        throw new RebaseConflictsException(rebaseResult.getConflicts());
      case STOPPED:
        // Trying to pull generated a real conflict, because of changes on the same line(s).
        pullResponseToReturn.setConflictingFiles(new HashSet<>(getConflictingFiles()));
        pullResponseToReturn.setStatus(PullStatus.CONFLICTS);
        break;
      case UNCOMMITTED_CHANGES:
        // We had changes in X and Y locally, and an incoming change from remote on X. 
        // We committed X and tried to pull with rebase, and we got into this situation...
        throw new RebaseUncommittedChangesException(rebaseResult.getUncommittedChanges()); 
      default:
        // Nothing
        break;
    }
  }

  /**
	 * Resolve HEAD.
	 * 
	 * @param repository The repo.
	 * 
	 * @return the HEAD.
	 */
  private ObjectId resolveHead(Repository repository) {
    ObjectId head = null;
    try {
      head = repository.resolve("HEAD^{tree}");
    } catch (RevisionSyntaxException | IOException e) {
      logger.error(e, e);
    }
    return head;
  }

	/**
	 * Treat merge result.
	 * 
	 * @param pullResponse   Pull response.
	 * @param mergeResult    Merge result.
	 * 
	 * @throws CheckoutConflictException when the command can't succeed because of unresolved conflicts.
	 */
  private void treatMergeResult(PullResponse pullResponse, MergeResult mergeResult) throws CheckoutConflictException {
    if (mergeResult != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Merge result: " + mergeResult);
        logger.debug("Merge result status: " + mergeResult.getMergeStatus());
      }

      if (mergeResult.getMergeStatus() == MergeStatus.FAILED) {
        if (logger.isDebugEnabled()) {
        logMergeFailure(mergeResult);
        }
        throw new CheckoutConflictException(
            new ArrayList<>(mergeResult.getFailingPaths().keySet()), 
            new org.eclipse.jgit.errors.CheckoutConflictException(""));
      } else if (mergeResult.getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
        pullResponse.setStatus(PullStatus.UP_TO_DATE);
      } else if (mergeResult.getConflicts() != null) {
        // Successful merge but there are conflicts that should be resolved by the user.
        Set<String> conflictingFiles = mergeResult.getConflicts().keySet();
        if (conflictingFiles != null) {
          pullResponse.setConflictingFiles(conflictingFiles);
          pullResponse.setStatus(PullStatus.CONFLICTS);
        }
      }
    }
  }

  /**
   * Log merge failure.
   * 
   * @param mergeResult The merge result.
   */
  private void logMergeFailure(MergeResult mergeResult) {
    if (logger.isDebugEnabled()) {
      Map<String, MergeFailureReason> failingPaths = mergeResult.getFailingPaths();
      if (failingPaths != null) {
        logger.debug("NOW LOG MERGE FAILURE PATHS:");
        Set<String> keySet = failingPaths.keySet();
        for (String string : keySet) {
          logger.debug("  Path: " + string);
          logger.debug("  Reason: " + failingPaths.get(string));
        }
      }
    }
  
  }
  
	/**
	 * Create lock failure message when pulling/fetching, if needed.
	 * 
	 * @param trackingRefUpdates Give us details about the fetch result.
	 * 
	 * @return the message.
	 */
  private String createLockFailureMessageIfNeeded(Collection<TrackingRefUpdate> trackingRefUpdates) {
    StringBuilder fetchResultStringBuilder = new StringBuilder();
    for (Iterator<TrackingRefUpdate> iterator = trackingRefUpdates.iterator(); iterator.hasNext();) {
      TrackingRefUpdate trackingRefUpdate = iterator.next();
      if (trackingRefUpdate.getResult() == RefUpdate.Result.LOCK_FAILURE) {
        if (fetchResultStringBuilder.length() > 0) {
          fetchResultStringBuilder.append("\n\n");
        }
        fetchResultStringBuilder.append(translator.getTranslation(Tags.ERROR) + ": ");
				fetchResultStringBuilder.append(
						MessageFormat.format(translator.getTranslation(Tags.CANNOT_LOCK_REF), trackingRefUpdate.getLocalName())
								+ " ");
        try {
          String repoDir = getRepository().getDirectory().getAbsolutePath();
          File lockFile = new File(repoDir, trackingRefUpdate.getLocalName() + ".lock");
					fetchResultStringBuilder.append(
							MessageFormat.format(translator.getTranslation(Tags.UNABLE_TO_CREATE_FILE), lockFile.getAbsolutePath())
									+ " ");
          if (lockFile.exists()) {
            fetchResultStringBuilder.append(translator.getTranslation(Tags.FILE_EXISTS) + "\n");
          }
        } catch (NoRepositorySelected e) {
          logger.error(e, e);
        }
        fetchResultStringBuilder.append(translator.getTranslation(Tags.LOCK_FAILED_EXPLANATION));
      }
    }
    return fetchResultStringBuilder.toString();
  }

	/**
	 * Refresh the Project view.
	 * 
	 * @param repository The current repository.      
	 * @param oldHead    The old HEAD (before pull).
	 * @param head       The new HEAD (after pull).
	 * 
	 * @throws GitAPIException when error occurs during diff.
	 */
  private void refreshProject(Repository repository, ObjectId oldHead, ObjectId head) throws GitAPIException {
    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
    try {
      ObjectReader reader = repository.newObjectReader();
      oldTreeIter.reset(reader, oldHead);
      newTreeIter.reset(reader, head);
			List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
      
      Set<File> pulledFilesParentDirs = new HashSet<>();
      String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
      for (DiffEntry diffEntry : diffs) {
        if (diffEntry.getChangeType() == ChangeType.ADD) {
          pulledFilesParentDirs.add(new File(selectedRepository, diffEntry.getNewPath()).getParentFile());
        } else if (diffEntry.getChangeType() == ChangeType.DELETE) {
          pulledFilesParentDirs.add(new File(selectedRepository, diffEntry.getOldPath()).getParentFile());
        }
      }
      // Refresh the Project view
      StandalonePluginWorkspace wsAccess = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      wsAccess.getProjectManager().refreshFolders(new File[] { FileHelper.getCommonDir(pulledFilesParentDirs) });
    } catch (IOException e) {
      logger.error(e, e);
    }
  }

	/**
	 * Finds the common base for the given commit "a" and the given commit "b"
	 * 
	 * @param walk - used to browse through commits
	 * @param a    - commit "a"
	 * @param b    - commit "b"
	 *          
	 * @return the base commit
	 * 
	 * @throws IOException
	 */
	private RevCommit getCommonAncestor(RevWalk walk, RevCommit a, RevCommit b) throws IOException {
		walk.reset();
		walk.setRevFilter(RevFilter.MERGE_BASE);
		walk.markStart(a);
		walk.markStart(b);
		final RevCommit base = walk.next();
		if (base == null)
			return null;
		final RevCommit base2 = walk.next();
		if (base2 != null) {
			throw new NoMergeBaseException(MergeBaseFailureReason.MULTIPLE_MERGE_BASES_NOT_SUPPORTED,
					MessageFormat.format(JGitText.get().multipleMergeBasesFor, a.name(), b.name(), base.name(), base2.name()));
		}
		return base;
	}

	/**
	 * Adds a single file to the staging area. Preparing it for commit
	 * 
	 * @param file - the name of the file to be added
	 */
	public void add(FileStatus file) {
	  Collection<String> filePaths = getFilePaths(Arrays.asList(file));
	  try {
	    fireOperationAboutToStart(new FileGitEventInfo(GitOperation.STAGE, filePaths));
	    if (file.getChangeType().equals(GitChangeType.REMOVED)) {
	      git.rm().addFilepattern(file.getFileLocation()).call();
	    } else {
	      git.add().addFilepattern(file.getFileLocation()).call();
	    }
	    fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.STAGE, filePaths));
	  } catch (GitAPIException e) {
	    fireOperationFailed(new FileGitEventInfo(GitOperation.STAGE, filePaths), e);
	    logger.error(e, e);
		}
	}

	/**
	 * Adds multiple files to the staging area. Preparing the for commit
	 * 
	 * @param fileNames - the names of the files to be added
	 */
	public void addAll(List<FileStatus> files) {
	  Collection<String> filePaths = getFilePaths(files);
		try {
		  fireOperationAboutToStart(new FileGitEventInfo(GitOperation.STAGE, filePaths));
		  
		  RmCommand removeCmd = null;
		  AddCommand addCmd = null;
		  
			for (FileStatus file : files) {
				if (file.getChangeType() == GitChangeType.MISSING) {
				  if (removeCmd == null) {
				    removeCmd = git.rm().setCached(true);
				  }
          removeCmd.addFilepattern(file.getFileLocation());
				} else {
				  if (addCmd == null) {
				    addCmd = git.add();
				  }
          addCmd.addFilepattern(file.getFileLocation());
				}
			}
			
			if (addCmd != null) {
			  addCmd.call();
			}
			
			if (removeCmd != null) {
			  removeCmd.call();
			}
			
			fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.STAGE, filePaths));
		} catch (GitAPIException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.STAGE, filePaths), e);
		  logger.error(e, e);
		}
	}
	
	 /**
   * Gets all the files from the index.
   * 
   * @return - a set containing all the staged file names
   */
  public List<FileStatus> getStagedFiles() {
    return getStagedFile(Collections.<String>emptyList());
  }
  
  /**
	 * Checks which files from the given subset are in the Index and returns their
	 * state.
   * 
   * @param paths The files of interest.
   * 
   * @return - a set containing the subset of files present in the INDEX.
   */
  public List<FileStatus> getStagedFile(Collection<String> paths) {
    if (git != null) {
      StatusCommand statusCmd = git.status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        String path = iterator.next();
        statusCmd.addPath(path);
      }

      try {
        Status status = statusCmd.call();
        return getStagedFiles(status);
			} catch (GitAPIException e) {
			  logger.error(e, e);
      }
    }
    
    return Collections.emptyList();
  }

	/**
	 * Checks which files from the given subset are in the Index and returns their
	 * state.
	 * 
	 * @param statusCmd Status command to execute.
	 * 
	 * @return - a set containing the subset of files present in the INDEX.
	 */
  private List<FileStatus> getStagedFiles(Status status) {
    List<FileStatus> stagedFiles = new ArrayList<>();
    Set<String> submodules = getSubmodules();

    for (String fileName : status.getChanged()) {
      // File from INDEX, modified from HEAD
      if (submodules.contains(fileName)) {
        stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
      } else {
        stagedFiles.add(new FileStatus(GitChangeType.CHANGED, fileName));
      }
    }
    for (String fileName : status.getAdded()) {
      // Newly created files added in the INDEX
      if (submodules.contains(fileName)) {
        stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
      } else {
        stagedFiles.add(new FileStatus(GitChangeType.ADD, fileName));
      }
    }
    for (String fileName : status.getRemoved()) {
      // A delete added in the INDEX, file is present in HEAD.
      if (submodules.contains(fileName)) {
        stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
      } else {
        stagedFiles.add(new FileStatus(GitChangeType.REMOVED, fileName));
      }
    }

    return stagedFiles;
  }

	/**
	 * Gets the conflicting file from the git status
	 * 
	 * @return the conflicting files list. Never <code>null</code>.
	 */
	public Set<String> getConflictingFiles() {
		if (git != null) {
			try {
				return git.status().call().getConflicting();
			} catch (GitAPIException e) {
			  logger.error(e, e);
			}
		}
		return Collections.emptySet();
	}

	/**
	 * Reset all the specified files from the staging area.
	 * 
	 * @param fileNames - the list of file to be removed
	 */
	public void resetAll(List<FileStatus> files) {
	  Collection<String> filePaths = getFilePaths(files);
		try {
		  fireOperationAboutToStart(new FileGitEventInfo(GitOperation.UNSTAGE, filePaths));
			if (!files.isEmpty()) {
				ResetCommand reset = git.reset();
				for (FileStatus file : files) {
					reset.addPath(file.getFileLocation());
				}
				reset.call();
			}
			fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.UNSTAGE, filePaths));
		} catch (GitAPIException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.UNSTAGE, filePaths), e);
		  logger.error(e, e);
		}
	}

	/**
	 * Gets the host name from the repositoryURL
	 * 
	 * @return The host name. An empty string if not connected. Never
	 *         <code>null</code>.
	 */
	public String getHostName() {
		if (git != null) {
			Config storedConfig = git.getRepository().getConfig();
			// TODO How we should react when there are multiple remote repositories?
			String url = storedConfig.getString(ConfigConstants.CONFIG_KEY_REMOTE, Constants.DEFAULT_REMOTE_NAME, "url");
			if (url == null) {
			  Set<String> remoteNames = git.getRepository().getRemoteNames();
			  Iterator<String> iterator = remoteNames.iterator();
			  if (iterator.hasNext()) {
			    url = storedConfig.getString(ConfigConstants.CONFIG_KEY_REMOTE, iterator.next(), "url");
			  }
			}
			try {
				URL u = new URL(url);
				url = u.getHost();
				return url;
			} catch (MalformedURLException e) {
				return "";
			}
		}
		return "";

	}

	/**
	 * Finds the last local commit in the repository
	 * 
	 * @return the last local commit
	 */
	public ObjectId getLastLocalCommit() {
		Repository repo = git.getRepository();
		try {
			return repo.resolve("HEAD^{commit}");
		} catch (IOException e) {
		  logger.error(e, e);
		}
		return null;
	}

	/**
	 * Finds the base commit from the last local commit and the remote commit
	 * 
	 * @param branchInfo Current branch info or <code>null</code> if unknown.
	 * 
	 * @return the base commit
	 */
	public ObjectId getBaseCommit(BranchInfo branchInfo) {
	  if (branchInfo == null) {
	    branchInfo = getBranchInfo();
	  }
		Repository repository = git.getRepository();
		RevWalk walk = new RevWalk(repository);
		ObjectId localCommit = null;
		ObjectId remoteCommit = null;
		ObjectId baseCommit = null;
		try {
			remoteCommit = repository.resolve(Constants.DEFAULT_REMOTE_NAME + "/" 
			    + branchInfo.getBranchName() + "^{commit}");
			localCommit = repository.resolve("HEAD^{commit}");
			if (remoteCommit != null && localCommit != null) {
				RevCommit base = getCommonAncestor(walk, walk.parseCommit(localCommit), walk.parseCommit(remoteCommit));
				if (base != null) {
					baseCommit = base.toObjectId();
				}
			}
		} catch (IOException e) {
		  logger.error(e, e);
		}
		walk.close();
		return baseCommit;
	}

	/**
	 * Gets the loader for a file from a specified commit and its path
	 * 
	 * @param commit - the commit from which to get the loader
	 * @param path   - the path to the file
	 * @return the loader
	 * 
	 * @throws IOException
	 */
	public ObjectLoader getLoaderFrom(ObjectId commit, String path) throws IOException {
		Repository repository = git.getRepository();
		RevWalk revWalk = new RevWalk(repository);
		RevCommit revCommit = revWalk.parseCommit(commit);
		// and using commit's tree find the path
		RevTree tree = revCommit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(PathFilter.create(path));

		ObjectLoader loader = null;
		if (treeWalk.next()) {
			ObjectId objectId = treeWalk.getObjectId(0);
			loader = repository.open(objectId);
		}

		treeWalk.close();
		revWalk.close();
		return loader;
	}

	/**
	 * Gets the InputStream for the file that is found in the given commit at the
	 * given path
	 * 
	 * @param commitID - the commit in which the file exists
	 * @param path     - the path to the file
	 *          
	 * @return the InputStream for the file
	 * 
	 * @throws IOException
	 */
	public InputStream getInputStream(ObjectId commitID) throws IOException {
		InputStream toReturn = null;
		if (commitID != null) {
			ObjectLoader loader = git.getRepository().open(commitID);
			toReturn = loader.openStream();
		} else {
			throw new IOException("The commit ID can't be null");
		}
		return toReturn;
	}

  /**
   * Resets the current branch to a specified commit.
   * 
   * @param resetType The reset type to perform on the current branch.
   * @param commitId  The commit id to which to reset.
   */
  public void resetToCommit(ResetType resetType, String commitId) {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.RESET_TO_COMMIT));
    try {
      git.reset().setMode(resetType).setRef(commitId).call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.RESET_TO_COMMIT));
    } catch (GitAPIException e) {
      fireOperationFailed(new GitEventInfo(GitOperation.RESET_TO_COMMIT), e);
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
    }
  }

	/**
	 * Restores the last commit file content to the local file at the given path.
	 * Both files must have the same path, otherwise it will not work.
	 * 
	 * @param file - the path to the file you want to restore
	 */
	public void restoreLastCommitFile(List<String> paths) {
		try {
		  fireOperationAboutToStart(new FileGitEventInfo(GitOperation.DISCARD, paths));
		  CheckoutCommand checkoutCmd = git.checkout();
		  checkoutCmd.addPaths(paths);
			checkoutCmd.call();
			fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.DISCARD, paths));
		} catch (GitAPIException e) {
      fireOperationFailed(new FileGitEventInfo(GitOperation.DISCARD, paths), e);
		  logger.error(e, e);
		}
	}

	/**
	 * Calculates how many commits the local repository is ahead from the current
	 * local repository base commit
	 * 
	 * @return the number of commits ahead
	 * 
	 * @throws RepoNotInitializedException when the remote repo has not been initialized.
	 */
	public int getPushesAhead() throws RepoNotInitializedException {
		int numberOfCommits = 0;
		
	  try {
	    String branchName = getBranchInfo().getBranchName();
	    if (branchName != null && branchName.length() > 0) {
	      BranchTrackingStatus bts = BranchTrackingStatus.of(getRepository(), branchName);
	      if (bts != null) {
	        numberOfCommits = bts.getAheadCount();
	      } else {
	        throw new RepoNotInitializedException();
	      }
	    }
	  } catch (IOException | NoRepositorySelected e) {
	    logger.error(e, e);
	  }
	  
	  return numberOfCommits;
	}

	/**
	 * Calculates how many commits the remote repository is ahead from the local
	 * repository base commit
	 * 
	 * @return the number of commits the remote is ahead
	 */
	public int getPullsBehind() {
	  int numberOfCommits = 0;

	  try {
	    String branchName = getBranchInfo().getBranchName();
	    if (branchName != null && branchName.length() > 0) {
	      BranchTrackingStatus bts = BranchTrackingStatus.of(getRepository(), branchName);
	      if (bts != null) {
	        numberOfCommits = bts.getBehindCount();
	      }
	    }
	  } catch (IOException | NoRepositorySelected e) {
	    logger.error(e, e);
	  }

		return numberOfCommits;
	}

	/**
	 * Brings all the commits to the local repository but does not merge them.
	 * 
	 * @throws PrivateRepositoryException 
	 */
	public void fetch()
			throws SSHPassphraseRequiredException, PrivateRepositoryException, RepositoryUnavailableException {
	  logger.debug("Begin fetch");
    if (git == null) {
      throw new RepositoryUnavailableException(new NoRepositorySelected("Repository is empty"));
    }
	  
		AuthenticationInterceptor.install();
		
		String hostName = getHostName();
    UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
		String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
		SSHCapableUserCredentialsProvider credentialsProvider = new SSHCapableUserCredentialsProvider(
		    gitCredentials.getUsername(),
		    gitCredentials.getPassword(),
				sshPassphrase,
				hostName);
		try {
			StoredConfig config = git.getRepository().getConfig();
			Set<String> sections = config.getSections();
			if (sections.contains(ConfigConstants.CONFIG_KEY_REMOTE)) {
        git.fetch()
            .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
            .setCheckFetchedObjects(true)
            .setRemoveDeletedRefs(true)
						.setCredentialsProvider(credentialsProvider)
						.call();
			}
		} catch (TransportException e) {
		  logger.debug(e, e);
			
			Throwable cause = e;
	    while (cause.getCause() != null) {
	      cause = cause.getCause();
	    }

			String message = e.getMessage();
      if (message != null && (message.contains("Authentication is required but no CredentialsProvider has been registered")
					|| message.contains("not authorized"))) {
				throw new PrivateRepositoryException(e);
			} else if (message != null && message.contains("Auth fail") && credentialsProvider.isPassphaseRequested()
			    || (cause instanceof SshException)
              && ((SshException) cause).getDisconnectCode() == SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE) {
			  throw new SSHPassphraseRequiredException(e);
			} else {
			  throw new RepositoryUnavailableException(e);
			}
		} catch (GitAPIException | RevisionSyntaxException e) {
		  logger.error(e, e);
    } 
		logger.debug("End fetch");
	}

	/**
	 * Replace with remote content. Useful when resolving a conflict using 'theirs'.
	 * 
	 * @param filePath File path.
	 */
	public void replaceWithRemoteContent(String filePath) {
		try {
			git.checkout().setStage(Stage.THEIRS).addPath(filePath).call();
		} catch (Exception e) {
		  logger.error(e, e);
		}
	}

	/**
	 * Restore to the initial state of the repository. Only applicable if the
	 * repository has conflicts
	 * 
	 * @return The restart merge task.
	 */
	public ScheduledFuture<?> restartMerge() {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.MERGE_RESTART));
	  return GitOperationScheduler.getInstance().schedule(() -> {
	    try {
	      RepositoryState repositoryState = getRepository().getRepositoryState();
	      if (repositoryState == RepositoryState.REBASING_MERGE) {
	        git.rebase().setOperation(Operation.ABORT).call();
	        UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(getHostName());
	        String username = gitCredentials.getUsername();
	        String password = gitCredentials.getPassword();
	        pull(username, password, PullType.REBASE);
	      } else {
	        AnyObjectId commitToMerge = getRepository().resolve("MERGE_HEAD");
	        git.clean().call();
	        git.reset().setMode(ResetType.HARD).call();
	        git.merge().include(commitToMerge).setStrategy(MergeStrategy.RECURSIVE).call();
	      }
	      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.MERGE_RESTART));
	    } catch (IOException | NoRepositorySelected | GitAPIException e) {
	      fireOperationFailed(new GitEventInfo(GitOperation.MERGE_RESTART), e);
	      logger.error(e, e);
	    }
	  });
	}

	/**
	 * Checks whether or not he branch is detached. If the branch is detached it
	 * stores the state and the name of the commit on which it is. If the branch is
	 * not detached then it stores the branch name. After this it returns this
	 * information
	 * 
	 * @return An object specifying the branch name and if it is detached or not
	 */
	public BranchInfo getBranchInfo() {
		if (git != null) {
			BranchInfo branchInfo = new BranchInfo();
			String branchName = "";
			try {
				branchName = git.getRepository().getBranch();
				branchInfo.setBranchName(branchName);
				Iterable<RevCommit> results = git.log().call();
				for (RevCommit revCommit : results) {
					if (revCommit.getId().name().equals(branchName)) {
						branchInfo.setDetached(true);
						branchInfo.setShortBranchName(
						    revCommit.getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name());
						break;
					}
				}
				return branchInfo;
			} catch (NoHeadException e) {
			  logger.debug(e, e);
			  return new BranchInfo(branchName, false);
			} catch (IOException | GitAPIException e) {
			  logger.error(e, e);
			}
		}
		return new BranchInfo("", false);
	}

	/**
	 * Sets the given branch as the current branch
	 * 
	 * @param branch The short name of the branch to set.
	 * 
	 * @throws GitAPIException
	 */
	public void setBranch(String branch) throws GitAPIException {
	  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CHECKOUT, branch));
	  try {
	    git.checkout().setName(branch).call();
	    fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CHECKOUT, branch));
	  } catch (GitAPIException e) {
	    fireOperationFailed(new BranchGitEventInfo(GitOperation.CHECKOUT, branch), e);
	    throw e;
	  }
	}
	
	/**
   * Creates a local branch for a remote branch (which it starts tracking), and sets it as the current branch.
   * 
   * @param newBranchName The name of the new branch created at checkout.
   * @param remoteBranchName The branch to checkout (short name).
   * @throws GitAPIException 
   */
  public void checkoutRemoteBranchWithNewName(String newBranchName, String remoteBranchName) throws GitAPIException{
    fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CHECKOUT, newBranchName));
    try {
      git.checkout()
          .setCreateBranch(true)
          .setName(newBranchName)
          .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
          .setStartPoint(Constants.DEFAULT_REMOTE_NAME + "/" + remoteBranchName)
          .call();
      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CHECKOUT, newBranchName));
    } catch (GitAPIException e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.CHECKOUT, newBranchName), e);
      throw e;
    }
  }
	
	/**
	 * Check out a specific commit and create a branch with it.
	 * 
	 * @param branchName The name of the new branch.
	 * @param commitID   The ID of the commit to be checked-out as a new branch.
	 * 
	 * @throws GitAPIException 
	 */
	public void checkoutCommitAndCreateBranch(String branchName, String commitID) throws GitAPIException {
	  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CHECKOUT, branchName));
    try {
  	  git.checkout()
  	      .setCreateBranch(true)
  	      .setName(branchName)
  	      .setStartPoint(commitID)
  	      .call();
  	  fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CHECKOUT, branchName));
    } catch (GitAPIException e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.CHECKOUT, branchName), e);
      throw e;
    }
	}

	/**
	 * Return the submodule head commit to the previously one
	 * 
	 * @throws GitAPIException when an error occurs while trying to discard the
	 *                         submodule.
	 */
	public void discardSubmodule() throws GitAPIException {
	  git.submoduleSync().call();
	  git.submoduleUpdate().setStrategy(MergeStrategy.RECURSIVE).call();
	}

	/**
	 * Locates the file with the given path in the index.
	 * 
	 * @param path File path.
	 * 
	 * @return The ID or null if not found.
	 * 
	 * @throws IOException Unable to read the index.
	 */
	public ObjectId locateObjectIdInIndex(String path)  throws IOException {
	  DirCache dc = git.getRepository().readDirCache();
	  int firstIndex = dc.findEntry(path);
	  if (firstIndex < 0) {
	    return null;
	  }

	  org.eclipse.jgit.dircache.DirCacheEntry firstEntry = dc.getEntry(firstIndex);
	  
	  return firstEntry.getObjectId();
	}
	
	/**
	 * Get the name of the remote for the given branch.
	 * 
	 * @param branchName The name of the branch for which to retrieve the remote
	 *                   name.
	 * 
	 * @return The name of the remote.
	 * 
	 * @throws NoRepositorySelected 
	 */
	public String getRemoteFromConfig(String branchName) throws NoRepositorySelected {
	  Repository repository = GitAccess.getInstance().getRepository();
	  return repository.getConfig().getString("branch", branchName, ConfigConstants.CONFIG_KEY_REMOTE);
	}

	/**
	 * Gets the full remote-tracking branch name or null is the local branch is not tracking a remote branch.
	 * 
	 * ex: refs/remotes/origin/dev
	 * 
	 * @param localBranchShortName The short branch name.
	 * 
	 * @return The full remote-tracking branch name or null is the local branch is not tracking a remote branch.
	 */
	public String getUpstreamBranchNameFromConfig(String localBranchShortName) {
	  return RevCommitUtil.getUpstreamBranchName(git.getRepository(), localBranchShortName);
  }
	
	 /**
   * Gets Get a shortened more user friendly ref name for the remote-tracking branch name or null is the local branch is not tracking a remote branch.
   * 
   * ex: origin/dev
   * 
   * @param localBranchShortName The short branch name.
   * 
   * @return The short remote-tracking branch name or null is the local branch is not tracking a remote branch.
   */
	public String getUpstreamBranchShortNameFromConfig(String localBranchShortName) {
	  String remoteTrackingBranch = getUpstreamBranchNameFromConfig(localBranchShortName);

	  if (remoteTrackingBranch != null) {
	    return org.eclipse.jgit.lib.Repository.shortenRefName(remoteTrackingBranch);
	  }

	  return null;
	}

	/**
	 * Returns the SHA-1 commit id for a file by specifying what commit to get for
	 * that file and it's path
	 * 
	 * @param commit - specifies the commit to return(MINE, THEIRS, BASE, LOCAL)
	 * @param path   - the file path for the specified commit
	 * @return the SHA-1 commit id
	 */
	public ObjectId getCommit(Commit commit, String path) {
	  ObjectId toReturn = null;
		try {
		  List<DiffEntry> entries = git.diff().setPathFilter(PathFilter.create(path)).call();
			boolean isTwoWayDiff = entries.size() < 3;
			int index = 0;
			switch (commit) {
			  case MINE:
			    index = isTwoWayDiff ? 0 : 1;
	        if (index >= entries.size()) {
	          throw new IOException("No diff info available for path: '" + path + "' and commit: '" + commit + "'");
	        }
	        toReturn =  entries.get(index).getOldId().toObjectId();
			    break;
			  case THEIRS:
			    index = isTwoWayDiff ? 1 : 2;
	        if (index >= entries.size()) {
	          throw new IOException("No diff info available for path: '" + path + "' and commit: '" + commit + "'");
	        }
	        toReturn =  entries.get(index).getOldId().toObjectId();
			    break;
			  case BASE:
			    toReturn = entries.get(index).getOldId().toObjectId();
			    break;
			  case LOCAL:
	        ObjectId lastLocalCommit = getLastLocalCommit();
	        RevWalk revWalk = new RevWalk(git.getRepository());
	        RevCommit revCommit = revWalk.parseCommit(lastLocalCommit);
	        RevTree tree = revCommit.getTree();
	        TreeWalk treeWalk = new TreeWalk(git.getRepository());
	        treeWalk.addTree(tree);
	        treeWalk.setRecursive(true);
	        treeWalk.setFilter(PathFilter.create(path));
	        if (treeWalk.next()) {
	          toReturn = treeWalk.getObjectId(0);
	        }
	        treeWalk.close();
	        revWalk.close();
			    break;
			}
		} catch (GitAPIException | IOException e) {
		  logger.error(e, e);
		}
		return toReturn;
	}
	
	 /**
   * Clean up.
   */
  public void cleanUp() {
    gitEventListeners.clear();
    closeRepo();
  }
	
	/**
	 * Get the {@link Git} object through which to interact with the repository.
	 */
	public Git getGit() {
	  return git;
	}

  /**
   * No repository.
   */
  public static final String NO_REPOSITORY = "[No repository]";

  /**
	 * @return The name of the Workking copy or {@link NO_REPOSITORY} if there is no
	 *         working copy opened.
   */
  public String getWorkingCopyName() {
    String rootFolder = NO_REPOSITORY;
    try {
      rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
    return rootFolder;
  }
  
  /**
   * Abort merge.
   */
  public void abortMerge() {
    Set<String> conflictingFiles = getConflictingFiles();
    fireOperationAboutToStart(new FileGitEventInfo(GitOperation.ABORT_MERGE, conflictingFiles));
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        // Clear the merge state
        Repository repository = getRepository();
        repository.writeMergeCommitMsg(null);
        repository.writeMergeHeads(null);
        
        // Reset the index and work directory to HEAD
        git.reset().setMode(ResetType.HARD).call();
        
        fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.ABORT_MERGE, conflictingFiles));
      } catch (GitAPIException | IOException | NoRepositorySelected e) {
        fireOperationFailed(new FileGitEventInfo(GitOperation.ABORT_MERGE, conflictingFiles), e);
        logger.error(e, e);
      }
    });
  }
  
	/**
   * Aborts and resets the current rebase
   */
  public void abortRebase() {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.ABORT_REBASE));
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        git.rebase().setOperation(Operation.ABORT).call();
        fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.ABORT_REBASE));
      } catch (GitAPIException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.ABORT_REBASE), e);
        logger.error(e, e);
      }
    });
  }
  
  /**
   * Continue rebase after a conflict resolution.
   */
  public void continueRebase() {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.CONTINUE_REBASE));
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        RebaseResult result = git.rebase().setOperation(Operation.CONTINUE).call();
        if (result.getStatus() == RebaseResult.Status.NOTHING_TO_COMMIT) {
          skipCommit();
        }
        fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.CONTINUE_REBASE));
      } catch (UnmergedPathsException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.CONTINUE_REBASE), e);
        logger.debug(e, e);
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
            .showWarningMessage(translator.getTranslation(Tags.CANNOT_CONTINUE_REBASE_BECAUSE_OF_CONFLICTS));
      } catch (GitAPIException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.CONTINUE_REBASE), e);
        logger.debug(e, e);
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
        .showErrorMessage(e.getMessage());
      }
    });
  }
  
  /**
   * Skip the current commit when rebasing.
   */
  private void skipCommit() {
    try {
      git.rebase().setOperation(Operation.SKIP).call();
    } catch (GitAPIException e) {
      logger.error(e, e);
    }
  }
  
  /**
	 * Compute a Vector with the characteristics of each commit.
	 * 
	 * @param filePath A resource for which we are interested in its history. If <code>null</code>, 
	 * the repository history will be computed.
	 * 
	 * @return a Vector with commits characteristics of the current repository.
	 */
	public List<CommitCharacteristics> getCommitsCharacteristics(String filePath) {
		List<CommitCharacteristics> revisions = new ArrayList<>();

		try {
			Repository repository = this.getRepository();
			if (filePath == null && git.status().call().hasUncommittedChanges()) {
				revisions.add(UNCOMMITED_CHANGES);
			}

			RevCommitUtil.collectCurrentBranchRevisions(filePath, revisions, repository);
		} catch (NoWorkTreeException | GitAPIException | NoRepositorySelected | IOException e) {
			logger.error(e, e);
		}
		
		return revisions;
	}

	/**
	 * Get a LinkedHashMap with all tag names in current repository.
	 * Map shows: key = commitID, value = list of tag names.
	 * 
	 * @param repository The current repository.
	 * 
	 * @return the map, never <code>null</code>.
	 * 
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public Map<String, List<String>> getTagMap(Repository repository)
			throws GitAPIException, IOException {
		Map<String, List<String>> commitTagMap = new LinkedHashMap<>();
		List<Ref> call = git.tagList().call();
		
		// search through all commits for tags
		for (Ref ref : call) {
			List<String> tagList = new ArrayList<>();
			String tagName = ref.getName();
			StringTokenizer st = new StringTokenizer(tagName, "/");
			while (st.hasMoreTokens()) {
				tagName = st.nextToken();
			}
			LogCommand log = git.log();

			Ref peeledRef = repository.getRefDatabase().peel(ref);
			if (peeledRef.getPeeledObjectId() != null) {
				log.add(peeledRef.getPeeledObjectId());
			} else {
				log.add(ref.getObjectId());
			}
			Iterable<RevCommit> logs = log.call();
			tagList.add(tagName);
			commitTagMap.put(
			    logs.iterator().next().getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name(),
			    tagList);
		}
		return commitTagMap;
	}
	
	/**
	 * Get the linkedHashMap with all local/remote branch names in current repository.
	 * Map shows: key = commitID, value = list of branch names.
	 * 
	 * @param repository The current repository.
	 * @param branchType The local / remote branch.
	 * @return the local / remote branchMap
	 */
	public Map<String, List<String>> getBranchMap(Repository repository, String branchType) {
		Map<String, List<String>> branchMap = new LinkedHashMap<>();
		
		List<Ref> localBranchList = null;
		String prefix = "";
		if (branchType.equals(ConfigConstants.CONFIG_KEY_LOCAL)) {
			localBranchList = getLocalBranchList();
			prefix = "heads/";
		} else if (branchType.equals(ConfigConstants.CONFIG_KEY_REMOTE)) {
			localBranchList = getRemoteBrachListForCurrentRepo();
			prefix = "remotes/";
		}
		
		for (Ref ref : localBranchList) {
      // refresh and populate local branch list for each commit
      int indexOfPrefix = ref.getName().indexOf(prefix);
      if (indexOfPrefix != -1) {
        int refIdx = indexOfPrefix + prefix.length();
        String branchName = ref.getName().substring(refIdx);
        
        String commit = ref.getObjectId().getName().substring(0, 7);
        List<String> values = branchMap.computeIfAbsent(commit, t -> new ArrayList<>());
        values.add(branchName);
      }
    }

		return branchMap;
	}

	/**
	 * @return <code>true</code> if a repository was initialized.
	 */
	public boolean isRepoInitialized() {
	  return git != null;
	}
	
	/**
	 * Get latest commit om current branch.
	 * 
	 * @throws GitAPIException 
	 * @throws IOException 
	 */
	public RevCommit getLatestCommitOnCurrentBranch() throws GitAPIException, IOException {
	  String branchNAme = getBranchInfo().getBranchName();
	  Repository repo = git.getRepository();
	  RevWalk revWalk = (RevWalk) git.log().add(repo.resolve(branchNAme)).call();
	  revWalk.sort(RevSort.COMMIT_TIME_DESC);
	  return revWalk.next();
	}
	
}
