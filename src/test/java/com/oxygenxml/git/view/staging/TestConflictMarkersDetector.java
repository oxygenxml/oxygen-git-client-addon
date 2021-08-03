package com.oxygenxml.git.view.staging;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the com.oxygenxml.git.view.staging.GitResourceContextualMenu.containsConflictMarkers(
 * final List<FileStatus> allSelectedResources, final File workingCopy) API
 * 
 * @author Alex_Smarandache
 *
 */
public class TestConflictMarkersDetector {

  /**
   * Tests the com.oxygenxml.git.view.staging.GitResourceContextualMenu.containsConflictMarkers(
   * final List<FileStatus> allSelectedResources, final File workingCopy) API
   * 
   * @throws IOException
   */
  @Test
  public void testConflictMarkersDetector() throws IOException {
    
    FileStatus file1 = new FileStatus(GitChangeType.CONFLICT, "file1.txt");
    FileStatus file2 = new FileStatus(GitChangeType.CONFLICT, "file2.txt");
    FileStatus file3 = new FileStatus(GitChangeType.CONFLICT, "file3.txt");
    File workingCopy = new File("src/test/resources/EXM-47777");

    List<FileStatus> files = new ArrayList<>();

    files.add(file1);
    assertTrue(GitResourceContextualMenu.containsConflictMarkers(files, workingCopy));
    files.add(file2);
    assertTrue(GitResourceContextualMenu.containsConflictMarkers(files, workingCopy));
    files.add(file3);
    assertTrue(GitResourceContextualMenu.containsConflictMarkers(files, workingCopy));
    files.remove(0);
    assertTrue(GitResourceContextualMenu.containsConflictMarkers(files, workingCopy));
    files.remove(1);
    assertFalse(GitResourceContextualMenu.containsConflictMarkers(files, workingCopy));
  }

}
