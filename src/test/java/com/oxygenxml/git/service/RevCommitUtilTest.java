package com.oxygenxml.git.service;

import java.io.File;
import java.net.URL;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

/**
 * Utility methods Test case.
 */
public class RevCommitUtilTest extends GitTestBase {

  /**
   * Identify rename between two revisions.
   */
  @Test
  public void testFindRename() throws Exception {

    URL script = getClass().getClassLoader().getResource("scripts/history_script_follow_rename.txt");
    File wcTree = new File("target/gen/GitHistoryTest_testHistory");
    
    generateRepositoryAndLoad(script, wcTree);
    
    Git git = GitAccess.getInstance().getGitForTests();
    Repository repository = git.getRepository();
    
    // a RevWalk allows to walk over commits based on some filtering that is defined
    // EXM-44307 Show current branch commits only.
    String fullBranch = repository.getFullBranch();
    Ref branchHead = repository.exactRef(fullBranch);
    ObjectId objectId = branchHead.getObjectId();
    try(RevWalk revWalk = new RevWalk(repository)) {
      RevCommit head = revWalk.parseCommit(objectId);
      revWalk.markStart(head);
      
      RevCommit next = revWalk.next();
      assertEquals("Third commit.", next.getFullMessage());
      
      RevCommit renameRev = revWalk.next();
      assertEquals("Rename.", renameRev.getFullMessage());
      
      RevCommit initialRev = revWalk.next();
      assertEquals("First commit.", initialRev.getFullMessage());
      
      Optional<DiffEntry> findRename = RevCommitUtil.findRename(repository, initialRev, renameRev, "file_renamed.txt");
      
      assertTrue(findRename.isPresent());
      
      DiffEntry diffEntry = findRename.get();
      
      assertEquals("file.txt", diffEntry.getOldPath());
      assertEquals("file_renamed.txt", diffEntry.getNewPath());
    }
  }
}
