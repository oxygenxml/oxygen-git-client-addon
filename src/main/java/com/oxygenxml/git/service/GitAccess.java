package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.StashCreateCommand;
import org.eclipse.jgit.api.StashListCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
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
import org.eclipse.jgit.transport.CredentialsProvider;
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
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.BranchGitEventInfo;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.stash.StashApplyFailureWithStatusException;
import com.oxygenxml.git.view.stash.StashApplyStatus;

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
   * The default name for the first branch created:"main" 
   */
  public static final String DEFAULT_BRANCH_NAME = "main";
  
  /**
   * The length of the short commit ID.
   */
  public static final int SHORT_COMMIT_ID_LENGTH = 7;
  /**
	 * A synthetic object representing the uncommitted changes.
	 */
  public static final CommitCharacteristics UNCOMMITED_CHANGES = new CommitCharacteristics(
      Translator.getInstance().getTranslation(Tags.UNCOMMITTED_CHANGES), null, "*", "*", "*", null, null);
  /**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(GitAccess.class);
	/**
	 * Listeners notifications.
	 */
	private GitListeners listeners = GitListeners.getInstance();
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
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Singleton instance.
	 */
	private GitAccess() {}

	/**
	 * @return the singleton instance.
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
    
		ProgressMonitor progressMonitor = createCloneProgressMonitor(progressDialog);
		if (progressDialog != null) {
		  progressDialog.setNote("Initializing...");
		}
		
    CloneCommand cloneCommand = 
		    Git.cloneRepository()
		    .setCloneSubmodules(true)
		    .setURI(url.toString())
		    .setDirectory(directory)
		    .setCredentialsProvider(AuthUtil.getCredentialsProvider(host))
		    .setProgressMonitor(progressMonitor);
		
		fireOperationAboutToStart(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, directory));
		try {
		  if (branchName != null) {
		    git = cloneCommand.setBranch(branchName).call();
		  } else {
		    git = cloneCommand.call();
		  } 
		  fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, directory));
		} catch (GitAPIException ex)  {
		  fireOperationFailed(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, directory), ex);
      throw ex;
		}
		
	}

	/**
	 * Create progress monitor for the clone operation.
	 * 
	 * @param progressDialog Progress dialog.
	 * 
	 * @return the progress monitor. Never <code>null</code>.
	 */
  private ProgressMonitor createCloneProgressMonitor(final ProgressDialog progressDialog) {
    return new ProgressMonitor() {
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
	      LOGGER.debug(e, e);
	    }
	  });
	}
  
  /**
   * Sets the Git repository on the current thread. The repository file path must exist.
   * 
   * @param path A string that specifies the Git repository folder.
   * 
   * @throws IOException
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
   * @param path The path of the repo to open.
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

    if (LOGGER.isDebugEnabled()) {
      logSshKeyLoadingData();
    }

    fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, workingCopy));
  }

	/**
	 * Log SSH key loading location data.
	 */
  private void logSshKeyLoadingData() {
    // Debug data for the SSH key load location.
    LOGGER.debug("Java env user home: " + System.getProperty("user.home"));
    try {
      Repository repository = getRepository();
      LOGGER.debug("Load repository " + repository.getDirectory());
      
      FS fs = repository.getFS();
      if (fs != null) {
        File userHome = fs.userHome();
        LOGGER.debug("User home " + userHome);

        File sshDir = new File(userHome, ".ssh");

        boolean exists = sshDir.exists();
        LOGGER.debug("SSH dir exists " + exists);
        if (exists) {
          File[] listFiles = sshDir.listFiles();
					for (File listFile : listFiles) {
						LOGGER.debug("SSH resource path " + listFile);
					}
        }
      } else {
        LOGGER.debug("Null FS");
      }
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e, e);
    }
  }
  
  /**
   * Fire operation about to start.
   * 
   * @param info event info.
   */
  private void fireOperationAboutToStart(GitEventInfo info) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation about to start: " + info);
    }
    listeners.fireOperationAboutToStart(info);
  }
  
  /**
   * Fire operation successfully ended.
   * 
   * @param info event info.
   */
  private void fireOperationSuccessfullyEnded(GitEventInfo info) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation successfully ended: " + info);
    }
    listeners.fireOperationSuccessfullyEnded(info);
  }
  
  /**
   * Fire operation failed.
   * 
   * @param info event info.
   * @param t related exception/error. May be <code>null</code>.
   */
  void fireOperationFailed(GitEventInfo info, Throwable t) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation failed: " + info + ". Reason: " + t.getMessage());
    }
    listeners.fireOperationFailed(info, t);
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
	 * @param path A string that specifies the git Repository folder
	 * 
	 * @throws GitAPIException
	 */
	public void createNewRepository(String path) throws GitAPIException {
	  File wc = new File(path);
	  fireOperationAboutToStart(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, wc));
    closeRepo();
    try {
      git = Git.init().setInitialBranch(DEFAULT_BRANCH_NAME).setBare(false).setDirectory(wc).call();
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
	      LOGGER.debug("-- Compute our GitStatus -> getStatus() --");
	      Status status = git.status().call();
	      LOGGER.debug("-- Get JGit status -> git.status().call() --");
	      gitStatus = new GitStatus(getUnstagedFiles(status), getStagedFiles(status));
	    } catch (GitAPIException e) {
	      LOGGER.error(e, e);
	    }
	  }
    return gitStatus != null ? gitStatus 
        : new GitStatus(Collections.emptyList(),Collections.emptyList());
  }
	
	/**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.<br><br>
   * 
   * NOTE: if the staged files are also needed, use {@link #getStatus()} method instead.
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
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("PUBLIC - GET UNSTAGED FILES");
        LOGGER.debug("Prepare fot JGit status, in paths " + paths);
      }
      
      StatusCommand statusCmd = git.status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        statusCmd.addPath(iterator.next());
      }
      try {
        Status status = statusCmd.call();
        LOGGER.debug("JGit Status computed: " + status);
        return getUnstagedFiles(status);
      } catch (GitAPIException e) {
        LOGGER.error(e, e);
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
	  LOGGER.debug("PRIVATE - GET UNSTAGE FOR GIVEN STATUS " + status);
		List<FileStatus> unstagedFiles = new ArrayList<>();
		if (git != null) {
			try {
				Set<String> submodules = getSubmoduleAccess().getSubmodules();
        addSubmodulesToUnstaged(unstagedFiles, submodules);
				addUntrackedFilesToUnstaged(status, unstagedFiles, submodules);
        addModifiedFilesToUnstaged(status, unstagedFiles, submodules);
        addMissingFilesToUnstaged(status, unstagedFiles, submodules);
				addConflictingFilesToUnstaged(status, unstagedFiles);
			} catch (NoWorkTreeException | GitAPIException e1) {
			  LOGGER.error(e1, e1);
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addConflictingFilesToUnstaged: " + status.getConflicting());
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addMissingFilesToUnstaged: " + status.getMissing());
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addModifiedFilesToUnstaged " + status.getModified());
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addUntrackedFilesToUnstaged " + status.getUntracked());
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addSubmodulesToUnstaged " + submodules);
    }
    for (String submodulePath : submodules) {
    	SubmoduleStatus submoduleStatus = git.submoduleStatus().call().get(submodulePath);
			if (submoduleStatus != null && submoduleStatus.getHeadId() != null
    	    && !submoduleStatus.getHeadId().equals(submoduleStatus.getIndexId())) {
			  
    		unstagedFiles.add(
    		    new FileStatus(GitChangeType.SUBMODULE, submodulePath).setDescription(
    		        RepoUtil.extractSubmoduleChangeDescription(git.getRepository(), submoduleStatus)));
    	}
    }
  }

  /**
   * @return API for working with submodules.
   */
	public SubmoduleAccess getSubmoduleAccess() {
	  return SubmoduleAccess.wrap(() -> git);
	}

	/**
	 * Sets the given submodule as the current repository
	 * 
	 * @param submodule - the name of the submodule.
	 * 
	 * @throws IOException Failed to load the submodule.
	 * @throws GitAPIException Failed to load the submodule.
	 */
	public void setSubmodule(String submodule) throws IOException, GitAPIException {
		Repository parentRepository = git.getRepository();
		File submoduleDir = SubmoduleWalk.getSubmoduleDirectory(parentRepository, submodule);

		fireOperationAboutToStart(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, submoduleDir, true));
		
		try {
		  Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		  if (submoduleRepository == null) {
		    // The submodule wasn't updated.
		    git.submoduleInit().call();
		    
		    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
        git.submoduleUpdate().setCredentialsProvider(credentialsProvider).call();

		    submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		  }
		  
		  // Close the current repository.
		  closeRepo();

		  git = Git.wrap(submoduleRepository);
		  
		  // Start intercepting authentication requests.
		  AuthenticationInterceptor.bind(getHostName());
		  
		  fireOperationSuccessfullyEnded(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, submoduleDir, true));
		} catch (Exception e) {
		  fireOperationFailed(new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, submoduleDir, true), e);
		}
		
	}
	
	 /**
   * Commits a single file locally
   * 
   * @param message Message for the commit
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
	 * @param message Message for the commit
	 * @param isAmendLastCommit <code>true</code> if the last commit should be amended.
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
		  LOGGER.error(e, e);
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
		if(git != null) {
			try {
				branches = git.branchList().call();
				// EXM-47153: if we are on a detached HEAD, 
				// remove it from the list of local branches
				Iterator<Ref> iterator = branches.iterator();
				while (iterator.hasNext()) {
					Ref ref = iterator.next();
					if (Constants.HEAD.equals(ref.getName())) {
						iterator.remove();
						break;
					}
				}
			} catch (GitAPIException e) {
				LOGGER.error(e, e);
			}
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
    if(git != null) {
    	try {
    		branches = git.branchList().setListMode(ListMode.REMOTE).call();
    	} catch (GitAPIException e) {
    		LOGGER.error(e, e);
    	}
    }
    return branches;
  }
  
  /**
   * List the remote branches for the given repository URL.
   * 
   * @param sourceURL         The repository URL.
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("START LISTING REMOTE BRANCHES FOR: " + repoURL);
    }
    do {
      SSHCapableUserCredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(host);
      try {
        LOGGER.debug("Now do list the remote branches...");
        remoteRefs = Git.lsRemoteRepository()
            .setHeads(true)
            .setRemote(repoURL.toString())
            .setCredentialsProvider(credentialsProvider)
            .call();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("BRANCHES: " + remoteRefs);
        }
        shouldStopTryingLogin = true;
      } catch (TransportException ex) {
        
        LOGGER.debug(ex, ex);
        
        boolean retryLogin = AuthUtil.handleAuthException(
            ex,
            host,
            excMessPresenter,
            !credentialsProvider.wasResetCalled());
        if (!retryLogin || credentialsProvider.shouldCancelLogin()) {
          LOGGER.debug("STOP TRYING TO LOGIN!");
          shouldStopTryingLogin = true;
        }
      } catch (GitAPIException e) {
        LOGGER.error(e, e);
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
	 * 
	 * @throws GitAPIException 
	 */
	public void createBranchFromLocalBranch(
	    String newBranchName,
	    String sourceBranch) throws GitAPIException {
	  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName));
	  try {
      git.branchCreate()
        .setName(newBranchName)
        .setStartPoint(sourceBranch)
        .call();
      
      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName));
    } catch (GitAPIException e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, newBranchName), e);
      throw e;
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
	 * @param credentialsProvider The credentials provider.
	 * 
	 * @return a response.
	 *          
	 * @throws GitAPIException
	 */
	public PushResponse push(CredentialsProvider credentialsProvider)
			throws GitAPIException {

		AuthenticationInterceptor.install();

		PushResponse response = new PushResponse();

		Repository repo = git.getRepository();
		RepositoryState repoState = repo.getRepositoryState();
		if (repoState == RepositoryState.MERGING
				|| repoState == RepositoryState.REBASING
				|| repoState == RepositoryState.REBASING_MERGE
				|| repoState == RepositoryState.REBASING_REBASING
				|| repoState == RepositoryState.REVERTING) {
			response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
			response.setMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
			return response;
		}

		if (getPullsBehind() > 0) {
			response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD);
			response.setMessage(TRANSLATOR.getTranslation(Tags.BRANCH_BEHIND));
			return response;
		}

		try {
			if (getPushesAhead() == 0) {
				response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE);
				response.setMessage(TRANSLATOR.getTranslation(Tags.PUSH_UP_TO_DATE));
				return response;
			}
		} catch (RepoNotInitializedException e) {
			LOGGER.debug(e, e);
		}


		PushCommand pushCommand = git.push().setCredentialsProvider(credentialsProvider);
		String localBranchName = getBranchInfo().getBranchName();
		String upstreamBranch = getUpstreamBranchShortNameFromConfig(localBranchName);
		if (upstreamBranch != null) {
			pushCommand.setRefSpecs(
					Arrays.asList(
							new RefSpec(localBranchName + ":" + upstreamBranch.substring(upstreamBranch.indexOf('/') + 1))));
		}
		Iterable<PushResult> pushResults = pushCommand.call();

		LOGGER.debug("Push Ended");

		Iterator<PushResult> results = pushResults.iterator();
		while (results.hasNext()) {
			PushResult result = results.next();
			for (RemoteRefUpdate info : result.getRemoteUpdates()) {
				try {
					if (getRemoteFromConfig(localBranchName) == null) {
						repo.getConfig().setString(
								ConfigConstants.CONFIG_BRANCH_SECTION,
								localBranchName,
								ConfigConstants.CONFIG_KEY_REMOTE,
								Constants.DEFAULT_REMOTE_NAME);
						repo.getConfig().setString(
								ConfigConstants.CONFIG_BRANCH_SECTION,
								localBranchName,
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
		response.setMessage(TRANSLATOR.getTranslation(Tags.PUSH_FAILED_UNKNOWN));




		return response;
	}

	/**
	 * Pulls the files that are not on the local repository from the remote
	 * repository
	 * 
	 * @param credentialsProvider Credentials provider.
	 * @param pullType            One of ff, no-ff, ff-only, rebase.
	 * @param updateSubmodules    <code>true</code> to execute the equivalent of a "git submodule update --recursive"
	 * 
	 * @return The result, if successful.
	 *  
	 * @throws CheckoutConflictException There is a conflict between the local
	 *                                   repository and the remote one. The same
	 *                                   file that is in conflict is changed inside
	 *                                   the working copy so operation is aborted.
	 * @throws GitAPIException other errors.
	 */
  public PullResponse pull(
      CredentialsProvider credentialsProvider,
      PullType pullType,
      boolean updateSubmodules) throws GitAPIException {
	  PullResponse pullResponseToReturn = new PullResponse(PullStatus.OK, new HashSet<>());
	  AuthenticationInterceptor.install();

		if (!getConflictingFiles().isEmpty()) {
			pullResponseToReturn.setStatus(PullStatus.REPOSITORY_HAS_CONFLICTS);
		} else {
		  git.reset().call();

		  // Call "Pull"
		  Repository repository = git.getRepository();
		  ObjectId oldHead = resolveHead(repository);
      PullCommand pullCmd = git.pull()
          .setRebase(PullType.REBASE == pullType)
          .setCredentialsProvider(credentialsProvider);
      PullResult pullCommandResult = pullCmd.call();

		  // Get fetch result
		  Collection<TrackingRefUpdate> trackingRefUpdates = pullCommandResult.getFetchResult().getTrackingRefUpdates();
		  String lockFailureMessage = createLockFailureMessageIfNeeded(trackingRefUpdates);
		  if (!lockFailureMessage.isEmpty()) {
		    // Lock failure
		   PluginWorkspaceProvider.getPluginWorkspace()
            .showErrorMessage(TRANSLATOR.getTranslation(lockFailureMessage));
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
		
		if (updateSubmodules && pullResponseToReturn.getStatus().isSuccessful()) {
		  try {
        RepoUtil.updateSubmodules(git);
      } catch (IOException e) {
        throw new GitAPIException(e.getMessage(), e) {};
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Rebase result status: " + status);
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
      LOGGER.error(e, e);
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
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Merge result: " + mergeResult);
        LOGGER.debug("Merge result status: " + mergeResult.getMergeStatus());
      }

      if (mergeResult.getMergeStatus() == MergeStatus.FAILED) {
        if (LOGGER.isDebugEnabled()) {
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
    if (LOGGER.isDebugEnabled()) {
      Map<String, MergeFailureReason> failingPaths = mergeResult.getFailingPaths();
      if (failingPaths != null) {
        LOGGER.debug("NOW LOG MERGE FAILURE PATHS:");
        Set<String> keySet = failingPaths.keySet();
        for (String string : keySet) {
          LOGGER.debug("  Path: " + string);
          LOGGER.debug("  Reason: " + failingPaths.get(string));
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
		for (TrackingRefUpdate trackingRefUpdate : trackingRefUpdates) {
			if (trackingRefUpdate.getResult() == RefUpdate.Result.LOCK_FAILURE) {
				if (fetchResultStringBuilder.length() > 0) {
					fetchResultStringBuilder.append("\n\n");
				}
				fetchResultStringBuilder.append(TRANSLATOR.getTranslation(Tags.ERROR)).append(": ");
				fetchResultStringBuilder.append(MessageFormat.format(TRANSLATOR.getTranslation(Tags.CANNOT_LOCK_REF), trackingRefUpdate.getLocalName())).append(" ");
				try {
					String repoDir = getRepository().getDirectory().getAbsolutePath();
					File lockFile = new File(repoDir, trackingRefUpdate.getLocalName() + ".lock");
					fetchResultStringBuilder.append(MessageFormat.format(TRANSLATOR.getTranslation(Tags.UNABLE_TO_CREATE_FILE), lockFile.getAbsolutePath())).append(" ");
					if (lockFile.exists()) {
						fetchResultStringBuilder.append(TRANSLATOR.getTranslation(Tags.FILE_EXISTS)).append("\n");
					}
				} catch (NoRepositorySelected e) {
					LOGGER.error(e, e);
				}
				fetchResultStringBuilder.append(TRANSLATOR.getTranslation(Tags.LOCK_FAILED_EXPLANATION));
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
      wsAccess.getProjectManager().refreshFolders(new File[] { FileUtil.getCommonDir(pulledFilesParentDirs) });
    } catch (IOException e) {
      LOGGER.error(e, e);
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
		if (base == null) {
			return null;
		}
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
	    LOGGER.error(e, e);
		}
	}

	/**
	 * Adds multiple files to the staging area. Preparing the for commit
	 * 
	 * @param files The files to be added.
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
		  LOGGER.error(e, e);
		}
	}
	
	 /**
   * Gets all the files from the index.<br><br>
   * 
   * NOTE: if the unstaged files are also needed, use {@link #getStatus()} method instead.
   * 
   * @return A set containing all the staged file names
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
			for (String path : paths) {
				statusCmd.addPath(path);
			}

      try {
        Status status = statusCmd.call();
        return getStagedFiles(status);
			} catch (GitAPIException e) {
			  LOGGER.error(e, e);
      }
    }
    
    return Collections.emptyList();
  }

	/**
	 * Checks which files from the given subset are in the Index and returns their
	 * state.
	 * 
	 * @param status The current status.
	 * 
	 * @return - a set containing the subset of files present in the INDEX.
	 */
  private List<FileStatus> getStagedFiles(Status status) {
    List<FileStatus> stagedFiles = new ArrayList<>();
    Set<String> submodules = getSubmoduleAccess().getSubmodules();

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
			  LOGGER.error(e, e);
			}
		}
		return Collections.emptySet();
	}

	/**
	 * Reset all the specified files from the staging area.
	 * 
	 * @param files The list of file to be removed
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
		  LOGGER.error(e, e);
		}
	}

	/**
	 * Gets the host name from the repositoryURL
	 * 
	 * @return The host name. An empty string if not connected. Never
	 *         <code>null</code>.
	 */
	public String getHostName() {
	  String hostName = "";
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
				hostName = new URIish(url).getHost();
			} catch (URISyntaxException e) {
				LOGGER.debug(e, e);
			}
		}
		return hostName;

	}

	/**
	 * Finds the last local commit in the repository
	 * 
	 * @return the last local commit
	 */
	public ObjectId getLastLocalCommitInRepo() {
		return RevCommitUtil.getLastLocalCommitInRepo(git);
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
		  LOGGER.error(e, e);
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
	 * @param commitID The commit in which the file exists
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
   * Checkout the current file to a specified commit version.
   * 
   * @author Alex_Smarandache
   * 
   * @param path       The path of the file to reset.
   * @param commitId   The commit id to which to reset.
   */
  public void checkoutCommitForFile(String path, String commitId) {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.CHECKOUT_FILE));
    try {
      CheckoutCommand checkOut = GitAccess.getInstance().getGit().checkout();
      checkOut.setStartPoint(commitId);
      checkOut.addPath(path);
      checkOut.call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.CHECKOUT_FILE));
    } catch (GitAPIException e) {
      fireOperationFailed(new GitEventInfo(GitOperation.CHECKOUT_FILE), e);
      LOGGER.error(e, e);
    }
  }
  
  
  /**
   * Reverts the given commit.
   * 
   * @param commitId  The commit id to which to reset.
   * 
   * @throws IOException 
   * @throws NoRepositorySelected 
   * @throws GitAPIException 
   */
  public void revertCommit(String commitId) throws IOException, NoRepositorySelected, GitAPIException { 
    Status gitStatus = git.status().call();
    if (!gitStatus.getConflicting().isEmpty()) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(
          Translator.getInstance().getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
    } else if (gitStatus.hasUncommittedChanges()) {
      FileStatusDialog.showErrorMessage(
          Translator.getInstance().getTranslation(Tags.REVERT_COMMIT),
          new ArrayList<>(gitStatus.getUncommittedChanges()),
          Translator.getInstance().getTranslation(Tags.REVERT_COMMIT_FAILED_UNCOMMITTED_CHANGES_MESSAGE));
    } else {
      fireOperationAboutToStart(new GitEventInfo(GitOperation.REVERT_COMMIT));
      Repository repo = git.getRepository();
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit revcom = revWalk.parseCommit(getRepository().resolve(commitId));
        git.revert().include(revcom).call();
        Set<String> conflictingFiles = getConflictingFiles();
        if (!conflictingFiles.isEmpty()) {
          FileStatusDialog.showWarningMessage(
              TRANSLATOR.getTranslation(Tags.REVERT_COMMIT),
              new ArrayList<>(conflictingFiles),
              TRANSLATOR.getTranslation(Tags.REVERT_COMMIT_RESULTED_IN_CONFLICTS));
        }
        fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.REVERT_COMMIT));
      } catch (GitAPIException | RevisionSyntaxException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.REVERT_COMMIT), e);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
      }
    }
  }
  

	/**
	 * Restores the last commit file content to the local file at the given path.
	 * Both files must have the same path, otherwise it will not work.
	 * 
	 * @param paths The paths to the files to restore.
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
		  LOGGER.error(e, e);
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
	    LOGGER.error(e, e);
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
	    LOGGER.error(e, e);
	  }

		return numberOfCommits;
	}

	/**
	 * Brings all the commits to the local repository but does not merge them.
	 * 
	 * @throws SSHPassphraseRequiredException
	 * @throws PrivateRepositoryException
	 * @throws RepositoryUnavailableException
	 */
	public void fetch()
			throws SSHPassphraseRequiredException, PrivateRepositoryException, RepositoryUnavailableException {
	  LOGGER.debug("Begin fetch");
    if (git == null) {
      throw new RepositoryUnavailableException(new NoRepositorySelected("Repository is empty"));
    }
	  
		AuthenticationInterceptor.install();
		
		SSHCapableUserCredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
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
		  LOGGER.debug(e, e);
			
			Throwable cause = e;
	    while (cause.getCause() != null) {
	      cause = cause.getCause();
	    }

			String message = e.getMessage();
      if (message != null && (message.contains("Authentication is required but no CredentialsProvider has been registered")
					|| message.contains(AuthUtil.NOT_AUTHORIZED))) {
				throw new PrivateRepositoryException(e);
			} else if (message != null && message.toLowerCase().contains(AuthUtil.AUTH_FAIL) 
			    && credentialsProvider.isPassphaseRequested()
			    || (cause instanceof SshException)
              && ((SshException) cause).getDisconnectCode() == SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE) {
			  throw new SSHPassphraseRequiredException(e);
			} else {
			  throw new RepositoryUnavailableException(e);
			}
		} catch (GitAPIException | RevisionSyntaxException e) {
		  LOGGER.error(e, e);
    } 
		LOGGER.debug("End fetch");
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
		  LOGGER.error(e, e);
		}
	}

	/**
	 * Restore to the initial state of the repository. Only applicable if the
	 * repository has conflicts
	 * 
	 * @return The restart merge task.
	 */
	@SuppressWarnings("java:S1452")
	public ScheduledFuture<?> restartMerge() {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.MERGE_RESTART));
	  return GitOperationScheduler.getInstance().schedule(() -> {
	    try {
	      Repository repo = getRepository();
        RepositoryState repositoryState = repo.getRepositoryState();
	      if (repositoryState == RepositoryState.REBASING_MERGE) {
	        git.rebase().setOperation(Operation.ABORT).call();
	        // EXM-47461 Should update submodules as well.
	        CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
	        pull(credentialsProvider, PullType.REBASE, OptionsManager.getInstance().getUpdateSubmodulesOnPull());
	      } else {
	        AnyObjectId commitToMerge = repo.resolve("MERGE_HEAD");
	        git.clean().call();
	        git.reset().setMode(ResetType.HARD).call();
	        git.merge().include(commitToMerge).setStrategy(MergeStrategy.RECURSIVE).call();
	      }
	      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.MERGE_RESTART));
	    } catch (IOException | NoRepositorySelected | GitAPIException e) {
	      fireOperationFailed(new GitEventInfo(GitOperation.MERGE_RESTART), e);
	      LOGGER.error(e, e);
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
	  BranchInfo branchInfo = new BranchInfo("", false);
		if (git != null) {
			branchInfo = new BranchInfo();
			String branchName = "";
			try {
				branchName = git.getRepository().getBranch();
				branchInfo.setBranchName(branchName);
				LogCommand log = git.log();
				if(log != null) {
					Iterable<RevCommit> results = log.call();
					for (RevCommit revCommit : results) {
						if (revCommit.getId().name().equals(branchName)) {
							branchInfo.setDetached(true);
							branchInfo.setShortBranchName(
									revCommit.getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name());
							break;
						}
					}
				}
			} catch (NoHeadException e) {
			  branchInfo = new BranchInfo(branchName, false);
			  LOGGER.debug(e, e);
			} catch (IOException | GitAPIException e) {
			  LOGGER.error(e, e);
			}
		}
		return branchInfo;
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
   * Get the URL of the current remote.
   * 
   * @return The URL of the remote.
   * 
   * @throws NoRepositorySelected 
   */
  public String getRemoteURLFromConfig() throws NoRepositorySelected {
    Repository repository = GitAccess.getInstance().getRepository();
    return repository.getConfig().getString(ConfigConstants.CONFIG_KEY_REMOTE, Constants.DEFAULT_REMOTE_NAME, "url");
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
	  return git != null ? RevCommitUtil.getUpstreamBranchName(git.getRepository(), localBranchShortName) : null;
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
			switch (commit) {
			  case MINE:
			    toReturn = RevCommitUtil.getMyCommit(git, path);
			    break;
			  case THEIRS:
			    toReturn = RevCommitUtil.getTheirCommit(git, path);
			    break;
			  case BASE:
			    toReturn = RevCommitUtil.getBaseCommit(git, path);
			    break;
			  case LOCAL:
	        toReturn = RevCommitUtil.getLastLocalCommitForPath(git, path);
			    break;
			  default:
			    break;
			}
		} catch (GitAPIException | IOException e) {
		  LOGGER.error(e, e);
		}
		return toReturn;
	}

	 /**
   * Clean up.
   * <br><br>
   * Delete all listeners and close the current repository.
   */
  public void cleanUp() {
    listeners.clear();
    closeRepo();
  }
	
	/**
	 * Get the {@link Git} object through which to interact with the repository.
	 * 
	 * @return the Git object.
	 */
	public Git getGit() {
	  return git;
	}
	
	/**
	 * <<< !!!FOR TESTS!!! >>>
	 * 
	 * @param git Git.
	 */
	public void setGit(Git git) {
    this.git = git;
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
      LOGGER.debug(e, e);
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
        LOGGER.error(e, e);
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
        LOGGER.error(e, e);
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
        LOGGER.debug(e, e);
        PluginWorkspaceProvider.getPluginWorkspace()
            .showErrorMessage(TRANSLATOR.getTranslation(Tags.CANNOT_CONTINUE_REBASE_BECAUSE_OF_CONFLICTS));
      } catch (GitAPIException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.CONTINUE_REBASE), e);
        LOGGER.debug(e, e);
        PluginWorkspaceProvider.getPluginWorkspace()
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
      LOGGER.error(e, e);
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
			LOGGER.error(e, e);
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
		if(git != null) {
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
        
        String commit = ref.getObjectId().getName().substring(0, SHORT_COMMIT_ID_LENGTH);
        branchMap.computeIfAbsent(commit, t -> new ArrayList<>()).add(branchName);
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
	 * Get latest commit on current branch.
	 * 
	 * @return the latest commit.
	 * 
	 * @throws GitAPIException 
	 * @throws IOException 
	 * @throws NoRepositorySelected 
	 */
	public RevCommit getLatestCommitOnCurrentBranch() throws GitAPIException, IOException, NoRepositorySelected {
	  Repository repo = getRepository();
	  String branchNAme = getBranchInfo().getBranchName();
	  RevWalk revWalk = (RevWalk) git.log().add(repo.resolve(branchNAme)).call();
	  revWalk.sort(RevSort.COMMIT_TIME_DESC);
	  return revWalk.next();
	}
	

	/**
   * Get latest commit on the branch with given name.
   * 
   * @param branchNAme name of the target branch.
   * 
   * @return the latest commit.
   * 
   * @throws GitAPIException 
   * @throws IOException 
	 * @throws NoRepositorySelected 
   */
  public RevCommit getLatestCommitForBranch (String branchNAme) throws GitAPIException, IOException, NoRepositorySelected {
    Repository repo = getRepository();
    RevWalk revWalk = (RevWalk) git.log().add(repo.resolve(branchNAme)).call();
    revWalk.sort(RevSort.COMMIT_TIME_DESC);
    return revWalk.next();
  }

  /**
   * Merge the given branch into the current branch.
   * 
   * @param branchName The full name of the branch to be merged into the current
   *                   one (e.g. refs/heads/dev).
   * 
   * @throws IOException
   * @throws NoRepositorySelected
   * @throws GitAPIException
   */
  public void mergeBranch(String branchName) throws IOException, NoRepositorySelected, GitAPIException {
    fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.MERGE, branchName));
    try {
      ObjectId mergeBase = getRepository().resolve(branchName);
      MergeResult res = git.merge().include(mergeBase).call();
      if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("We have conflicts here:" + res.getConflicts().toString());
        }
        List<String> conflictingFiles = new ArrayList<>(res.getConflicts().keySet());
        FileStatusDialog.showWarningMessage(
            TRANSLATOR.getTranslation(Tags.MERGE_CONFLICTS_TITLE),
            conflictingFiles,
            TRANSLATOR.getTranslation(Tags.MERGE_CONFLICTS_MESSAGE));
      } else if (res.getMergeStatus().equals(MergeResult.MergeStatus.FAILED)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Failed because of this files:" + res.getFailingPaths());
        }
        List<String> failingFiles = new ArrayList<>(res.getFailingPaths().keySet());
        FileStatusDialog.showErrorMessage(
            TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE),
            failingFiles,
            TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_MESSAGE));
      }

      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.MERGE, branchName));

    } catch (RevisionSyntaxException | IOException | NoRepositorySelected e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.MERGE, branchName), e);
      throw e;
    } catch (CheckoutConflictException e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.MERGE, branchName), e);
      FileStatusDialog.showWarningMessage(
        TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE),
        e.getConflictingPaths(),
        TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_MESSAGE));
    }
  }
  
  
  /**
   * Stash List command.
   *
   * @return the list of all stashes.
   */
  public Collection<RevCommit> listStashes() {
	  Collection<RevCommit> stashedRefsCollection = null;
	  if(git != null) {
		  try {
		    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_LIST));
			  StashListCommand stashList = git.stashList();
			  stashedRefsCollection = stashList.call();
			  fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.STASH_LIST, getBranchInfo().getBranchName()));
		  } catch (Exception e) {
			  LOGGER.debug(e, e);
			  fireOperationFailed(new BranchGitEventInfo(GitOperation.STASH_LIST, getBranchInfo().getBranchName()), e);
		  }
	  }
    return stashedRefsCollection;
  }

  
  /**
   * Create a new stash command.
   *
   * @param includeUntrackedFiles <code>True</code> if the stash should include the untracked files.
   * @param description           The description of stash. May be <code>null</code>.
   * 
   * @return The created stash.
   */
  public RevCommit createStash(boolean includeUntrackedFiles, String description) {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_CREATE));
    RevCommit stash = null;
    try {
      StashCreateCommand createStashCmd = git.stashCreate().setIncludeUntracked(includeUntrackedFiles);
      if (description != null) {
        createStashCmd.setWorkingDirectoryMessage(description);
      }
      stash = createStashCmd.call();
      fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.STASH_CREATE, getBranchInfo().getBranchName()));
    } catch (GitAPIException e) {
      boolean isBecauseConflicts = getUnstagedFiles() != null 
          && !getUnstagedFiles().isEmpty() 
          && getUnstagedFiles().stream().anyMatch(file -> file.getChangeType() == GitChangeType.CONFLICT);
      if(isBecauseConflicts) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
      } else {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(
            TRANSLATOR.getTranslation(Tags.STASH_CANNOT_BE_CREATED) + e.getMessage(), e);
        LOGGER.error(e, e);
      }
      fireOperationFailed(new BranchGitEventInfo(GitOperation.STASH_CREATE, getBranchInfo().getBranchName()), e);
    }

    return stash;
  }
  
  
  /**
   * Pop the given stash.
   *
   * @param stashRef the stash which will be applied.
   * 
   * @return the status for stash apply operation.
   *
   * @throws GitAPIException 
   */
  public StashApplyStatus popStash(String stashRef) throws GitAPIException {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_APPLY));
    StashApplyStatus status = StashApplyStatus.NOT_APPLIED_UNKNOWN_CAUSE;
    try {
      checkIfStashIsApplicable(stashRef);

      git.stashApply().setStashRef(stashRef).call();

      List<RevCommit> stashes = new ArrayList<>(listStashes());
        
      status = StashApplyStatus.APPLIED_SUCCESSFULLY;
        
      for(int i = 0; i < stashes.size(); i++) {
        if(stashRef.equals(stashes.get(i).getName())) {
          dropStash(i);
          break;
        }
      }

      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_APPLY));

    } catch (StashApplyFailureWithStatusException e) {
      status = e.getStatus();
      displayStashApplyFailedCauseMessage(true, status ,e);
    } catch (StashApplyFailureException | IOException e) {   
      displayStashApplyFailedCauseMessage(true, status ,e);
    }

    return status;
  }


  /**
   * Display the stash apply failed cause message for user.
   *
   * @param isPop      <code>true</code> if the pop method.
   * @param status     stash status operation.
   * @param exception  the exception that cause the fail in apply stash.
   */
  private void displayStashApplyFailedCauseMessage(boolean isPop, StashApplyStatus status, Exception exception) {
    List<String> conflictingList = new ArrayList<>(getConflictingFiles());
    if(!conflictingList.isEmpty() && status != StashApplyStatus.CANNOT_START_APPLY_BECAUSE_CONFLICTS) {
      status = StashApplyStatus.APPLIED_SUCCESSFULLY_WITH_CONFLICTS;
    }
    switch (status) {
      case APPLIED_SUCCESSFULLY_WITH_CONFLICTS:
        if(isPop) {
          FileStatusDialog.showWarningMessage(TRANSLATOR.getTranslation(Tags.APPLY_STASH),
                  conflictingList,
                  TRANSLATOR.getTranslation(Tags.STASH_GENERATE_CONFLICTS)
                          + " "
                          + TRANSLATOR.getTranslation(Tags.STASH_WAS_KEPT)
          );
        } else {
          FileStatusDialog.showWarningMessage(TRANSLATOR.getTranslation(Tags.APPLY_STASH),
                  conflictingList,
                  TRANSLATOR.getTranslation(Tags.STASH_GENERATE_CONFLICTS)
          );
        }
        fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_APPLY));
        break;
      case CANNOT_START_APPLY_BECAUSE_CONFLICTS:
        FileStatusDialog.showErrorMessage(
                TRANSLATOR.getTranslation(Tags.APPLY_STASH),
                new ArrayList<>(getConflictingFiles()),
                TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH)
                        + ". "
                        + TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception, exception);
        break;
      case CANNOT_START_APPLY_BECAUSE_UNCOMMITTED_FILES:
        FileStatusDialog.showErrorMessage(
                TRANSLATOR.getTranslation(Tags.APPLY_STASH),
                null,
                TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH)
                        + ". "
                        + TRANSLATOR.getTranslation(Tags.STASH_SOLUTIONS_TO_APPLY));
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception, exception);
        break;
      case CANNOT_START_BECAUSE_STAGED_FILES:
        FileStatusDialog.showErrorMessage(
                TRANSLATOR.getTranslation(Tags.APPLY_STASH),
                null,
                TRANSLATOR.getTranslation(Tags.STASH_REMOVE_STAGED_CHANGES));
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception, exception);
        break;
      default:
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH) + ".",
                exception);
        LOGGER.error(exception, exception);
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        break;
    }
  }


  /**
   * Apply the given stash.
   *
   * @param stashRef the stash which will be applied.
   * 
   * @return the status for stash apply operation.
   * 
   * @throws GitAPIException 
   */
  public StashApplyStatus applyStash(String stashRef) throws GitAPIException {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_APPLY));
    StashApplyStatus status = StashApplyStatus.NOT_APPLIED_UNKNOWN_CAUSE;
    try {
      checkIfStashIsApplicable(stashRef);

      git.stashApply().setStashRef(stashRef).call();

      status = StashApplyStatus.APPLIED_SUCCESSFULLY;

      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_APPLY));

    } catch (StashApplyFailureWithStatusException e) {
      status = e.getStatus();
      displayStashApplyFailedCauseMessage(false, status ,e);
    } catch (StashApplyFailureException | IOException e) {
      displayStashApplyFailedCauseMessage(false, status ,e);
    }

    return status;
  }

  
  
  /**
   * @param stashRef       The stash reference.
   *
   * @throws IOException
   * @throws GitAPIException
   */
  private void checkIfStashIsApplicable(String stashRef) throws IOException, GitAPIException {
    List<FileStatus> list = RevCommitUtil.getChangedFiles(stashRef);
    List<FileStatus> unstagedFiles = new ArrayList<>(getUnstagedFiles());

    for (FileStatus fileStatus : list) {

      for (FileStatus file : unstagedFiles) {
        if (file.getChangeType() == GitChangeType.CONFLICT) {
          throw new StashApplyFailureWithStatusException(StashApplyStatus.CANNOT_START_APPLY_BECAUSE_CONFLICTS, "Impossible to apply");
        } else if (file.getFileLocation().compareTo(fileStatus.getFileLocation()) == 0) {
          throw new StashApplyFailureWithStatusException(StashApplyStatus.CANNOT_START_APPLY_BECAUSE_UNCOMMITTED_FILES, "Impossible to apply");
        }
      }

      if(!getStagedFiles().isEmpty()) {
        throw new StashApplyFailureWithStatusException(StashApplyStatus.CANNOT_START_BECAUSE_STAGED_FILES, "Impossible to apply");
      }
      
    }

  }
  
  
  /**
   * Drops one stash item from the list of stashes.
   * 
   * @param stashIndex The index of the stash item to be dropped.
   * 
   * @throws GitAPIException
   */
  public void dropStash(int stashIndex) throws GitAPIException {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_DROP));
    try {
      git.stashDrop().setStashRef(stashIndex).call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_DROP));
    } catch (GitAPIException e) {
      LOGGER.error(e, e);
      fireOperationFailed(new GitEventInfo(GitOperation.STASH_DROP), e);
      throw e;
    }
  }


  /**
   * Drops all stashes.
   * 
   * @throws GitAPIException
   */
  public void dropAllStashes() throws GitAPIException {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.STASH_DROP));
    try {
      git.stashDrop().setAll(true).call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_DROP));
    } catch (GitAPIException e) {
      LOGGER.error(e, e);
      fireOperationFailed(new GitEventInfo(GitOperation.STASH_DROP), e);
      throw e;
    }
  }
	
