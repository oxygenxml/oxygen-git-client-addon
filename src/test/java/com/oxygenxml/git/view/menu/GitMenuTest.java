package com.oxygenxml.git.view.menu;

import static org.junit.Assert.assertEquals;

import javax.swing.JMenu;

import org.junit.Test;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.actions.GitActionsMenuBar;
import com.oxygenxml.git.view.event.GitController;

/**
 * This class is used for tests about Git menu.
 * 
 * @author alex_smarandache
 *
 */
public class GitMenuTest {
  
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
