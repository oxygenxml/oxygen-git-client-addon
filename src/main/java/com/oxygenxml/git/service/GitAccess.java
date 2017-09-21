package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.errors.NoMergeBaseException.MergeBaseFailureReason;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import com.oxygenxml.git.CustomAuthenticator;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;
import com.oxygenxml.git.view.dialog.ProgressDialog;

import de.schlichtherle.io.FileInputStream;

/**
 * Implements some basic git functionality like commit, push, pull, retrieve
 * File status(staged, unstaged)
 * 
 * TODO Beni Add monitors and progress.
 * 
 * @author Beniamin Savu
 *
 */
public class GitAccess {
	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(GitAccess.class);

	private Git git;

	private static GitAccess instance;

	private Translator translator = new TranslatorExtensionImpl();

	private boolean unavailable = false;

	private boolean privateRepository = false;

	private boolean sshChecked = false;

	private boolean isSSHPassphrase;

	private GitAccess() {

	}

	public boolean isSshChecked() {
		return sshChecked;
	}

	public void setSshChecked(boolean sshChecked) {
		this.sshChecked = sshChecked;
	}

	public static GitAccess getInstance() {
		if (instance == null) {
			instance = new GitAccess();
		}
		return instance;
	}

	public void clone(URL url, File directory, final ProgressDialog progressDialog)
			throws GitAPIException, URISyntaxException {
		if (git != null) {
			git.close();
		}
		URI uri = url.toURI();
		UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(url.getHost());
		String username = gitCredentials.getUsername();
		String password = gitCredentials.getPassword();

		ProgressMonitor p = new ProgressMonitor() {
			String taskTitle;
			float totalWork;
			float currentWork = 0;

			public void update(int completed) {
				currentWork += completed;
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

			public void start(int totalTasks) {
				currentWork = 0;
			}

			public boolean isCancelled() {
				if (progressDialog.isCanceled()) {
					progressDialog.setNote("Canceling...");
				}
				return progressDialog.isCanceled();
			}

			public void endTask() {
				currentWork = 0;
			}

			public void beginTask(String title, int totalWork) {
				currentWork = 0;
				this.taskTitle = title;
				this.totalWork = totalWork;
			}
		};
		git = Git.cloneRepository().setURI(uri.toString()).setDirectory(directory).setCloneSubmodules(true)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).setProgressMonitor(p)
				.call();
	}

