package com.oxygenxml.git.validation;

import java.io.File;
import java.net.URL;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.validation.gitoperation.PreCommitValidation;
import com.oxygenxml.git.validation.internal.ICollector;
import com.oxygenxml.git.validation.internal.IValidator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.dialog.internal.MessageDialog;
import com.oxygenxml.git.view.dialog.internal.MessageDialogBuilder;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.CommitAndStatusPanel;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Contains tests for commit validation.
 * 
 * @author alex_smarandache
 *
 */
public class PreCommitValidationTest extends GitTestBase {

  /**
   * Path for a local repository.
   */
  private final static String LOCAL_REPO = "target/test-resources/TestPreCommitValidation/localRepository";

  /**
   * Path for remote repository.
   */
  private final static String REMOTE_REPO = "target/test-resources/TestPreCommitValidation/remoteRepository";

  /**
   * Manager for options.
   */
  private final static OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  /**
   * The git access.
   */
  private GitAccess gitAccess;

  /**
   * Provide files and initial states for each test.
   */
  @Override
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

    // Create four files: 2 valid and 2 invalid.
    // The files will be added in staging area.
    File file = new File(LOCAL_REPO, "valid.css");
    file.createNewFile();
    setFileContent(file, "* {}");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "valid.css"));

    file = new File(LOCAL_REPO, "invalid.css");
    file.createNewFile();
    setFileContent(file, "* {\r\n" + 
        "    abc\r\n" + 
        "}");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "invalid.css"));

    file = new File(LOCAL_REPO, "valid.dita");
    file.createNewFile();
    setFileContent(file, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
        "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">\r\n" + 
        "<topic id=\"t1\">\r\n" + 
        "    <title>T1</title>\r\n" + 
        "    <body>\r\n" + 
        "        <p></p>\r\n" + 
        "    </body>\r\n" + 
        "</topic>\r\n" + 
        "");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "valid.dita"));

    file = new File(LOCAL_REPO, "invalid.dita");
    file.createNewFile();
    setFileContent(file, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
        "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">\r\n" + 
        "<topic id=\"t1\">\r\n" + 
        "    <title>T1</title>\r\n" + 
        "    <body abc=\"def\" def=\"hij\">\r\n" + 
        "        <p></p>\r\n" + 
        "    </body>\r\n" + 
        "</topic>\r\n" + 
        "");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "invalid.dita"));    
  }
  
  /**
   * <p><b>Description:</b> Tests the default options for pre-commit validation.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50586</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPreCommitValidationDefaultValue() {
    assertFalse(OPTIONS_MANAGER.isFilesValidatedBeforeCommit());
    assertFalse(OPTIONS_MANAGER.isCommitRejectedOnValidationProblems());
  }

  /**
   * <p><b>Description:</b> This test cover pre-commit validation behavior for case when this option
   * is not enabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-47776</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPreCommitValidationDisabled() {
    // Disable this option
    OPTIONS_MANAGER.setValidateFilesBeforeCommit(false);

    // Set the commit validator
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true; 
        });
    ValidationManager.getInstance().setPreCommitValidator(new PreCommitValidation(validator, 
        null));

    // try to make a commit
    final CommitAndStatusPanel commitPanel = new CommitAndStatusPanel(new GitController(gitAccess));
    commitPanel.getCommitMessageArea().setText("commit test");
    commitPanel.getCommitButton().doClick();
    waitForScheduler();

    // test if the all staged files were commited
    assertTrue(gitAccess.getStagedFiles().isEmpty());
  }

  /**
   * <p><b>Description:</b> This test cover pre-commit validation behavior for case when this option
   * is not enabled and reject commit option is disabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-47776</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPreCommitValidationEnabledAndRejectCommitDisabled() {
    // Enable validate pre-commit option
    OPTIONS_MANAGER.setValidateFilesBeforeCommit(true);
    // Disable reject commit on validation problems option
    OPTIONS_MANAGER.setRejectCommitOnValidationProblems(false);

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

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_CANCEL;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Commit anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = { false, false };

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_precommit", DialogType.ERROR) {

      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] = Tags.COMMIT_ANYWAY.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
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
    ValidationManager.getInstance().setPreCommitValidator(new PreCommitValidation(validator, 
        null));

    // try to make a commit
    final CommitAndStatusPanel commitPanel = new CommitAndStatusPanel(new GitController(gitAccess));
    commitPanel.getCommitMessageArea().setText("commit test");
    commitPanel.getCommitButton().doClick();
    waitForScheduler();

    // Test if the dialog were displayed and no files are commited
    assertEquals(4, gitAccess.getStagedFiles().size());
    assertTrue(dialogPresentedFlags[0]);
    assertTrue(dialogPresentedFlags[1]);

    // Test case when use try to commit and the files should be commited
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    commitPanel.getCommitButton().doClick();
    waitForScheduler();
    assertTrue(gitAccess.getStagedFiles().isEmpty());
  }
  
  /**
   * <p><b>Description:</b> This test cover pre-commit validation behavior for case when the unstaged files don't blocks the pre-commit validation</p>
   * 
   * <p><b>Bug ID:</b> EXM-50700</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testUnstagedFilesDontBlocksCommitValidation() throws Exception {
    // Enable validate pre-commit option
    OPTIONS_MANAGER.setValidateFilesBeforeCommit(true);
    // Enable reject commit on validation problems option
    OPTIONS_MANAGER.setRejectCommitOnValidationProblems(true);
    
    final UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccessMock.isUnhandledBinaryResourceURL(Mockito.any(URL.class))).then((Answer<Boolean>) 
        invocation -> {
          return false;
        });
    final StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    final ResultsManager resultManager = Mockito.mock(ResultsManager.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(pluginWSMock.getAllEditorLocations(Mockito.anyInt())).thenReturn(new URL[0]);
    Mockito.when(pluginWSMock.getResultsManager()).thenReturn(resultManager);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
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

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_CANCEL;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Commit anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = { false, false, false };

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_precommit", DialogType.WARNING) {

      @Override
      public MessageDialogBuilder setOkButtonVisible(final boolean isOkButtonVisible) {
        dialogPresentedFlags[1] = !isOkButtonVisible;
        return super.setOkButtonVisible(isOkButtonVisible);
      }
      
      @Override
          public MessageDialogBuilder setMessage(final String questionMessage) {
            dialogPresentedFlags[2] |= !Tags.FAILED_COMMIT_VALIDATION_MESSAGE.equals(questionMessage);
            return super.setMessage(questionMessage);
          }

      @Override
      public MessageDialog buildAndShow() {
        // a single dialog should be displayed, to show the error only and the commit would be rejected after this dialog.
        dialogPresentedFlags[0] = !dialogPresentedFlags[0];
        return dialog;
      }
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

    ValidationManager.getInstance().setPreCommitValidator(new PreCommitValidation(validator, 
        null));

    File file = new File(LOCAL_REPO, "unsts.css");
    file.createNewFile();
    setFileContent(file, "* {}");
    assertFalse(gitAccess.getUnstagedFiles().isEmpty());

    // try to make a commit
    final CommitAndStatusPanel commitPanel = new CommitAndStatusPanel(new GitController(gitAccess));
    commitPanel.getCommitMessageArea().setText("commit test");
    commitPanel.getCommitButton().doClick();
    waitForScheduler();

    // Test if the dialog were displayed and no files are commited
    assertEquals(4, gitAccess.getStagedFiles().size());
    assertTrue(dialogPresentedFlags[0]);
    assertTrue(dialogPresentedFlags[1]);
    assertFalse(dialogPresentedFlags[2]);
  }

  /**
   * <p><b>Description:</b> This test cover pre-commit validation behavior for case when this option
   * is not enabled and reject commit option is enabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-47776</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPreCommitValidationEnabledAndRejectCommitEnabled() {
    // Enable validate pre-commit option
    OPTIONS_MANAGER.setValidateFilesBeforeCommit(true);
    // Enable reject commit on validation problems option
    OPTIONS_MANAGER.setRejectCommitOnValidationProblems(true);

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

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_CANCEL;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Commit anyway" button is available and the dialog is shows.
    final boolean[] dialogPresentedFlags = { false, false };
    
    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_precommit", DialogType.ERROR) {

      @Override
      public MessageDialogBuilder setOkButtonName(String okButtonName) {
        dialogPresentedFlags[1] = Tags.COMMIT_ANYWAY.equals(okButtonName);
        return super.setOkButtonName(okButtonName);
      }

      @Override
      public MessageDialog buildAndShow() {
        dialogPresentedFlags[0] = true;
        return dialog;
      }
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

    ValidationManager.getInstance().setPreCommitValidator(new PreCommitValidation(validator, 
        null));

    // try to make a commit
    final CommitAndStatusPanel commitPanel = new CommitAndStatusPanel(new GitController(gitAccess));
    commitPanel.getCommitMessageArea().setText("commit test");
    commitPanel.getCommitButton().doClick();
    waitForScheduler();

    // Test if the dialog were displayed and "Commit anyway" button is not visible and no files are commited
    assertEquals(4, gitAccess.getStagedFiles().size());
    assertTrue(dialogPresentedFlags[0]);
    assertFalse(dialogPresentedFlags[1]);
  }

}
