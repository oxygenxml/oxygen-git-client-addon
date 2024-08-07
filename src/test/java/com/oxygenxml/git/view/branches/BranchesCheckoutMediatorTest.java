package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
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
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Contains tests for branch checkout mediator.
 * 
 * @author alex_smarandache
 */
public class BranchesCheckoutMediatorTest extends JFCTestCase {
  
  /**
   * The mock of the git controller.
   */
  private GitController gitCtrlMock;
  
  /**
   * The setup to be called before each test.
   * 
   * @throws NoRepositorySelected
   */
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
    BranchCheckoutMediator branchesMediator = new BranchCheckoutMediator(gitCtrlMock);
    Mockito.when(gitCtrlMock.getBranchesCheckoutMediator()).thenReturn(branchesMediator);
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is not displayed if repository is up to date.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  
  public void testConfirmationDialog_NotDisplayed() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    //simulateBranchCreationRequest(wasCalledCreateBranchOp, true);
    SwingUtilities.invokeLater(() -> new CreateBranchDialog("Create Branch", "bla", false));
    
    ((CreateBranchDialog) TestHelper.findDialog("Create Branch")).getCancelButton().doClick();
    assertFalse(wasCalledCreateBranchOp.get());
  }

  /**
   * Simulates the user request to create a branch.
   * 
   * @param wasCalledCreateBranchOp           This atomic boolean will be set true if the user create the branch.
   * @param isRepositoryUpToDate              <code>true</code> if the repository is up to date.
   * @param warnUserIfRepositoryOutdated      <code>true</code> if the user should be warned if the repository is outdated.
   */
  private void simulateBranchCreationRequest(AtomicBoolean wasCalledCreateBranchOp, boolean isRepositoryUpToDate, boolean warnUserIfRepositoryOutdated) {
    Mockito.when(gitCtrlMock.getGitAccess().getPullsBehind()).thenReturn(isRepositoryUpToDate ? 0 : 15);

    SwingUtilities.invokeLater(() -> {
      gitCtrlMock.getBranchesCheckoutMediator().createBranch("Create Branch", "My Branch", false, new IBranchesCreator() {
        @Override
        public void createBranch(String branchName, boolean shouldCheckoutBranch) {
          wasCalledCreateBranchOp.set(true);
        }
      }, 
          warnUserIfRepositoryOutdated);
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
  public void testConfirmationDialog_Displayed_Cancel() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false, true);
    
    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getCancelButton().doClick();
    
    assertNull(TestHelper.findDialog("Create Branch"));
    assertFalse(wasCalledCreateBranchOp.get());
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog not displayed and create branch dialog is displayed if the repository is outdated but the user should be not warned.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  public void testConfirmationDialog_NotDisplayed_NotWarnUser() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false, false);
    
    assertNull(TestHelper.findDialog(Tags.REPOSITORY_OUTDATED));
    CreateBranchDialog createBranchDialog = (CreateBranchDialog) TestHelper.findDialog("Create Branch");
    createBranchDialog.getOkButton().doClick();
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> wasCalledCreateBranchOp.get());
  }
  
  /**
   * <p><b>Description:</b> Tests if the confirmation dialog is displayed and the create branch is also displayed when create branch anyway option is chosen.</p>
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  public void testConfirmationDialog_Displayed_CreateBranchAnyway() throws Exception {
    AtomicBoolean wasCalledCreateBranchOp = new AtomicBoolean(false);
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false, true);
    
    OKOtherAndCancelDialog confirmationDialog = (OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED);

    confirmationDialog.getOtherButton().doClick();
    
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
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false, true);

    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getOKButton().doClick();
    
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
    
    simulateBranchCreationRequest(wasCalledCreateBranchOp, false, true);

    ((OKOtherAndCancelDialog) TestHelper.findDialog(Tags.REPOSITORY_OUTDATED)).getOKButton().doClick();
    
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> !listeners.isEmpty());
    
    assertEquals(1, listeners.size());
    
    listeners.get(0).operationSuccessfullyEnded(new GitEventInfo(GitOperation.PULL));
    
    CreateBranchDialog createBranchDialog = (CreateBranchDialog) TestHelper.findDialog("Create Branch");
    createBranchDialog.getOkButton().doClick();
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> wasCalledCreateBranchOp.get());
  }

}