	/**
	 * Sets the git repository. File path must exist
	 * 
	 * @param path
	 *          - A string that specifies the git Repository folder
	 */
	public void setRepository(String path) throws IOException, RepositoryNotFoundException {
		if (git != null) {
			git.close();
			sshChecked = false;
			CustomUserCredentials.passphraseChecked = false;
		}
		git = Git.open(new File(path + "/.git"));
		
		if (logger.isDebugEnabled()) {
		  logger.debug("Load repository " + path);
		  try {
		    FS fs = getRepository().getFS();
		    if (fs != null) {
		      File userHome = fs.userHome();
		      logger.debug("User home " + userHome);
		    } else {
		      logger.debug("Null FS");
		    }
		  } catch (NoRepositorySelected e) {
		    logger.debug(e, e);
		  }
		}
		
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
	 * Creates a blank new Repository.
	 * 
	 * @param path
	 *          - A string that specifies the git Repository folder
	 */
	public void createNewRepository(String path) {

		try {
			git = Git.init().setBare(false).setDirectory(new File(path)).call();
		} catch (IllegalStateException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}

	}

	/**
	 * Makes a diff between the files from the last commit and the files from the
	 * working directory. If there are diffs, they will be saved and returned.
	 * 
	 * @return - A list with all unstaged files
	 */
	public List<FileStatus> getUnstagedFiles() {
		List<FileStatus> unstagedFiles = new ArrayList<FileStatus>();
		if (git != null) {
			try {
				Status status = git.status().call();
				Set<String> submodules = getSubmodules();
				for (String string : submodules) {
					SubmoduleStatus submoduleStatus = git.submoduleStatus().call().get(string);
					if (!submoduleStatus.getHeadId().equals(submoduleStatus.getIndexId())) {
						unstagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, string));
					}
				}
				for (String string : status.getUntracked()) {
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.ADD, string));
					}
				}
				for (String string : status.getModified()) {
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.MODIFY, string));
					}
				}
				for (String string : status.getMissing()) {
					if (!submodules.contains(string)) {
						unstagedFiles.add(new FileStatus(GitChangeType.DELETE, string));
					}
				}
				unstagedFiles.addAll(getConflictingFiles());

			} catch (NoWorkTreeException e1) {
				if (logger.isDebugEnabled()) {
					logger.debug(e1, e1);
				}
			} catch (GitAPIException e1) {
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
			if (index) {
				return submoduleStatus.getIndexId();
			} else {
				return submoduleStatus.getHeadId();
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
		return new HashSet<String>();
	}

	/**
	 * Sets the given submodule as the current repository
	 * 
	 * @param submodule
	 *          - the name of the submodule
	 * @throws IOException
	 * @throws GitAPIException
	 */
	public void setSubmodule(String submodule) throws IOException, GitAPIException {
		// git.submoduleUpdate().addPath(submodule).call();
		Repository parentRepository = git.getRepository();
		Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		git = Git.wrap(submoduleRepository);
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
			git.commit().setMessage(message).call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
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
	 * Gets all branches
	 * 
	 * @return - All the branches from the repository
	 */
	public List<Ref> getBrachList() {
		List<Ref> branches = null;
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
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 * @throws IOException
	 */
	public PushResponse push(final String username, final String password)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {

		// List<TransportProtocol> transportProtocols =
		// Transport.getTransportProtocols();
		// for (TransportProtocol transportProtocol : transportProtocols) {
		// transportProtocol.
		// }

		CustomAuthenticator.install();
		PushResponse response = new PushResponse();

		try {

			RepositoryState repositoryState = git.getRepository().getRepositoryState();

			if (repositoryState == RepositoryState.MERGING) {
				response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
				response.setMessage(translator.getTraslation(Tags.PUSH_WITH_CONFLICTS));
				return response;
			}
			if (getPullsBehind() > 0) {
				response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
				response.setMessage(translator.getTraslation(Tags.BRANCH_BEHIND));
				return response;
			}
			String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
			Iterable<PushResult> call = git.push()
					.setCredentialsProvider(new CustomUserCredentials(username, password, sshPassphrase)).call();
			Iterator<PushResult> results = call.iterator();
			logger.debug("Push Ended");
			while (results.hasNext()) {
				PushResult result = results.next();
				for (RemoteRefUpdate info : result.getRemoteUpdates()) {
					response.setStatus(info.getStatus());
					return response;
				}
			}
		} finally {

		}
		response.setStatus(org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
		response.setMessage(translator.getTraslation(Tags.PUSH_FAILED_UNKNOWN));
		return response;
	}

	/**
	 * Pulls the files that are not on the local repository from the remote
	 * repository
	 * 
	 * @param username
	 *          - Git username
	 * @param password
	 *          - Git password
	 * @return
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws NoHeadException
	 * @throws RefNotAdvertisedException
	 * @throws RefNotFoundException
	 * @throws CanceledException
	 * @throws InvalidRemoteException
	 * @throws DetachedHeadException
	 * @throws InvalidConfigurationException
	 * @throws WrongRepositoryStateException
	 * @throws IOException
	 * @throws IncorrectObjectTypeException
	 * @throws AmbiguousObjectException
	 * @throws RevisionSyntaxException
	 */
	public PullResponse pull(String username, String password) throws WrongRepositoryStateException,
			InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
			RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException, GitAPIException,
			RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		CustomAuthenticator.install();

		PullResponse response = new PullResponse(PullStatus.OK, new HashSet<String>());
		if (getConflictingFiles().size() > 0) {
			response.setStatus(PullStatus.REPOSITORY_HAS_CONFLICTS);
			// } else if (getUnstagedFiles().size() > 0 || getStagedFile().size() > 0)
			// {
			// response.setStatus(PullStatus.UNCOMITED_FILES);
		} else {
			git.reset().call();
			String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
			PullResult call = git.pull().setCredentialsProvider(new CustomUserCredentials(username, password, sshPassphrase))
					.call();
			MergeResult mergeResult = call.getMergeResult();
			if (mergeResult != null) {
				if (mergeResult.getConflicts() != null) {
					Set<String> conflictingFiles = mergeResult.getConflicts().keySet();
					if (conflictingFiles != null) {
						response.setConflictingFiles(conflictingFiles);
						response.setStatus(PullStatus.CONFLICTS);
					}
				}
			}
			if (call.getMergeResult().getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
				response.setStatus(PullStatus.UP_TO_DATE);
			}
		}

		return response;

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
			throws IncorrectObjectTypeException, IOException {
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
			if (file.getChangeType().equals("DELETE")) {
				git.rm().addFilepattern(file.getFileLocation()).call();
			} else {
				git.add().addFilepattern(file.getFileLocation()).call();
			}
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
			for (FileStatus file : files) {
				if (file.getChangeType() == GitChangeType.DELETE) {
					git.rm().addFilepattern(file.getFileLocation()).call();
				} else {
					git.add().addFilepattern(file.getFileLocation()).call();
				}
			}
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Well show all the files that have been added(also called staged) and are
	 * ready to be commited.
	 * 
	 * @return - a set containing all the staged file names
	 */
	public List<FileStatus> getStagedFile() {
		if (git != null) {
			try {
				Status status = git.status().call();
				List<FileStatus> stagedFiles = new ArrayList<FileStatus>();

				Set<String> submodules = getSubmodules();

				for (String fileName : status.getChanged()) {
					if (submodules.contains(fileName)) {
						stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
					} else {
						stagedFiles.add(new FileStatus(GitChangeType.MODIFY, fileName));
					}
				}
				for (String fileName : status.getAdded()) {
					if (submodules.contains(fileName)) {
						stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
					} else {
						stagedFiles.add(new FileStatus(GitChangeType.ADD, fileName));
					}
				}
				for (String fileName : status.getRemoved()) {
					if (submodules.contains(fileName)) {
						stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
					} else {
						stagedFiles.add(new FileStatus(GitChangeType.DELETE, fileName));
					}
				}
				return stagedFiles;
			} catch (GitAPIException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			}
		}
		return new ArrayList<FileStatus>();
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
				List<FileStatus> stagedFiles = new ArrayList<FileStatus>();
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

		return new ArrayList<FileStatus>();
	}

	/**
	 * Removes a single file from the staging area.
	 * 
	 * @param fileName
	 *          - the file to be removed from the staging area
	 */
	public void remove(FileStatus file) {
		try {
			ResetCommand reset = git.reset();
			reset.addPath(file.getFileLocation());
			reset.call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Removes all the specified files from the staging area.
	 * 
	 * @param fileNames
	 *          - the list of file to be removed
	 */
	public void removeAll(List<FileStatus> files) {
		try {
			if (files.size() > 0) {
				ResetCommand reset = git.reset();
				for (FileStatus file : files) {
					reset.addPath(file.getFileLocation());

				}
				reset.call();
			}
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}

	/**
	 * Gets the host name from the repositoryURL
	 * 
	 * @return the host name
	 */
	public String getHostName() {
		if (git != null) {
			Config storedConfig = git.getRepository().getConfig();
			String url = storedConfig.getString("remote", "origin", "url");
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
			ObjectId localCommit = repo.resolve("HEAD^{commit}");
			return localCommit;
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
	public ObjectId getRemoteCommit() {
		Repository repo = git.getRepository();
		ObjectId remoteCommit = null;
		try {
			remoteCommit = repo.resolve("origin/" + getBranchInfo().getBranchName() + "^{commit}");
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
	 * @return the base commit
	 */
	public ObjectId getBaseCommit() {
		Repository repository = git.getRepository();
		RevWalk walk = new RevWalk(repository);
		ObjectId localCommit = null;
		ObjectId remoteCommit = null;
		ObjectId baseCommit = null;
		try {
			remoteCommit = repository.resolve("origin/" + getBranchInfo().getBranchName() + "^{commit}");
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
			throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
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
	 * @param commit
	 *          - the commit in which the file exists
	 * @param path
	 *          - the path to the file
	 * @return the InputStream for the file
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public InputStream getInputStream(ObjectId commit)
			throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		ObjectLoader loader = git.getRepository().open(commit);
		if (loader == null) {
			File file = File.createTempFile("test", "poc");
			InputStream input = new FileInputStream(file);
			return input;
		}
		return loader.openStream();
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
	 * @param fileLocation
	 *          - the path to the file you want to restore
	 */
	public void restoreLastCommitFile(String fileLocation) {
		try {
			git.checkout().addPath(fileLocation).call();
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

		if (git != null) {
			Repository repository = git.getRepository();
			RevWalk walk = new RevWalk(repository);
			walk.reset();
			try {
				ObjectId local = getLastLocalCommit();
				ObjectId base = getBaseCommit();
				if (local != null && base != null) {
					RevCommit localCommit = walk.parseCommit(local);
					RevCommit baseCommit = walk.parseCommit(base);
					numberOfCommits = RevWalkUtils.count(walk, localCommit, baseCommit);
				}
				if (base == null) {
					Iterable<RevCommit> results = git.log().call();
					for (RevCommit revCommit : results) {
						if (revCommit.getId().name().equals(git.getRepository().getBranch())) {
							numberOfCommits = 0;
							break;
						}
						numberOfCommits++;
					}
				}
			} catch (IOException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			} catch (GitAPIException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			}
			walk.close();
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
		if (git != null) {
			Repository repository = git.getRepository();
			RevWalk walk = new RevWalk(repository);
			walk.reset();
			try {
				if (getRemoteCommit() != null && getBaseCommit() != null) {
					RevCommit remoteCommit = walk.parseCommit(getRemoteCommit());
					RevCommit baseCommit = walk.parseCommit(getBaseCommit());
					numberOfCommits = RevWalkUtils.count(walk, remoteCommit, baseCommit);
				}
				if (getBaseCommit() == null) {
					if (repository.resolve("remotes/origin/" + git.getRepository().getBranch()) != null) {
						LogCommand log = git.log();
						if (repository.resolve("HEAD") != null) {
							log.not(repository.resolve("HEAD"));
						}
						Iterable<RevCommit> logs = git.log()
								.add(repository.resolve("remotes/origin/" + git.getRepository().getBranch())).call();
						for (RevCommit revCommit : logs) {
							numberOfCommits++;
						}
					}
				}
			} catch (IOException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			} catch (GitAPIException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			}
			walk.close();
		}
		return numberOfCommits;

	}

	/**
	 * Brings all the commits to the local repository but does not merge them
	 */
	public void fetch() {
		if (logger.isDebugEnabled()) {
			logger.debug("Begin fetch");
		}
		CustomAuthenticator.install();
		
		UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(getHostName());
		String username = gitCredentials.getUsername();
		String password = gitCredentials.getPassword();
		String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
		CustomUserCredentials credentialsProvider = new CustomUserCredentials(username, password, sshPassphrase);
		try {
			unavailable = false;
			privateRepository = false;
			isSSHPassphrase = false;
			StoredConfig config = git.getRepository().getConfig();
			Set<String> sections = config.getSections();
			if (sections.contains("remote")) {
        git.fetch().setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*")).setCheckFetchedObjects(true)
						.setCredentialsProvider(credentialsProvider).call();
			}
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (TransportException e) {
			e.printStackTrace();
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}

			if (e.getMessage().contains("Authentication is required but no CredentialsProvider has been registered")
					|| e.getMessage().contains("not authorized")) {
				privateRepository = true;
				unavailable = false;
			} else if (e.getMessage().contains("Auth fail")
			    // A SSH pass phase was requested.
			    && credentialsProvider.isPassphaseRequested()) {
				isSSHPassphrase = true;
			} else {
				unavailable = true;
			}
		} catch (GitAPIException e) {
			e.printStackTrace();
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (RevisionSyntaxException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		System.out.println("end fetch");
		if (logger.isDebugEnabled()) {
			logger.debug("End fetch");
		}
	}

	public void updateWithRemoteFile(String filePath) {
		try {
			RevWalk walk = new RevWalk(git.getRepository());
			walk.reset();
			RevCommit commit = walk.parseCommit(git.getRepository().resolve("MERGE_HEAD"));
			git.checkout().setStartPoint(commit).addPath(filePath).call();
			walk.close();
		} catch (CheckoutConflictException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (RevisionSyntaxException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (MissingObjectException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (IncorrectObjectTypeException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (AmbiguousObjectException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (IOException e) {
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
			AnyObjectId commitToMerge = git.getRepository().resolve("MERGE_HEAD");// getRemoteCommit();
			git.clean().call();
			git.reset().setMode(ResetType.HARD).call();
			git.merge().include(commitToMerge).setStrategy(MergeStrategy.RECURSIVE).call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("End fetch");
			}
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("End fetch");
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
			} catch (IOException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
			} catch (NoHeadException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
				return new BranchInfo(branchName, false);
			} catch (GitAPIException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e, e);
				}
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
	public void setBranch(String selectedBranch) throws RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException, GitAPIException {
		git.checkout().setName(selectedBranch).call();

	}

	/**
	 * Shows if the remote repository is available or not
	 * 
	 * @return true if the repository is up, and false if the repository is down
	 */
	public boolean isUnavailable() {
		return unavailable;
	}

	/**
	 * Return the submodule head commit to the previously one
	 */
	public void discardSubmodule() {
		try {
			git.submoduleSync().call();
			git.submoduleUpdate().setStrategy(MergeStrategy.RECURSIVE).call();
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("End fetch");
			}
		}
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
		} catch (GitAPIException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
		return null;
	}

	public boolean isPrivateRepository() {
		return privateRepository;
	}

	public boolean isSShPassphrase() {
		return isSSHPassphrase;
	}

}
