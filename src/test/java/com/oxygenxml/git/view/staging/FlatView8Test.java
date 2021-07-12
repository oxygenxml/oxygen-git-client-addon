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
import org.junit.Before;
import org.junit.Test;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.CommitAndStatusPanel;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Test cases.
 */
public class FlatView8Test extends FlatViewTestBase {
  
  
  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger logger = LogManager.getLogger(FlatView8Test.class.getName());

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was not yet pushed. Edit the file content.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasNotPushed_editFileContent() throws Exception {
    String localTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editFileContent_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editFileContent_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    // Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    RevCommit firstCommit = getLastCommit();
    
    assertFalse(commitPanel.getCommitButton().isEnabled());
    
    // Change the file again.
    setFileContent(file, "modified");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitMessageArea().setText("REPLACE THIS, PLEASE"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(false));
    flushAWT();
    assertFalse(amendBtn.isSelected());
    assertEquals("REPLACE THIS, PLEASE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitButton().doClick());
    waitForScheluerBetter();
    flushAWT();
    sleep(500);
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    RevCommit lastCommit = getLastCommit();
    final List<DiffEntry> diffs = GitAccess.getInstance().getGit().diff()
        .setOldTree(prepareTreeParser(GitAccess.getInstance().getRepository(), firstCommit.getName()))
        .setNewTree(prepareTreeParser(GitAccess.getInstance().getRepository(), lastCommit.getName()))
        .call();
    assertEquals(1, diffs.size());
    
    DiffEntry diffEntry = diffs.get(0);
    assertEquals("DiffEntry[MODIFY test.txt]", diffEntry.toString());
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
