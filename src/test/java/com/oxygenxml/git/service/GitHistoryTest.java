package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

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
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");
    
    File wcTree = new File("target/gen/GitHistoryTest_testHistory");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      
      FileUtils.writeStringToFile(new File(wcTree, "root.txt"), "changed" , "UTF-8");
      
      GitAccess.getInstance().setRepository(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dump(commitsCharacteristics);
      System.out.println(dump);

      String expected = "[ Uncommitted changes , {date} , * , * , null , null ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
          "";
      
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
//      assertEquals(
//          expected, dump);


      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics("root.txt");

      dump = dump(commitsCharacteristics);

      expected = 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      assertEquals(
          "root.txt was created and changed in the last two commit",
          expected, dump);
    } finally {
      GitAccess.getInstance().close();
      
      FileUtils.deleteDirectory(wcTree);
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
      
      GitAccess.getInstance().setRepository(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
      assertEquals(5, commitsCharacteristics.size());
      
      Map<String, String> fileLists = new LinkedHashMap<>();
      commitsCharacteristics.stream().forEach(t -> {
        try {
          fileLists.put(getAssertableID(t.getCommitAbbreviatedId()), dumpFS(RevCommitUtil.getChangedFiles(t.getCommitId())));
        } catch (IOException | GitAPIException e) {
          fileLists.put(getAssertableID(t.getCommitAbbreviatedId()), e.getMessage());
        }
      });
      
      fileLists.keySet().stream().forEach(k -> {
        System.out.println(k);
        System.out.println(fileLists.get(k));
        System.out.println();
      });
      
      
      Map<String, String> expected = new LinkedHashMap<>();
      expected.put("*", "(changeType=MODIFIED, fileLocation=root.txt)\n");
      expected.put("1", "(changeType=CHANGED, fileLocation=root.txt)\n");
      expected.put("2", "(changeType=ADD, fileLocation=root.txt)\n");
      expected.put(
          "3", "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
          "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file3.txt)\n" + 
          "(changeType=ADD, fileLocation=f2/file3_renamed.txt)\n" + 
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
      GitAccess.getInstance().close();
      
      FileUtils.deleteDirectory(wcTree);
    }
  }

  private String dumpFS(List<FileStatus> changes) {
    StringBuilder b = new StringBuilder();
    changes.stream().forEach(t -> b.append(t.toString()).append("\n"));
    return b.toString();
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

    commitsCharacteristics.stream().forEach(t -> b.append(toString(t)).append("\n"));

    return b.toString();
  }
  
  /**
   * Maps Git revision IDs into predictable values that can be asserted in a test.
   */
  private Map<String, String> idMapper = new HashMap<String, String>();
  /**
   * Id generation counter.
   */
  private int counter = 1;
  
  /**
   * Maps Git revision IDs into predictable values that can be asserted in a test.
   * 
   * @param id Git commit id.
   * 
   * @return A value that can be asserted in a test.
   */
  private String getAssertableID(String id) {
    if (id == null || "*".equals(id)) {
      return id;
    }
    String putIfAbsent = idMapper.putIfAbsent(id, String.valueOf(counter));
    if (putIfAbsent == null) {
      counter ++;
    }
    
    return idMapper.get(id);
  }
  
  /**
   * Serialize the given commit.
   * 
   * @param c Commit data.
   * 
   * @return A string representation that can be asserted.
   */
  public String toString(CommitCharacteristics c) {
    return "[ " + c.getCommitMessage() + " , " + dumpDate(c) + " , " + c.getAuthor() + " , " + getAssertableID(c.getCommitAbbreviatedId()) + " , " 
        + c.getCommitter() + " , " + ( c.getParentCommitId() != null ? c.getParentCommitId().stream().map(id -> getAssertableID(id)).collect(Collectors.toList()) : null) + " ]";

  }

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");
  /**
   * Serializes the commit date into a "d MMM yyyy" format that can be asserted inside tests.
   * 
   * @param c Commit data.
   * 
   * @return A string representation.
   */
  private String dumpDate(CommitCharacteristics c) {
    return c.getDate() != null ? DATE_FORMAT.format(c.getDate()) : DATE_FORMAT.format(new Date());
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
      GitAccess.getInstance().setRepository(wcTree.getAbsolutePath());
      
      System.out.println(GitAccess.getInstance().getGitForTests().status().call().hasUncommittedChanges());
      
      GitAccess.getInstance().setBranch("master");
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dump(commitsCharacteristics);
  
      String expected = 
          "[ Changed on master branch. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
          "";
      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
      
      assertEquals(
          expected, dump);
      
      Map<String, List<String>> branchMap = GitAccess.getInstance().getBranchMap(GitAccess.getInstance().getRepository(), GitAccess.LOCAL);
      
      //---------------
      // Assert the branch names.
      //----------------
      final StringBuilder mapDump = new StringBuilder();
      branchMap.forEach((k,v ) -> {mapDump.append(getAssertableID(k)).append(" -> ").append(v.toString()).append("\n");});
      assertEquals(
          "6 -> [feature]\n" + 
          "1 -> [master]\n" + 
          "", mapDump.toString());
  
  
      GitAccess.getInstance().setBranch("feature");
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      dump = dump(commitsCharacteristics);
  
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
      GitAccess.getInstance().close();
      
      FileUtils.deleteDirectory(wcTree);
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
      GitAccess.getInstance().setRepository(wcTree.getAbsolutePath());
      
      System.out.println(GitAccess.getInstance().getGitForTests().status().call().hasUncommittedChanges());
      
      GitAccess.getInstance().setBranch("master");
      
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dump(commitsCharacteristics);
  
      String expected = 
          "[ Another commit on master. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Merge branch 'feature' , {date} , AlexJitianu <jitianualex83@gmail.com> , 2 , AlexJitianu , [3, 4] ]\n" + 
          "[ Changed on master branch. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [5] ]\n" + 
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
  
  
      GitAccess.getInstance().setBranch("feature");
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      dump = dump(commitsCharacteristics);
  
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
      GitAccess.getInstance().close();
      
      FileUtils.deleteDirectory(wcTree);
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
    RepoGenerationScript.generateRepository(script, wcTree);
    
    File remoteDir = new File("target/gen/GitHistoryTest_testHistoryRemote_RemoteRepo");
    Repository remoteRepository = null;
    try {
      
      Repository localRepository = GitAccess.getInstance().getRepository();
      remoteDir.mkdirs();
      GitAccess.getInstance().createNewRepository(remoteDir.getAbsolutePath());
      remoteRepository = GitAccess.getInstance().getRepository();
      bindLocalToRemote(localRepository, remoteRepository);
      
      GitAccess.getInstance().setRepository(localRepository.getWorkTree().getAbsolutePath());
      GitAccess.getInstance().push("Alex", "");
      
      // Make the remote evolve.
      // Not sure why we need this sleep. Commit order is messed up with out it.
      Thread.sleep(1000);
      GitAccess.getInstance().setRepository(remoteRepository.getWorkTree().getAbsolutePath());
      FileUtils.writeStringToFile(new File(remoteRepository.getWorkTree(), "root.txt"), "changed on the remote" , "UTF-8");
      GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "root.txt"));
      GitAccess.getInstance().commit("Change on the remote.");

      // Switch to local.
      GitAccess.getInstance().setRepository(localRepository.getWorkTree().getAbsolutePath());
      GitAccess.getInstance().fetch();
      
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dump(commitsCharacteristics);
      System.out.println(dump);

      String expected = 
          "[ Change on the remote. , {date} , AlexJitianu <jitianualex83@gmail.com> , 1 , AlexJitianu , [2] ]\n" + 
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
      GitAccess.getInstance().close();
      remoteRepository.close();
      
      FileUtils.deleteDirectory(wcTree);
      FileUtils.deleteDirectory(remoteDir);
    }
  
  }
}
