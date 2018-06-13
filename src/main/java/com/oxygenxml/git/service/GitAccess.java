package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleStatusCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.errors.NoMergeBaseException.MergeBaseFailureReason;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import com.oxygenxml.git.ProjectViewManager;
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
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommand;

/**
 * Implements some basic git functionality like commit, push, pull, retrieve
 * File status(staged, unstaged)
 * 
 * @author Beniamin Savu
 */
public class GitAccess {
  /**
   * "remote"
   */
  private static final String REMOTE = "remote";
  /**
   * "End fetch" debug message.
   */
	private static final String END_FETCH_DEBUG_MESSAGE = "End fetch";
  /**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(GitAccess.class);
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
	private Translator translator = Translator.getInstance();
	/**
	 * Receive notifications when things change.
	 */
	private Set<GitEventListener> listeners = new LinkedHashSet<>();

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
	 * @param branchName The name of the branch to clone and checkout.
	 * 
	 * @throws GitAPIException
	 * @throws URISyntaxException
	 */
	public void clone(URIish url, File directory, final ProgressDialog progressDialog, String branchName)
			throws GitAPIException {
		if (git != null) {
		  AuthenticationInterceptor.unbind(getHostName());
			git.close();
		}
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
    CloneCommand cloneCommand = Git.cloneRepository()
		    .setURI(url.toString())
		    .setDirectory(directory)
		    .setCloneSubmodules(true)
		    .setCredentialsProvider(new SSHCapableUserCredentialsProvider(username, password, pass, url.getHost()))
		    .setProgressMonitor(p);
		if (branchName != null) {
      git = cloneCommand
		      .setBranchesToClone(Arrays.asList(branchName))
		      .setBranch(branchName)
		      .call();
		} else {
		  git = cloneCommand.call();
		}
		
		fireRepositoryChanged();
	}

	/**
	 * Sets the Git repository. The file path must exist.
	 * 
	 * @param path A string that specifies the Git repository folder.
	 */
	public void setRepository(String path) throws IOException {
	  File currentRepo = new File(path + "/.git");
	  boolean sameOldRepo = git != null && currentRepo.equals(git.getRepository().getDirectory());
	  
	  // Change the repository only if it is a different one
	  if (!sameOldRepo ) {
	    if (git != null) {
	      // Stop intercepting authentication requests.
	      AuthenticationInterceptor.unbind(getHostName());
	      git.close();
	    }

	    git = Git.open(currentRepo);
	    // Start intercepting authentication requests.
	    AuthenticationInterceptor.bind(getHostName());

	    if (logger.isDebugEnabled()) {
	      logSshKeyLoadingData(path);
	    }

	    fireRepositoryChanged();
	  }
	}

