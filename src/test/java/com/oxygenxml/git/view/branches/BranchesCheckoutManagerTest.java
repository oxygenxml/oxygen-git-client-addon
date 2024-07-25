package com.oxygenxml.git.view.branches;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.TestHelper;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.branches.BranchCheckoutMediator.IRepositoryInfo;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Contains tests for branch checkout mediator.
 * 
 * @author alex_smarandache
 */
public class BranchesCheckoutManagerTest {
  
  /**
   * The mock of the git controller.
   */
  private GitController gitCtrlMock;
  
  /**
   * The setup to be called before each test.
   * 
   * @throws NoRepositorySelected
   */
  @Before
  public void setUp() throws NoRepositorySelected {
    gitCtrlMock = Mockito.mock(GitController.class);
    GitAccess gitAccessMock = Mockito.mock(GitAccess.class);
    Mockito.when(gitCtrlMock.getGitAccess()).thenReturn(gitAccessMock);
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    OptionsManager.getInstance().loadOptions(wsOptions);
    Mockito.when(gitAccessMock.getBranchInfo()).thenReturn(new BranchInfo("MyBranchRemote", false));
    Mockito.when(gitAccessMock.getRepository()).thenReturn(Mockito.mock(Repository.class));
    
    BranchCheckoutMediator.getInstance().init(gitCtrlMock);
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is not displayed if repository is up to date.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  @Test
  public void testConfirmationDialog_NotDisplayed() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, true);
    
    ((CreateBranchDialog) TestHelper.findDialog("Create Branch")).getCancelButton().doClick();
    assertFalse(wasCalledCreateBranchOp.get());
  }

  /**
   * Simulates the user request to create a branch.
   * 
   * @param wasCalledCreateBranchOp This atomic boolean will be set true if the user create the branch.
   * @param isRepositoryUpToDate    <code>true</code> if the repository is up to date.
   */
  private void simulateBranchCreationRequest(AtomicBoolean wasCalledCreateBranchOp, boolean isRepositoryUpToDate) {
    BranchCheckoutMediator.getInstance().setRepositoryInfo(new IRepositoryInfo() {
      @Override
      public boolean isRepositoryUpToDate() throws Exception {
        return isRepositoryUpToDate;
      }
    });

    SwingUtilities.invokeLater(() -> {
      BranchCheckoutMediator.getInstance().createBranch("Create Branch", "My Branch", false, new IBranchesCreator() {
        @Override
        public void createBranch(String branchName, boolean shouldCheckoutBranch) {
          wasCalledCreateBranchOp.set(true);
        }
      });
    });
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is displayed and the create branch is not displayed when user choose to cancel the operation.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  @Test
  public void testConfirmationDialog_Displayed_Cancel() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false);
    
    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getCancelButton().doClick();
    
    assertNull(TestHelper.findDialog("Create Branch"));
    assertFalse(wasCalledCreateBranchOp.get());
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is displayed and the create branch is also displayed when create branch anyway option is chosen.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  @Test
  public void testConfirmationDialog_Displayed_CreateBranchAnyway() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false);
    
    OKOtherAndCancelDialog confirmationDialog = (OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED);

    confirmationDialog.getOKButton().doClick();
    
    CreateBranchDialog createBranchDialog = (CreateBranchDialog) TestHelper.findDialog("Create Branch");
    createBranchDialog.getOkButton().doClick();
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> wasCalledCreateBranchOp.get());
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is displayed and the create branch is not displayed when pull fails.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */ 
  @Test
  public void testConfirmationDialog_Displayed_PullFailed() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    List<GitEventListener> listeners = new ArrayList<>();
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        listeners.add((GitEventListener) invocation.getArgument(0));
        return null;
      }
    }).when(gitCtrlMock).addGitListener(Mockito.any());
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false);

    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getOtherButton().doClick();
    
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> !listeners.isEmpty());
    
    assertEquals(1, listeners.size());
    
    listeners.get(0).operationFailed(new GitEventInfo(GitOperation.PULL), new Exception());
    
    assertNull(TestHelper.findDialog("Create Branch"));
    assertFalse(wasCalledCreateBranchOp.get());
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is displayed and the create branch is also displayed when pull succeed.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */ 
  @Test
  public void testConfirmationDialog_Displayed_PullSuccess() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    List<GitEventListener> listeners = new ArrayList<>();
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        listeners.add((GitEventListener) invocation.getArgument(0));
        return null;
      }
    }).when(gitCtrlMock).addGitListener(Mockito.any());
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false);

    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getOtherButton().doClick();
    
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> !listeners.isEmpty());
    
    assertEquals(1, listeners.size());
    
    listeners.get(0).operationSuccessfullyEnded(new GitEventInfo(GitOperation.PULL));
    
    CreateBranchDialog createBranchDialog = (CreateBranchDialog) TestHelper.findDialog("Create Branch");
    createBranchDialog.getOkButton().doClick();
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> wasCalledCreateBranchOp.get());
  }

}
