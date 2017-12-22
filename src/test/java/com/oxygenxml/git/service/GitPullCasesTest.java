package com.oxygenxml.git.service;

import java.io.File;


import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;

/**
 * Some situations that can happen while executing a pull.
 */
public class GitPullCasesTest extends GitTestBase {
  
  /**
   * There are uncommitted changes that overlap with the incoming changes.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts() throws Exception {
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
    instance.setRepository(local1Repository);
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
    instance.setRepository(local2Repository);
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
    
    instance.setRepository(local1Repository);
    setFileContent(local1File, "changed in local 1");
    
    final org.eclipse.jgit.api.errors.CheckoutConflictException[] ex = new 
        org.eclipse.jgit.api.errors.CheckoutConflictException[1];
    PushPullController pc = new PushPullController() {
      protected void showPullFailedBecauseofConflict(org.eclipse.jgit.api.errors.CheckoutConflictException e) {
        ex[0] = e;
      };
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    pc.execute(Command.PULL).join();
    
    assertNotNull(ex[0]);
    assertEquals("[test.txt]", ex[0].getConflictingPaths().toString());
    assertEquals("Status: STARTED, message: Pull_In_Progress\n" + 
        "Status: FINISHED, message: \n" + 
        "", b.toString());
  }
  
}
