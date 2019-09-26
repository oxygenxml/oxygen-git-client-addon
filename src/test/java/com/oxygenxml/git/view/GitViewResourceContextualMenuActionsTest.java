package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.PopupMenuSerializer;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.event.StageController;

/**
 * Test cases for the actions from the contextual menu of the staged/unstaged resources.
 */
public class GitViewResourceContextualMenuActionsTest extends GitTestBase {
  
  /**
   * Staging controller.
   */
  private StageController stagingCtrl = new StageController();
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_1() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.ADD, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_2() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.ADD, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.ADD, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_3() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.ADD, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.CHANGED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_4() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.CHANGED, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_5() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testActionsEnabling5_local";
    String remoteTestRepository = "target/test-resources/testActionsEnabling5_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.CONFLICT, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        // In merging state
        true);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [DISABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [ENABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [ENABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [ENABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [ENABLED]\n" + 
        "Contextual_Menu_Discard [DISABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_6() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testActionsEnabling6_local";
    String remoteTestRepository = "target/test-resources/testActionsEnabling6_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.CONFLICT, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.CONFLICT, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        // In merging state
        true);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [DISABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [ENABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [ENABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [ENABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [ENABLED]\n" + 
        "Contextual_Menu_Discard [DISABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_7() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testActionsEnabling7_local";
    String remoteTestRepository = "target/test-resources/testActionsEnabling7_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.CONFLICT, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.ADD, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        // In merging state
        true);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [DISABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [ENABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [ENABLED]\n" + 
        "Contextual_Menu_Discard [DISABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_8() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.MISSING, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_9() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.MISSING, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.MISSING, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_10() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.MISSING, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.UNTRACKED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_11() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.REMOVED, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_12() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.REMOVED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.REMOVED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_13() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.REMOVED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.CHANGED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_14() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.UNTRACKED, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_15() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.UNTRACKED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.UNTRACKED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_16() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.UNTRACKED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.MODIFIED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_17() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.MODIFIED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the unstaged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_18() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.MODIFIED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.MODIFIED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For unstaged resources
        false,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Stage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_19() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.CHANGED, "test.xml"));
            fileStatuses.add(new FileStatus(GitChangeType.CHANGED, "test2.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [ENABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the submodules.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_20() throws Exception {
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            List<FileStatus> fileStatuses = new ArrayList<FileStatus>();
            fileStatuses.add(new FileStatus(GitChangeType.SUBMODULE, "test.xml"));
            return fileStatuses;
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        false);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Unstage [ENABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [DISABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [ENABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [DISABLED]\n" + 
        "Contextual_Menu_Discard [ENABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
  /**
   * <p><b>Description:</b> test the enabling state of the actions
   * from the contextual menu of the staged resources. Empty root. Merging state.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testActionsEnablingState_21() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testActionsEnabling6_local";
    String remoteTestRepository = "target/test-resources/testActionsEnabling6_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    GitViewResourceContextualMenu menu = new GitViewResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return getAllSelectedResources();
          }
          
          @Override
          public List<FileStatus> getAllSelectedResources() {
            return Collections.emptyList();
          }
        },
        stagingCtrl, null,
        // For staged resources
        true,
        // In merging state
        true);
    
    assertEquals("\n" + 
        "Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "Contextual_Menu_Open [DISABLED]\n" + 
        "Contextual_Menu_Unstage [DISABLED]\n" + 
        "Contextual_Menu_Resolve_Conflict [ENABLED]\n" + 
        "  Contextual_Menu_Open_In_Compare [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Resolve_Using_Mine [DISABLED]\n" + 
        "  Contextual_Menu_Resolve_Using_Theirs [DISABLED]\n" + 
        "  Contextual_Menu_Mark_Resolved [DISABLED]\n" + 
        "  ----\n" + 
        "  Contextual_Menu_Restart_Merge [ENABLED]\n" + 
        "Contextual_Menu_Discard [DISABLED]",
        PopupMenuSerializer.serializePopupStructure(menu, true, true));
  }
  
}
