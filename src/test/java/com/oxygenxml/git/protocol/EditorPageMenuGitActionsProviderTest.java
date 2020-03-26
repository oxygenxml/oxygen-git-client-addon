package com.oxygenxml.git.protocol;

import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;

import com.oxygenxml.git.EditorPageMenuGitActionsProvider;
import com.oxygenxml.git.service.GitTestBase;

public class EditorPageMenuGitActionsProviderTest extends GitTestBase {
  
  /**
   * <p><b>Description:</b> avoid NPE when invoking the context menu 
   * on an editor corresponding to a remote file.</p>
   * <p><b>Bug ID:</b> EXM-45234</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testAvoidNPEForRemoteEditorContextMenu() throws Exception {
    EditorPageMenuGitActionsProvider provider = new EditorPageMenuGitActionsProvider(null);
    try {
      List<AbstractAction> actions = provider.getActionsForCurrentEditorPage(new URL("http://ceva.com/aaa.ext"));
      assertEquals("[]", actions.toString());
    } catch (NullPointerException e) {
      fail(e.getMessage());
    }
  }

}
