package com.oxygenxml.git.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

public class GitAccessRestoreLastCommitTest extends TestCase {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessRestoreLastCommitTest";
	private GitAccess gitAccess =  GitAccess.getInstance();
	
	public GitAccessRestoreLastCommitTest() {
	  StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage mockedWsOptionsStorage = Mockito.mock(WSOptionsStorage.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(mockedWsOptionsStorage).setOption(Mockito.anyString(), Mockito.any());
    
    Mockito.doAnswer(new Answer<WSOptionsStorage>() {
      @Override
      public WSOptionsStorage answer(InvocationOnMock invocation) throws Throwable {
        return mockedWsOptionsStorage;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getOptionsStorage();
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(pluginWSMock.getXMLUtilAccess()).thenReturn(xmlUtilAccess);

    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    });
  }

	@Override
	protected void setUp() throws Exception {
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");

		try {
			file.createNewFile();
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
		gitAccess.commit("file test added");
	}
	
	@Override
	protected void tearDown() {
	  gitAccess.cleanUp();
	  File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
	  FileUtil.deleteRecursivelly(dirToDelete);
	}

	public void testRestoreLastCommit() throws IOException {
		String actual = getFileContent();
		
		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("hellllo");
		out.close();

		gitAccess.restoreLastCommitFile(Arrays.asList("test.txt"));
		String expected = getFileContent();
		assertEquals(actual, expected);
	}

	private String getFileContent() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(LOCAL_TEST_REPOSITPRY + "/test.txt");
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
