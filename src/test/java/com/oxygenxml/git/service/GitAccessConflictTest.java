package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jgit.lib.RepositoryState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class GitAccessConflictTest extends GitAccessPullTest {
  
  private String[] shownWarningMess = new String[1];
  
  @Override
  @Before
  public void init() throws Exception {
    super.init();
    
    StandalonePluginWorkspace pluginWorkspaceMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspaceMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        shownWarningMess[0] = message;
        return null;
      }
    }).when(pluginWorkspaceMock).showWarningMessage(Mockito.anyString());
    shownWarningMess[0] = "";
  }

	@Test
	public void testResolveUsingTheirs() throws Exception {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		FileStatus fileStatus = new FileStatus(GitChangeType.ADD, "test.txt");
    gitAccess.add(fileStatus);
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		
		gitAccess.reset(fileStatus);
		gitAccess.updateWithRemoteFile("test.txt");
		gitAccess.add(fileStatus);
		
		String expected = "hellllo";
		String actual = getFileContent();
		assertEquals(expected, actual);
		
		// Pulling now will say that the merge was not concluded and we should commit
    assertEquals(RepositoryState.MERGING_RESOLVED, gitAccess.getRepository().getRepositoryState());

    PushPullController ppc = new PushPullController();
    ppc.execute(Command.PULL);
    Thread.sleep(1200);

    assertEquals("Conclude_Merge_Message", shownWarningMess[0]);
	}

	@Test
	public void testRestartMerge() throws Exception {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		for (FileStatus fileStatus : gitAccess.getUnstagedFiles()) {
			if(fileStatus.getChangeType() == GitChangeType.CONFLICT){
				gitAccess.add(fileStatus);
			}
		}
		RepositoryState actual = gitAccess.getRepository().getRepositoryState();
		RepositoryState expected = RepositoryState.MERGING_RESOLVED;
		assertEquals(expected, actual);
		
		gitAccess.restartMerge();
		actual = gitAccess.getRepository().getRepositoryState();
		expected = RepositoryState.MERGING;
		assertEquals(expected, actual);
	}

	private String getFileContent() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		BufferedReader br = new BufferedReader(fr);

		String sCurrentLine;

		String content = "";
		while ((sCurrentLine = br.readLine()) != null) {
			content += sCurrentLine;
		}
		br.close();
		fr.close();
		return content;
	}
}
