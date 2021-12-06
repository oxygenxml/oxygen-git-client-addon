package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog.RemoteBranchItem;
import com.oxygenxml.git.view.remotes.RemotesRepositoryDialog;
import com.oxygenxml.git.view.remotes.RemotesTableModel;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.ToolbarPanel;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;
import ro.sync.exml.workspace.api.util.ColorTheme;

/**
 * Contains tests for remote action for remote repositories feature.
 * 
 * @author Alex_Smarandache
 *
 */
public class RemoteVisualTests extends GitTestBase {

  private final static String LOCAL_REPO = "target/test-resources/GitAccessRemote/localRepository";
  private final static String REMOTE_REPO = "target/test-resources/GitAccessRemote/remoteRepository";
  private final static String REMOTE_REPO2 = "target/test-resources/GitAccessRemote/remoteRepository2";
  private final static String LOCAL_BRANCH = "LocalBranch";

  private GitAccess gitAccess;
  private StagingPanel stagingPanel;
  private Repository localRepository;

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
    localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }
  

  /**
   * <p><b>Description:</b> Tests the action for manage remote from remote button of git staging toolbar.</p>
   * <p><b>Bug ID:</b> EXM-40858</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testManageBasicsRemote() throws Exception {
 
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
      
      SplitMenuButton remoteButton = toolbarPanel.getRemoteButton();
      
      JMenuItem manageRemoteItem =  remoteButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      // -----> TEST ADD REMOTE OPTION <----- //
      
      RemotesRepositoryDialog[] manageRemoteDialog = new RemotesRepositoryDialog[1];
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      RemotesTableModel model = manageRemoteDialog[0].getModel();
      final String firstRemoteName = (String)model.getValueAt(0, 0);
      final String firstRemoteURL = (String)model.getValueAt(0, 1);
      assertEquals(1, model.getRowCount());
      JButton doAddButton = findFirstButton(manageRemoteDialog[0], Tags.ADD);
      assertNotNull(doAddButton);
      SwingUtilities.invokeLater(() -> doAddButton.doClick());
      flushAWT();
      OKCancelDialog addRemoteDialog = (OKCancelDialog) findDialog(Tags.ADD_REMOTE);
      assertNotNull(addRemoteDialog);
      
      flushAWT();
      JTextField remoteNameTF = TestUtil.findFirstTextField(addRemoteDialog);
      assertNotNull(remoteNameTF);
      SwingUtilities.invokeLater(() -> remoteNameTF.setText("Custom remote"));
      
      flushAWT();
      JTextField[] remoteURLTF = new JTextField[1];
      remoteURLTF[0] = TestUtil.findNthTextField(addRemoteDialog, 2);
      assertNotNull(remoteURLTF[0]);
      SwingUtilities.invokeLater(() -> remoteURLTF[0].setText("https/custom_link.ro"));
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> addRemoteDialog.getOkButton().doClick());
      
      flushAWT();
      assertEquals(2, model.getRowCount());
      assertEquals("Custom remote", (String)model.getValueAt(1, 0));
      assertEquals("https/custom_link.ro", (String)model.getValueAt(1, 1));
      
      flushAWT();
      SwingUtilities.invokeLater(() -> manageRemoteDialog[0].getOkButton().doClick());
      
      // Test if the remotes has been saved after confirmation.
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      
      assertEquals(2, model.getRowCount());
      assertEquals("Custom remote", (String)model.getValueAt(0, 0));
      assertEquals("https/custom_link.ro", (String)model.getValueAt(0, 1));
      
      
      
      // -----> TEST EDIT REMOTE OPTION <----- //
      
      JTable[] table = new JTable[1]; 
      table[0] = manageRemoteDialog[0].getTable();
      SwingUtilities.invokeLater(() -> table[0].addRowSelectionInterval(0, 0));
      flushAWT();
      
      JButton editButton = findFirstButton(manageRemoteDialog[0], Tags.EDIT);
      assertNotNull(editButton);
      SwingUtilities.invokeLater(() -> editButton.doClick());
      flushAWT();
      OKCancelDialog editRemoteDialog = (OKCancelDialog) findDialog(Tags.EDIT_REMOTE);
      assertNotNull(editRemoteDialog);
      
      flushAWT();
      remoteURLTF[0] = TestUtil.findNthTextField(editRemoteDialog, 2);
      assertNotNull(remoteURLTF[0]);
      SwingUtilities.invokeLater(() -> remoteURLTF[0].setText("https/edit_link.ro"));
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> editRemoteDialog.getOkButton().doClick());
      
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      assertEquals(2, model.getRowCount());
      assertEquals("Custom remote", (String)model.getValueAt(0, 0));
      assertEquals("https/edit_link.ro", (String)model.getValueAt(0, 1));
      
      flushAWT();
      SwingUtilities.invokeLater(() -> manageRemoteDialog[0].getOkButton().doClick());
      
      // Test if the remotes has been saved after confirmation.
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      
      assertEquals(2, model.getRowCount());
      assertEquals("Custom remote", (String)model.getValueAt(0, 0));
      assertEquals("https/edit_link.ro", (String)model.getValueAt(0, 1));
      
      
      // -----> TEST DELETE REMOTE OPTION <----- //
      
      table[0] = manageRemoteDialog[0].getTable();
      SwingUtilities.invokeLater(() -> table[0].addRowSelectionInterval(0, 0));
      flushAWT();
      
      JButton deleteButton = findFirstButton(manageRemoteDialog[0], Tags.DELETE);
      assertNotNull(deleteButton);
      SwingUtilities.invokeLater(() -> deleteButton.doClick());
      flushAWT();
      OKCancelDialog deleteRemoteDialog = (OKCancelDialog) findDialog(Tags.DELETE_REMOTE);
      assertNotNull(deleteRemoteDialog);
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> deleteRemoteDialog.getOkButton().doClick());
      
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      assertEquals(1, model.getRowCount());
      assertEquals(firstRemoteName, (String)model.getValueAt(0, 0));
      assertEquals(firstRemoteURL, (String)model.getValueAt(0, 1));
      
      flushAWT();
      SwingUtilities.invokeLater(() -> manageRemoteDialog[0].getOkButton().doClick());
      
      // Test if the remotes has been saved after confirmation.
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      
      assertEquals(1, model.getRowCount());
      assertEquals(firstRemoteName, (String)model.getValueAt(0, 0));
      assertEquals(firstRemoteURL, (String)model.getValueAt(0, 1));
      
      table[0] = manageRemoteDialog[0].getTable();
      SwingUtilities.invokeLater(() -> table[0].addRowSelectionInterval(0, 0));
      flushAWT();
      
      JButton eButton = findFirstButton(manageRemoteDialog[0], Tags.EDIT);
      assertNotNull(eButton);
      SwingUtilities.invokeLater(() -> eButton.doClick());
      flushAWT();
      OKCancelDialog eRemoteDialog = (OKCancelDialog) findDialog(Tags.EDIT_REMOTE);
      assertNotNull(eRemoteDialog);
      
      flushAWT();
      remoteURLTF[0] = TestUtil.findNthTextField(eRemoteDialog, 2);
      assertNotNull(remoteURLTF[0]);
      SwingUtilities.invokeLater(() -> remoteURLTF[0].setText("https/edit_link_test.ro"));
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> eRemoteDialog.getOkButton().doClick());
      
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      assertEquals(1, model.getRowCount());
      assertEquals(firstRemoteName, (String)model.getValueAt(0, 0));
      assertEquals("https/edit_link_test.ro", (String)model.getValueAt(0, 1));
      
      flushAWT();
      SwingUtilities.invokeLater(() -> manageRemoteDialog[0].getCancelButton().doClick());
      
      // Test if the remotes has not been saved after the user cancel dialog.
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      model = manageRemoteDialog[0].getModel();
      
      assertEquals(1, model.getRowCount());
      assertEquals(firstRemoteName, (String)model.getValueAt(0, 0));
      assertEquals(firstRemoteURL, (String)model.getValueAt(0, 1));
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests corner case when user tries to add an existing remote.</p>
   * <p><b>Bug ID:</b> EXM-40858</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testManageAdvancedRemote() throws Exception {
 
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
      
      SplitMenuButton remoteButton = toolbarPanel.getRemoteButton();
      
      JMenuItem manageRemoteItem =  remoteButton.getItem(0);
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
     
      RemotesRepositoryDialog[] manageRemoteDialog = new RemotesRepositoryDialog[1];
      manageRemoteDialog[0] = (RemotesRepositoryDialog) findDialog(Tags.REMOTES_DIALOG_TITLE);
      assertNotNull(manageRemoteDialog);
      flushAWT();
      RemotesTableModel model = manageRemoteDialog[0].getModel();
      final String firstRemoteName = (String)model.getValueAt(0, 0);
      assertEquals(1, model.getRowCount());
      JButton doAddButton = findFirstButton(manageRemoteDialog[0], Tags.ADD);
      assertNotNull(doAddButton);
      SwingUtilities.invokeLater(() -> doAddButton.doClick());
      flushAWT();
      OKCancelDialog addRemoteDialog = (OKCancelDialog) findDialog(Tags.ADD_REMOTE);
      assertNotNull(addRemoteDialog);
      
      flushAWT();
      JTextField remoteNameTF = TestUtil.findFirstTextField(addRemoteDialog);
      assertNotNull(remoteNameTF);
      SwingUtilities.invokeLater(() -> remoteNameTF.setText(firstRemoteName));
      
      flushAWT();
      JTextField[] remoteURLTF = new JTextField[1];
      remoteURLTF[0] = TestUtil.findNthTextField(addRemoteDialog, 2);
      assertNotNull(remoteURLTF[0]);
      SwingUtilities.invokeLater(() -> remoteURLTF[0].setText("https/custom_link.ro"));
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> addRemoteDialog.getOkButton().doClick());
      
      final OKCancelDialog confirmDialog = (OKCancelDialog) findDialog(Tags.ADD_REMOTE);
      assertNotNull(confirmDialog);
      SwingUtilities.invokeLater(() -> confirmDialog.getOkButton().doClick());
    
      flushAWT();
      assertEquals(1, model.getRowCount());
      assertEquals(firstRemoteName, (String)model.getValueAt(0, 0));
      assertEquals("https/custom_link.ro", (String)model.getValueAt(0, 1));
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "Track remote branch" action from remote button of git staging toolbar.</p>
   * <p><b>Bug ID:</b> EXM-40858</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testTrackRemoteBranch() throws Exception {
    
    final String remoteRepo2Branch = "branch_remote_repo_2";
    final String remote2 = "remote2_name";
    
    //Creates the remote repository.
    createRepository(REMOTE_REPO2);
    Repository remoteRepository = gitAccess.getRepository();
    addRemote(localRepository, remoteRepository, remote2);
    gitAccess.createBranch(remoteRepo2Branch);
    File file = new File(REMOTE_REPO2, "remote2.txt");
    file.createNewFile();
    setFileContent(file, "remote2content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote2.txt"));
    gitAccess.commit("First remote2 commit.");
    
   
    //Make the first commit for the local repository
    file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
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
      
      SplitMenuButton remoteButton = toolbarPanel.getRemoteButton();
      
      JMenuItem trackRemoteItem =  remoteButton.getItem(1);
      
      SwingUtilities.invokeLater(() -> trackRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
      
      CurrentBranchRemotesDialog[] trackRemoteDialog = new CurrentBranchRemotesDialog[1];
      trackRemoteDialog[0] = (CurrentBranchRemotesDialog) findDialog(Tags.CONFIGURE_REMOTE_FOR_BRANCH);
      assertNotNull(trackRemoteDialog);
      flushAWT();
      
      JComboBox<RemoteBranchItem> remoteBranches = trackRemoteDialog[0].getRemoteBranchItems();
      assertNotNull(remoteBranches);
      assertEquals(2, remoteBranches.getItemCount());
      assertEquals("origin", gitAccess.getRemoteFromCurrentBranch());
      RemoteBranchItem currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("origin/refs/heads/main", currentSelected.toString());
      
      remoteBranches.setSelectedIndex(1);
      currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("remote2_name/refs/heads/main", currentSelected.toString());
      
      flushAWT();
      sleep(500);
      SwingUtilities.invokeLater(() -> trackRemoteDialog[0].getOkButton().doClick());
      
      
      // Test if the config file is updated after user confirmation.
      SwingUtilities.invokeLater(() -> trackRemoteItem.getAction().actionPerformed(null));
      flushAWT();
      trackRemoteDialog[0] = (CurrentBranchRemotesDialog) findDialog(Tags.CONFIGURE_REMOTE_FOR_BRANCH);
      assertNotNull(trackRemoteDialog);
      flushAWT();
      
      remoteBranches = trackRemoteDialog[0].getRemoteBranchItems();
      assertNotNull(remoteBranches);
      assertEquals(remote2, gitAccess.getRemoteFromCurrentBranch());
      currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("remote2_name/refs/heads/main", currentSelected.toString());

    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  
  /**
   * <p><b>Description:</b> Tests the "Edit config" action from remote button of git staging toolbar.</p>
   * <p><b>Bug ID:</b> EXM-40858</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testEditGitConfigFile() throws Exception {
 
    String[] urlString = new String[1];
    String[] textTypeString = new String[1];
    ColorTheme colorTheme = Mockito.mock(ColorTheme.class);
    Mockito.when(colorTheme.isDarkTheme()).thenReturn(false);
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.open((URL) Mockito.any(), (String) Mockito.any(), (String) Mockito.any())).then(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        urlString[0] = ((URL)invocation.getArgument(0)).toURI().toString();
        textTypeString[0] = (String)invocation.getArgument(2);
        return true;
      }
    });
    Mockito.when(pluginWSMock.getColorTheme()).thenReturn(colorTheme);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
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
      
      SplitMenuButton remoteButton = toolbarPanel.getRemoteButton();
      
      JMenuItem manageRemoteItem =  remoteButton.getItem(2);
      
      SwingUtilities.invokeLater(() -> manageRemoteItem.getAction().actionPerformed(null));
      
      flushAWT();
    
      assertTrue(urlString[0].replace('/', '\\').endsWith("target\\test-resources\\GitAccessRemote\\localRepository\\.git\\config"));
      assertEquals("text/plain", textTypeString[0]);

    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
}