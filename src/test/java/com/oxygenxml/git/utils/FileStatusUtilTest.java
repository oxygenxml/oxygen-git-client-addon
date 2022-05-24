package com.oxygenxml.git.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;

import junit.framework.TestCase;

/**
 * @author Alex_Smarandache
 */
public class FileStatusUtilTest extends TestCase {

  private List<FileStatus> files;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    files = new ArrayList<>();
    files.add(new FileStatus(GitChangeType.CHANGED, "folder/project.xpr"));
    files.add(new FileStatus(GitChangeType.CHANGED, "folder/project.java"));
    files.add(new FileStatus(GitChangeType.ADD, "file.xpr"));
    files.add(new FileStatus(GitChangeType.ADD, "file.java"));
  }
  
  @Test
  public void testRemoveFilesByExtension() {
    assertEquals(4, files.size());
    FileStatusUtil.removeFilesByExtension(files, ".xpr");
    assertEquals(2, files.size());
    assertEquals("folder/project.java", files.get(0).getFileLocation());
    assertEquals("file.java", files.get(1).getFileLocation());
  }



}
