package com.oxygenxml.git.view.menu;

import static org.junit.Assert.assertEquals;

import javax.swing.JMenu;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.actions.GitActionsMenuBar;
import com.oxygenxml.git.view.event.GitController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * This class is used for tests about Git menu.
 * 
 * @author alex_smarandache
 *
 */
public class GitMenuTest {
  
  @Before
  public void setUp() {
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
  }
  
  @After
  public void tearDown() {
    PluginWorkspaceProvider.setPluginWorkspace(null);
  }
  
  /**
   * <p><b>Description:</b> list the Settings menu actions.</p>
   * <p><b>Bug ID:</b> EXM-46442</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSettingsMenu() throws Exception {
    final JMenu settingsMenu = new GitActionsMenuBar(
        new GitActionsManager(new GitController(), null, null, null)).getSettingsMenu();
    assertEquals(3, settingsMenu.getItemCount());
    assertEquals(Tags.PREFERENCES, settingsMenu.getItem(0).getText());
    // item 1 is a separator
    assertEquals(Tags.RESET_ALL_CREDENTIALS, settingsMenu.getItem(2).getText());
  }

}
