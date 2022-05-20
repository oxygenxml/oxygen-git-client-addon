package com.oxygenxml.git.validation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.dialog.internal.MessageDialog;
import com.oxygenxml.git.view.dialog.internal.MessageDialogBuilder;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Contains tests for pre-push validation.
 * 
 * @author alex_smarandache
 *
 */
public class PrePushValidationTest extends GitTestBase {

  /**
   * The first local repository path.
   */
  private final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local";

  /**
   * A remote repository.
   */
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/remote";

  /**
   * The second test repository path.
   */
  private final static String SECOND_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local2";

  /**
   * The first local repository.
   */
  private Repository firstLocalRepo;

  /**
   * The remote repository
   */
  private Repository remoteRepo;

  /**
   * The second local repository.
   */
  private Repository secondLocalRepo;

  /**
   * The git access.
   */
  private GitAccess gitAccess;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITPRY);
    firstLocalRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITPRY);
    secondLocalRepo = gitAccess.getRepository();

    File file = new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.xpr");
    try {
      file.createNewFile();
      file = new File(SECOND_LOCAL_TEST_REPOSITPRY + "/test2.xpr");
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    final StoredConfig config = gitAccess.getRepository().getConfig();
    final RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    final URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    remoteConfig.update(config);
    config.save();

  }

  /**
   * <p><b>Description:</b> @OxygenAPIWrapper usefully only when the getMainFiles is not accessible.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   * 
   * @see OxygenAPIWrapper
   * @see FilesValidator
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testOxygenAPIWrapper() throws Exception {
     assertFalse("The classes marked with @see must be refactorized because the API is already available.", 
         OxygenAPIWrapper.getInstance().isGetMainFilesAccessible());
  }

  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is not enabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationDisabled() throws Exception {
    // Disable pre-push validation option 
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return false;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");

    push("", "");

    assertEquals(firstLocalRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"),
        remoteRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"));

  }
  
  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is enabled and the push should be rejected on validation problem.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenPushIsRejected() throws Exception {
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(true);
    
    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final ProjectController projectController = Mockito.mock(ProjectController.class);
    Mockito.when(projectController.getCurrentProjectURL()).thenReturn(
        firstLocalRepo.getDirectory().toURI().toURL());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);
    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_CANCEL;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = new boolean[2];
    dialogPresentedFlags[0] = false;
    dialogPresentedFlags[1] = false;

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] = Tags.PUSH_ANYWAY.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
    });

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return false;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);
   
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertFalse(ValidationManager.getInstance().checkPushValid());
    assertTrue(dialogPresentedFlags[0]);
    assertFalse(dialogPresentedFlags[1]);
  }
  
  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is enabled and the push should not be rejected on validation problem.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenPushIsNotRejected() throws Exception {
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    
    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final ProjectController projectController = Mockito.mock(ProjectController.class);
    Mockito.when(projectController.getCurrentProjectURL()).thenReturn(
        firstLocalRepo.getDirectory().toURI().toURL());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);
    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = new boolean[2];
    dialogPresentedFlags[0] = false;
    dialogPresentedFlags[1] = false;

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] = Tags.PUSH_ANYWAY.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
    });

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return false;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
   
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    assertTrue(dialogPresentedFlags[0]);
    assertTrue(dialogPresentedFlags[1]);
  }

  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is enabled and the push should not be rejected on validation problem and the not same project is loaded 
   * in "Project" View and Git Staging.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50540</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenNotSameProjectOpened() throws Exception {
    // Enable push validation and disable reject push option
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final ProjectController projectController = Mockito.mock(ProjectController.class);
    Mockito.when(projectController.getCurrentProjectURL()).thenReturn(
        secondLocalRepo.getDirectory().toURI().toURL());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);
    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = new boolean[3];
    dialogPresentedFlags[0] = false;
    dialogPresentedFlags[1] = false;
    dialogPresentedFlags[2] = false;

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialogBuilder setQuestionMessage(String questionMessage) {
        dialogPresentedFlags[2] |= Tags.NOT_SAME_PROJECT_MESSAGE.equals(questionMessage);
        return super.setQuestionMessage(questionMessage);
      }

      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] |= Tags.LOAD.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
    });

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);

    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    assertTrue(dialogPresentedFlags[0]);
    assertTrue(dialogPresentedFlags[1]);
    assertTrue(dialogPresentedFlags[2]);
  }
  
  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is enabled and the push should not be rejected on validation problem and there are uncommited changes</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenUncommitedChangesExist() throws Exception {
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    
    StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final ProjectController projectController = Mockito.mock(ProjectController.class);
    Mockito.when(projectController.getCurrentProjectURL()).thenReturn(
        secondLocalRepo.getDirectory().toURI().toURL());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);
    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Stash" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = new boolean[3];
    dialogPresentedFlags[0] = false;
    dialogPresentedFlags[1] = false;
    dialogPresentedFlags[2] = false;

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {
     
      @Override
          public MessageDialogBuilder setQuestionMessage(String questionMessage) {
            dialogPresentedFlags[2] |= Tags.PUSH_VALIDATION_UNCOMMITED_CHANGES.equals(questionMessage);
            return super.setQuestionMessage(questionMessage);
          }
      
      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] |= Tags.STASH.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
    });

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);
   
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(gitAccess.hasFilesChanged());
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    assertTrue(dialogPresentedFlags[0]);
    assertTrue(dialogPresentedFlags[1]);
    assertTrue(dialogPresentedFlags[2]);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    gitAccess.closeRepo();
    flushAWT();
    firstLocalRepo.close();
    flushAWT();
    remoteRepo.close();
    flushAWT();
    secondLocalRepo.close();
    flushAWT();
    try {
      File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
      dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
      dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
