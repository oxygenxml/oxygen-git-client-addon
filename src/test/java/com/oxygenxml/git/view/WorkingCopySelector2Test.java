package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.GitOperationScheduler;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases for working copy combo.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.swing.*")
public class WorkingCopySelector2Test extends JFCTestCase {
  
  /**
   * A good repository.
   */
  private File wcTree = new File("target/gen/WorkingCopySelector2Test");
  /**
   * A directory where there is no git repository.
   */
  private File badWcTree = new File("target/gen/WorkingCopySelector2Test_bad");
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    // super.setUp(); already mock the plugin workspace. We will just contribute to it. 
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
            wcTree.getAbsolutePath(),
            badWcTree.getAbsolutePath()));
    PowerMockito.when(optMngMock.getSelectedRepository()).thenReturn(wcTree.getAbsolutePath());

    GitAccess.getInstance().createNewRepository(wcTree.getAbsolutePath());
  }

  /**
   * <p><b>Description:</b> A non existing repository is removed from the combo.</p>
   * <p><b>Bug ID:</b> EXM-44820</p>
   * 
   * @author alex_jitianu
   * 
   * @throws Exception When failing.
   */
  @Test
  public void testRemoveStaleEntries() throws Exception {
    JFrame frame = new JFrame();
    try {
      WorkingCopySelectionPanel wcPanel = new WorkingCopySelectionPanel();
      frame.getContentPane().add(wcPanel);
      frame.pack();
      frame.setVisible(true);
      
      JComboBox<String> workingCopyCombo = wcPanel.getWorkingCopyCombo();
      ComboBoxModel<String> model = workingCopyCombo.getModel();
      StringBuilder sb = new StringBuilder();
      for (int i = 0 ; i < model.getSize(); i++) {
        sb.append(model.getElementAt(i)).append("\n");
      }
      
      assertEquals(
          wcTree.getAbsolutePath() + "\n" + 
              badWcTree.getAbsolutePath() + "\n" + 
              "CLEAR_HISTORY\n" + 
              "",
              sb.toString());

      // Select the bad WC.
      workingCopyCombo.setSelectedItem(badWcTree.getAbsolutePath());
      
      // Wait for the selection task to be completed by posting another task and waiting for its end.
      ScheduledFuture schedule = GitOperationScheduler.getInstance().schedule(() -> {});
      schedule.get();
      
      sb = new StringBuilder();
      for (int i = 0 ; i < model.getSize(); i++) {
        sb.append(model.getElementAt(i)).append("\n");
      }
      
      assertEquals(
          wcTree.getAbsolutePath() + "\n" + 
              "CLEAR_HISTORY\n" + 
              "",
              sb.toString());
      
      assertEquals(wcTree.getAbsolutePath(), workingCopyCombo.getSelectedItem().toString());
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }

}
