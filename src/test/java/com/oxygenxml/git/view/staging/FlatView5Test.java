package com.oxygenxml.git.view.staging;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.mockito.Mockito;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases.
 */
public class FlatView5Test extends FlatViewTestBase {
  
  
  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger logger = LogManager.getLogger(FlatView5Test.class.getName());

  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was not yet pushed. Edit only the commit message.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testAmendCommitThatWasNotPushed_editCommitMessage() throws Exception {
    String localTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editCommitMessage_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editCommitMessage_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "content");
    
    // Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeAndWait(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    RevCommit firstCommit = getLastCommit();
    
    assertFalse(commitPanel.getCommitButton().isEnabled());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(false));
    flushAWT();
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    assertTrue(commitPanel.getCommitButton().isEnabled());
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitMessageArea().setText("EDITED MESSAGE"));
    SwingUtilities.invokeLater(() -> commitPanel.getCommitButton().doClick());
    waitForScheluerBetter();
    flushAWT();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    RevCommit lastCommit = getLastCommit();
    final List<DiffEntry> diffs = GitAccess.getInstance().getGit().diff()
        .setOldTree(prepareTreeParser(GitAccess.getInstance().getRepository(), firstCommit.getName()))
        .setNewTree(prepareTreeParser(GitAccess.getInstance().getRepository(), lastCommit.getName()))
        .call();
    assertEquals(0, diffs.size());
    assertEquals("EDITED MESSAGE", lastCommit.getFullMessage());
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was pushed. Edit commit message.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testAmendCommitThatWasPushed_editCommitMesage() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testAmendCommitThatWasPushed_2_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasPushed_2_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "content");
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // >>> Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    // >>> Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    // >>> Push
    push("", "");
    waitForScheluerBetter();
    refreshSupport.call();
    waitForScheluerBetter();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("SECOND COMMIT MESSAGE");
      });
    flushAWT();
    
    PluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    // The amend was cancelled. We must not see the first commit message.
    assertEquals("SECOND COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    // Not enabled because we have don't have staged files.
    assertFalse(commitPanel.getCommitButton().isEnabled());
    assertFalse(amendBtn.isSelected());
  }
  
  /**
   * Prepare tree parser.
   * 
   * @param repository Repository.
   * @param objectId   Commit.
   * 
   * @return Tree iterator.
   * 
   * @throws IOException
   */
  private AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
    try (RevWalk walk = new RevWalk(repository)) {
        RevCommit commit = walk.parseCommit(repository.resolve(objectId));
        RevTree tree = walk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree.getId());
        }
        walk.dispose();
        return treeParser;
    }
  }

}
