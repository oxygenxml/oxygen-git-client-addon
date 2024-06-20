package com.oxygenxml.git.view.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog.DestinationPathUpdater;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
  
  /**
   * Tests the information label from a @CloneRepositoryDialog.
   */
  @Test
  public void testInformationLabel() {
    try {
      final StandalonePluginWorkspace pluginWS = Mockito.mock(StandalonePluginWorkspace.class);
      WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
      Mockito.when(pluginWS.getOptionsStorage()).thenReturn(wsOptions);
      Mockito.when(pluginWS.getParentFrame()).then((Answer<Object>) 
          invocation -> {
            return new JFrame();
          }); 
      PluginWorkspaceProvider.setPluginWorkspace(pluginWS);
      final JLabel errorInfo = new CloneRepositoryDialog().getInformationLabel();
      String givenText = "12345";
      String expectedText = givenText;
      errorInfo.setText(givenText);
      assertEquals(expectedText, errorInfo.getText());
      assertEquals(givenText, errorInfo.getToolTipText());
      final int noOfAppendsForExpectedText = CloneRepositoryDialog.ERROR_MESSAGE_MAX_LENGTH / givenText.length();
      final StringBuilder strBuilder = new StringBuilder();
      for(int i = 1; i < 31; i++) {
        strBuilder.append(givenText);
        if(i == noOfAppendsForExpectedText) {
          expectedText = strBuilder.toString() + CloneRepositoryDialog.THREE_DOTS;
        }
      }
      givenText = strBuilder.toString();
      errorInfo.setText(givenText);
      assertEquals(expectedText, errorInfo.getText());
      assertEquals(givenText, errorInfo.getToolTipText());
      givenText = "";
      errorInfo.setText(givenText);
      assertEquals("", errorInfo.getText());
      assertNull(errorInfo.getToolTipText());

      givenText = null;
      errorInfo.setText(givenText);
      assertNull(errorInfo.getText());
      assertNull(errorInfo.getToolTipText());
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(null);
    }
  }
  
}
