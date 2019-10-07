package com.oxygenxml.git.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

import ro.sync.io.FileSystemUtil;
import ro.sync.util.URLUtil;

/**
 * Tests for the code related with history.
 */
public class GitHistoryTest extends GitTestBase {

  /**
   * Tests the commit revisions retrieval.
   * 
   * @throws Exception
   */
  @Test
  public void testHistory() throws Exception {
    URL resource = getClass().getClassLoader().getResource("repos/history");
    File repository = URLUtil.getCanonicalFileFromFileUrl(resource);


    File gitRepos = initRepository(repository);
    try {
      String absolutePath = repository.getAbsolutePath();
      GitAccess.getInstance().setRepository(absolutePath);

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dump(commitsCharacteristics);

      assertEquals(
          "[ Uncommitted changes , null , * , * , * , null , null ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , [c902b75] ]\n" + 
          "[ Changes. , Fri Oct 04 10:42:09 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , c902b75 , c902b758113c8703f75883aa5a3dbe4addaa4d8f , AlexJitianu , [18245ad] ]\n" + 
          "[ First commit. , Fri Oct 04 10:41:16 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , 18245ad , 18245ad2c01f1466adbde681056e2864e85adaa8 , AlexJitianu , null ]\n" + 
          "", dump);


      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics("root.txt");

      dump = dump(commitsCharacteristics);

      assertEquals(
          "root.txt was created and changed in the last two commit",
          "[ Uncommitted changes , null , * , * , * , null , null ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , null ]\n" + 
          "", dump);
    } finally {
      GitAccess.getInstance().close();
      cleanRepository(gitRepos);
    }
  }
  
  /**
   * Tests the files that are contained in each commit.
   * 
   * @throws Exception
   */
  @Test
  public void testCommitFiles() throws Exception {
    File repository = new File("target/test-resources/repos/history");

    File gitRepos = initRepository(repository);
    try {
      String absolutePath = repository.getAbsolutePath();
      GitAccess.getInstance().setRepository(absolutePath);

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
      assertEquals(5, commitsCharacteristics.size());
      
      Map<String, String> fileLists = new LinkedHashMap<>();
      commitsCharacteristics.stream().forEach(t -> {
        try {
          fileLists.put(t.getCommitAbbreviatedId(), dumpFS(RevCommitUtil.getChanges(t.getCommitId())));
        } catch (IOException | GitAPIException e) {
          fileLists.put(t.getCommitAbbreviatedId(), e.getMessage());
        }
      });
      
      fileLists.keySet().stream().forEach(k -> {
        System.out.println(k);
        System.out.println(fileLists.get(k));
        System.out.println();
      });
      
      
      Map<String, String> expected = new LinkedHashMap<>();
      expected.put("*", "(changeType=MODIFIED, fileLocation=root.txt)\n");
      expected.put("011cd47", "(changeType=CHANGED, fileLocation=root.txt)\n");
      expected.put("969e9c3", "(changeType=ADD, fileLocation=root.txt)\n");
      expected.put(
          "c902b75", "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
          "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file3.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file3_renamed.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file4.txt)\n");
      expected.put(
          "18245ad", "(changeType=ADD, fileLocation=f1/file1.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file2.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file3.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file4.txt)\n" + 
          "(changeType=ADD, fileLocation=newProject.xpr)\n" + 
          "");
      
      assertEquals(expected.keySet(), fileLists.keySet());
      
      
      Set<String> keySet = expected.keySet();
      for (String commitID : keySet) {
        assertEquals("Unnexpected file list for commit id " + commitID, expected.get(commitID), fileLists.get(commitID));
      }
    } finally {
      GitAccess.getInstance().close();
      cleanRepository(gitRepos);
    }
  }

  private String dumpFS(List<FileStatus> changes) {
    StringBuilder b = new StringBuilder();
    changes.stream().forEach(t -> b.append(t.toString()).append("\n"));
    return b.toString();
  }

