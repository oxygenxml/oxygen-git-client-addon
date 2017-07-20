package com.oxygenxml.sdksamples.workspace.git.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.util.io.NullOutputStream;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

/**
 * Implements some basic git functionality like commit, push, pull, retrieve
 * File status(staged, unstaged)
 * 
 * @author intern2
 *
 */
public class GitAccess {

	private Git git;

	public GitAccess() {

	}

	/**
	 * Sets the git repository. File path must exist
	 * 
	 * @param path
	 *          - A string that specifies the git Repository folder
	 */
	public void setRepository(String path) {

		try {
			git = Git.open(new File(path + "/.git"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return the git Repository
	 */
	public Repository getRepository() {
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
			git = Git.init().setDirectory(new File(path)).call();
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
		List<FileStatus> stagedFiles = getStagedFile();

		// needed only to pass it to the formatter
		OutputStream out = NullOutputStream.INSTANCE;

		DiffFormatter formatter = new DiffFormatter(out);
		formatter.setRepository(git.getRepository());
		try {
			AbstractTreeIterator commitTreeIterator = getLastCommitTreeIterator(git.getRepository(), Constants.HEAD);
			FileTreeIterator workTreeIterator = new FileTreeIterator(git.getRepository());
			List<DiffEntry> diffEntries = formatter.scan(commitTreeIterator, workTreeIterator);
			for (DiffEntry entry : diffEntries) {
				ChangeType changeType = entry.getChangeType();

				if (entry.getChangeType().equals(ChangeType.ADD)
						|| entry.getChangeType().equals(ChangeType.COPY)
						|| entry.getChangeType().equals(ChangeType.RENAME)) {
					String filePath = entry.getNewPath();
					FileStatus unstageFile = new FileStatus(changeType, filePath);
					if (!stagedFiles.contains(unstageFile)) {
						unstagedFiles.add(unstageFile);
					}
				} else {
					String filePath = entry.getOldPath();
					FileStatus unstageFile = new FileStatus(changeType, filePath);
					if (!stagedFiles.contains(unstageFile)) {
						unstagedFiles.add(unstageFile);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		formatter.close();

		return unstagedFiles;

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
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	private AbstractTreeIterator getLastCommitTreeIterator(Repository repository, String branch) throws Exception {
		Ref head = repository.exactRef(branch);

		RevWalk walk = new RevWalk(repository);
		RevCommit commit = walk.parseCommit(head.getObjectId());
		RevTree tree = walk.parseTree(commit.getTree().getId());

		CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
		ObjectReader oldReader = repository.newObjectReader();
		oldTreeParser.reset(oldReader, tree.getId());
		walk.close();
		return oldTreeParser;
	}

	/**
	 * Frees resources associated with the git instance.
	 */
	public void close() {
		git.close();

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
	 */
	public void push(String username, String password) throws InvalidRemoteException, TransportException, GitAPIException {
		/*
		 * StoredConfig config = git.getRepository().getConfig();
		 * config.setString("remote", "origin", "url",
		 * "https://github.com/BeniaminSavu/test.git"); try { config.save(); } catch
		 * (IOException e1) { e1.printStackTrace(); }
		 */

		git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();

	}

	/**
	 * Pulls the files that are not on the local repository from the remote
	 * repository
	 * 
	 * @param username
	 *          - Git username
	 * @param password
	 *          - Git password
	 */
	public void pull(String username, String password) {
		try {

			PullResult result = git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
					.call();
			System.out.println(result.isSuccessful());
			System.out.println(result.getFetchedFrom());

		} catch (GitAPIException e) {
			e.printStackTrace();
		}
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
				if (file.getChangeType() == ChangeType.DELETE) {
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
		try {
			Status status = git.status().call();
			List<FileStatus> stagedFiles = new ArrayList<FileStatus>();
			for (String fileName : status.getChanged()) {
				stagedFiles.add(new FileStatus(ChangeType.MODIFY, fileName));
			}
			for (String fileName : status.getAdded()) {
				stagedFiles.add(new FileStatus(ChangeType.ADD, fileName));
			}
			for (String fileName : status.getRemoved()) {
				stagedFiles.add(new FileStatus(ChangeType.DELETE, fileName));
			}
			return stagedFiles;
		} catch (NoWorkTreeException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
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
			ResetCommand reset = git.reset();
			for (FileStatus file : files) {
				reset.addPath(file.getFileLocation());

			}
			reset.call();
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public String getHostName(){
		Config storedConfig = git.getRepository().getConfig();
		String url = storedConfig.getString("remote", "origin", "url");
		try {
			URL u = new URL(url);
			url = u.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		
		
		return url;
	}
}