/**
 * Creates a tag commit
 * 
 * @param name the name of the tag
 * @param message the message of the tag
 * @param commitId the id of the commit where the tag will be
 * 
 * @throws GitAPIException 
 * @throws NoRepositorySelected
 * @throws IOException
 */
	public void tagCommit(String name, String message, String commitId) throws GitAPIException, NoRepositorySelected, IOException {
	  fireOperationAboutToStart(new GitEventInfo(GitOperation.TAG_COMMIT));
	  try {
	    RevWalk walk = new RevWalk(getRepository());
	    RevCommit id = walk.parseCommit(getRepository().resolve(commitId));
      git.tag()
        .setName(name)
        .setMessage(message)
        .setObjectId(id)
        .setForceUpdate(true)
        .call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.TAG_COMMIT));
    } catch (GitAPIException | NoRepositorySelected | RevisionSyntaxException | IOException e) {
      LOGGER.error(e, e);
      fireOperationFailed(new GitEventInfo(GitOperation.TAG_COMMIT), e);
      throw e;
    }
	}
	
	/**
	 * Check if the given tag exists
	 * 
	 * @param name The name of the tag
	 * 
	 * @return <code>true</code> if this tag exists
	 * 
	 * @throws NoRepositorySelected
	 * @throws IOException
	 */
	public boolean existsTag(String name) throws NoRepositorySelected, IOException {
	  Repository repo;
      repo = getRepository();
      Ref tag = repo.exactRef(Constants.R_TAGS + name);
      return tag != null;
	}
	
	/**
	 * Push a given local tag
	 * 
	 * @param name The name of the tag
	 * 
	 * @throws GitAPIException
	 */
	public void pushTag(String name) throws GitAPIException {
      CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
      getGit()
        .push()
        .setCredentialsProvider(credentialsProvider)
        .setRefSpecs(new RefSpec("refs/tags/"+ name +":refs/tags/" + name))
        .call();
	}
	
	/**
	 * Delete a given Tag
	 * 
	 * @param name The name of the tag to be deleted
	 * 
	 * @throws GitAPIException
	 */
	public void deleteTag(String name) throws GitAPIException  {
	  fireOperationAboutToStart(new GitEventInfo(GitOperation.TAG_DELETE));
	  try {
      getGit()
        .tagDelete()
        .setTags(name)
        .call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.TAG_DELETE));
    } catch (GitAPIException e) {
      LOGGER.error(e, e);
      fireOperationFailed(new GitEventInfo(GitOperation.TAG_DELETE), e);
      throw e;
    }
	}
}
