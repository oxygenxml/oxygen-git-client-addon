package com.oxygenxml.git.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;

/**
 * Some situations that can happen while executing a pull.
 */
public class GitPullCasesTest extends GitTestBase {
  
  /**
   * Pull (merge). There are uncommitted changes that overlap with the incoming changes.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Merge() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    PullResponse pull = instance.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("original", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "changed in local 2");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    instance.setRepositorySynchronously(local1Repository);
    setFileContent(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.pull().get();
    
    assertEquals("[test.txt]", filesWithChanges.toString());
    assertEquals("Status: STARTED, message: Pull_In_Progress\n" + 
        "Status: FINISHED, message: \n" + 
        "", b.toString());
    
    filesWithChanges.clear();
    // Commit.
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Another");
    instance.push("", "");
    
    pc.pull().get();
    
    assertTrue(filesWithChanges.isEmpty());
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflicts.toString());
  }
  
  /**
   * Pull (rebase). There are uncommitted changes that overlap with the incoming changes.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-rebase-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-rebase-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullWithConflicts-rebase-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    PullResponse pull = instance.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("original", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "changed in local 2");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Al doilea");
    instance.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    instance.setRepositorySynchronously(local1Repository);
    setFileContent(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.pull(PullType.REBASE).get();
    
    assertEquals("[test.txt]", filesWithChanges.toString());
    assertEquals("Status: STARTED, message: Pull_In_Progress\n" + 
        "Status: FINISHED, message: \n" + 
        "", b.toString());
    
    filesWithChanges.clear();
    // Commit.
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Another");
    instance.push("", "");
    
    pc.pull(PullType.REBASE).get();
    
    assertTrue(filesWithChanges.isEmpty());
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflicts.toString());
  }
  
  /**
   * <p><b>Description:</b> A file is modified both in the remote and the local repository. The same file
   * is changed inside the working copy.</p>
   * <p><b>Bug ID:</b> EXM-41770</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testPullWithConflicts_EXM_41770() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts_EXM_41770-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullWithConflicts_EXM_41770-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullWithConflicts_EXM_41770-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    // Second change.
    setFileContent(local1File, "local1-changed");
    instance.commit("Al doilea");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    PullResponse pull = instance.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("original", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "changed in local 2");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
  
    //----------------
    // LOCAL 1
    //----------------
    
    instance.setRepositorySynchronously(local1Repository);
    setFileContent(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.pull().get();
    
    assertEquals("[test.txt]", filesWithChanges.toString());
    assertEquals("Status: STARTED, message: Pull_In_Progress\n" + 
        "Status: FINISHED, message: \n" + 
        "", b.toString());
    
    filesWithChanges.clear();
    // Commit.
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Another");
    instance.push("", "");
    
    pc.pull().get();
    
    assertTrue(filesWithChanges.isEmpty());
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflicts.toString());
  }

  /**
   * <p><b>Description:</b> test that we use rebase instead of merge when pulling.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testPullRebase() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullRebase-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullRebase-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullRebase-remote";
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repo = createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repo, remoteRepo);
    
    // LOCAL 1
    GitAccess gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(local1Repository);
    File file = new File(local1Repository, "test.txt");
    setFileContent(file, "1\n2\n3");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First commit");
    gitAccess.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(local2Repository);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("1\n2\n3", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "11\n2\n3");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Edited first line");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    gitAccess.setRepositorySynchronously(local1Repository);
    setFileContent(file, "1\n2\n33");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Edited last line");
    
    // Now pull with rebase 
    gitAccess.pull("", "", PullType.REBASE);
    sleep(200);
    
    File firstRepofile = new File( new File(local1Repository), "test.txt");
    assertEquals("11\n2\n33", read(firstRepofile.toURI().toURL()));
    
    Git git = GitAccess.getInstance().getGitForTests();
    Iterable<RevCommit> commits = git.log().add(local1Repo.resolve("refs/heads/master")).call();
    StringBuilder sb = new StringBuilder("Commits from last to first:\n");
    for (RevCommit commit : commits) {
      sb.append("    ").append(commit.getFullMessage()).append("\n");
    }
    assertEquals(
        "Commits from last to first:\n" + 
        "    Edited last line\n" + 
        "    Edited first line\n" + 
        "    First commit\n" + 
        "", sb.toString());

  }
  
  /**
   * <p><b>Description:</b> test that we use merge when pulling.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testPullMerge() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullRebase-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullRebase-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullRebase-remote";
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repo = createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repo, remoteRepo);
    
    // LOCAL 1
    GitAccess gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(local1Repository);
    File file = new File(local1Repository, "test.txt");
    setFileContent(file, "1\n2\n3");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First commit");
    gitAccess.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(local2Repository);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("1\n2\n3", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "11\n2\n3");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Edited first line");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    gitAccess.setRepositorySynchronously(local1Repository);
    setFileContent(file, "1\n2\n33");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Edited last line");
    
    // Now pull to merge
    gitAccess.pull("", "");
    sleep(200);
    
    File firstRepofile = new File( new File(local1Repository), "test.txt");
    assertEquals("11\n2\n33", read(firstRepofile.toURI().toURL()));
    
    Git git = GitAccess.getInstance().getGitForTests();
    Iterable<RevCommit> commits = git.log().add(local1Repo.resolve("refs/heads/master")).call();
    StringBuilder sb = new StringBuilder("Commits from last to first:\n");
    for (RevCommit commit : commits) {
      sb.append("    ").append(commit.getFullMessage()).append("\n");
    }
    String repoURI = new File("target/test-resources/GitPullCasesTest/testPullRebase-remote/.git/")
        .toURI()
        .toString();
    
    System.err.println();
    System.err.println("================================");
    System.err.println("repoURI.startsWith(\"file:/\"): " + repoURI.startsWith("file:/"));
    System.err.println("!repoURI.startsWith(\"file:///\"): " + !repoURI.startsWith("file:///"));
    System.err.println("System.getProperty(\"os.name\").toUpperCase(): " + System.getProperty("os.name").toUpperCase());
    System.err.println("isWin: " + System.getProperty("os.name").toUpperCase().startsWith("WIN"));
    System.err.println("================================");
    System.err.println();
    
    if (repoURI.startsWith("file:/")
        && !repoURI.startsWith("file:///")
        // Only on Windows
        && System.getProperty("os.name").toUpperCase().startsWith("WIN")) {
      System.err.println("OLD: " + repoURI);
      repoURI = "file:///" + repoURI.substring("file:/".length());
      System.err.println("NEW: " + repoURI);
    }
    assertEquals(
        "Commits from last to first:\n" + 
        "    Merge branch 'master' of " + repoURI + "\n" + 
        "    Edited last line\n" + 
        "    Edited first line\n" + 
        "    First commit\n" + 
        "", sb.toString());
  }
  
  @Test
  public void testPullWithRebase_UncommittedNewFileConflict() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullWithRebase_UncommittedNewFileConflict-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullWithRebase_UncommittedNewFileConflict-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullWithRebase_UncommittedNewFileConflict-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    File local2File = new File( new File(local2Repository), "test.txt");
    local2File.createNewFile();
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
        messages.add(message);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.pull(PullType.REBASE).get();

    assertEquals(
        "[Pull_rebase_failed_because_conflicting_paths] FOR [test.txt]",
        messages + " FOR " + filesWithChanges);
  }
  
  @Test
  public void testPullWithMerge_UncommittedNewFileConflict() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullWithMerge_UncommittedNewFileConflict-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullWithMerge_UncommittedNewFileConflict-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullWithMerge_UncommittedNewFileConflict-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    File local2File = new File( new File(local2Repository), "test.txt");
    local2File.createNewFile();
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
        messages.add(message);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.pull().get();

    assertEquals(
        "[Pull_would_overwrite_uncommitted_changes] FOR [test.txt]",
        messages + " FOR " + filesWithChanges);
  }
  
  /**
   * We had changes in X and Y locally, and an incoming change from remote on X. 
   * We committed X and tried to pull with rebase, and we got into this situation...
   * 
   * @throws Exception
   */
  @Test
  public void testPullRebaseUncommittedChanges() throws Exception {
    String local1Repository = "target/test-resources/GitPullCasesTest/testPullRebaseUncommittedChanges-rebase-local";
    String local2Repository = "target/test-resources/GitPullCasesTest/testPullRebaseUncommittedChanges-rebase-local-second";
    String remoteRepository = "target/test-resources/GitPullCasesTest/testPullRebaseUncommittedChanges-rebase-remote";
    
    Repository local1Repo = createRepository(local1Repository);
    Repository local2Repos= createRepository(local2Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    
    bindLocalToRemote(local1Repo, remoteRepo);
    bindLocalToRemote(local2Repos, remoteRepo);
    
    //----------------
    // LOCAL 1
    //----------------
    GitAccess instance = GitAccess.getInstance();
    instance.setRepositorySynchronously(local1Repository);
    // Create a file in the remote.
    File remoteParent = new File(local1Repository);
    remoteParent.mkdirs();
    File local1File = new File(local1Repository, "test.txt");
    setFileContent(local1File, "original");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Primul");
    instance.push("", "");
    
    File local1_2File = new File(local1Repository, "test_1_2.txt");
    setFileContent(local1_2File, "original 1 2");
    instance.add(new FileStatus(GitChangeType.ADD, "test_1_2.txt"));
    instance.commit("Primul 1 2");
    instance.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    instance.setRepositorySynchronously(local2Repository);
    PullResponse pull = instance.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File( new File(local2Repository), "test.txt");
    assertEquals("original", read(local2File.toURI().toURL()));
    
    setFileContent(local2File, "changed in local 2");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Al doilea");
    instance.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    final StringBuilder pullWithConflicts = new StringBuilder();
    final List<String> filesWithChanges = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> files, String message) {
        filesWithChanges.addAll(files);
        messages.add(message);
      };
      
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflicts.append(response);
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // FIRST REPO
    instance.setRepositorySynchronously(local1Repository);
    setFileContent(local1File, "changed in local 1");
    instance.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    instance.commit("Another");
    
    // Another change, uncommitted
    setFileContent(local1_2File, "updated 1 2");
    
    pc.pull(PullType.REBASE).get();
    
    assertEquals(
        "[Pull_rebase_failed_because_uncommitted] FOR [test_1_2.txt]",
        messages + " FOR " + filesWithChanges);
  }
  
}
