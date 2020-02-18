package com.oxygenxml.git.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.io.NullOutputStream;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

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
   * @return The Object identifying the file at the given revision or <code>null</code> if the path is not found in the given commit.
   * 
   * @throws IOException Unable to identify the commit.
   */
  public static ObjectId getObjectID(Repository repository, String commitID, String path) throws IOException {
    ObjectId head = repository.resolve(commitID);

    try (RevWalk rw = new RevWalk(repository)) {
      RevCommit commit = rw.parseCommit(head);
      
      try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
        return treeWalk != null ? treeWalk.getObjectId(0) : null;
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
    List<DiffEntry> diffs = diff(repository, newCommit, oldCommit);
    
    return diffs
        .stream()
        .map(t -> new FileStatusOverDiffEntry(t, newCommit.getId().name(), oldCommit.getId().name()))
        .collect(Collectors.toList());
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
  private static List<DiffEntry> diff(
      Repository repository, 
      RevCommit newCommit, 
      RevCommit oldCommit) throws IOException, GitAPIException {
    List<DiffEntry> collect = Collections.emptyList();
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
        
        // Identify potential renames.
        RenameDetector rd = new RenameDetector(git.getRepository());
        rd.addAll(diffs);
        collect = rd.compute();
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
        collect.add(new FileStatusOverDiffEntry(diff, commit.getId().name(), null));
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
  
  /**
   * Checks if the given path was renamed between the two revisions.
   * 
   * @return If the resource was renamed between the two revisions, it will return a diff information.
   * 
   * @throws IOException
   * @throws GitAPIException 
   */
  public static Optional<DiffEntry> findRename(
      Repository repository,
      RevCommit parent, 
      RevCommit commit,
      String path) throws IOException, GitAPIException {
    DiffEntry toReturn = null;
    List<DiffEntry> diffs = diff(repository, commit, parent);
    for (DiffEntry diffEntry : diffs) {
      if (isRename(diffEntry) && diffEntry.getNewPath().equals(path)) {
        toReturn = diffEntry;
        break;
      }
    }

    return Optional.ofNullable(toReturn);
  }

  /**
   * Checks the change type to see if it represents a rename.
   * 
   * @param ent Diff entry.
   * 
   * @return <code>true</code> if this change represents a rename.
   */
  public static boolean isRename(DiffEntry ent) {
    return ent.getChangeType() == ChangeType.RENAME
        || ent.getChangeType() == ChangeType.COPY;
  }
  
  

  /**
   * Collects the revisions from the current branch and the remote branch linked to it.
   * 
   * @param filePath An optional resource path. If not null, only the revisions that changed this resource are collected.
   * @param revisions Revisions are collected in here.
   * @param repository Loaded repository.
   * 
   * @throws IOException 
   * @throws GitAPIException
   */
  public static void collectCurrentBranchRevisions(
      String filePath, 
      List<CommitCharacteristics> revisions, 
      Repository repository) throws IOException, GitAPIException {
    
 // a RevWalk allows to walk over commits based on some filtering that is defined
    // EXM-44307 Show current branch commits only.
    String fullBranch = repository.getFullBranch();
    Ref branchHead = repository.exactRef(fullBranch);
    if (branchHead != null) {
      try (RevWalk revWalk = new RevWalk(repository)) {
        
        RevCommit revCommit = revWalk.parseCommit(branchHead.getObjectId());
        revWalk.markStart(revCommit);

        // If we have a remote, put it as well.
        String fullRemoteBranchName = getUpstreamBranchName(repository, repository.getBranch());
        if (fullRemoteBranchName != null) {
          Ref fullRemoteBranchHead = repository.exactRef(fullRemoteBranchName);
          if (fullRemoteBranchHead != null) {
            revWalk.markStart(revWalk.parseCommit(fullRemoteBranchHead.getObjectId()));
          }
        }
        
        collectRevisions(filePath, revisions, repository, revWalk);
      }
      
    } else {
      // Probably a new repository without any history. 
    }
  }
  
  /**
   * Gets the full remote-tracking branch name or null is the local branch is not tracking a remote branch.
   * 
   * ex: refs/remotes/origin/dev
   * @param repository 
   * 
   * @param localBranchShortName The short branch name.
   * 
   * @return The full remote-tracking branch name or null is the local branch is not tracking a remote branch.
   */
  public static String getUpstreamBranchName(Repository repository, String localBranchShortName) {
    BranchConfig branchConfig = new BranchConfig(repository.getConfig(), localBranchShortName);
    return branchConfig.getRemoteTrackingBranch();
  }

  /**
   * Collects all the revisions by waking the revision iterator.
   * 
   * @param filePath An optional resource path. If not null, only the revisions that changed this resource are collected.
   * @param revisions Revisions are collected in here.
   * @param repository Loaded repository.
   * @param revWalk Revision iterator.
   * 
   * @throws IOException
   * @throws GitAPIException
   */
  private static void collectRevisions(
      String filePath, 
      List<CommitCharacteristics> commitVector, 
      Repository repository,
      RevWalk revWalk) throws IOException, GitAPIException {
    
    if (filePath != null) {
      revWalk.setTreeFilter(
          AndTreeFilter.create(
              PathFilterGroup.createFromStrings(filePath),
              TreeFilter.ANY_DIFF)
          );
    }

    RevCommit lastProcessedRevision = null;
    for (RevCommit commit : revWalk) {
      appendRevCommit(commitVector, commit);
      
      lastProcessedRevision = commit;
    }
    
    // If we are following a resource, check for rename events.
    if (filePath != null && lastProcessedRevision != null) {
      handleRename(filePath, commitVector, repository, lastProcessedRevision);
    }
  }

  /**
   * Checks for a rename operation. If the resource was renamed between the current revision and the previous one,
   * it will continue collecting revisions based on the previous resource name/path.  
   * 
   * @param filePath An optional resource path. If not null, only the revisions that changed this resource are collected.
   * @param revisions Revisions are collected in here.
   * @param repository Loaded repository.
   * @param current The last revision encountered for the file path.
   * 
   * @throws IOException
   * @throws GitAPIException
   */
  private static void handleRename(
      String filePath, 
      List<CommitCharacteristics> commitVector, 
      Repository repository,
      RevCommit current)
      throws IOException, GitAPIException {
    try (RevWalk revWalk = new RevWalk(repository);) {
      current = revWalk.parseCommit(current.getId());

      if (current.getParentCount() > 0) {
        RevCommit parent = current.getParent(0);
        revWalk.parseHeaders(parent);

        Optional<DiffEntry> renameRev = RevCommitUtil.findRename(repository, parent, current, filePath);
        if (renameRev.isPresent()) {
          String oldPath = renameRev.get().getOldPath();

          revWalk.markStart(current);
          
          // We will re-append this commit but this time it will be linked to 
          commitVector.remove(commitVector.size() - 1);
          collectRevisions(oldPath, commitVector, repository, revWalk);
        }
      }
    }
  }

  /**
   * Adds the revision into the collecting list.
   * 
   * @param revisions Revisions are collected in here.
   * @param commit Revision to collect.
   */
  private static void appendRevCommit(List<CommitCharacteristics> revisions, RevCommit commit) {
    String commitMessage = commit.getFullMessage();
    PersonIdent authorIdent = commit.getAuthorIdent();
    String author = authorIdent.getName() + " <" + authorIdent.getEmailAddress() + ">";
    Date authorDate = authorIdent.getWhen();
    String abbreviatedId = commit.getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name();
    String id = commit.getId().getName();

    PersonIdent committerIdent = commit.getCommitterIdent();
    String committer = committerIdent.getName();
    List<String> parentsIds = getParentsId(commit);

    // add commit element in vector
    revisions.add(new CommitCharacteristics(commitMessage, authorDate, author, abbreviatedId, id,
        committer, parentsIds));
  }

  /**
   * Get a list with all the parent IDs of the current commit.
   * 
   * @param commit The current commit.
   * @return The list with parents commit IDs.
   */
  private static List<String> getParentsId(RevCommit commit) {
    List<String> parentsIds = null;

    // add list of parent commits.
    if (commit.getParentCount() > 0) {
      parentsIds = new ArrayList<>();
      for (RevCommit parentCommit : commit.getParents()) {
        parentsIds.add(parentCommit.getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name());
      }
    }
    return parentsIds;
  }
}