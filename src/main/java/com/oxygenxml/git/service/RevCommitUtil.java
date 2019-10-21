package com.oxygenxml.git.service;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.NullOutputStream;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;

/**
 * Utility methods for working with commits.
 */
public class RevCommitUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RevCommitUtil.class);
  
  /**
   * Utility class. Not indented to be instantiated.
   */
  private RevCommitUtil() {}
  
  /**
   * Get changed files as compared with the parent version.
   * 
   * @param commitID The commit ID.
   * 
   * @return A list with changed files. Never <code>null</code>.
   * 
   * @throws IOException
   * @throws GitAPIException
   */
  public static List<FileStatus> getChangedFiles(String commitID) throws IOException, GitAPIException {
    List<FileStatus> changedFiles = Collections.emptyList();
    try {
      Repository repository = GitAccess.getInstance().getRepository();
      if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(commitID)) {
        ObjectId head = repository.resolve(commitID);

        try (RevWalk rw = new RevWalk(repository)) {
          RevCommit commit = rw.parseCommit(head);
          
          if (commit.getParentCount() > 0) {
            RevCommit oldC = rw.parseCommit(commit.getParent(0));

            changedFiles = RevCommitUtil.getChanges(repository, commit, oldC);
          } else {
            changedFiles = RevCommitUtil.getFiles(repository, commit);
          }
        }
      } else {
        changedFiles = GitAccess.getInstance().getUnstagedFiles();
      }
    } catch (GitAPIException | RevisionSyntaxException | IOException | NoRepositorySelected e) {
      logger.error(e, e);
    }

    return changedFiles;
  }
  
  /**
   * Gets the Object ID for a file path at a given revision.
   * 
   * @param repository Git repository.
   * @param commitID Revision commit ID.
   * @param path File path, relative to the working tree directory.
   * 
   * @return The Object identifying the file at the given revision.
   * 
   * @throws IOException Unable to identify the commit.
   */
  public static ObjectId getObjectID(Repository repository, String commitID, String path) throws IOException {
    ObjectId head = repository.resolve(commitID);

    try (RevWalk rw = new RevWalk(repository)) {
      RevCommit commit = rw.parseCommit(head);
      
      try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
        return treeWalk.getObjectId(0);
      }
    }
  }

  /**
   * Gets all the files changed between two revisions.
   * 
   * @param repository Repository.
   * @param newCommit The new commit.
   * @param oldCommit The previous commit.
   * 
   * @return A list with changed files. Never <code>null</code>.
   * @throws IOException
   * @throws GitAPIException
   */
  private static List<FileStatus> getChanges(Repository repository, RevCommit newCommit, RevCommit oldCommit) throws IOException, GitAPIException {
    List<FileStatus> collect = Collections.emptyList();
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
      newTreeIter.reset(reader, newCommit.getTree().getId());
      
      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      if (oldCommit != null) {
        oldTreeIter.reset(reader, oldCommit.getTree().getId());
      }
      

      // finally get the list of changed files
      try (Git git = new Git(repository)) {
        List<DiffEntry> diffs= git.diff()
            .setNewTree(newTreeIter)
            .setOldTree(oldTreeIter)
            .call();
        
        collect = diffs.stream().map(FileStatusOverDiffEntry::new).collect(Collectors.toList());
      }
    }

    return collect;
  }
  
  /**
   * Iterates over the entire tree of files inside a commit. <b>Note:</b> Not just hte changes, the entire tree.
   * 
   * @param repository Git repository.
   * @param commit Commit object.
   *  
   * @return All the files present in the repository at the time of that commit.
   * 
   * @throws IOException If it fails.
   */
  private static List<FileStatus> getFiles(Repository repository, RevCommit commit) throws IOException {
    List<FileStatus> collect = new LinkedList<>();

    try (DiffFormatter diffFmt = new DiffFormatter(NullOutputStream.INSTANCE)) {
      diffFmt.setRepository(repository);

      for(DiffEntry diff: diffFmt.scan(null, commit.getTree())) {
        collect.add(new FileStatusOverDiffEntry(diff));
      }
    }

    return collect;
  }

  /**
   * Get changed files as compared with the parent version.
   * 
   * @param repository The repository.
   * @param commitID The commit ID.
   * 
   * @return A list with changed files. Can be <code>null</code>.
   * 
   * @throws IOException Unable to retrieve commit information.
   */
  public static RevCommit[] getParents(Repository repository, String commitID) throws IOException {
    if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(commitID)) {
      ObjectId head = repository.resolve(commitID);

      try (RevWalk rw = new RevWalk(repository)) {
        RevCommit commit = rw.parseCommit(head);

        return commit.getParents();
      }
    }

    return new RevCommit[0];
  }
}