package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

import junit.framework.TestCase;
import ro.sync.basic.util.URLUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * @author Alex_Smarandache
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginWorkspaceProvider.class})
@PowerMockIgnore({"javax.management.*", "javax.script.*",  "javax.xml.*", "org.xml.*"})
public class FileHelperTest extends TestCase {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/FileHelperTest/local";


	/**
	 * Initialize pluginWorkspace.
	 *
	 * @throws IOException
	 */
	protected void setUp() throws Exception {
	  StandalonePluginWorkspace pluginWorkspace = Mockito.mock(StandalonePluginWorkspace.class);
		UtilAccess utilAccess = Mockito.mock(UtilAccess.class);
		Mockito.when(pluginWorkspace.getUtilAccess()).thenReturn(utilAccess);
		Mockito.when(utilAccess.createReader(Mockito.any(URL.class), Mockito.any(String.class)))
    		.then((Answer<FileReader>) invocation -> {
    		  File currentFile = URLUtil.getAbsoluteFileFromFileUrl((invocation.getArgument(0)));
    		  return new FileReader(currentFile);
    		});
		PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
	}
	
	protected void tearDown() {
	  try {
	    File dir = new File(LOCAL_TEST_REPOSITPRY);
	    FileUtils.deleteDirectory(dir);
	  } catch (IOException e) {
	    e.printStackTrace();
	  }
	}

	/**
	 * Tests the com.oxygenxml.git.utils.FileUtil.containsConflictMarkers(
	 * final List<FileStatus> allSelectedResources, final File workingCopy) API
	 *
	 */
	public void testConflictMarkersDetector() {

		FileStatus file1 = new FileStatus(GitChangeType.CONFLICT, "file1.txt");
		FileStatus file2 = new FileStatus(GitChangeType.CONFLICT, "file2.txt");
		FileStatus file3 = new FileStatus(GitChangeType.CONFLICT, "file3.txt");
		File workingCopy = new File("src/test/resources/EXM-47777");

		List<FileStatus> files = new ArrayList<>();

		files.add(file1);
		assertTrue(FileUtil.containsConflictMarkers(files, workingCopy));
		files.add(file2);
		assertTrue(FileUtil.containsConflictMarkers(files, workingCopy));
		files.add(file3);
		assertTrue(FileUtil.containsConflictMarkers(files, workingCopy));
		files.remove(0);
		assertTrue(FileUtil.containsConflictMarkers(files, workingCopy));
		files.remove(1);
		assertFalse(FileUtil.containsConflictMarkers(files, workingCopy));
	}

	public void testIsNotGitRepositoery() throws IOException{
		File file = new File(LOCAL_TEST_REPOSITPRY);
		
		file.mkdirs();
		
		boolean actual = FileUtil.isGitRepository(file.getAbsolutePath());
		boolean expected = false;
		
		assertEquals(expected, actual);
	}
	
	public void testIsGitRepositoery() throws IOException{
		File repositoryDirectory = new File(LOCAL_TEST_REPOSITPRY);
		repositoryDirectory.mkdirs();
		File gitDirectory = new File(LOCAL_TEST_REPOSITPRY + "/.git");
		gitDirectory.mkdirs();
		
		boolean actual = FileUtil.isGitRepository(repositoryDirectory.getAbsolutePath());
		boolean expected = true;
		
		assertEquals(expected, actual);
	}
	
	/**
	 * Test the {@link FileUtil#getCommonDir(java.util.Set)} method.
	 * 
	 * @throws IOException
	 */
  public void testGetCommonDirectory() throws IOException {
	  Set<File> files = new HashSet<>();
	  File firstFile = new File("/home/user1/tmp/coverage/test");
    files.add(firstFile);
	  files.add(new File("/home/user1/tmp/covert/operator"));
	  files.add(new File("/home/user1/tmp/coven/members"));
	  assertEquals(new File("/home/user1/tmp/").getCanonicalPath(), FileUtil.getCommonDir(files).getCanonicalPath());
	  
	  files.remove(firstFile);
	  files.add(new File("/notHome/user1/tmp/coverage/test"));
	  assertEquals(new File("/").getCanonicalPath(), FileUtil.getCommonDir(files).getCanonicalPath());
	}
}