	/**
	 * Log SSH key loading location data.
	 * 
	 * @param path The path to the Git repository folder.
	 */
  private void logSshKeyLoadingData(String path) {
    // Debug data for the SSH key load location.
    logger.debug("Java env user home: " + System.getProperty("user.home"));
    logger.debug("Load repository " + path);
    try {
      FS fs = getRepository().getFS();
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
	 * Notify that the loaded repository changed.
	 */
	private void fireRepositoryChanged() {
	  for (GitEventListener gitEventListener : listeners) {
      gitEventListener.repositoryChanged();
    }
  }
	
  /**
   * Notify the some files changed their state.
   */
  private void fireFileStateChanged(ChangeEvent changeEvent) {
    for (GitEventListener gitEventListener : listeners) {
      gitEventListener.stateChanged(changeEvent);
    }
  }
	
	public void addGitListener(GitEventListener l) {
	  listeners.add(l);
  }

  /**
	 * 
	 * @return the git Repository
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
	 * @param path
	 *          - A string that specifies the git Repository folder
	 */
	public void createNewRepository(String path) {
    if (git != null) {
      // Stop intercepting authentication requests.
      AuthenticationInterceptor.unbind(getHostName());
      git.close();
    }

		try {
			git = Git.init().setBare(false).setDirectory(new File(path)).call();
		} catch (IllegalStateException | GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		
		fireRepositoryChanged();
	}
	
	/**
	 * @return A status of the Working Copy, with the unstaged and staged files.
	 */
	public GitStatus getStatus() {
	  GitStatus gitStatus = null;
	  if (git != null) {
	    StatusCommand statusCmd = git.status();

	    try {
	      Status status = statusCmd.call();
	      List<FileStatus> unstagedFiles = getUnstagedFiles(status);
	      List<FileStatus> stagedFiles = getStagedFiles(status);

	      gitStatus = new GitStatus(unstagedFiles, stagedFiles);
	    } catch (GitAPIException e) {
	      if (logger.isDebugEnabled()) {
	        logger.debug(e, e);
	      }
	    }
	  }
    
    return gitStatus != null ? gitStatus : new GitStatus(Collections.emptyList(), Collections.emptyList());
  }
	
	/**
	 * 
	 * 
	 * @param paths A subset of interest.
	 *  
   * @return A status of the Working Copy, with the unstaged and staged files.
   */
  public GitStatus getStatus(Collection<String> paths) {
    GitStatus gitStatus = null;
    if (git != null) {
      StatusCommand statusCmd = git.status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        String path = iterator.next();

        statusCmd.addPath(path);
      }

      try {
        Status status = statusCmd.call();
        List<FileStatus> unstagedFiles = getUnstagedFiles(status);
        List<FileStatus> stagedFiles = getStagedFiles(status);

        gitStatus = new GitStatus(unstagedFiles, stagedFiles);
      } catch (GitAPIException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
    }
    
    return gitStatus != null ? gitStatus : new GitStatus(Collections.emptyList(), Collections.emptyList());
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
   * @return - A list with the files from the given set htat are un-staged as well as 
   * their states.
   */
  public List<FileStatus> getUnstagedFiles(Collection<String> paths) {
    if (git != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Prepare fot Git status, in paths " + paths);
      }

      StatusCommand statusCmd = git.status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        String path = iterator.next();

        statusCmd.addPath(path);
      }

      try {
        Status status = statusCmd.call();
        return getUnstagedFiles(status);
      } catch (GitAPIException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
    }
    
    return Collections.emptyList();
  }

	/**
	 * Makes a diff between the files from the last commit and the files from the
	 * working directory. If there are diffs, they will be saved and returned.
	 * 
	 * @param paths A subset of interest.
	 * 
	 * @return - A list with the files from the given set htat are un-staged as well as 
	 * their states.
	 */
	private List<FileStatus> getUnstagedFiles(Status status) {
		List<FileStatus> unstagedFiles = new ArrayList<>();
		if (git != null) {
			try {
				Set<String> submodules = getSubmodules();
				
        if (logger.isDebugEnabled()) {
          logger.debug("submodules " + submodules);
        }
				
				for (String string : submodules) {
					SubmoduleStatus submoduleStatus = git.submoduleStatus().call().get(string);
					if (submoduleStatus != null 
					    && submoduleStatus.getHeadId() != null
					    && !submoduleStatus.getHeadId().equals(submoduleStatus.getIndexId())) {
						unstagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, string));
					}
				}
				
				if (logger.isDebugEnabled()) {
				  logger.debug("untracked " + status.getUntracked());
				}
				
				for (String string : status.getUntracked()) {
				  // A newly created file, not yet in the INDEX.
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.UNTRACKED, string));
					}
				}
				
        if (logger.isDebugEnabled()) {
          logger.debug("modified " + status.getModified());
        }
        
				for (String string : status.getModified()) {
				  // A file that was modified compared to the one from INDEX.
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.MODIFIED, string));
					}
				}
				
        if (logger.isDebugEnabled()) {
          logger.debug("missing " + status.getMissing());
        }
				
				for (String string : status.getMissing()) {
				  // A missing file that is present in the INDEX.
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.MISSING, string));
					}
				}
				
        for (String fileName : status.getConflicting()) {
          unstagedFiles.add(new FileStatus(GitChangeType.CONFLICT, fileName));
        }
			} catch (NoWorkTreeException | GitAPIException e1) {
				if (logger.isDebugEnabled()) {
					logger.debug(e1, e1);
				}
			}
		}
		return unstagedFiles;
	}

	/**
	 * Returns for the given submodule the SHA-1 commit id for the Index if the
	 * given index boolean is <code>true</code> or the SHA-1 commit id for the
	 * HEAD if the given index boolean is <code>false</code>
	 * 
	 * @param submodulePath
	 *          - the path to get the submodule
	 * @param index
	 *          - boolean to determine what commit id to return
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
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
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
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		return new HashSet<>();
	}

	/**
	 * Sets the given submodule as the current repository
	 * 
	 * @param submodule
	 *          - the name of the submodule
	 * @throws IOException
	 * @throws GitAPIException
	 */
	public void setSubmodule(String submodule) throws IOException {
		Repository parentRepository = git.getRepository();
		Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		git = Git.wrap(submoduleRepository);
		
		fireRepositoryChanged();
	}

	/**
	 * Commits a single file locally
	 * 
	 * @param file
	 *          - File to be commited
	 * @param message
	 *          - Message for the commit
	 */
	public void commit(String message) {
		try {
		  List<FileStatus> files = getStagedFiles();
		  
			git.commit().setMessage(message).call();
			
      fireFileStateChanged(new ChangeEvent(GitCommand.COMMIT, getPaths(files)));
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	private Collection<String> getPaths(List<FileStatus> files) {
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
	public void close() {
		if (git != null) {
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
		try {
			branches = git.branchList().call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
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
    try {
      branches = git.branchList().setListMode(ListMode.REMOTE).call();
    } catch (GitAPIException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
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
            + " and password: " + password);
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
        if (logger.isDebugEnabled()) {
          logger.debug(ex, ex);
        }
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
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
    } while (!shouldStopTryingLogin);
    
    return remoteRefs;
  }

	/**
	 * Creates a new branch in the repository
	 * 
	 * @param branchName
	 *          - Name for the new branch
	 */
	public void createBranch(String branchName) {
		try {
			git.branchCreate().setName(branchName).call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}

	}

	/**
	 * Delete a branch from the repository
	 * 
	 * @param branchName
	 *          - Name for the branch to delete
	 */
	public void deleteBranch(String branchName) {
		try {
			git.branchDelete().setBranchNames(branchName).call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Pushes all the commits from the local repository to the remote repository
	 * 
	 * @param username
	 *          - Git username
	 * @param password
	 *          - Git password
	 *          
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 */
	public PushResponse push(final String username, final String password)
	    throws GitAPIException {

	  AuthenticationInterceptor.install();
	  PushResponse response = new PushResponse();

	  RepositoryState repositoryState = git.getRepository().getRepositoryState();

	  if (repositoryState == RepositoryState.MERGING) {
	    response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
	    response.setMessage(translator.getTranslation(Tags.PUSH_WITH_CONFLICTS));
	    return response;
	  }
	  if (getPullsBehind() > 0) {
	    response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
	    response.setMessage(translator.getTranslation(Tags.BRANCH_BEHIND));
	    return response;
	  }
	  String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
	  Iterable<PushResult> call = git.push().setCredentialsProvider(
	      new SSHCapableUserCredentialsProvider(username, password, sshPassphrase, getHostName())).call();
	  Iterator<PushResult> results = call.iterator();
	  logger.debug("Push Ended");
	  while (results.hasNext()) {
	    PushResult result = results.next();
	    for (RemoteRefUpdate info : result.getRemoteUpdates()) {
	      response.setStatus(info.getStatus());
	      return response;
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
	 * 
	 * @return The result, if successful.
	 *  
	 * @throws GitAPIException If it fails.
	 */
	public PullResponse pull(String username, String password) throws GitAPIException {
	  AuthenticationInterceptor.install();

		PullResponse response = new PullResponse(PullStatus.OK, new HashSet<String>());
		if (!getConflictingFiles().isEmpty()) {
			response.setStatus(PullStatus.REPOSITORY_HAS_CONFLICTS);
		} else {
			git.reset().call();
			String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
			Repository repository = git.getRepository();
			
			ObjectId oldHead = null;
			try {
        oldHead = repository.resolve("HEAD^{tree}");
      } catch (RevisionSyntaxException | IOException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
			// Call "Pull"
			PullResult call = git.pull().setCredentialsProvider(
			    new SSHCapableUserCredentialsProvider(username, password, sshPassphrase, getHostName())).call();
			ObjectId head = null;
			try {
        head = repository.resolve("HEAD^{tree}");
      } catch (RevisionSyntaxException | IOException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
			
			if (oldHead != null && head != null) {
			  refreshProject(repository, oldHead, head);
			}
			
			MergeResult mergeResult = call.getMergeResult();
			if (mergeResult != null && mergeResult.getConflicts() != null) {
			  Set<String> conflictingFiles = mergeResult.getConflicts().keySet();
			  if (conflictingFiles != null) {
			    response.setConflictingFiles(conflictingFiles);
			    response.setStatus(PullStatus.CONFLICTS);
			  }
			}
			if (call.getMergeResult().getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
				response.setStatus(PullStatus.UP_TO_DATE);
			}
		}

		return response;

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
      List<DiffEntry> diffs= git.diff()
          .setNewTree(newTreeIter)
          .setOldTree(oldTreeIter)
          .call();
      
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
      ProjectViewManager.refreshFolders(new File[] {FileHelper.getCommonDir(pulledFilesParentDirs)});
    } catch (IOException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    }
  }

	/**
	 * Finds the common base for the given commit "a" and the given commit "b"
	 * 
	 * @param walk
	 *          - used to browse through commits
	 * @param a
	 *          - commit "a"
	 * @param b
	 *          - commit "b"
	 * @return the bese commit
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private RevCommit getCommonAncestor(RevWalk walk, RevCommit a, RevCommit b)
			throws IOException {
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
	 * @param file
	 *          - the name of the file to be added
	 */
	public void add(FileStatus file) {
		try {
			if (file.getChangeType().equals(GitChangeType.REMOVED)) {
				git.rm().addFilepattern(file.getFileLocation()).call();
			} else {
				git.add().addFilepattern(file.getFileLocation()).call();
			}
			
			fireFileStateChanged(new ChangeEvent(GitCommand.STAGE, getPaths(Arrays.asList(file))));
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Adds multiple files to the staging area. Preparing the for commit
	 * 
	 * @param fileNames
	 *          - the names of the files to be added
	 */
	public void addAll(List<FileStatus> files) {

		try {
		  
		  RmCommand removeCmd = null;
		  AddCommand addCmd = null;
		  
			for (FileStatus file : files) {
				if (file.getChangeType() == GitChangeType.MISSING) {
				  if (removeCmd == null) {
				    removeCmd = git.rm();
				  }
          removeCmd.addFilepattern(file.getFileLocation());
				} else {
				  if (addCmd == null) {
				    addCmd = git.add();
				  }
          addCmd.addFilepattern(file.getFileLocation());
				}
			}
			
			if (removeCmd != null) {
			  removeCmd.call();
			}
			
			if (addCmd != null) {
			  addCmd.call();
			}
			
			fireFileStateChanged(new ChangeEvent(GitCommand.STAGE, getPaths(files)));
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
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
   * Checks which files from the given subset are in the Index and returns their state.
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
      }catch (GitAPIException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e, e);
        }
      }
    }
    
    return Collections.emptyList();
  }

	/**
	 * Checks which files from the given subset are in the Index and returns their state.
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
	 * @return the conflicting files list
	 */
	public List<FileStatus> getConflictingFiles() {
		if (git != null) {
			try {
				Status status = git.status().call();
				List<FileStatus> stagedFiles = new ArrayList<>();
				for (String fileName : status.getConflicting()) {
					stagedFiles.add(new FileStatus(GitChangeType.CONFLICT, fileName));
				}

				return stagedFiles;
			} catch (GitAPIException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			}
		}

		return new ArrayList<>();
	}

	/**
	 * Reset a single file from the staging area.
	 * 
	 * @param fileName
	 *          - the file to be removed from the staging area
	 */
	public void reset(FileStatus file) {
		try {
			ResetCommand reset = git.reset();
			reset.addPath(file.getFileLocation());
			reset.call();
			
			fireFileStateChanged(new ChangeEvent(GitCommand.UNSTAGE, getPaths(Arrays.asList(file))));
			
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Reset all the specified files from the staging area.
	 * 
	 * @param fileNames
	 *          - the list of file to be removed
	 */
	public void resetAll(List<FileStatus> files) {
		try {
			if (!files.isEmpty()) {
				ResetCommand reset = git.reset();
				for (FileStatus file : files) {
					reset.addPath(file.getFileLocation());

				}
				reset.call();
			}
			
	     fireFileStateChanged(new ChangeEvent(GitCommand.UNSTAGE, getPaths(files)));
	     
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Gets the host name from the repositoryURL
	 * 
	 * @return The host name. An empty string if not connected. Never <code>null</code>.
	 */
	public String getHostName() {
		if (git != null) {
			Config storedConfig = git.getRepository().getConfig();
			// TODO How we should react when there are multiple remote repositories???
			String url = storedConfig.getString(REMOTE, "origin", "url");
			if (url == null) {
			  Set<String> remoteNames = git.getRepository().getRemoteNames();
			  Iterator<String> iterator = remoteNames.iterator();
			  if (iterator.hasNext()) {
			    url = storedConfig.getString(REMOTE, iterator.next(), "url");
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
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		return null;
	}

	/**
	 * Finds the last remote commit from the remote repository
	 * 
	 * @return the last remote commit
	 */
	public ObjectId getRemoteCommit(BranchInfo branchInfo) {
		Repository repo = git.getRepository();
		ObjectId remoteCommit = null;
		try {
			remoteCommit = repo.resolve("origin/" + branchInfo.getBranchName() + "^{commit}");
			return remoteCommit;
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		return remoteCommit;
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
			remoteCommit = repository.resolve("origin/" + branchInfo.getBranchName() + "^{commit}");
			localCommit = repository.resolve("HEAD^{commit}");
			if (remoteCommit != null && localCommit != null) {
				RevCommit base = getCommonAncestor(walk, walk.parseCommit(localCommit), walk.parseCommit(remoteCommit));
				if (base != null) {
					baseCommit = base.toObjectId();
				}
			}
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		walk.close();
		return baseCommit;
	}

	/**
	 * Gets the loader for a file from a specified commit and its path
	 * 
	 * @param commit
	 *          - the commit from which to get the loader
	 * @param path
	 *          - the path to the file
	 * @return the loader
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public ObjectLoader getLoaderFrom(ObjectId commit, String path)
			throws IOException {
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
	 * @param commitID
	 *          - the commit in which the file exists
	 * @param path
	 *          - the path to the file
	 *          
	 * @return the InputStream for the file
	 * 
	 * @throws IOException
	 */
	public InputStream getInputStream(ObjectId commitID) throws IOException {
		InputStream toReturn = null;
		if (commitID != null) {
			ObjectLoader loader = git.getRepository().open(commitID);
			if (loader == null) {
			  throw new IOException("Cannot obtain an object loader for the commit ID: " + commitID);
			} else {
				toReturn = loader.openStream();
			}
		} else {
			throw new IOException("The commit ID can't be null");
		}
		return toReturn;
	}

	/**
	 * Performs a git reset. The equivalent of the "git reset" command
	 */
	public void reset() {
		try {
			git.reset().call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Restores the last commit file content to the local file at the given path.
	 * Both files must have the same path, otherwise it will not work.
	 * 
	 * @param file
	 *          - the path to the file you want to restore
	 */
	public void restoreLastCommitFile(List<String> paths) {
		try {
		  CheckoutCommand checkoutCmd = git.checkout();
		  checkoutCmd.addPaths(paths);
			checkoutCmd.call();
			
			fireFileStateChanged(new ChangeEvent(GitCommand.DISCARD, paths));
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Calculates how many commits the local repository is ahead from the current
	 * local repository base commit
	 * 
	 * @return the number of commits ahead
	 */
	public int getPushesAhead() {
		int numberOfCommits = 0;
		
	  try {
	    String branchName = getBranchInfo().getBranchName();
	    if (branchName != null && branchName.length() > 0) {
	      BranchTrackingStatus bts = BranchTrackingStatus.of(getRepository(), branchName);
	      if (bts != null) {
	        numberOfCommits = bts.getAheadCount();
	      }
	    }
	  } catch (IOException | NoRepositorySelected e) {
	    if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
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
	    if (logger.isDebugEnabled()) {
	      logger.debug(e, e);
	    }
	  }

	  return numberOfCommits;}

	/**
	 * Brings all the commits to the local repository but does not merge them.
	 * 
	 * @throws PrivateRepositoryException 
	 */
	public void fetch() throws SSHPassphraseRequiredException, PrivateRepositoryException, RepositoryUnavailableException {
		if (logger.isDebugEnabled()) {
			logger.debug("Begin fetch");
		}
		AuthenticationInterceptor.install();
		
		UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(getHostName());
		String username = gitCredentials.getUsername();
		String password = gitCredentials.getPassword();
		String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
		SSHCapableUserCredentialsProvider credentialsProvider = 
		    new SSHCapableUserCredentialsProvider(username, password, sshPassphrase, getHostName());
		try {
			StoredConfig config = git.getRepository().getConfig();
			Set<String> sections = config.getSections();
			if (sections.contains(REMOTE)) {
        git.fetch().setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*")).setCheckFetchedObjects(true)
						.setCredentialsProvider(credentialsProvider).call();
			}
		} catch (TransportException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}

			if (e.getMessage().contains("Authentication is required but no CredentialsProvider has been registered")
					|| e.getMessage().contains("not authorized")) {
				throw new PrivateRepositoryException(e);
			} else if (e.getMessage().contains("Auth fail")
			    // A SSH pass phase was requested.
			    && credentialsProvider.isPassphaseRequested()) {
			  throw new SSHPassphraseRequiredException(e);
			} else {
			  throw new RepositoryUnavailableException(e);
			}
		} catch (GitAPIException | RevisionSyntaxException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    } 
		if (logger.isDebugEnabled()) {
			logger.debug(END_FETCH_DEBUG_MESSAGE);
		}
	}

	public void updateWithRemoteFile(String filePath) {
		try {
			RevWalk walk = new RevWalk(git.getRepository());
			walk.reset();
			RevCommit commit = walk.parseCommit(git.getRepository().resolve("MERGE_HEAD"));
			git.checkout().setStartPoint(commit).addPath(filePath).call();
			walk.close();
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Restore to the initial state of the repository. Only applicable if the
	 * repository has conflicts
	 */
	public void restartMerge() {
		try {
			AnyObjectId commitToMerge = git.getRepository().resolve("MERGE_HEAD");
			git.clean().call();
			git.reset().setMode(ResetType.HARD).call();
			git.merge().include(commitToMerge).setStrategy(MergeStrategy.RECURSIVE).call();
			
			fireFileStateChanged(new ChangeEvent(GitCommand.MERGE_RESTART, Collections.<String> emptyList()));
		} catch (GitAPIException | IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(END_FETCH_DEBUG_MESSAGE);
			}
		}
	}

	/**
	 * Checks whether or not he branch is detached. If the branch is detached it
	 * stores the state and the name of the commit on which it is. If the branch
	 * is not detached then it stores the branch name. After this it returns this
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
						branchInfo.setShortBranchName(revCommit.getId().abbreviate(7).name());
						break;
					}
				}
				return branchInfo;
			} catch (NoHeadException e) {
			  logger.debug(e, e);
			  return new BranchInfo(branchName, false);
			} catch (IOException | GitAPIException e) {
			  logger.debug(e, e);
			}
		}
		return new BranchInfo("", false);
	}

	/**
	 * Sets the given branch as the current branch
	 * 
	 * @param selectedBranch
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws CheckoutConflictException
	 * @throws GitAPIException
	 */
	public void setBranch(String selectedBranch) throws GitAPIException {
		git.checkout().setName(selectedBranch).call();

	}

	/**
	 * Return the submodule head commit to the previously one
	 * 
	 * @throws GitAPIException when an error occurs while trying to discard the submodule.
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
	 * Returns the SHA-1 commit id for a file by specifying what commit to get for
	 * that file and it's path
	 * 
	 * @param commit
	 *          - specifies the commit to return(MINE, THEIRS, BASE, LOCAL)
	 * @param path
	 *          - the file path for the specified commit
	 * @return the SHA-1 commit id
	 */
	public ObjectId getCommit(Commit commit, String path) {
		List<DiffEntry> entries;
		boolean baseIsNull = false;
		int index = 0;
		try {
			entries = git.diff().setPathFilter(PathFilter.create(path)).call();
			if (entries.size() == 2) {
				baseIsNull = true;
			}
			if (commit == Commit.MINE) {
				if (baseIsNull) {
					index = 0;
				} else {
					index = 1;
				}
				return entries.get(index).getOldId().toObjectId();
			} else if (commit == Commit.THEIRS) {
				if (baseIsNull) {
					index = 1;
				} else {
					index = 2;
				}
				return entries.get(index).getOldId().toObjectId();
			} else if (commit == Commit.BASE) {
				return entries.get(index).getOldId().toObjectId();
			} else if (commit == Commit.LOCAL) {
				ObjectId lastLocalCommit = getLastLocalCommit();
				RevWalk revWalk = new RevWalk(git.getRepository());
				RevCommit revCommit = revWalk.parseCommit(lastLocalCommit);
				RevTree tree = revCommit.getTree();
				TreeWalk treeWalk = new TreeWalk(git.getRepository());
				treeWalk.addTree(tree);
				treeWalk.setRecursive(true);
				treeWalk.setFilter(PathFilter.create(path));
				ObjectId objectId = null;
				if (treeWalk.next()) {
					objectId = treeWalk.getObjectId(0);
				}
				treeWalk.close();
				revWalk.close();
				return objectId;
			}
		} catch (GitAPIException |IOException e) {
		  logger.debug(e, e);
		}
		return null;
	}
	
	/**
	 * Clean up.
	 */
	public void cleanUp() {
	  listeners.clear();
  }
	
	/**
	 * Submodule add command.
	 * 
	 * @return the command to call.
	 */
	public SubmoduleAddCommand submoduleAdd() {
	  return git.submoduleAdd();
	}
	
	/**
	 * Submodule status command.
	 * 
	 * @return the command to call.
	 */
	public SubmoduleStatusCommand submoduleStatus() {
	  return git.submoduleStatus();
	}
	
	/**
	 * !!! FOR TESTS !!!
	 * 
	 * Get the {@link Git} object through which
	 * to interact with the repository.
	 */
	public Git getGitForTests() {
	  return git;
	}

}
