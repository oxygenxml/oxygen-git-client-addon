package com.oxygenxml.git.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.StagingResourcesTableCellRenderer;
import com.oxygenxml.git.view.staging.ToolbarPanel;
import com.oxygenxml.git.view.stash.FilesTableModel;
import com.oxygenxml.git.view.stash.ListStashesDialog;
import com.oxygenxml.git.view.stash.StashesTableModel;

import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Contains visual tests for stash support.
 * 
 * @author Alex_Smarandache
 *
 */
public class StashVisualTests extends GitTestBase {

  private final static String LOCAL_REPO = "target/test-resources/GitAccessStash/localRepository";
  private final static String REMOTE_REPO = "target/test-resources/GitAccessStash/remoteRepository";
  private final static String LOCAL_BRANCH = "LocalBranch";

  private GitAccess gitAccess;
  private StagingPanel stagingPanel;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();

    //Creates the remote repository.
    createRepository(REMOTE_REPO);
    Repository remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(LOCAL_REPO);
    Repository localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }
  

  /**
   * <p><b>Description:</b> Tests the "Stash" button basic characteristics and the "Stash Changes" functionality.</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testStashChanges() throws Exception {
  //Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
    
    
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
      // Test the "Stash" button tooltip text
      assertEquals(Tags.STASH, stashButton.getToolTipText());
      
      // Test if the button is disabled if none actions are possible.
      assertFalse(stashButton.isEnabled());
      
      makeLocalChange("new 2");
      
      JMenuItem stashChangesItem =  stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      // Stash changes and test if the actions become disabled.
      JDialog stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);
      flushAWT();
      JButton doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      flushAWT();
      toolbarPanel.refreshStashButton();
      flushAWT();
      assertFalse(stashChangesItem.isEnabled());
      
      // Test if the stash were created.
      List<RevCommit> stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(1, stashes.size());
      
      makeLocalChange("new 3");
      
      SwingUtilities.invokeLater(() -> {
        stashChangesItem.setSelected(true);
        stashChangesItem.getAction().actionPerformed(null);
      });
      
      flushAWT();
      
      // Test if the user can add a custom text
      stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);
      flushAWT();
      JTextField textField = TestUtil.findFirstTextField(stashChangesDialog);
      assertNotNull(textField);
      textField.setText("Some custom text by user.");
      flushAWT();
      doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      flushAWT();
      toolbarPanel.refreshStashButton();
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(2, stashes.size());
      assertEquals("Some custom text by user.", stashes.get(0).getFullMessage());
      
      makeLocalChange("new 4");
      
      SwingUtilities.invokeLater(() -> stashChangesItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      // Stash changes and test if the actions become disabled.
      stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);
      flushAWT();
      JButton cancelStashButton = findFirstButton(stashChangesDialog, Tags.CANCEL);
      assertNotNull(cancelStashButton);
      cancelStashButton.doClick();
      refreshSupport.call();
      flushAWT();
      toolbarPanel.refreshStashButton();
      flushAWT();
      assertTrue(stashChangesItem.isEnabled());
      
      // Test if the stash wasn't created.
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(2, stashes.size());
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "List stashes" delete all action</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testListStashDeleteAllAction() throws Exception {
    // Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
    
    
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
    
      initStashes(toolbarPanel);
      
      List<RevCommit> stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(3, stashes.size());
      
      JMenuItem[] listStashesItem =  new JMenuItem[1];
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));

      ListStashesDialog listStashesDialog = (ListStashesDialog)findDialog(Tags.STASHES);
      assertNotNull(listStashesDialog);
      assertEquals(3, listStashesDialog.getStashesTable().getModel().getRowCount());
      assertEquals(1, listStashesDialog.getAffectedFilesTable().getModel().getRowCount());
      JButton[] deleteAllStashesButton = new JButton[1];
      deleteAllStashesButton[0] = findFirstButton(listStashesDialog, Tags.DELETE_ALL);
      assertNotNull(deleteAllStashesButton);
      SwingUtilities.invokeLater(() -> deleteAllStashesButton[0].doClick());
      
      flushAWT();
      
      // Test the no button.
      JDialog deleteAllStashesDialog = findDialog(Tags.DELETE_ALL_STASHES);
      assertNotNull(deleteAllStashesDialog);
      JButton[] noButton = new JButton[1];
      flushAWT();
      noButton[0] = findFirstButton(deleteAllStashesDialog, Tags.NO);
      assertNotNull(noButton[0]);
      SwingUtilities.invokeLater(() -> noButton[0].doClick());
      flushAWT();
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(3, stashes.size());
      flushAWT();
      JButton cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
      
      listStashesItem[0] =  stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      listStashesDialog = (ListStashesDialog)findDialog(Tags.STASHES);
      assertNotNull(listStashesDialog);
      deleteAllStashesButton[0] = findFirstButton(listStashesDialog, Tags.DELETE_ALL);
      assertNotNull(deleteAllStashesButton);
      SwingUtilities.invokeLater(() -> deleteAllStashesButton[0].doClick());
      flushAWT();
      
      // Test the yes button.
      deleteAllStashesDialog = findDialog(Tags.DELETE_ALL_STASHES);
      assertNotNull(deleteAllStashesDialog);
      flushAWT();
      JButton yesButton = findFirstButton(deleteAllStashesDialog, Tags.YES);
      assertNotNull(yesButton);
      SwingUtilities.invokeLater(yesButton::doClick);
      flushAWT();
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(0, stashes.size());
      
      cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "List stashes" delete action</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testListStashDeleteAction() throws Exception {
    // Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
    
    
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
    
      initStashes(toolbarPanel);
      
      List<RevCommit> stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(3, stashes.size());
      
      JMenuItem[] listStashesItem =  new JMenuItem[1];
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      ListStashesDialog listStashesDialog = (ListStashesDialog) findDialog(Tags.STASHES);
      assertNotNull(listStashesDialog);
      assertEquals(3, listStashesDialog.getStashesTable().getModel().getRowCount());
      JButton[] deleteSelectedStashButton = new JButton[1];
      flushAWT();
      deleteSelectedStashButton[0] = findFirstButton(listStashesDialog, Tags.DELETE);
      assertNotNull(deleteSelectedStashButton);
      SwingUtilities.invokeLater(() -> deleteSelectedStashButton[0].doClick());
      
      flushAWT();
      
      // Test the no button.
      JDialog deleteSelectedStashDialog = findDialog(Tags.DELETE_STASH);
      assertNotNull(deleteSelectedStashDialog);
      JButton[] noButton = new JButton[1];
      flushAWT();
      noButton[0] = findFirstButton(deleteSelectedStashDialog, Tags.NO);
      assertNotNull(noButton[0]);
      SwingUtilities.invokeLater(() -> noButton[0].doClick());
      flushAWT();
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(3, stashes.size());
      
      listStashesItem[0] =  stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      // Test the yes button.
      String[] stashesMessages = {
          "Stash1", "Stash0"
      };
      // Delete all stashes one by one
      for(int i = 0; i < 3; i++) {
        flushAWT();
        deleteSelectedStashButton[0] = findFirstButton(listStashesDialog, Tags.DELETE);
        assertNotNull(deleteSelectedStashButton);
        SwingUtilities.invokeLater(() -> deleteSelectedStashButton[0].doClick());
        flushAWT();
        deleteSelectedStashDialog = findDialog(Tags.DELETE_STASH);
        assertNotNull(deleteSelectedStashDialog);
        flushAWT();
        JButton yesButton = findFirstButton(deleteSelectedStashDialog, Tags.YES);
        assertNotNull(yesButton);
        SwingUtilities.invokeLater(yesButton::doClick);
        flushAWT();
        stashes = new ArrayList<>(gitAccess.listStashes());
        assertEquals(3 - i - 1, stashes.size());
        assertEquals(3 - i - 1, listStashesDialog.getStashesTable().getRowCount());
        int stashIndex = 0;
        for(int j = i + 1; j < 3; j++) {
          assertEquals(stashesMessages[i + stashIndex], 
              stashes.get(stashIndex).getFullMessage());
          assertEquals(stashesMessages[i + stashIndex], 
              listStashesDialog.getStashesTable().getValueAt(stashIndex++, 1));
        }
      }
      
      flushAWT();
      JButton cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "List stashes" apply and pop actions</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testListStashApplyAndPopAction() throws Exception {
    // Init UI
    JFrame frame = new JFrame();
    GitController gitCtrl = new GitController(GitAccess.getInstance());
    stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
    ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
    frame.getContentPane().add(stagingPanel);
    frame.pack();
    frame.setVisible(true);
    flushAWT();
    toolbarPanel.refresh();
    refreshSupport.call();
    flushAWT();
    
    // Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
    
    try {
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
      
      makeLocalChange("some_modification");
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      JMenuItem[] stashChangesItem = new JMenuItem[1];
      stashChangesItem[0] = stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      
      // Stash changes and test if the actions become disabled.
      JDialog stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      flushAWT();
      assertNotNull(stashChangesDialog);
      JButton doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      assertEquals("local content", getFileContent(LOCAL_REPO + "/local.txt"));
      
      JMenuItem[] listStashesItem =  new JMenuItem[1];
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      ListStashesDialog listStashesDialog = (ListStashesDialog) findDialog(Tags.STASHES);
      flushAWT();
      assertNotNull(listStashesDialog);
      JCheckBox[] popStashCheckBox = new JCheckBox[1];
      flushAWT();
      popStashCheckBox[0] = findCheckBox(listStashesDialog, Tags.DELETE_STASH_AFTER_APPLIED);
      assertNotNull(popStashCheckBox[0]);
      SwingUtilities.invokeLater(() -> popStashCheckBox[0].setSelected(true));
      flushAWT();
      
      List<RevCommit> stashes = new ArrayList<>(gitAccess.listStashes());
      assertFalse(stashes.isEmpty());
      assertEquals(1, listStashesDialog.getStashesTable().getRowCount());
      
      JButton[] applyButton = new JButton[1];
      flushAWT();
      applyButton[0] = findFirstButton(listStashesDialog, Tags.APPLY);
      assertNotNull(applyButton[0]);
      SwingUtilities.invokeLater(() -> applyButton[0].doClick());
      flushAWT();
      
      // Check if the stash was been deleted and applied
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertTrue(stashes.isEmpty());
      assertEquals(0, listStashesDialog.getStashesTable().getRowCount());
      assertEquals("some_modification", getFileContent(LOCAL_REPO + "/local.txt"));
      
      JButton cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();

      stashChangesItem[0] = stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      
      stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);
      doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      assertEquals("local content", getFileContent(LOCAL_REPO + "/local.txt"));
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      listStashesDialog = (ListStashesDialog) findDialog(Tags.STASHES);
      assertNotNull(listStashesDialog);
      popStashCheckBox[0] = findCheckBox(listStashesDialog, Tags.DELETE_STASH_AFTER_APPLIED);
      assertNotNull(popStashCheckBox[0]);
      SwingUtilities.invokeLater(() -> popStashCheckBox[0].setSelected(false));
      flushAWT();
      
      applyButton[0] = findFirstButton(listStashesDialog, Tags.APPLY);
      assertNotNull(applyButton[0]);
      SwingUtilities.invokeLater(() -> applyButton[0].doClick());
      flushAWT();
      
      stashes = new ArrayList<>(gitAccess.listStashes());
      assertFalse(stashes.isEmpty());
      assertEquals(1, listStashesDialog.getStashesTable().getRowCount());
      assertEquals("some_modification", getFileContent(LOCAL_REPO + "/local.txt"));
      
      cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
           
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "List stashes" affected files table</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testListStashAffectedFilesTable() throws Exception {
    // Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
    
    
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
      
      String[] filesNames = {
          "local.txt",
          "local1.txt",
          "local2.txt",
          "local3.txt",
          "local4.txt",
      };
      
      String[] foldersName = {
          "folder0",
          "folder1",
          "folder2",
          "folder3",
          "folder4",
          "folder5",
          "very_very_veeeeeeeeeeeeeeeeeery_long_folder_name"
      };
      
      String path = LOCAL_REPO + "/";
      String fileWithLongPathName = "file_with_long_path.txt";
      
      for(int i = 0; i < foldersName.length; i++) {
        path += foldersName[i];
        file = new File(path);
        assertTrue(file.mkdir());
      }
      
      makeLocalChange("some_modification");
      for (int i = 1; i < filesNames.length; i++) {
        file = new File(LOCAL_REPO, filesNames[i]);
        assertTrue(file.createNewFile());
        setFileContent(file, "local content" + i);
        gitAccess.add(new FileStatus(GitChangeType.ADD, filesNames[i]));
      }
      
      file = new File(path, fileWithLongPathName);
      assertTrue(file.createNewFile());
      setFileContent(file, "local content");
      flushAWT();
      gitAccess.add(new FileStatus(GitChangeType.ADD, path + fileWithLongPathName));
      path = path.substring((LOCAL_REPO + "/").length());
      
      toolbarPanel.refreshStashButton();
      flushAWT();
      JMenuItem[] stashChangesItem = new JMenuItem[1];
      stashChangesItem[0] = stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      
      JDialog stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      flushAWT();
      assertNotNull(stashChangesDialog);
      JButton doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      flushAWT();
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      makeLocalChange("another_modification");
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      stashChangesItem[0] = stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem[0].getAction().actionPerformed(null));
      
      flushAWT();
      
      stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      flushAWT();
      assertNotNull(stashChangesDialog);
      doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      toolbarPanel.refreshStashButton();
      flushAWT();
      
      JMenuItem[] listStashesItem =  new JMenuItem[1];
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      ListStashesDialog listStashesDialog = (ListStashesDialog) findDialog(Tags.STASHES);
      flushAWT();
      assertNotNull(listStashesDialog);
      
      StagingResourcesTableCellRenderer filesRender = (StagingResourcesTableCellRenderer) listStashesDialog.getAffectedFilesTable()
          .getDefaultRenderer(FileStatus.class);
      
      
      FilesTableModel stashFilesTableModel = (FilesTableModel) listStashesDialog.getAffectedFilesTable().getModel();
      assertEquals(GitChangeType.CHANGED, stashFilesTableModel.getValueAt(0, 0));
      assertEquals(filesNames[0], ((FileStatus) stashFilesTableModel.getValueAt(0, 1)).getFileLocation());
      stashFilesTableModel = (FilesTableModel) listStashesDialog.getAffectedFilesTable().getModel();
      SwingUtilities.invokeLater(() -> listStashesDialog.getStashesTable().setRowSelectionInterval(1, 1));
      flushAWT();
     
      for (int i = 0; i < filesNames.length - 1; i++) {
        assertEquals(GitChangeType.ADD, stashFilesTableModel.getValueAt(i, 0));
        assertEquals(filesNames[i + 1], ((FileStatus) stashFilesTableModel.getValueAt(i, 1)).getFileLocation());
        String toolTipFileText = ((JLabel)filesRender.getTableCellRendererComponent(listStashesDialog.getAffectedFilesTable(), 
            stashFilesTableModel.getValueAt(i, 1), true, true, i, 1)).getToolTipText();
        assertEquals(filesNames[i + 1], toolTipFileText);
      }
      
      int length = filesNames.length;
      assertEquals(GitChangeType.CHANGED, stashFilesTableModel.getValueAt(length - 1, 0));
      assertEquals(filesNames[0], ((FileStatus) stashFilesTableModel.getValueAt(length - 1, 1)).getFileLocation());
      String toolTipFileText = ((JLabel)filesRender.getTableCellRendererComponent(listStashesDialog.getAffectedFilesTable(), 
          stashFilesTableModel.getValueAt(length - 1, 1), true, true, length - 1, 1)).getToolTipText();
      assertEquals(filesNames[0], toolTipFileText);
      
      toolTipFileText = ((JLabel)filesRender.getTableCellRendererComponent(listStashesDialog.getAffectedFilesTable(), 
          stashFilesTableModel.getValueAt(length, 1), true, true, length , 1)).getToolTipText();
      assertEquals(fileWithLongPathName + " - " + path, toolTipFileText);
      flushAWT();

      stashFilesTableModel = (FilesTableModel) listStashesDialog.getAffectedFilesTable().getModel();
      SwingUtilities.invokeLater(() -> listStashesDialog.getStashesTable().setRowSelectionInterval(0, 0));
      flushAWT();
      assertEquals(GitChangeType.CHANGED, stashFilesTableModel.getValueAt(0, 0));
      assertEquals(filesNames[0], ((FileStatus) stashFilesTableModel.getValueAt(0, 1)).getFileLocation()); 
      
      JButton cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
           
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "List stashes" table values</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testListStashesTableValues() throws Exception {
    // Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();
      
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      SplitMenuButton stashButton = toolbarPanel.getStashButton();
    
      initStashes(toolbarPanel);
      
      List<RevCommit> stashes = new ArrayList<>(gitAccess.listStashes());
      assertEquals(3, stashes.size());
      
      JMenuItem[] listStashesItem =  new JMenuItem[1];
      listStashesItem[0] = stashButton.getItem(1);

      SwingUtilities.invokeLater(() -> listStashesItem[0].getAction().actionPerformed(null));
      
      ListStashesDialog listStashesDialog = (ListStashesDialog) findDialog(Tags.STASHES);
      assertNotNull(listStashesDialog);
      StashesTableModel model = (StashesTableModel) listStashesDialog.getStashesTable().getModel();
      assertEquals(3, model.getRowCount());
      
      assertEquals(stashes.get(0).getFullMessage(), model.getValueAt(0, StashesTableModel.STASH_DESCRIPTION_COLUMN));
      assertEquals(stashes.get(1).getFullMessage(), model.getValueAt(1, StashesTableModel.STASH_DESCRIPTION_COLUMN));
      assertEquals(stashes.get(2).getFullMessage(), model.getValueAt(2, StashesTableModel.STASH_DESCRIPTION_COLUMN));
      
      assertEquals(stashes.get(0).getAuthorIdent().getWhen(), model.getValueAt(0, StashesTableModel.STASH_DATE_COLUMN));
      assertEquals(stashes.get(1).getAuthorIdent().getWhen(), model.getValueAt(1, StashesTableModel.STASH_DATE_COLUMN));
      assertEquals(stashes.get(2).getAuthorIdent().getWhen(), model.getValueAt(2, StashesTableModel.STASH_DATE_COLUMN));
      
      JButton cancelButton = findFirstButton(listStashesDialog, Tags.CLOSE);
      assertNotNull(cancelButton);
      cancelButton.doClick();
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  
  
  /**
   * Make local change in file "local.txt" file with the "text" that will be the new file content. 
   * 
   * @param text         The new file text.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  private void makeLocalChange(String text) throws Exception {
    File file = new File(LOCAL_REPO, "local.txt");
    setFileContent(file, text);
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    refreshSupport.call();
    stagingPanel.getToolbarPanel().refreshStashButton();
    flushAWT();    
  }

  
  /**
   * Initialise 3 stashes.
   * 
   * @param toolbarPanel The toolbar Panel.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  private void initStashes(ToolbarPanel toolbarPanel) throws Exception {
    SplitMenuButton stashButton = toolbarPanel.getStashButton();
    
    String[] stashesMessages = {
        "Stash0", "Stash1", "Stash2",
    };
    
    for (String stashMessage : stashesMessages) {
      makeLocalChange(stashMessage);
      
      JMenuItem stashChangesItem =  stashButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> stashChangesItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      // Test if the user can add a custom text
      JDialog stashChangesDialog = findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);
      JTextField textField = TestUtil.findFirstTextField(stashChangesDialog);
      assertNotNull(textField);
      textField.setText(stashMessage);
      flushAWT();
      JButton doStashButton = findFirstButton(stashChangesDialog, Tags.STASH);
      assertNotNull(doStashButton);
      doStashButton.doClick();
      refreshSupport.call();
      flushAWT();
      toolbarPanel.refreshStashButton();
    }
  }
  
  
  /**
   * Get the content for a file.
   * 
   * @return The content.
   * 
   * @author Alex_Smarandache
   *
   */
  private String getFileContent(String path)  {
    
    String content = "";
    
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
      content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      e.printStackTrace();
    } 
    
    return content;     
  }

  
}
