package com.oxygenxml.git.view;

import java.io.File;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.Command;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class FlatView2Test extends FlatViewTestBase {

  /**
   * <p><b>Description:</b> lock fail test.</p>
   * <p><b>Bug ID:</b> EXM-42867</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  public void testAPullCannotLockRef() throws Exception {
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    try {
      final boolean[] showErrorMessageCalled = new boolean[] {false};
      
      StandalonePluginWorkspace pluginWorkspaceMock = Mockito.mock(StandalonePluginWorkspace.class);
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          showErrorMessageCalled[0] = true;
          return null;
        }
      }).when(pluginWorkspaceMock).showErrorMessage(Mockito.anyString());
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspaceMock);
      
      String localTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_local_pullCannotLock";
      String remoteTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_remote_pullCannotLock";

      // Create repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo = createRepository(localTestRepository);
      bindLocalToRemote(localRepo , remoteRepo);

      // Create a new file and push it.
      String fileName = "test.txt";
      File file = commitNewFile(localTestRepository, fileName, "content");
      PushResponse push = gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Create lock files
      String repoDir = gitAccess.getRepository().getDirectory().getAbsolutePath();
      Ref ref = gitAccess.getRemoteBrachListForCurrentRepo().get(0);
      File lockFile = new File(repoDir, ref.getName() + ".lock");
      boolean createNewFile = lockFile.createNewFile();
      assertTrue("Unnable to create lock file " + lockFile.getAbsolutePath(), createNewFile);
      setFileContent(lockFile, gitAccess.getLastLocalCommit().getName());

      // Commit a new version of the file.
      setFileContent(file, "modified");
      gitAccess.add(new FileStatus(GitChangeType.ADD, fileName));
      gitAccess.commit("modified");
      gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Pull should throw "Lock failed" error
      PullResponse pullResponse = gitAccess.pull("", "");
      assertEquals(PullStatus.LOCK_FAILED, pullResponse.getStatus());
      assertTrue(showErrorMessageCalled[0]);
      stagingPanel.getPushPullController().execute(Command.PULL);
      sleep(300);
      assertEquals("Lock_failed", stagingPanel.getCommitPanel().getStatusLabel().getText());
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    }
  }
}
