package com.oxygenxml.git.service;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.PushPullEvent;

/**
 * Test utility methods.
 * 
 * @author alex_jitianu
 */
public class TestUtil {
  /**
   * Utility class.
   */
  private TestUtil() {}
  
  /**
   * Adds a listener that will collect push events.
   * 
   * @param pc Git controller.
   * @param b Event collector.
   */
  public static void collectPushPullEvents(GitController pc, final StringBuilder b) {
    pc.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: STARTED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: FINISHED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info instanceof PushPullEvent) {
          b.append("Status: FAILED, message: ").append(((PushPullEvent) info).getMessage()).append("\n");
        }
      }
    });
  }
  
  /**
   * Changes and commits one file.
   * 
   * @return The created revision.
   * @throws Exception If it fails.
   */
  public static final RevCommit commitOneFile(Repository repository, String fileName, String fileContent) throws Exception {
    try (Git git = new Git(repository)) {
      PrintWriter out = new PrintWriter(new File(repository.getWorkTree(), fileName));
      out.println(fileContent);
      out.close();
      git.add().addFilepattern(fileName).call();
      return git.commit().setMessage("New file: " + fileName).call();
    }
  }
}
