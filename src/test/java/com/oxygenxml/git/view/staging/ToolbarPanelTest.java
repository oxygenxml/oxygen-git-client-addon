package com.oxygenxml.git.view.staging;

import org.mockito.Mockito;

import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class ToolbarPanelTest extends TestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
  }
  
  @Override
  protected void tearDown() throws Exception {
    PluginWorkspaceProvider.setPluginWorkspace(null);
    
    super.tearDown();
  }

  /**
   * <p><b>Description:</b> get the number of skipped commits in push/pull tooltips.</p>
   * <p><b>Bug ID:</b> EXM-44564</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testNoOfSkippedCommitsInPushPullToolbars() throws Exception {
	final GitController gitController = new GitController();
    final ToolbarPanel toolbarPanel = new ToolbarPanel(gitController, 
    		new GitActionsManager(gitController, null, null, null));
    assertEquals(0, toolbarPanel.getNoOfSkippedCommits(5));
    assertEquals(1, toolbarPanel.getNoOfSkippedCommits(6));
    assertEquals(2, toolbarPanel.getNoOfSkippedCommits(7));
  }
  
}
