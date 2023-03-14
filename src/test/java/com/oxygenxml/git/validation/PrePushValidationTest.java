package com.oxygenxml.git.validation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
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
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.validation.gitoperation.GitValidationUtil;
import com.oxygenxml.git.validation.gitoperation.PrePushValidation;
import com.oxygenxml.git.validation.internal.ICollector;
import com.oxygenxml.git.validation.internal.IValidator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.dialog.internal.MessageDialog;
import com.oxygenxml.git.view.dialog.internal.MessageDialogBuilder;
import com.oxygenxml.git.view.event.PullType;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectChangeListener;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.standalone.project.ProjectIndexer;
import ro.sync.exml.workspace.api.standalone.project.ProjectPopupMenuCustomizer;
import ro.sync.exml.workspace.api.standalone.project.ProjectRendererCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.util.EditorVariablesResolver;
import ro.sync.exml.workspace.api.util.ImageHolder;
import ro.sync.exml.workspace.api.util.UtilAccess;

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
  private final static String FIRST_LOCAL_TEST_REPOSITORY = "target/test-resources/PrePushValidationTest/local";

  /**
   * A remote repository.
   */
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/PrePushValidationTest/remote";

  /**
   * The second test repository path.
   */
  private final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/PrePushValidationTest/local2";

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
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITORY);
    remoteRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITORY);
    firstLocalRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
    secondLocalRepo = gitAccess.getRepository();

    File file = new File(FIRST_LOCAL_TEST_REPOSITORY + "/test.xpr");
    try {
      file.createNewFile();
      file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test2.xpr");
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }

    StoredConfig config = firstLocalRepo.getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    remoteConfig.update(config);
    config.save();

    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    File local1File = new File(REMOTE_TEST_REPOSITORY, "test1.txt");
    local1File.createNewFile();
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test1.txt"));
    gitAccess.commit("First");

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITORY);
    pull("", "", PullType.MERGE_FF, false);

    String branchName = GitAccess.DEFAULT_BRANCH_NAME;
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();

    bindLocalToRemote(firstLocalRepo , remoteRepo);
    pushOneFileToRemote(FIRST_LOCAL_TEST_REPOSITORY, "test_second_local.txt", "hellllo");
    flushAWT();
    
  }

  @Override
  public void tearDown() throws Exception {
    gitAccess.closeRepo();
    firstLocalRepo.close();
    remoteRepo.close();
    secondLocalRepo.close();
    
    sleep(2000);
    
      File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITORY);
      FileUtil.deleteRecursivelly(dirToDelete);
      dirToDelete = new File(REMOTE_TEST_REPOSITORY);
      FileUtil.deleteRecursivelly(dirToDelete);
      dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITORY);
      FileUtil.deleteRecursivelly(dirToDelete);

    super.tearDown();
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
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

    initProjectController(FIRST_LOCAL_TEST_REPOSITORY, firstLocalRepo);

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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

    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(validator, null));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");

    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Warning32.png\n" + 
        "targetFiles = null\n" + 
        "message = Failed_Push_Validation_Message\n" + 
        "questionMessage = null\n" + 
        "okButtonName = Push_Anyway\n" + 
        "cancelButtonName = Cancel\n" + 
        "showOkButton = true\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
  }

  /**
   * Initialize the project controller.
   * 
   * @param repoPath  The current repository project to be returned fot this controller.
   * @param repo      The current repository.
   * 
   * @throws MalformedURLException
   */
  private void initProjectController(final String repoPath, 
      final Repository repo) throws MalformedURLException {
    // Configure the custom project controller.
    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final List<URL> mainFilesURL = new ArrayList<>();
    mainFilesURL.add(new File(repoPath).toURI().toURL());
    final ProjectController projectController = createProjectControllerForTest(
        repo.getDirectory().toURI().toURL(), mainFilesURL.iterator());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);
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

    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
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
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(true);
    
    initProjectController(FIRST_LOCAL_TEST_REPOSITORY, firstLocalRepo);

    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.txt", "");
   
    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_CANCEL;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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

    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));

    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertFalse(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Error32.png\n" + 
        "targetFiles = null\n" + 
        "message = Failed_Push_Validation_Message\n" + 
        "questionMessage = null\n" + 
        "okButtonName = null\n" + 
        "cancelButtonName = Close\n" + 
        "showOkButton = false\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
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

    initProjectController(SECOND_LOCAL_TEST_REPOSITORY, secondLocalRepo);

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.txt", "");

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final String dialogToString[] = new String[1];
    
    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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

    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));

    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Warning32.png\n" + 
        "targetFiles = null\n" + 
        "message = null\n" + 
        "questionMessage = Not_Same_Project_Message\n" + 
        "okButtonName = Load\n" + 
        "cancelButtonName = null\n" + 
        "showOkButton = true\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
  }
  
  /**
   * <p><b>Description:</b> Check that when the project loaded in the Project view
   * is not from the current loaded repository and this repository contains multiple projects, 
   * Oxygen will offer the possibility to choose which project to load in the Project view.</p>
   * 
   * <p><b>Bug ID:</b> EXM-51988</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationMultipleProjectsFound() throws Exception {
    // Enable push validation and disable reject push option
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    
    final File file = new File(FIRST_LOCAL_TEST_REPOSITORY + "/test2.xpr");
    file.createNewFile();

    initProjectController(SECOND_LOCAL_TEST_REPOSITORY, secondLocalRepo);

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.txt", "");

    // Detects if the "Push anyway" button is available and the dialog is shows.
    final String dialogToString[] = new String[1];
    
    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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

    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
    final boolean wasValidationPassed[] = new boolean[1];
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    new Thread(() -> {
      wasValidationPassed[0] = ValidationManager.getInstance().checkPushValid();    
    }).start();
    waitForScheduler();
    final OKCancelDialog dialogFound = (OKCancelDialog)findDialog(Tags.DETECT_AND_OPEN_XPR_FILES_DIALOG_TITLE);
    assertNotNull(dialogFound);
    final JButton openButton = findFirstButton(dialogFound, Tags.OPEN);
    assertNotNull(openButton);
    openButton.doClick();
    
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> wasValidationPassed[0]);
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Warning32.png\n" + 
        "targetFiles = null\n" + 
        "message = null\n" + 
        "questionMessage = Not_Same_Project_Message\n" + 
        "okButtonName = Load\n" + 
        "cancelButtonName = null\n" + 
        "showOkButton = true\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
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
    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.xml", "");
  
    initProjectController(FIRST_LOCAL_TEST_REPOSITORY, firstLocalRepo);
    initUtilAccess();

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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
    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(gitAccess.hasFilesChanged());
    assertTrue(GitValidationUtil.hasUncommitedChanges(true));
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Warning32.png\n" + 
        "targetFiles = null\n" + 
        "message = null\n" + 
        "questionMessage = Push_Validation_Uncommited_Changes\n" + 
        "okButtonName = Stash_And_Continue\n" + 
        "cancelButtonName = null\n" + 
        "showOkButton = true\n" + 
        "showCancelButton = true";
     Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> expectedDialog.equals(dialogToString[0]));
  }

  /**
   * Sometimes is needed to override some methods from util access because it is mocked before.
   */
  private void initUtilAccess() {
    final UtilAccess originalUtilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
    final UtilAccess newUtilAccess =  new UtilAccess() {
      
      @Override
      public String uncorrectURL(String url) {
        return originalUtilAccess.uncorrectURL(url);
      }
      
      @Override
      public URL removeUserCredentials(URL url) {
       return originalUtilAccess.removeUserCredentials(url);
      }
      
      @Override
      public void removeCustomEditorVariablesResolver(EditorVariablesResolver resolver) {
       originalUtilAccess.removeCustomEditorVariablesResolver(resolver);
      }
      
      @Override
      public ImageHolder optimizeImage(URL imageUrl) throws IOException {
        return originalUtilAccess.optimizeImage(imageUrl);
      }
      
      @Override
      public String makeRelative(URL baseURL, URL childURL) {
        return originalUtilAccess.makeRelative(baseURL, childURL);
      }
      
      @Override
      public File locateFile(URL url) {
        return originalUtilAccess.locateFile(url);
      }
      
      @Override
      public boolean isUnhandledBinaryResourceURL(URL url) {
        return false;
      }
      
      @Override
      public boolean isSupportedImageURL(URL url) {
        return originalUtilAccess.isSupportedImageURL(url);
      }
      
      @Override
      public String getFileName(String urlPath) {
        return originalUtilAccess.getFileName(urlPath);
      }
      
      @Override
      public String getExtension(URL url) {
       return originalUtilAccess.getExtension(url);
      }
      
      @Override
      public String getContentType(String systemID) {
        return systemID;
      }
      
      @Override
      public String expandEditorVariables(String pathWithEditorVariables, URL currentEditedURL,
          boolean expandAskEditorVariables) {
        return originalUtilAccess.expandEditorVariables(pathWithEditorVariables, currentEditedURL, expandAskEditorVariables);
      }
      
      @Override
      public String expandEditorVariables(String pathWithEditorVariables, URL currentEditedURL) {
        return originalUtilAccess.expandEditorVariables(pathWithEditorVariables, currentEditedURL);
      }
      
      @Override
      public String encrypt(String toEncrypt) {
        return originalUtilAccess.encrypt(toEncrypt);
      }
      
      @Override
      public String decrypt(String toDecrypt) {
        return originalUtilAccess.decrypt(toDecrypt);
      }
      
      @Override
      public Reader createReader(URL url, String defaultEncoding) throws IOException {
        return originalUtilAccess.createReader(url, defaultEncoding);
      }
      
      @Override
      public BufferedImage createImage(String imageUrl) throws IOException {
        return originalUtilAccess.createImage(imageUrl);
      }
      
      @Override
      public String correctURL(String url) {
        return originalUtilAccess.correctURL(url);
      }
      
      @Override
      public URL convertFileToURL(File file) throws MalformedURLException {
        return originalUtilAccess.convertFileToURL(file);
      }
      
      @Override
      public void addCustomEditorVariablesResolver(EditorVariablesResolver resolver) {
        originalUtilAccess.addCustomEditorVariablesResolver(resolver);
      }
    };
    
    Mockito.when(PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess()).thenReturn(newUtilAccess);
  }

  /**
   * <p><b>Description:</b> Tests the default options for pre-push validation.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50586</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushtValidationDefaultValue() {
    assertFalse(OptionsManager.getInstance().isMainFilesValidatedBeforePush());
    assertFalse(OptionsManager.getInstance().isPushRejectedOnValidationProblems());
  }


  /**
   * <p><b>Description:</b> If we don't have any push ahead, let's not validate the project before the push.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50639</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenNoPushesAhead() throws Exception {
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

    initProjectController(SECOND_LOCAL_TEST_REPOSITORY, secondLocalRepo);
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

    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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
    int noOfPushes = 0;
    try {
      noOfPushes = GitAccess.getInstance().getPushesAhead();
    } catch (Exception e){
      noOfPushes = -1;
    }
    assertFalse(noOfPushes > 0);
    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertFalse(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertTrue(ValidationManager.getInstance().checkPushValid());
    assertNull(dialogToString[0]); // The validation will be not started and no dialog occur.
  }

  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when the Main files support is disabled or no files are in Main files.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50660</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenNoMainFilesSupport() throws Exception {
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    
    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.txt", "");

    // Configure the custom project controller.
    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final ProjectController projectController = createProjectControllerForTest(
        firstLocalRepo.getDirectory().toURI().toURL(), new ArrayList<URL>().iterator());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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
    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertFalse(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Error32.png\n" + 
        "targetFiles = null\n" + 
        "message = Main_Files_Support_Not_Enabled\n" + 
        "questionMessage = null\n" + 
        "okButtonName = null\n" + 
        "cancelButtonName = Close\n" + 
        "showOkButton = false\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
  }
  
  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when at least one main file was not found.</p>
   * 
   * <p><b>Bug ID:</b> EXM-52337</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationWhenMainFileNotFound() throws Exception {
    OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    // Disable main files support
    commitOneFile(FIRST_LOCAL_TEST_REPOSITORY, "ttt.txt", "");

    // Configure the custom project controller.
    final StandalonePluginWorkspace spw =  (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final List<URL> mainFilesURLs = new ArrayList<>();
    mainFilesURLs.add(new File(FIRST_LOCAL_TEST_REPOSITORY).toURI().toURL());
    mainFilesURLs.add(new File("blabla").toURI().toURL());
    final ProjectController projectController = createProjectControllerForTest(
        firstLocalRepo.getDirectory().toURI().toURL(), mainFilesURLs.iterator());
    Mockito.when(spw.getProjectManager()).thenReturn(projectController);

    // Create a custom dialog to return a custom result. Usefully to simulate a dialog showing.
    final int[] dialogResult = new int[1];
    dialogResult[0] = OKCancelDialog.RESULT_OK;
    final MessageDialog dialog = Mockito.mock(MessageDialog.class);
    Mockito.when(dialog.getResult()).then((Answer<Integer>) 
        invocation -> {
          return dialogResult[0];
        }); 

    final String dialogToString[] = new String[1];

    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_push", DialogType.ERROR) {

      @Override
      public MessageDialog buildAndShow() {
        dialogToString[0] = dialogInfo.toString();
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
    ValidationManager.getInstance().setPrePushValidator(new PrePushValidation(
        validator, null));
    assertTrue(PluginWorkspaceProvider.getPluginWorkspace() instanceof StandalonePluginWorkspace);
    assertTrue(ValidationManager.getInstance().isPrePushValidationEnabled());
    assertFalse(ValidationManager.getInstance().checkPushValid());
    final String expectedDialog = "title = Pre_Push_Validation\n" + 
        "iconPath = /images/Error32.png\n" + 
        "targetFiles = null\n" + 
        "message = Pre_Push_Main_Files_Not_Found_Message\n" + 
        "questionMessage = null\n" + 
        "okButtonName = null\n" + 
        "cancelButtonName = Close\n" + 
        "showOkButton = false\n" + 
        "showCancelButton = true";
    assertEquals(expectedDialog, dialogToString[0]);
  }

  /**
   * Create a custom project controller used in tests.
   * 
   * @param currentProjectURL The current project URL.
   * @param mainFilesIterator An iterator for Main files list. If there are no Main files, this iterator should be empty.
   * 
   * @return The created project controller.
   */
  private ProjectController createProjectControllerForTest(@NonNull final URL currentProjectURL, 
      @NonNull final Iterator<URL> mainFilesIterator) {
    return new ProjectController() {

      @Override
      public void addProjectChangeListener(ProjectChangeListener projectChangeListener) {
        // Not needed.
      }

      @Override
      public void removeProjectChangeListener(ProjectChangeListener projectChangeListener) {
        // Not needed.
      }

      @Override
      public URL getCurrentProjectURL() {
        return currentProjectURL;
      }

      @Override
      public void addPopUpMenuCustomizer(ProjectPopupMenuCustomizer popUpCustomizer) {
        // Not needed.
      }

      @Override
      public void removePopUpMenuCustomizer(ProjectPopupMenuCustomizer popUpCustomizer) {
        // Not needed.
      }

      @Override
      public File[] getSelectedFiles() {
        return null;
      }

      @Override
      public void refreshFolders(File[] folders) {
        // Not needed.
      }

      @Override
      public void addLinksToFoldersInProjectRoot(File[] folders) {
        // Not needed.
      }

      @Override
      public void addRendererCustomizer(ProjectRendererCustomizer rendererCustomizer) {
        // Not needed.
      }

      @Override
      public void removeRendererCustomizer(ProjectRendererCustomizer rendererCustomizer) {
        // Not needed.
      }

      @Override
      public void loadProject(File project) {
        // Not needed.
      }

      /**
       * This method is created to perform a successfully extraction on @OxygenAPIWrapper.
       * This method is available from Oxygen 25.
       * 
       * @return The main files iterator.
       */
      @Override
      public Iterator<URL> getMainFileResourcesIterator() {
        return mainFilesIterator;
      }

      @Override
      public ProjectIndexer getProjectIndexer() {
        return null;
      }
    };
    
  }
}
