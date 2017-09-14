package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.Side;
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

import com.oxygenxml.git.CustomAuthenticator;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.translator.TranslatorExtensionImpl;

import de.schlichtherle.io.FileInputStream;

/**
 * Implements some basic git functionality like commit, push, pull, retrieve
 * File status(staged, unstaged)
 * 
 * TODO Beni Add monitors and progress.
 * 
 * @author intern2
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

	private boolean unavailable;

	private boolean privateRepository = false;

	private GitAccess() {

	}

	public static GitAccess getInstance() {
		if (instance == null) {
			instance = new GitAccess();
		}
		return instance;
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
		}
		git = Git.open(new File(path + "/.git"));
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
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		/*
		 * List<FileStatus> stagedFiles = getStagedFile(); List<FileStatus>
		 * conflictingFiles = getConflictingFiles(); List<String> conflictingPaths =
		 * new ArrayList<String>(); for (FileStatus conflictingFile :
		 * conflictingFiles) {
		 * conflictingPaths.add(conflictingFile.getFileLocation()); }
		 */
		if (git != null) {
			try {
				/*
				 * System.out.println(); List<DiffEntry> call =
				 * git.diff().setPathFilter(PathFilter.create("asd.txt")).call(); for
				 * (DiffEntry diffEntry : call) {
				 * System.out.println(diffEntry.getChangeType()); ObjectLoader open =
				 * git.getRepository().open(diffEntry.getOldId().toObjectId());
				 * open.copyTo(System.out); System.out.println(); }
				 */
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
				e1.printStackTrace();
			} catch (GitAPIException e1) {
				e1.printStackTrace();
			}
		}
		return unstagedFiles;
		// TODO Check if these are the staged files
		// git.status().call().getChanged()
		// TODO Check if we can get the unstaged files like this.
		// git.status().call().getModified()
		// needed only to pass it to the formatter
		/*
		 * OutputStream out = NullOutputStream.INSTANCE;
		 * 
		 * DiffFormatter formatter = new DiffFormatter(out); if (git != null) {
		 * formatter.setRepository(git.getRepository()); try { AbstractTreeIterator
		 * commitTreeIterator = getLastCommitTreeIterator(git.getRepository(),
		 * Constants.HEAD); if (commitTreeIterator != null) { FileTreeIterator
		 * workTreeIterator = new FileTreeIterator(git.getRepository());
		 * List<DiffEntry> diffEntries = formatter.scan(commitTreeIterator,
		 * workTreeIterator); for (DiffEntry entry : diffEntries) { GitChangeType
		 * changeType = null; if (entry.getChangeType() == ChangeType.ADD) {
		 * changeType = GitChangeType.ADD; } else if (entry.getChangeType() ==
		 * ChangeType.MODIFY) { changeType = GitChangeType.MODIFY; } else if
		 * (entry.getChangeType() == ChangeType.DELETE) { changeType =
		 * GitChangeType.DELETE; }
		 * 
		 * String filePath = null; if (entry.getChangeType().equals(ChangeType.ADD)
		 * || entry.getChangeType().equals(ChangeType.COPY) ||
		 * entry.getChangeType().equals(ChangeType.RENAME)) { filePath =
		 * entry.getNewPath(); } else { filePath = entry.getOldPath(); }
		 * 
		 * FileStatus unstageFile = new FileStatus(changeType, filePath); if
		 * (!stagedFiles.contains(unstageFile)) { if
		 * (!conflictingPaths.contains(filePath)) { unstagedFiles.add(unstageFile);
		 * } } } unstagedFiles.addAll(conflictingFiles); } else { String
		 * selectedRepository =
		 * OptionsManager.getInstance().getSelectedRepository(); List<String>
		 * fileNames = FileHelper.search(selectedRepository); for (String fileName :
		 * fileNames) { selectedRepository = selectedRepository.replace("\\", "/");
		 * int cut =
		 * selectedRepository.substring(selectedRepository.lastIndexOf("/") +
		 * 1).length(); String file = fileName.substring(cut + 1); FileStatus
		 * unstageFile = new FileStatus(GitChangeType.ADD, file);
		 * unstagedFiles.add(unstageFile); } } } catch (Exception e) {
		 * e.printStackTrace(); } }
		 * 
		 * formatter.close();
		 * 
		 * return unstagedFiles;
		 */
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
			e.printStackTrace();
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
			e.printStackTrace();
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
		git.submoduleUpdate().addPath(submodule).call();
		Repository parentRepository = git.getRepository();
		Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(parentRepository, submodule);
		git = Git.wrap(submoduleRepository);
	}

	/*
	 * public List<FileStatus> getSubmodules2() throws GitAPIException {
	 * List<FileStatus> submodules = new ArrayList<FileStatus>(); List<String>
	 * submodulesNames = new ArrayList<String>(); for (String string :
	 * git.submoduleStatus().call().keySet()) { submodulesNames.add(string);
	 * submodules.add(new FileStatus(GitChangeType.SUBMODULE, string)); }
	 * submodules.clear(); int submoduleCount = 0; Repository parentRepository =
	 * git.getRepository(); try { SubmoduleWalk walk =
	 * SubmoduleWalk.forIndex(parentRepository); while (walk.next()) { Repository
	 * submoduleRepository = walk.getRepository(); Git git2 =
	 * Git.wrap(submoduleRepository); Status status = git2.status().call(); for
	 * (String string : status.getUntracked()) { submodules.add(new
	 * FileStatus(GitChangeType.ADD, submodulesNames.get(0) + "/" + string)); }
	 * for (String string : status.getModified()) { System.out.println(string);
	 * submodules.add(new FileStatus(GitChangeType.MODIFY, submodulesNames.get(0)
	 * + "/" + string)); } for (String string : status.getMissing()) {
	 * submodules.add(new FileStatus(GitChangeType.DELETE, submodulesNames.get(0)
	 * + "/" + string)); }
	 * System.out.println(submoduleRepository.getWorkTree().getAbsolutePath());
	 * submoduleRepository.close(); submoduleCount++; } walk.close(); } catch
	 * (IOException e) { e.printStackTrace(); }
	 * System.out.println(submoduleCount); return submodules; }
	 */

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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	/*
	 * private AbstractTreeIterator getLastCommitTreeIterator(Repository
	 * repository, String branch) throws Exception { Ref head =
	 * repository.findRef(branch);
	 * 
	 * if (head.getObjectId() != null) { RevWalk walk = new RevWalk(repository);
	 * RevCommit commit = walk.parseCommit(head.getObjectId()); RevTree tree =
	 * walk.parseTree(commit.getTree().getId());
	 * 
	 * CanonicalTreeParser oldTreeParser = new CanonicalTreeParser(); ObjectReader
	 * oldReader = repository.newObjectReader(); oldTreeParser.reset(oldReader,
	 * tree.getId()); walk.close(); return oldTreeParser; } else { return null; }
	 * }
	 */

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
			e.printStackTrace();
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
		} catch (RefAlreadyExistsException e) {
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidRefNameException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		} catch (NotMergedException e) {
			e.printStackTrace();
		} catch (CannotDeleteCurrentBranchException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
			Iterable<PushResult> call = git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
			Iterator<PushResult> results = call.iterator();
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
		} else if (getUnstagedFiles().size() > 0 || getStagedFile().size() > 0) {
			response.setStatus(PullStatus.UNCOMITED_FILES);
		} else {
			git.reset().call();
			PullResult call = git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
			} catch (NoWorkTreeException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
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
			} catch (NoWorkTreeException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		} catch (CheckoutConflictException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
		} catch (RefAlreadyExistsException e) {
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidRefNameException e) {
			e.printStackTrace();
		} catch (CheckoutConflictException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		/*
		 * File file = new File(OptionsManager.getInstance().getSelectedRepository()
		 * + "/" + fileLocation); OutputStream out = null; try { if (!file.exists())
		 * { file.getParentFile().mkdirs(); file.createNewFile(); } out = new
		 * FileOutputStream(file); } catch (FileNotFoundException e) {
		 * e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 * ObjectId lastCommitId = getLastLocalCommit(); ObjectLoader loader; try {
		 * loader = getLoaderFrom(lastCommitId, fileLocation); loader.copyTo(out);
		 * 
		 * } catch (MissingObjectException e) { e.printStackTrace(); } catch
		 * (IncorrectObjectTypeException e) { e.printStackTrace(); } catch
		 * (IOException e) { e.printStackTrace(); } finally { try { out.close(); }
		 * catch (IOException e) { e.printStackTrace(); } }
		 */
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
			} catch (MissingObjectException e) {
				e.printStackTrace();
			} catch (IncorrectObjectTypeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoHeadException e) {
				numberOfCommits = 0;
			} catch (GitAPIException e) {
				e.printStackTrace();
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
			} catch (RevisionSyntaxException e) {
				e.printStackTrace();
			} catch (AmbiguousObjectException e) {
				e.printStackTrace();
			} catch (IncorrectObjectTypeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoHeadException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
			walk.close();
		}
		return numberOfCommits;

	}

	/**
	 * Brings all the commits to the local repository but does not merge them
	 */
	public void fetch() {

		CustomAuthenticator.install();
		try {
			StoredConfig config = git.getRepository().getConfig();
			Set<String> sections = config.getSections();
			UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(getHostName());
			String username = gitCredentials.getUsername();
			String password = gitCredentials.getPassword();
			System.out.println(username);
			System.out.println(password);
			if (sections.contains("remote")) {
				git.fetch().setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
						.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
			}
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		} catch (TransportException e) {
			System.out.println(e.getMessage());
			if (e.getMessage().contains("Authentication is required but no CredentialsProvider has been registered")
					|| e.getMessage().contains("not authorized")) {
				privateRepository = true;
				return;
			}
			unavailable = true;
			return;
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		}
		unavailable = false;
		privateRepository = false;
	}

	public void updateWithRemoteFile(String filePath) {
		try {
			RevWalk walk = new RevWalk(git.getRepository());
			walk.reset();
			RevCommit commit = walk.parseCommit(git.getRepository().resolve("MERGE_HEAD"));
			git.checkout().setStartPoint(commit).addPath(filePath).call();
			walk.close();
		} catch (CheckoutConflictException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (MissingObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (NoWorkTreeException e) {
			e.printStackTrace();
		} catch (NoHeadException e) {
			e.printStackTrace();
		} catch (ConcurrentRefUpdateException e) {
			e.printStackTrace();
		} catch (CheckoutConflictException e) {
			e.printStackTrace();
		} catch (InvalidMergeHeadsException e) {
			e.printStackTrace();
		} catch (WrongRepositoryStateException e) {
			e.printStackTrace();
		} catch (NoMessageException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
				e.printStackTrace();
			} catch (NoHeadException e) {
				return new BranchInfo(branchName, false);
			} catch (GitAPIException e) {
				e.printStackTrace();
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
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		} catch (NoHeadException e) {
			e.printStackTrace();
		} catch (ConcurrentRefUpdateException e) {
			e.printStackTrace();
		} catch (CheckoutConflictException e) {
			e.printStackTrace();
		} catch (InvalidMergeHeadsException e) {
			e.printStackTrace();
		} catch (WrongRepositoryStateException e) {
			e.printStackTrace();
		} catch (NoMessageException e) {
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
			e.printStackTrace();
		} catch (MissingObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isPrivateRepository() {
		return privateRepository;
	}

}
