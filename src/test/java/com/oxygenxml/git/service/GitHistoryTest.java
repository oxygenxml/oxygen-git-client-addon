package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.history.RenameTracker;
import com.oxygenxml.git.view.historycomponents.HistoryPanelTestBase;

/**
 * Tests for the code related with history.
 */
public class GitHistoryTest extends HistoryPanelTestBase {

  /**
   * Tests the commit revisions retrieval.
   * 
   * @throws Exception
   */
  @Test
  public void testHistory() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistory");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      
      FileUtils.writeStringToFile(new File(wcTree, "root.txt"), "changed" , "UTF-8");
      
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance()
          .getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
      String dump = dumpHistory(commitsCharacteristics);

      String expected = "[ Uncommitted_changes , {date} , * , * , null , null ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
          "";
      
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(expected, dump);


      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, "root.txt", new RenameTracker());

      dump = dumpHistory(commitsCharacteristics);

      expected = 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      assertEquals(
          "root.txt was created and changed in the last two commit",
          expected, dump);
    } finally {
      GitAccess.getInstance().closeRepo();
      
      FileUtil.deleteRecursivelly(wcTree);
    }
  }
  
  /**
   * Tests the files that are contained in each commit.
   * 
   * @throws Exception
   */
  @Test
  public void testCommitFiles() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistory");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      
      FileUtils.writeStringToFile(new File(wcTree, "root.txt"), "changed" , "UTF-8");
      
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
      assertEquals(5, commitsCharacteristics.size());
      
      Map<String, String> fileLists = new LinkedHashMap<>();
      commitsCharacteristics.stream().forEach(t -> {
        try {
          fileLists.put(getAssertableID(t.getCommitAbbreviatedId()), dumpFileStatuses(RevCommitUtil.getChangedFiles(t.getCommitId())));
        } catch (IOException | GitAPIException e) {
          fileLists.put(getAssertableID(t.getCommitAbbreviatedId()), e.getMessage());
        }
      });
      
