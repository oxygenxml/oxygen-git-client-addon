package com.oxygenxml.git.view;

import java.util.Arrays;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitControllerBase;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases for refreshing the Project view.
 * 
 * @author sorin_carbunaru
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.swing.*")
public class WorkingCopySelectorTest extends JFCTestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    StandalonePluginWorkspace pluginWorkspace = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWorkspace.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    
    OptionsManager optMngMock = PowerMockito.mock(OptionsManager.class);
    Whitebox.setInternalState(OptionsManager.class, "instance", optMngMock);
    PowerMockito.when(optMngMock.getRepositoryEntries()).thenReturn(
        Arrays.asList(
            "D:/folder/WC1",
            "D:/folder/WC2",
            "D:/folder_doi/WC"));
    PowerMockito.when(optMngMock.getSelectedRepository()).thenReturn("D:/folder/WC1");
    
  }

  /**
   * <p><b>Description:</b> test the "Clear history..." action.</p>
   * <p><b>Bug ID:</b> EXM-44205</p>
   * 
   * @author sorin_carbunaru
   * 
   * @throws Exception When failing.
   */
  @Test
  public void testClearHistory() throws Exception {
    JFrame frame = new JFrame();
    try {
      WorkingCopySelectionPanel wcPanel = new WorkingCopySelectionPanel(Mockito.mock(GitControllerBase.class));
      frame.getContentPane().add(wcPanel);
      frame.pack();
      SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
      sleep(150);
      
      wcPanel.initializeWorkingCopyCombo();
      JComboBox<String> workingCopyCombo = wcPanel.getWorkingCopyCombo();
      ComboBoxModel<String> model = workingCopyCombo.getModel();
      StringBuilder sb = new StringBuilder();
      for (int i = 0 ; i < model.getSize(); i++) {
        sb.append(model.getElementAt(i)).append("\n");
      }
      assertEquals(
          "D:/folder/WC1\n" + 
          "D:/folder/WC2\n" + 
          "D:/folder_doi/WC\n" + 
          "CLEAR_HISTORY\n",
          sb.toString());
      
      SwingUtilities.invokeAndWait(() -> workingCopyCombo.setSelectedItem("CLEAR_HISTORY"));
      sleep(150);
      
      sb = new StringBuilder();
      for (int i = 0 ; i < model.getSize(); i++) {
        sb.append(model.getElementAt(i)).append("\n");
      }
      assertEquals(
          "D:/folder/WC1\n",
          sb.toString());
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }

}
