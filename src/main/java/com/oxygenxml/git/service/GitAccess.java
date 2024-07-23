package com.oxygenxml.git.service;

import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.bouncycastle.openpgp.PGPException;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
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
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
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
import org.eclipse.jgit.lib.BranchConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.auth.AuthExceptionMessagePresenter;
import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.connection.ConnectionUtil;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoChangesInSquashedCommitException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.PrivateRepositoryException;
import com.oxygenxml.git.service.exceptions.RebaseConflictsException;
import com.oxygenxml.git.service.exceptions.RebaseUncommittedChangesException;
import com.oxygenxml.git.service.exceptions.RepoNotInitializedException;
import com.oxygenxml.git.service.exceptions.RepositoryUnavailableException;
import com.oxygenxml.git.service.exceptions.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.utils.URIUtil;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.dialog.GPGPassphraseDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.event.BranchGitEventInfo;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.history.RenameTracker;
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
  static {
    ConnectionUtil.installHttpConnectionFactory();
  }
  
  /**
   * The default name for the first branch created:"main" 
   */
  public static final String DEFAULT_BRANCH_NAME = "main";
  
  /**
   * The length of the short commit ID.
   */
  public static final int SHORT_COMMIT_ID_LENGTH = 7;
  
  /**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(GitAccess.class);
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
   * A synthetic object representing the uncommitted changes.
   */
  public static final CommitCharacteristics UNCOMMITED_CHANGES = new CommitCharacteristics(
      TRANSLATOR.getTranslation(Tags.UNCOMMITTED_CHANGES), null, "*", "*", "*", null, null) {
    
    @Override
    public String getCommitMessage() {
      // because the translation is not available for class loading in memory, so the message will be not processed correct.
      return TRANSLATOR.getTranslation(Tags.UNCOMMITTED_CHANGES);
    }
    
  };
  
	/**
	 * Keeps a cache of the computed status to avoid processing overhead.
	 */
	private StatusCache statusCache = null;

	/**
	 * Singleton instance.
	 */
	private GitAccess() {
	  statusCache = new StatusCache(GitListeners.getInstance(), this::getGit);
	}
	
	/**
	 * @return A cache of the computed status to avoid processing overhead.
	 */
	public StatusCache getStatusCache() {
    return statusCache;
  }

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
    
		ProgressMonitor progressMonitor = new GitOperationProgressMonitor(progressDialog);
		if (progressDialog != null) {
		  progressDialog.setNote("Initializing...");
		  progressMonitor.beginTask("Initializing ", 0);
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
	      LOGGER.debug(e.getMessage(), e);
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
    final File repo = new File(path + "/.git"); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
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
    LOGGER.debug("Java env user home: {}", System.getProperty("user.home"));
    try {
      Repository repository = getRepository();
      LOGGER.debug("Load repository: {}", repository.getDirectory());
      
      FS fs = repository.getFS();
      if (fs != null) {
        File userHome = fs.userHome();
        LOGGER.debug("User home: {}", userHome);

        File sshDir = new File(userHome, ".ssh");

        boolean exists = sshDir.exists();
        LOGGER.debug("SSH dir exists: {}", exists);
        if (exists) {
          File[] listFiles = sshDir.listFiles();
					for (File listFile : listFiles) {
						LOGGER.debug("SSH resource path: {}" , listFile);
					}
        }
      } else {
        LOGGER.debug("Null FS");
      }
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
    }
  }
  
  /**
   * Fire operation about to start.
   * 
   * @param info event info.
   */
  private void fireOperationAboutToStart(GitEventInfo info) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation about to start: {}", info);
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
      LOGGER.debug("Fire operation successfully ended: {}", info);
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
      LOGGER.debug("Fire operation failed: {}", info); 
      LOGGER.debug("Failure reason: {}", t.getMessage());
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
	  File wc = new File(path); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
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
    return statusCache.getStatus();
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
    List<FileStatus> unstagedFiles =  Collections.emptyList();
    if (git != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("PUBLIC - GET UNSTAGED FILES");
        LOGGER.debug("Prepare fot JGit status, in paths: {}", paths);
      }
      
      if (paths != null && !paths.isEmpty()) {
        // We have paths. Build a fresh copy.
        unstagedFiles = new GitStatusCommand(() -> git).getUnstagedFiles(paths);
      } else {
        unstagedFiles = statusCache.getStatus().getUnstagedFiles();
      }
      
    }
    
    return unstagedFiles;
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
		  git.commit()
		      .setMessage(message)
		      .setAmend(isAmendLastCommit)
		      .setCredentialsProvider(new GPGCapableCredentialsProvider(OptionsManager.getInstance().getGPGPassphrase()))
		      .call();
		  fireOperationSuccessfullyEnded(new FileGitEventInfo(GitOperation.COMMIT, filePaths));
		} catch (CanceledException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.COMMIT, filePaths), e);
		  LOGGER.debug(e.getMessage(), e);
		  throw e;
		} catch (GitAPIException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.COMMIT, filePaths), e);
		  LOGGER.error(e.getMessage(), e);
		  throw e;
		} catch (JGitInternalException e) {
		  fireOperationFailed(new FileGitEventInfo(GitOperation.COMMIT, filePaths), e);
		  LOGGER.error(e.getMessage(), e);
		  Throwable cause = e.getCause();
		  if (cause instanceof PGPException && cause.getMessage().toLowerCase().contains("passphrase")) {
		    GPGPassphraseDialog dlg = new GPGPassphraseDialog(Translator.getInstance().getTranslation(Tags.ENTER_GPG_PASSPHRASE) + ".");
		    if (dlg.getPassphrase() != null) {
		      commit(message, isAmendLastCommit);
		    } else {
		      throw new CanceledException("Commit signing was cancelled.");
		    }
		  } else {
		    throw e;
		  }
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
				LOGGER.error(e.getMessage(), e);
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
    		LOGGER.error(e.getMessage(), e);
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
  public Collection<Ref> doListRemoteBranchesInternal(
      URIish repoURL,
      AuthExceptionMessagePresenter excMessPresenter) {
    Collection<Ref> remoteRefs = Collections.emptySet();
    String host = repoURL.getHost();
    boolean shouldStopTryingLogin = false;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("START LISTING REMOTE BRANCHES FOR: {}", repoURL);
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
          LOGGER.debug("BRANCHES: {}", remoteRefs);
        }
        shouldStopTryingLogin = true;
      } catch (TransportException ex) {
        LOGGER.debug(ex.getMessage(), ex);
        
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
        LOGGER.error(e.getMessage(), e);
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
   * Creates a new branch in the repository
   * 
   * @param branchName   - Name for the new branch
   * @param sourceCommit - The commit source.    
   */
  public void createBranch(String branchName, String sourceCommit) {
    try {
      fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CREATE_BRANCH, branchName));
      git.branchCreate().setName(branchName).setStartPoint(sourceCommit).call();
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
			LOGGER.debug(e.getMessage(), e);
		}


		PushCommand pushCommand = git.push().setCredentialsProvider(credentialsProvider).setRemote(getRemoteFromCurrentBranch());
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
	 * @param monitor             The monitor progress for the pull operation.
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
	    ProgressMonitor monitor,
	    boolean updateSubmodules) throws GitAPIException {
	  PullResponse pullResponseToReturn = new PullResponse(PullStatus.OK, new HashSet<>());
	  AuthenticationInterceptor.install();

	  if (!getConflictingFiles().isEmpty()) {
	    pullResponseToReturn.setStatus(PullStatus.REPOSITORY_HAS_CONFLICTS);
	  } else {

	    // Call "Pull"
	    Repository repository = git.getRepository();
	    ObjectId oldHead = resolveHead(repository);
	    PullCommand pullCmd = git.pull()
	        .setRebase(PullType.REBASE == pullType)
	        .setCredentialsProvider(credentialsProvider)
	        .setProgressMonitor(monitor)
	        .setRemote(getRemoteFromCurrentBranch());
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
      LOGGER.debug("Rebase result status: {}", status);
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
      LOGGER.error(e.getMessage(), e);
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
        LOGGER.debug("Merge result: {}", mergeResult);
        LOGGER.debug("Merge result status: {}", mergeResult.getMergeStatus());
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
          LOGGER.debug("  Path: {}", string);
          LOGGER.debug("  Reason: {}", failingPaths.get(string));
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
				fetchResultStringBuilder.append(
				    MessageFormat.format(
				        TRANSLATOR.getTranslation(Tags.CANNOT_LOCK_REF),
				        trackingRefUpdate.getLocalName())).append(" ");
				try {
					String repoDir = getRepository().getDirectory().getAbsolutePath();
					File lockFile = new File(repoDir, trackingRefUpdate.getLocalName() + ".lock"); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false positive
					fetchResultStringBuilder.append(
					    MessageFormat.format(
					        TRANSLATOR.getTranslation(Tags.UNABLE_TO_CREATE_FILE),
					        lockFile.getAbsolutePath())).append(" ");
					if (lockFile.exists()) {
						fetchResultStringBuilder.append(TRANSLATOR.getTranslation(Tags.FILE_EXISTS)).append("\n");
					}
				} catch (NoRepositorySelected e) {
					LOGGER.error(e.getMessage(), e);
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
          pulledFilesParentDirs.add(
              new File(selectedRepository, diffEntry.getNewPath()).getParentFile()); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false positive
        } else if (diffEntry.getChangeType() == ChangeType.DELETE) {
          pulledFilesParentDirs.add(
              new File(selectedRepository, diffEntry.getOldPath()).getParentFile()); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false positive
        }
      }
      // Refresh the Project view
      StandalonePluginWorkspace wsAccess = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      wsAccess.getProjectManager().refreshFolders(new File[] { FileUtil.getCommonDir(pulledFilesParentDirs) });
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
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
	    LOGGER.error(e.getMessage(), e);
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
		  LOGGER.error(e.getMessage(), e);
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
    List<FileStatus> stagedFiles = Collections.emptyList();
    if (git != null) {
      if (paths != null && !paths.isEmpty()) {
        stagedFiles = new GitStatusCommand(() -> git).getStagedFile(paths);
      } else {
        stagedFiles = statusCache.getStatus().getStagedFiles();
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
			  LOGGER.error(e.getMessage(), e);
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
		  LOGGER.error(e.getMessage(), e);
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
			String url = storedConfig.getString(ConfigConstants.CONFIG_KEY_REMOTE, Constants.DEFAULT_REMOTE_NAME, "url");
			if (url == null) {
			  Set<String> remoteNames = git.getRepository().getRemoteNames();
			  Iterator<String> iterator = remoteNames.iterator();
			  if (iterator.hasNext()) {
			    url = storedConfig.getString(ConfigConstants.CONFIG_KEY_REMOTE, iterator.next(), "url");
			  }
			}
			hostName = URIUtil.extractHostName(url);
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
		  LOGGER.error(e.getMessage(), e);
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
      LOGGER.error(e.getMessage(), e);
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
      MessagePresenterProvider.getBuilder(
          TRANSLATOR.getTranslation(Tags.REVERT_COMMIT), DialogType.ERROR)
          .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(new ArrayList<>(gitStatus.getUncommittedChanges())))
          .setMessage(TRANSLATOR.getTranslation(Tags.REVERT_COMMIT_FAILED_UNCOMMITTED_CHANGES_MESSAGE))
          .setCancelButtonVisible(false)
          .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
          .buildAndShow();         
    } else {
      fireOperationAboutToStart(new GitEventInfo(GitOperation.REVERT_COMMIT));
      Repository repo = git.getRepository();
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit revcom = revWalk.parseCommit(getRepository().resolve(commitId));
        git.revert().include(revcom).call();
        Set<String> conflictingFiles = getConflictingFiles();
        if (!conflictingFiles.isEmpty()) {
          MessagePresenterProvider.getBuilder(
              TRANSLATOR.getTranslation(Tags.REVERT_COMMIT), DialogType.WARNING)
              .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(new ArrayList<>(conflictingFiles)))
              .setMessage(TRANSLATOR.getTranslation(Tags.REVERT_COMMIT_RESULTED_IN_CONFLICTS))
              .setCancelButtonVisible(false)
              .buildAndShow();         
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
		  LOGGER.error(e.getMessage(), e);
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
	    LOGGER.error(e.getMessage(), e);
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
	    LOGGER.error(e.getMessage(), e);
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
            .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/" + getRemoteFromCurrentBranch() + "/*"))
            .setCheckFetchedObjects(true)
            .setRemote(getRemoteFromCurrentBranch())
            .setRemoveDeletedRefs(true)
						.setCredentialsProvider(credentialsProvider)
						.call();
			}
		} catch (TransportException e) {
		  LOGGER.debug(e.getMessage(), e);
		
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
		  LOGGER.error(e.getMessage(), e);
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
		  LOGGER.error(e.getMessage(), e);
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
	        pull(credentialsProvider, PullType.REBASE, null, OptionsManager.getInstance().getUpdateSubmodulesOnPull());
	      } else {
	        AnyObjectId commitToMerge = repo.resolve("MERGE_HEAD");
	        git.clean().call();
	        git.reset().setMode(ResetType.HARD).call();
	        git.merge().include(commitToMerge).setStrategy(MergeStrategy.RECURSIVE).call();
	      }
	      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.MERGE_RESTART));
	    } catch (IOException | NoRepositorySelected | GitAPIException e) {
	      fireOperationFailed(new GitEventInfo(GitOperation.MERGE_RESTART), e);
	      LOGGER.error(e.getMessage(), e);
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
			  LOGGER.debug(e.getMessage(), e);
			} catch (IOException | GitAPIException e) {
			  LOGGER.error(e.getMessage(), e);
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
	public void setBranch(String branch) throws GitAPIException, IOException {
	  fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CHECKOUT, branch));
	  try {
	    
	    LogUtil.logSubmodule();
	    git.checkout().setName(branch).call();
	    LogUtil.logSubmodule();
	    
	    RepoUtil.checkoutSubmodules(git, e -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e));
	    
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
  public void checkoutRemoteBranchWithNewName(String newBranchName, String remoteBranchName, String...remote) throws GitAPIException{
    fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.CHECKOUT, newBranchName));
    String remoteString = remote.length ==  0 ? Constants.DEFAULT_REMOTE_NAME : remote[0];
    try {
      git.checkout()
          .setCreateBranch(true)
          .setName(newBranchName)
          .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
          .setStartPoint(remoteString + "/" + remoteBranchName)
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
	  return repository.getConfig().getString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE);
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
    return repository.getConfig().getString(ConfigConstants.CONFIG_KEY_REMOTE, 
        getRemoteFromCurrentBranch(), ConfigConstants.CONFIG_KEY_URL);
  }

  /**
   * Get the URL of the current remote.
   * 
   * @param remote The remote.
   * 
   * @return The URL of the remote.
   * 
   * @throws NoRepositorySelected 
   */
  public String getRemoteURLFromConfig(@NonNull final String remote) throws NoRepositorySelected {
    Repository repository = GitAccess.getInstance().getRepository();
    return repository.getConfig().getString(ConfigConstants.CONFIG_KEY_REMOTE, 
        remote, ConfigConstants.CONFIG_KEY_URL);
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
		  LOGGER.error(e.getMessage(), e);
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
    statusCache = new StatusCache(listeners, () -> git);
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
      LOGGER.debug(e.getMessage(), e);
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
        LOGGER.error(e.getMessage(), e);
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
        LOGGER.error(e.getMessage(), e);
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
        LOGGER.debug(e.getMessage(), e);
        PluginWorkspaceProvider.getPluginWorkspace()
            .showErrorMessage(TRANSLATOR.getTranslation(Tags.CANNOT_CONTINUE_REBASE_BECAUSE_OF_CONFLICTS));
      } catch (GitAPIException e) {
        fireOperationFailed(new GitEventInfo(GitOperation.CONTINUE_REBASE), e);
        LOGGER.debug(e.getMessage(), e);
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
      LOGGER.error(e.getMessage(), e);
    }
  }
  
   /**
	 * Compute a Vector with the characteristics of each commit.
	 * 
	 * @param filePath A resource for which we are interested in its history. If <code>null</code>, 
	 * the repository history will be computed.
	 * @param The rename tracker to follow rename path changes.
	 * 
	 * @return a Vector with commits characteristics of the current repository.
	 */
	public List<CommitCharacteristics> getCommitsCharacteristics(HistoryStrategy strategy,  String filePath, RenameTracker renameTracker) {
		List<CommitCharacteristics> revisions = new ArrayList<>();

		try {
			Repository repository = this.getRepository();
			if (filePath == null && statusCache.getStatus().hasUncommittedChanges()) {
				revisions.add(UNCOMMITED_CHANGES);
			}
            
			switch (strategy) {
			case ALL_BRANCHES:
				RevCommitUtil.collectAllBranchesRevisions(filePath, revisions, repository, renameTracker);
				break;
			case ALL_LOCAL_BRANCHES:
				RevCommitUtil.collectLocalBranchesRevisions(filePath, revisions, repository, renameTracker);
				break;
			case CURRENT_BRANCH:
				RevCommitUtil.collectCurrentBranchRevisions(filePath, revisions, repository, renameTracker);
				break;
			case CURRENT_LOCAL_BRANCH:
				RevCommitUtil.collectCurrentLocalBranchRevisions(filePath, revisions, repository, renameTracker);
				break;
			default:
			  break;
			}
			
		} catch (NoWorkTreeException | NoRepositorySelected | IOException e) {
			LOGGER.error(e.getMessage(), e);
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
				String commitToPut = logs.iterator().next().getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name();
				List<String> tags = commitTagMap.computeIfAbsent(commitToPut, key -> new ArrayList<>());
				tags.addAll(tagList);
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
        
        ObjectId objectId = ref.getObjectId();
        if (objectId != null) {
          String commit = objectId.getName().substring(0, SHORT_COMMIT_ID_LENGTH);
          branchMap.computeIfAbsent(commit, t -> new ArrayList<>()).add(branchName);
        }
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
  public void mergeBranch(String branchName) throws IOException, NoRepositorySelected, GitAPIException, NoChangesInSquashedCommitException {
    internalMerge(branchName, false, null);
  }
  
  /**
   * Squash and merge the given branch into the current branch.
   * 
   * @param branchName    The full name of the branch to be merged into the current one(e.g. refs/heads/dev).
   * @param commitMessage The commit message.
   * 
   * @throws IOException
   * @throws NoRepositorySelected
   * @throws GitAPIException
   */
  public void squashAndMergeBranch(final String branchName, final String commitMessage) 
      throws IOException, NoRepositorySelected, GitAPIException, NoChangesInSquashedCommitException {
     internalMerge(branchName, true, commitMessage);
  }

  /**
   * Merge the given branch into the current branch.
   * 
   * @param branchName    The full name of the branch to be merged into the current one(e.g. refs/heads/dev).
   * @param isSquashMerge <code>true</code> if is a squash commit. 
   * @param message       The commit message for squashed commit.
   * 
   * @throws IOException
   * @throws NoRepositorySelected
   * @throws GitAPIException
   */
  private void internalMerge(final String branchName, boolean isSquash, final String message)
      throws IOException, NoRepositorySelected, GitAPIException, NoChangesInSquashedCommitException {
    fireOperationAboutToStart(new BranchGitEventInfo(GitOperation.MERGE, branchName));
    
    try {
      final ObjectId mergeBase = getRepository().resolve(branchName);
      final MergeCommand mergeCommand = git.merge().include(mergeBase);
      if(isSquash) {
        mergeCommand.setStrategy(MergeStrategy.RESOLVE).setSquash(isSquash).setCommit(true);
      }
     
      final MergeResult res = mergeCommand.call();
      if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("We have conflicts here: {}", res.getConflicts());
        }
        final List<String> conflictingFiles = new ArrayList<>(res.getConflicts().keySet());
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.MERGE_CONFLICTS_TITLE), DialogType.WARNING)
            .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(new ArrayList<>(conflictingFiles)))
            .setMessage(TRANSLATOR.getTranslation(Tags.MERGE_CONFLICTS_MESSAGE))
            .setCancelButtonVisible(false)
            .buildAndShow(); 
        fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.MERGE, branchName));
      } else if (res.getMergeStatus().equals(MergeResult.MergeStatus.FAILED)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Failed because of this files: {}", res.getFailingPaths());
        }
        final List<String> failingFiles = new ArrayList<>(res.getFailingPaths().keySet());
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE), DialogType.ERROR)
            .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(new ArrayList<>(failingFiles)))
            .setMessage(TRANSLATOR.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_MESSAGE))
            .setCancelButtonVisible(false)
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
            .buildAndShow();  
        fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.MERGE, branchName));
      } else if(isSquash) {
          fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.MERGE, branchName));
          final List<FileStatus> stagedFiles = getStagedFiles();
          if(stagedFiles != null && !stagedFiles.isEmpty()) {
            commit(message != null? message : "");
          } else {
            throw new NoChangesInSquashedCommitException(MessageFormat.format(
                Translator.getInstance().getTranslation(Tags.SQUASH_NO_CHANGES_DETECTED_MESSAGE),
                TextFormatUtil.shortenText(branchName, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")));
          }
      } else {
        fireOperationSuccessfullyEnded(new BranchGitEventInfo(GitOperation.MERGE, branchName));
      }
      
    } catch(NoChangesInSquashedCommitException e) {
      throw e;
    } catch (GitAPIException | IOException | NoRepositorySelected e) {
      fireOperationFailed(new BranchGitEventInfo(GitOperation.MERGE, branchName), e);
      throw e;
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
			  StashListCommand stashList = git.stashList();
			  stashedRefsCollection = stashList.call();
		  } catch (Exception e) {
			  LOGGER.debug(e.getMessage(), e);
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
  @Nullable public RevCommit createStash(boolean includeUntrackedFiles, String description) {
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
      if(repositoryHasConflicts()) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
      } else {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(
            TRANSLATOR.getTranslation(Tags.STASH_CANNOT_BE_CREATED) + e.getMessage(), e);
        LOGGER.error(e.getMessage(), e);
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
    	tryRestoreUntrackedFiles(status, searchStashByID(stashRef).orElseThrow()); // should not happen
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
  private void displayStashApplyFailedCauseMessage(boolean isPop, 
  		StashApplyStatus status, 
  		Exception exception) {
    List<String> conflictingList = new ArrayList<>(getConflictingFiles());
    if(!conflictingList.isEmpty() && status != StashApplyStatus.CANNOT_START_APPLY_BECAUSE_CONFLICTS) {
      status = StashApplyStatus.APPLIED_SUCCESSFULLY_WITH_CONFLICTS;
    }
    switch (status) {
      case APPLIED_SUCCESSFULLY_WITH_CONFLICTS:
        if(isPop) {
          MessagePresenterProvider.getBuilder(
              TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.WARNING)
              .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(conflictingList))
              .setMessage(TRANSLATOR.getTranslation(Tags.STASH_GENERATE_CONFLICTS))
              .setCancelButtonVisible(false)
              .buildAndShow();
          
        } else {
          MessagePresenterProvider.getBuilder(
              TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.WARNING)
              .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(conflictingList))
              .setMessage(TRANSLATOR.getTranslation(Tags.STASH_GENERATE_CONFLICTS)
                  + " "
                  + TRANSLATOR.getTranslation(Tags.STASH_WAS_KEPT))
              .setCancelButtonVisible(false)
              .buildAndShow();            
        }
        fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.STASH_APPLY));
        break;
      case CANNOT_START_APPLY_BECAUSE_CONFLICTS:
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.ERROR)
            .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(new ArrayList<>(getConflictingFiles())))
            .setMessage(TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH)
                + ". "
                + TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST))
            .setCancelButtonVisible(false)
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
            .buildAndShow();  
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception.getMessage(), exception);
        break;
      case CANNOT_START_APPLY_BECAUSE_UNCOMMITTED_FILES:
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.ERROR)
            .setMessage(TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH)
                + ". "
                + TRANSLATOR.getTranslation(Tags.STASH_SOLUTIONS_TO_APPLY))
            .setCancelButtonVisible(false)
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
            .buildAndShow();     
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception.getMessage(), exception);
        break;
      case CANNOT_START_BECAUSE_STAGED_FILES:
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.ERROR)
            .setMessage(TRANSLATOR.getTranslation(Tags.STASH_REMOVE_STAGED_CHANGES))
            .setCancelButtonVisible(false)
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
            .buildAndShow();     
        fireOperationFailed(new GitEventInfo(GitOperation.STASH_APPLY), exception);
        LOGGER.error(exception.getMessage(), exception);
        break;
      default:
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(Tags.UNABLE_TO_APPLY_STASH) + ".",
                exception);
        LOGGER.error(exception.getMessage(), exception);
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
      displayStashApplyFailedCauseMessage(false, status ,e);
    } catch (StashApplyFailureException | IOException e) {
      tryRestoreUntrackedFiles(status, searchStashByID(stashRef).orElseThrow()); // should not happen
      displayStashApplyFailedCauseMessage(false, status ,e);
    }

    return status;
  }

  /**
   * Search a stash by its identifier.
   * 
   * @param stashRef The stash identifier.
   * 
   * @return An optional that contains the ObjectId of the founded stash,
   */
	private Optional<ObjectId> searchStashByID(final String stashRef) {
		return listStashes()
		  .stream()
		  .filter(commit -> Objects.equals(commit.getName(), stashRef))
		  .map(RevCommit::getId)
		  .findFirst();
	}

  
  /**
   * This method restore the untracked files when the stash fails because a conflict. 
   * <br>
   * If no untracked file is present, this method will do nothing.
   * 
   * @param status    The file status operation.
   * @param stashRef  The stash to be restored.
   */ 
  private void tryRestoreUntrackedFiles(StashApplyStatus status, ObjectId stashRef) {
  	final List<String> conflictingList = new ArrayList<>(getConflictingFiles());
  	final List<FileStatus> overwrittenFiles = new ArrayList<>();
  	if(!conflictingList.isEmpty() && status != StashApplyStatus.CANNOT_START_APPLY_BECAUSE_CONFLICTS) {
  		status = StashApplyStatus.APPLIED_SUCCESSFULLY_WITH_CONFLICTS;
  	}
  	
  	if(status == StashApplyStatus.APPLIED_SUCCESSFULLY_WITH_CONFLICTS) {
  		try {
  			final List<FileStatus> untrackedFiles = RevCommitUtil
  					.getChangedFiles(stashRef.getName())
  					.stream()
  					.filter(fileStatus -> GitChangeType.UNTRACKED.equals(fileStatus.getChangeType()))
  					.collect(Collectors.toList());
  			final File workingCopy = getWorkingCopy();
  			final RevCommit[] parents = RevCommitUtil.getParents(GitAccess.getInstance().getRepository(),
  					stashRef.getName());
  			if(parents.length < (RevCommitUtil.PARENT_COMMIT_UNTRACKED + 1)) {
  				return;
  			}
  			
  			final String untrackedFilesCommitId = parents[RevCommitUtil.PARENT_COMMIT_UNTRACKED].getId().getName();
  			final List<String> missingFiles = new ArrayList<>();
  			
  			untrackedFiles.forEach(
  					file -> {
							try {
								restoreUntrackedFile(stashRef, overwrittenFiles, workingCopy, 
										untrackedFilesCommitId, file);
							} catch (IOException e) {
								missingFiles.add(file.getFileLocation());
							}
						}
  		  );
  			
  			if(!missingFiles.isEmpty()) {
  				MessagePresenterProvider.getBuilder(
  	          TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.ERROR)
  	          .setTargetFilesWithTooltips(FileStatusUtil.comuteFilesTooltips(missingFiles))
  	          .setMessage(TRANSLATOR.getTranslation(Tags.MISSING_UNTRACKED_FILES))
  	          .setCancelButtonVisible(false)
  	          .buildAndShow();
  	  	}
  		} catch (IOException | GitAPIException | NoRepositorySelected e) {
  			LOGGER.error(e.getMessage(), e);
  		}
  	} 
  	
  	if(!overwrittenFiles.isEmpty()) {
  		MessagePresenterProvider.getBuilder(
          TRANSLATOR.getTranslation(Tags.APPLY_STASH), DialogType.WARNING)
          .setTargetFilesWithTooltips(FileStatusUtil
          		.comuteFilesTooltips(overwrittenFiles.stream()
          		.map(FileStatus::getFileLocation)
          		.collect(Collectors.toList())))
          .setMessage(TRANSLATOR.getTranslation(Tags.UNTRACKED_FILES_NAME_CHANGED))
          .setCancelButtonVisible(false)
          .buildAndShow();
  	}
  
  }

  /**
   * This method restore one untracked file from a stash.
   * 
   * @param stashRef                    The ID of the stash to restore the untracked file.
   * @param overwrittenFiles            A list with existing files that are overwritten.
   * @param workingCopy                 The WC local directory.
   * @param untrackedFilesCommitId      The untracked files commit ID.
   * @param file                        The file to be restored. 
   * 
   * @throws IOException When IO problems occur.
   */
	private void restoreUntrackedFile(ObjectId stashRef, 
			final List<FileStatus> overwrittenFiles, 
			final File workingCopy,
			final String untrackedFilesCommitId, 
			final FileStatus file) throws IOException {
		File fileToRestore = null;
		try {
			final ObjectId fileObject = RevCommitUtil.getObjectID(getRepository(), untrackedFilesCommitId, file.getFileLocation());
			fileToRestore = new File(workingCopy, file.getFileLocation()); // NOSONAR
			if(!fileToRestore.createNewFile()) {
				final String originalFileName = fileToRestore.getName();
				fileToRestore = new File(fileToRestore.getParentFile(), stashRef.getName() + "-" + originalFileName); // NOSONAR
				if(fileToRestore.createNewFile()) {
					overwrittenFiles.add(new FileStatus(file.getChangeType(), 
							file.getFileLocation().replaceAll(originalFileName, fileToRestore.getName())));
				}
			}
			
			if(fileToRestore.exists()) {
				try(FileOutputStream outputStream = new FileOutputStream(fileToRestore)) {
					final InputStream fileInputStream = getInputStream(fileObject);
					outputStream.write(fileInputStream.readAllBytes());
					fileInputStream.close();
				}
			}
		} catch (NoRepositorySelected e) {
			LOGGER.error(e.getMessage(), e);
		}
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
      LOGGER.error(e.getMessage(), e);
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
      LOGGER.error(e.getMessage(), e);
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
	  fireOperationAboutToStart(new GitEventInfo(GitOperation.CREATE_TAG));
	  try {
	    RevWalk walk = new RevWalk(getRepository());
	    RevCommit id = walk.parseCommit(getRepository().resolve(commitId));
      git.tag()
        .setName(name)
        .setMessage(message)
        .setObjectId(id)
        .setForceUpdate(true)
        .setCredentialsProvider(new GPGCapableCredentialsProvider(OptionsManager.getInstance().getGPGPassphrase()))
        .call();
      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.CREATE_TAG));
    } catch (GitAPIException | NoRepositorySelected | RevisionSyntaxException | IOException e) {
      LOGGER.error(e.getMessage(), e);
      fireOperationFailed(new GitEventInfo(GitOperation.CREATE_TAG), e);
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
  public void pushTag(final String name) throws GitAPIException {
      final CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
      final StringBuilder refTag = new StringBuilder(Constants.R_TAGS).append(name).append(":")
          .append(Constants.R_TAGS).append(name);
      getGit()
        .push()
        .setCredentialsProvider(credentialsProvider)
        .setRefSpecs(new RefSpec(refTag.toString()))
        .call();
  }
  
  /**
   * Delete all given tags.
   * 
   * @param tags           The names of the tags to be deleted.
   * @param includeRemotes <code>true</code> if the remote tags should be also deleted.
   * 
   * @throws GitAPIException
   */
  public void deleteTags(final boolean includeRemotes, final String... tags) throws GitAPIException  {
    fireOperationAboutToStart(new GitEventInfo(GitOperation.DELETE_TAG));
    try {
      getGit()
      .tagDelete()
      .setTags(tags)
      .call();
      final CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(getHostName());
      
      if(includeRemotes) {
        for(String name : tags) {
          final StringBuilder refTag = new StringBuilder(":").append(Constants.R_TAGS).append(name);
          getGit()
          .push()
          .setCredentialsProvider(credentialsProvider)
          .setRefSpecs(new RefSpec(refTag.toString()))
          .call();
        }
      }

      fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.DELETE_TAG));
    } catch (GitAPIException e) {
      fireOperationFailed(new GitEventInfo(GitOperation.DELETE_TAG), e);
      throw e;
    }
  }
	
	
	/**
	 * Used to do a checkout commit.
	 * 
	 * @param startPoint                    The start commit.
	 * @param branchName                    The new branch name(if shouldCreateANewBranch is <code>true</code>).
	 *
	 * @throws GitAPIException Errors while invoking git commands.
	 */
	public void checkoutCommit(RevCommit startPoint, 
			String branchName) throws GitAPIException {
		fireOperationAboutToStart(new GitEventInfo(GitOperation.CHECKOUT_COMMIT));
		CheckoutCommand checkoutCommand = this.git.checkout();
		checkoutCommand.setStartPoint(startPoint);
		doCheckoutCommit(checkoutCommand, branchName);
	}
	
	
	/**
	 * Used to do a checkout commit.
	 * 
	 * @param startPoint                    The start commit. <code>null</code> the index is used.
	 * @param branchName                    The new branch name. <code>null</code> to do a headless checkout.
	 *
	 * @throws GitAPIException Errors while invoking git commands.
	 */
	public void checkoutCommit(@Nullable String startPoint, 
			@Nullable String branchName) throws GitAPIException {
		fireOperationAboutToStart(new GitEventInfo(GitOperation.CHECKOUT_COMMIT));
		CheckoutCommand checkoutCommand = this.git.checkout();
		checkoutCommand.setStartPoint(startPoint);
		doCheckoutCommit(checkoutCommand, branchName);
	}
	
	
	/**
	 * Used to do a checkout commit. If the branchName is null, no branch will de created.
	 * 
	 * @param checkoutCommand         Checkout command to do the checkout.
	 * @param branchName              The new branch name.
	 * 
	 * @throws GitAPIException Errors while invoking git commands.
	 */
	private void doCheckoutCommit(CheckoutCommand checkoutCommand, String branchName) throws GitAPIException {
	  
	  if(checkoutCommand != null) {
	    
	    fireOperationAboutToStart(new GitEventInfo(GitOperation.CHECKOUT_COMMIT));
	    checkoutCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
	    if(branchName != null) {
	      checkoutCommand.setCreateBranch(true).setName(branchName);
	    } else {
	      checkoutCommand.setCreateBranch(false).setName(Constants.HEAD);
	    }
	    try {
	      checkoutCommand.call();
	    } catch(GitAPIException e) {
	      fireOperationFailed(new GitEventInfo(GitOperation.CHECKOUT_COMMIT), e);
	      throw e;
	    }

	    fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.CHECKOUT_COMMIT));
	  }
	}

	/**
	 * @param branchName The branch name.
	 * 
	 * @return The remote value from config.
	 * 
	 * @throws NoRepositorySelected
	 */
	public String getBranchRemoteFromConfig(String branchName) {
		String remoteConfig = null;
		try {
			Repository repository = this.getRepository();
			StoredConfig config = repository.getConfig();
			BranchConfig branchConfig = new BranchConfig(config, branchName);
			remoteConfig = branchConfig.getRemote();
		} catch (NoRepositorySelected e) {
			LOGGER.debug(e.getMessage(), e);
		}
	    
		return remoteConfig;
	}
	
	/**
	 * @return The remote value for current branch.
	 */
	public String getRemoteFromCurrentBranch()  {
		String remoteConfig = getBranchRemoteFromConfig(getBranchInfo().getBranchName());
		return remoteConfig != null ? remoteConfig : Constants.DEFAULT_REMOTE_NAME;
	}

	
	/**
	 * @param branchName      The branch name.
	 * @param newRemoteValue  The value for the new remote value in branch config.
	 * 
	 * @throws NoRepositorySelected
	 */
	public void setBranchRemoteFromConfig(String branchName, String newRemoteValue) throws NoRepositorySelected {
		Repository repository = this.getRepository();
		StoredConfig config = repository.getConfig();
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE, newRemoteValue);
	}
	
	
	/**
	 * Get all remotes from project config file.
	 * 
	 * @return A map when <code>key</code> = remote name, <code>value</code> = URL for this remote.
	 * 
	 * @throws NoRepositorySelected
	 */
	public Map<String, String> getRemotesFromConfig() throws NoRepositorySelected {
		Map<String, String> remotesMap = new TreeMap<>();
	
		StoredConfig config = getRepository().getConfig();
	    Set<String> remotes = config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE);
	    
	    for(String remote : remotes) {
	    	remotesMap.put(remote, config.getString(ConfigConstants.CONFIG_KEY_REMOTE, remote, "url"));
	    }
		
		return remotesMap;
	}
	
	
	/**
	 * Update remote values.
	 * <br><br>
	 * The config file will be not updated.
	 * 
	 * @param oldRemote   Old remote name.
	 * @param newRemote   New remote name.
	 * @param newURL      New URL.
	 * 
	 * @throws NoRepositorySelected
	 */
	public void updateRemote(String oldRemote, String newRemote, String newURL)  throws NoRepositorySelected {
		if(oldRemote != null && !oldRemote.equals(newRemote)) {
			removeRemote(oldRemote);
		}
		StoredConfig config = getRepository().getConfig();
		List<String> info = new ArrayList<>();
		info.add(newURL);
		config.setStringList(ConfigConstants.CONFIG_KEY_REMOTE, newRemote, ConfigConstants.CONFIG_KEY_URL, info);
		info.clear();
		info.add("+refs/heads/*:refs/remotes/" + newRemote + "/*");
		config.setStringList(ConfigConstants.CONFIG_KEY_REMOTE, newRemote, ConfigConstants.CONFIG_FETCH_SECTION, info);
	}
	
	
	/**
	 * Remove the given  remote. 
	 * <br><br>
	 * The config file will be not updated.
	 * 
	 * @param remote  Remote to remove
	 * 
	 * @throws NoRepositorySelected
	 */
	public void removeRemote(String remote) throws NoRepositorySelected {
		StoredConfig config = getRepository().getConfig();
		config.unsetSection(ConfigConstants.CONFIG_KEY_REMOTE, remote);
	
		// remove all branches with this remote
		Set<String> branchesSection = config.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION);
		branchesSection.forEach(branchName -> {
			if(config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE).equals(remote)) {
				config.unsetSection(ConfigConstants.CONFIG_BRANCH_SECTION, branchName);
			}
		});
	}
	
	
	/**
	 * Updates the config file with new configurations.
	 * 
	 * @throws NoRepositorySelected
	 */
	public void updateConfigFile() throws NoRepositorySelected {
		fireOperationAboutToStart(new GitEventInfo(GitOperation.UPDATE_CONFIG_FILE));
		
		try {
			GitAccess.getInstance().getRepository().getConfig().save();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			fireOperationFailed(new GitEventInfo(GitOperation.UPDATE_CONFIG_FILE), e);
		}
		
		fireOperationSuccessfullyEnded(new GitEventInfo(GitOperation.UPDATE_CONFIG_FILE));
	}
	
	
	/**
	 * @return Return path for config file of current repository.
	 * 
	 * @throws NoRepositorySelected
	 */
	public String getConfigFilePath() throws NoRepositorySelected {
		final String pathDelimiter = "/";
		return getRepository().getDirectory().getPath() + pathDelimiter + Constants.CONFIG;
	}
	
	
	/**
	 * Compute a list with all remote branches. 
	 * <br>
	 * This means that a list will be built with each remote branch that currently exists in each remote 
	 * in the configuration file.
	 * 
	 * @return A list with all remote branches.
	 * 
	 * @throws NoRepositorySelected
	 */
	@NonNull
	public List<String> getAllRemotesBranches() throws NoRepositorySelected {
	  final List<String> branchList = new ArrayList<>();
	  final StoredConfig config = getRepository().getConfig();
	  final Set<String> remotes = getRemotesFromConfig().keySet();
	  
	  for(String remote : remotes) {
	    try {
	      final URIish sourceURL = new URIish(config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
	          remote, ConfigConstants.CONFIG_KEY_URL));
	      Collection<Ref> branchesConfig = doListRemoteBranchesInternal(
	          sourceURL, null);
	      for(Ref branch: branchesConfig) {
	        final String branchName = Repository.shortenRefName(branch.getName());
	        branchList.add(Constants.R_REMOTES + remote + "/" + branchName); 
	      }
	    } catch (URISyntaxException e) {
	      LOGGER.error(e.getMessage(), e);
	    }
	  }

	  return branchList;
	}

	
	 /**
   * @return <code>true</code> if are any files modified (staged or unstaged).
   */
  public boolean hasFilesChanged() {
    List<FileStatus> unstagedFiles = getUnstagedFiles();
    boolean existsLocalFiles = unstagedFiles != null && !unstagedFiles.isEmpty();

    if(!existsLocalFiles) {
      List<FileStatus> stagedFiles = getStagedFiles();
      existsLocalFiles = stagedFiles != null && !stagedFiles.isEmpty();
    }
    return existsLocalFiles;
  }
  
  /**
   * @return <code>true</code> if repository has stashes.
   */
  public boolean hasStashes() {
    Collection<RevCommit> stashes = GitAccess.getInstance().listStashes();
    return stashes != null && !stashes.isEmpty();
  }
  
  /**
   * @return <code>true</code> if a repository is opened.
   */
  public boolean isRepositoryOpened() {
    boolean toReturn = false;
    try {
      toReturn = getRepository() != null;
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
    } 
    
    return toReturn;
  }
  
  /**
   * @return <code>true</code> if the repository has files in conflict or <code>false</code> otherwise.
   */
  public boolean repositoryHasConflicts() {
    return getStatus().repositoryHasConflicts();
  }
  
	
}
