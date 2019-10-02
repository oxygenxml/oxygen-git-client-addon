package com.oxygenxml.git.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;

/**
 * Utility methods for working with commits.
 */
public class RevCommitUtil {

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
  public static List<FileStatus> getChanges(Repository repository, RevCommit newCommit, RevCommit oldCommit) throws IOException, GitAPIException {
    List<FileStatus> collect = Collections.emptyList();
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
      newTreeIter.reset(reader, newCommit.
          getTree().
          getId());
      
      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      oldTreeIter.reset(reader, oldCommit.
          getTree().
          getId());
      

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
}