  /**
   * Initializes a Git repository for test that was renamed.
   * 
   * @param workingTreeDir The working tree directory.
   * 
   * @return The repository direcotry.
   */
  private File initRepository(File workingTreeDir) {
    File gitRepos = new File(workingTreeDir, ".git");
    FileSystemUtil.renameFileTo(new File(workingTreeDir, "git"), gitRepos);
    return gitRepos;
  }
  
  /**
   * Initializes a Git repository for test that was renamed.
   * 
   * @param gitRepos The working tree directory.
   * 
   * @return The repository direcotry.
   */
  private void cleanRepository(File gitRepos) {
    File originalRepos = new File(gitRepos.getParentFile(), "git");
    FileSystemUtil.renameFileTo(gitRepos, originalRepos);
  }

  /**
   * Dumps a string version of the commits.
   * 
   * @param commitsCharacteristics Commits.
   * 
   * @return A string representation.
   */
  private String dump(List<CommitCharacteristics> commitsCharacteristics) {
    StringBuilder b = new StringBuilder();

    commitsCharacteristics.stream().forEach(t -> b.append(t).append("\n"));

    return b.toString();
  }

  /**
   * We have a repository with multiple branches. The history should present the history for the current branch only.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testHistoryBranch() throws Exception {
    URL resource = getClass().getClassLoader().getResource("repos/history-branches");
    File repository = URLUtil.getCanonicalFileFromFileUrl(resource);
  
    File gitRepos = initRepository(repository);
    try {
      String absolutePath = repository.getAbsolutePath();
      GitAccess.getInstance().setRepository(absolutePath);
      
      System.out.println(GitAccess.getInstance().getGitForTests().status().call().hasUncommittedChanges());
      
      GitAccess.getInstance().setBranch("master");
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dump(commitsCharacteristics);
  
      assertEquals(
          "[ Changed on master branch.\n" + 
          " , Mon Oct 07 09:34:51 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 7267d76 , 7267d76288835637cb349090f2fe2e903d887e22 , AlexJitianu , [011cd47] ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , [c902b75] ]\n" + 
          "[ Changes. , Fri Oct 04 10:42:09 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , c902b75 , c902b758113c8703f75883aa5a3dbe4addaa4d8f , AlexJitianu , [18245ad] ]\n" + 
          "[ First commit. , Fri Oct 04 10:41:16 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , 18245ad , 18245ad2c01f1466adbde681056e2864e85adaa8 , AlexJitianu , null ]\n" + 
          "", dump);
  
  
      GitAccess.getInstance().setBranch("feature");
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      dump = dump(commitsCharacteristics);
  
      assertEquals(
          "[ Changed on feature branch.\n" + 
          " , Mon Oct 07 09:33:45 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 679624f , 679624f7aa1f4a60a31cff9d31c4c9307118cd98 , AlexJitianu , [a227369] ]\n" + 
          "[ Feature branch commit.\n" + 
          " , Mon Oct 07 09:32:44 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , a227369 , a227369a580868f45c9764995f8da36a095c424e , AlexJitianu , [011cd47] ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , [c902b75] ]\n" + 
          "[ Changes. , Fri Oct 04 10:42:09 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , c902b75 , c902b758113c8703f75883aa5a3dbe4addaa4d8f , AlexJitianu , [18245ad] ]\n" + 
          "[ First commit. , Fri Oct 04 10:41:16 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , 18245ad , 18245ad2c01f1466adbde681056e2864e85adaa8 , AlexJitianu , null ]\n" + 
          "", dump);
    } finally {
      GitAccess.getInstance().close();
      cleanRepository(gitRepos);
    }
  }

  /**
   * We have a repository with multiple branches. The history should present the history for the current branch only.
   * When a branch is merged into another, the common revisions appear in both branches.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testHistoryBranchMerged() throws Exception {
    URL resource = getClass().getClassLoader().getResource("repos/history-branches-merged");
    File repository = URLUtil.getCanonicalFileFromFileUrl(resource);
  
    File gitRepos = initRepository(repository);
    try {
      String absolutePath = repository.getAbsolutePath();
      GitAccess.getInstance().setRepository(absolutePath);
      
      System.out.println(GitAccess.getInstance().getGitForTests().status().call().hasUncommittedChanges());
      
      GitAccess.getInstance().setBranch("master");
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dump(commitsCharacteristics);
  
      assertEquals(
          "[ Another commit on master.\n" + 
          " , Mon Oct 07 10:04:34 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 8b3feaa , 8b3feaa9ba6243326534c9cc4e624196033f9a1a , AlexJitianu , [c0672a3] ]\n" + 
          "[ Merge branch 'feature'\n" + 
          " , Mon Oct 07 09:55:41 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , c0672a3 , c0672a3e109ba28879c043e8d7945110dfe2c683 , AlexJitianu , [7267d76, 679624f] ]\n" + 
          "[ Changed on master branch.\n" + 
          " , Mon Oct 07 09:34:51 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 7267d76 , 7267d76288835637cb349090f2fe2e903d887e22 , AlexJitianu , [011cd47] ]\n" + 
          "[ Changed on feature branch.\n" + 
          " , Mon Oct 07 09:33:45 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 679624f , 679624f7aa1f4a60a31cff9d31c4c9307118cd98 , AlexJitianu , [a227369] ]\n" + 
          "[ Feature branch commit.\n" + 
          " , Mon Oct 07 09:32:44 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , a227369 , a227369a580868f45c9764995f8da36a095c424e , AlexJitianu , [011cd47] ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , [c902b75] ]\n" + 
          "[ Changes. , Fri Oct 04 10:42:09 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , c902b75 , c902b758113c8703f75883aa5a3dbe4addaa4d8f , AlexJitianu , [18245ad] ]\n" + 
          "[ First commit. , Fri Oct 04 10:41:16 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , 18245ad , 18245ad2c01f1466adbde681056e2864e85adaa8 , AlexJitianu , null ]\n" + 
          "", dump);
  
  
      GitAccess.getInstance().setBranch("feature");
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      dump = dump(commitsCharacteristics);
  
      assertEquals(
          "[ Anotehr commit of feature branch.\n" + 
          " , Mon Oct 07 10:05:10 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 4aebeb5 , 4aebeb5b67a28a5af43e9cb3682d926bd05758b2 , AlexJitianu , [679624f] ]\n" + 
          "[ Changed on feature branch.\n" + 
          " , Mon Oct 07 09:33:45 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 679624f , 679624f7aa1f4a60a31cff9d31c4c9307118cd98 , AlexJitianu , [a227369] ]\n" + 
          "[ Feature branch commit.\n" + 
          " , Mon Oct 07 09:32:44 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , a227369 , a227369a580868f45c9764995f8da36a095c424e , AlexJitianu , [011cd47] ]\n" + 
          "[ Root file changed. , Fri Oct 04 12:18:23 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 011cd47 , 011cd47dc880998e3d40098cd6257859b3b14f34 , AlexJitianu , [969e9c3] ]\n" + 
          "[ Root file. , Fri Oct 04 12:18:09 EEST 2019 , AlexJitianu <jitianualex83@gmail.com> , 969e9c3 , 969e9c3c0011b3ed0c65d5619559b0340d38e91f , AlexJitianu , [c902b75] ]\n" + 
          "[ Changes. , Fri Oct 04 10:42:09 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , c902b75 , c902b758113c8703f75883aa5a3dbe4addaa4d8f , AlexJitianu , [18245ad] ]\n" + 
          "[ First commit. , Fri Oct 04 10:41:16 EEST 2019 , AlexJitianu <alex_jitianu@sync.ro> , 18245ad , 18245ad2c01f1466adbde681056e2864e85adaa8 , AlexJitianu , null ]\n" + 
          "", dump);
    } finally {
      GitAccess.getInstance().close();
      cleanRepository(gitRepos);
    }
  }

}