//      fileLists.keySet().stream().forEach(k -> {
//        System.out.println(k);
//        System.out.println(fileLists.get(k));
//        System.out.println();
//      });
      
      
      Map<String, String> expected = new LinkedHashMap<>();
      expected.put("*", "(changeType=MODIFIED, fileLocation=root.txt)\n");
      expected.put("1", "(changeType=CHANGED, fileLocation=root.txt)\n");
      expected.put("2", "(changeType=ADD, fileLocation=root.txt)\n");
      expected.put(
          "3", "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
          "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
          "(changeType=RENAME, fileLocation=f2/file3_renamed.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file4.txt)\n");
      expected.put(
          "4", "(changeType=ADD, fileLocation=f1/file1.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file2.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file3.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file4.txt)\n" + 
          "(changeType=ADD, fileLocation=newProject.xpr)\n" + 
          "");
      
      assertEquals(expected.keySet().toString(), fileLists.keySet().toString());
      
      
      Set<String> keySet = expected.keySet();
      for (String commitID : keySet) {
        assertEquals("Unnexpected file list for commit id " + commitID, expected.get(commitID), fileLists.get(commitID));
      }
    } finally {
      GitAccess.getInstance().closeRepo();
      flushAWT();
      waitForScheduler();
      flushAWT();
      
      FileUtil.deleteRecursivelly(wcTree);
    }
  }

  /**
   * We have a repository with multiple branches. The history should present the history for the current branch only.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testHistoryBranch() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_branches.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistoryBranch");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
      
      GitAccess.getInstance().setBranch("main", Optional.empty());
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
  
      String dump = dumpHistory(commitsCharacteristics);
  
      String expected = 
          "[ Changed on main branch. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          expected, dump);
      
      Map<String, List<String>> branchMap = GitAccess.getInstance().getBranchMap(GitAccess.getInstance().getRepository(), ConfigConstants.CONFIG_KEY_LOCAL);
      
      //---------------
      // Assert the branch names.
      //----------------
      final StringBuilder mapDump = new StringBuilder();
      branchMap.forEach((k,v ) -> {mapDump.append(getAssertableID(k)).append(" -> ").append(v.toString()).append("\n");});
      assertEquals(
          "6 -> [feature]\n" + 
          "1 -> [main]\n" + 
          "", mapDump.toString());
  
  
      GitAccess.getInstance().setBranch("feature", Optional.empty());
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
  
      dump = dumpHistory(commitsCharacteristics);
  
      expected = "[ Changed on feature branch. , {date} , Alex <alex_jitianu@sync.ro> , 6 , AlexJitianu , [7] ]\n" + 
          "[ Feature branch commit. , {date} , Alex <alex_jitianu@sync.ro> , 7 , AlexJitianu , [2] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          expected, dump);
      
    } finally {
      GitAccess.getInstance().closeRepo();
      
      FileUtil.deleteRecursivelly(wcTree);
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
    URL script = getClass().getClassLoader().getResource("scripts/history_script_branches_merged.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistoryBranchMerged");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
      
      GitAccess.getInstance().setBranch("main", Optional.empty());
      
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
  
      String dump = dumpHistory(commitsCharacteristics);
  
      String expected = 
          "[ Another commit on main. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Merge branch 'feature' , {date} , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3, 4] ]\n" + 
          "[ Changed on main branch. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [5] ]\n" + 
          "[ Changed on feature branch. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [6] ]\n" + 
          "[ Feature branch commit. , {date} , Alex <alex_jitianu@sync.ro> , 6 , AlexJitianu , [5] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , [7] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 7 , AlexJitianu , [8] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 8 , AlexJitianu , [9] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 9 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          expected, dump);
  
  
      GitAccess.getInstance().setBranch("feature", Optional.empty());
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
  
      dump = dumpHistory(commitsCharacteristics);
  
      expected = 
          "[ Anotehr commit of feature branch. , {date} , Alex <alex_jitianu@sync.ro> , 10 , AlexJitianu , [4] ]\n" + 
          "[ Changed on feature branch. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [6] ]\n" + 
          "[ Feature branch commit. , {date} , Alex <alex_jitianu@sync.ro> , 6 , AlexJitianu , [5] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , [7] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 7 , AlexJitianu , [8] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 8 , AlexJitianu , [9] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 9 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          expected, dump);
    } finally {
      GitAccess.getInstance().cleanUp();
      waitForScheduler();
      flushAWT();
      FileUtil.deleteRecursivelly(wcTree);
      flushAWT();
    }
  }

  /**
   * The upstream branch is ahead. In the history we should present the upstream branch as well.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testHistory_UpstreamBranchAhead() throws Exception {
    
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistoryRemote");
    File remoteDir = new File("target/gen/GitHistoryTest_testHistoryRemote_RemoteRepo");
    
    FileUtil.deleteRecursivelly(wcTree);
    FileUtil.deleteRecursivelly(remoteDir);
    
    RepoGenerationScript.generateRepository(script, wcTree);
    Repository remoteRepository = null;
    try {
      
      
      Repository localRepository = GitAccess.getInstance().getRepository();
      remoteDir.mkdirs();
      GitAccess.getInstance().createNewRepository(remoteDir.getAbsolutePath());
      remoteRepository = GitAccess.getInstance().getRepository();
      bindLocalToRemote(localRepository, remoteRepository);
      
      GitAccess.getInstance().setRepositorySynchronously(localRepository.getWorkTree().getAbsolutePath());
      
      push("Alex", "");
      
      // Make the remote evolve.
      // Not sure why we need this sleep. Commit order is messed up with out it.
      Thread.sleep(1000); // NOSONAR
      GitAccess.getInstance().setRepositorySynchronously(remoteRepository.getWorkTree().getAbsolutePath());
      FileUtils.writeStringToFile(new File(remoteRepository.getWorkTree(), "root.txt"), "changed on the remote" , "UTF-8");
      GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "root.txt"));
      
      RepoGenerationScript.setUserCredentials();
      GitAccess.getInstance().commit("Change on the remote.");

      // Switch to local.
      GitAccess.getInstance().setRepositorySynchronously(localRepository.getWorkTree().getAbsolutePath());
      GitAccess.getInstance().fetch();
      
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());

      String dump = dumpHistory(commitsCharacteristics);

      String expected = 
          "[ Change on the remote. , {date} , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
          "";
      
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          "root.txt was created and changed in the last two commit",
          expected, dump);
    } finally {
      GitAccess.getInstance().closeRepo();
      remoteRepository.close();
    }
  }

  /**
     * Tests the history can be shown for a resource that is not modified.
     * 
     * @throws Exception
     */
    @Test
    public void testHistory_ResourceUntouched() throws Exception {
      URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");
      
      File wcTree = new File("target/gen/testHistory_ResourceUntouched");
      RepoGenerationScript.generateRepository(script, wcTree);
      
      try {
        GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
  
        List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, "root.txt", new RenameTracker());
        String dump = dumpHistory(commitsCharacteristics);
  
        String expected = "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n";
        expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
        assertEquals(
            "root.txt was created and changed in the last two commit",
            expected, dump);
      } finally {
        GitAccess.getInstance().closeRepo();
        FileUtil.deleteRecursivelly(wcTree);
      }
    }

    /**
     * <p><b>Description:</b> The file was renamed in the working copy but not yet committed.</p>
     * <p><b>Bug ID:</b> EXM-44300</p>
     *
     * @author alex_jitianu
     *
     * @throws Exception If it fails.
     */
    @Test
    public void testRenamedResource_LocalCopyRenamed() throws Exception {
      
      assertTrue(true);
      // see EXM-49462 
      
      /*
      URL script = getClass().getClassLoader().getResource("scripts/history_script_follow_rename_copy.txt");
      File wcTree = new File("target/gen/testRenamedResource_LocalCopyRenamed");
      
      generateRepositoryAndLoad(script, wcTree);

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(
    		  HistoryStrategy.CURRENT_BRANCH, "file_renamed_again.txt", new RenameTracker()); 
      //--------------------------
      // Tests the detection of rename paths.
      //---------------------------
      
      Repository repository = GitAccess.getInstance().getRepository();
      
      File local = new File(repository.getWorkTree(), "file_renamed_again.txt");
      File dest = new File(repository.getWorkTree(), "new_name.txt");
      
      org.eclipse.jgit.util.FileUtils.rename(local, dest);
      
      assertTrue("Copy failed.", dest.exists());
      
      
      String startId = commitsCharacteristics.get(2).getCommitId();
      ObjectId start = repository.resolve(startId);
      RevCommit older = repository.parseCommit(start);
      
      String headId = commitsCharacteristics.get(0).getCommitId();
      ObjectId end = repository.resolve(headId);
      RevCommit newer = repository.parseCommit(end);
      
      
      assertEquals("The file was renamed", older.getFullMessage());
      assertEquals("The file was changed", newer.getFullMessage());

      
      String newPathInWorkingCopy = RevCommitUtil.getNewPathInWorkingCopy(GitAccess.getInstance().getGit(), "file_renamed_again.txt", headId);
      
      assertEquals("new_name.txt", newPathInWorkingCopy);
      */

    }


    /**
     * <p><b>Description:</b> Identify and follow renames.</p>
     * <p><b>Bug ID:</b> EXM-45037</p>
     *
     * @author alex_jitianu
     *
     * @throws Exception If it fails.
     */
    @Test
    public void testRenamedResource() throws Exception {
      assertTrue(true);
      // see EXM-49462 
      /*
      URL script = getClass().getClassLoader().getResource("scripts/history_script_follow_rename_copy.txt");
      File wcTree = new File("target/gen/GitHistoryTest_testHistory");
      
      generateRepositoryAndLoad(script, wcTree);
    
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(
    		  HistoryStrategy.CURRENT_BRANCH, "file_renamed_again.txt", new RenameTracker());
    
      String dump = dumpHistory(commitsCharacteristics);
    
      String expected = 
          "[ The file was changed , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ The file was renamed and moved , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ The file was renamed , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ The file was moved , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
          "[ The file was added , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
          "";
      
      
      //--------------------------
      // Tests the detection of rename paths.
      //---------------------------
      
      Repository repository = GitAccess.getInstance().getRepository();
      
      String startId = commitsCharacteristics.get(2).getCommitId();
      ObjectId start = repository.resolve(startId);
      RevCommit older = repository.parseCommit(start);
      
      String headId = commitsCharacteristics.get(0).getCommitId();
      ObjectId end = repository.resolve(headId);
      RevCommit newer = repository.parseCommit(end);
      
      assertEquals("The file was renamed", older.getFullMessage());
      assertEquals("The file was changed", newer.getFullMessage());
    
      String matchingPath = RevCommitUtil.getNewPath(
          GitAccess.getInstance().getGit(), 
          older, 
          newer,
          "child/file_renamed.txt");
      
      assertEquals("file_renamed_again.txt", matchingPath);
      
      System.out.println("===========================");
      System.out.println("===========================");
      
      matchingPath = RevCommitUtil.getOldPath(
          GitAccess.getInstance().getGit(), 
          older, 
          newer,
          "file_renamed_again.txt");
      
      assertEquals("child/file_renamed.txt", matchingPath);
      
      System.out.println("===========================");
      System.out.println("===========================");
      
      
      //--------------------------
      //---------------------------
      
    
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
    
      assertEquals(
          expected, dump);
      
    
      CommitCharacteristics cc = commitsCharacteristics.get(2);
      
      List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(cc.getCommitId());
      assertEquals("[(changeType=RENAME, fileLocation=child/file_renamed.txt)]", changedFiles.toString());
      
      Action action = getCompareWithWCAction(changedFiles.get(0), cc);
      assertEquals("Compare_file_with_working_tree_version", action.getValue(Action.NAME).toString());
      
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());
      
      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      // This is the current name and location of the renamed/moved file.
      File wcTreeCopy = new File(wcTree, "file_renamed_again.txt");
      assertEquals(wcTreeCopy.toURI().toURL().toString(), left.toString());
      assertEquals("git://" + cc.getCommitId() + "/child/file_renamed.txt", right.toString());
     */
    }
    
}
