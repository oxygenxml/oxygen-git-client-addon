package com.oxygenxml.git.service;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * Controller level tests.
 */
public class GitControllerTest {
  @Rule
  public Timeout globalTimeout= new Timeout(10, TimeUnit.SECONDS);


  /**
   * For asynchronous tasks we always issue fail events when exceptions are thrown.
   * 
   * @throws Exception
   */
  @Test
  public void testFailNotification() throws Exception {
    GitControllerBase ctrl = new GitController(GitAccess.getInstance());
    
    StringBuilder b = new StringBuilder();
    ctrl.addGitListener(new GitEventListener() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        b.append("E: " + info.toString()).append("\n");
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        b.append("F: ").append(info.toString()).append("\n");
        b.append("   ").append(t.getClass()).append("\n");
      }
      
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        b.append("S: ").append(info.toString()).append("\n");
      }
    });
    
    ScheduledFuture<?> future = ctrl.async(() -> {
      GitAccess.getInstance().createBranch(null);
    });
    
    future.get();
    
    Assert.assertEquals("Not the expected events", 
        "S: BranchGitEventInfo [Operation: CREATE_BRANCH, branch: null].\n" + 
        "F: BranchGitEventInfo [Operation: CREATE_BRANCH, branch: null].\n" + 
        "   class java.lang.NullPointerException\n" + 
        "", b.toString());
  }


  /**
   * For asynchronous tasks we always issue fail events when exceptions are thrown.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testFailPushNotification() throws Exception {
    GitAccess mock = Mockito.mock(GitAccess.class);
    // When Push will be invoked a runtime exception is thrown.
    GitController ctrl = new GitController(mock);
    
    StringBuilder b = new StringBuilder();
    ctrl.addGitListener(new GitEventListener() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        b.append("E: " + info.toString()).append("\n");
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        b.append("F: ").append(info.toString()).append("\n");
        b.append("   ").append(t.getClass()).append("\n");
      }
      
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        b.append("S: ").append(info.toString()).append("\n");
      }
    });
    
    Future<?> push = ctrl.push();
    push.get();
    
    Assert.assertEquals("Not the expected events", 
        "S: Status: null, message: Push_In_Progress\n" + 
        "F: Status: null, message: Push_failed: null\n" + 
        "   class java.lang.NullPointerException\n" + 
        "", b.toString());
  }
}
