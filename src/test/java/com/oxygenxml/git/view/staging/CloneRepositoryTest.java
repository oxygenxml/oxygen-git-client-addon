package com.oxygenxml.git.view.staging;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog.DestinationPathUpdater;

/**
 * Tests the clone repository elements.
 * 
 * @author alex_smarandache
 *
 */
public class CloneRepositoryTest {

  /**
   * <p><b>Description:</b> If the proposed file is computed correct.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50538</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testDestinationPathUpdater() {
    final DestinationPathUpdater pathUpdater = new CloneRepositoryDialog.DestinationPathUpdater();
    String proposedDest = pathUpdater.updateDestinationPath(
        "https://github.com/oxygenxml/oxygen-git-client-addon", new File("folder/my_workspace").getAbsolutePath());
    String newDestination = new File("folder/my_workspace/oxygen-git-client-addon").getAbsolutePath();
    assertEquals(newDestination, proposedDest);
    // Test if the new location is also correct.
    proposedDest = pathUpdater.updateDestinationPath(
        "https://github.com/p2", new File(newDestination).getAbsolutePath());
    newDestination = new File("folder/my_workspace/p2").getAbsolutePath();
    assertEquals(newDestination, proposedDest);
    // test same location again
    proposedDest = pathUpdater.updateDestinationPath(
        "https://github.com/p2", new File(newDestination).getAbsolutePath());
    newDestination = new File("folder/my_workspace/p2").getAbsolutePath();
    assertEquals(newDestination, proposedDest);
  }
  
}
