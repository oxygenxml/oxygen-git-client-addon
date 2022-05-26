package com.oxygenxml.git.validation;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.ProjectHelper;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.util.CollectionsUtil;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.results.ResultsManager.ResultType;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Manage the validations.
 * 
 * @author alex_smarandache
 *
 */
public class ValidationManager {

  /**
   * The commit files validator.
   */
  private IValidator commitFilesValidator;

  /**
   * The push main files validator.
   */
  private IValidator pushMainFilesValidator;
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationManager.class);

  /**
   * The translator for the messages that are displayed in this panel
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * Manager for options.
   */
  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  /**
   * The result manager.
   */
  private static final ResultsManager RESULT_MANAGER = PluginWorkspaceProvider.getPluginWorkspace().getResultsManager();


  /**
   * Hidden constructor.
   */
  private ValidationManager() {
    commitFilesValidator   = new FilesValidator(new ProblemsCollector());
    pushMainFilesValidator = new FilesValidator(new ProblemsCollector());
  }

  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class SingletonHelper {
    static final ValidationManager INSTANCE = new ValidationManager();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  public static ValidationManager getInstance() {
    return SingletonHelper.INSTANCE;
  }

  /**
   * @param preCommitFilesValidator The new pre-commit validator.
   */
  @TestOnly
  public void setPreCommitFilesValidator(final IValidator preCommitFilesValidator) {
    this.commitFilesValidator = preCommitFilesValidator;
  }

  /**
   * @param prePushFilesValidator The new pre-push validator.
   */
  @TestOnly
  public void setPrePushFilesValidator(final IValidator prePushFilesValidator) {
    this.pushMainFilesValidator = prePushFilesValidator;
  }
  
  /**
   * @return <code>true</code> if the files can and should be validated before commit.
   */
  public boolean isPreCommitValidationEnabled() {
    return OPTIONS_MANAGER.isFilesValidatedBeforeCommit() && isAvailable();
  }

  /**
   * @return <code>true</code> if the validation support is available(for Oxygen 25 and newer).
   */
  public boolean isAvailable() {
    return commitFilesValidator.isAvailable();
  }

  /**
   * Validate files before to commit.
   * 
   * @param files The files to be validated.
   *  
   * @return <code>true</code> if no problems are found.
   */
  public boolean validateFilesBeforeCommit(final List<URL> files) {
    commitFilesValidator.validate(files);
    return commitFilesValidator.getCollector().isEmpty();
  }

  /**
   * Validate main files before to push.
   * 
   * @param files The files to be validated.
   *  
   * @return <code>true</code> if no problems are found.
   */
  public boolean validateMainFilesBeforePush(final List<URL> files) {
    pushMainFilesValidator.validate(files);
    return pushMainFilesValidator.getCollector().isEmpty();
  }

  /**
   * Show problems that occurs on last commit validation.
   * 
   * @return <code>true</code> if the commit can be performed.
   */
  public boolean showCommitFilesProblems() {
    final List<DocumentPositionedInfo> problems = Arrays.asList(
        commitFilesValidator.getCollector().getAll());
    RESULT_MANAGER.setResults(TRANSLATOR.getTranslation(Tags.PRE_COMMIT_VALIDATION), 
        problems, ResultType.PROBLEM);
    boolean toReturn = false;
    if(OPTIONS_MANAGER.isCommitRejectedOnValidationProblems()) {
      MessagePresenterProvider.getBuilder(TRANSLATOR.getTranslation(
          Tags.PRE_COMMIT_VALIDATION), DialogType.ERROR)
      .setOkButtonVisible(false)
      .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
      .setMessage(TRANSLATOR.getTranslation(Tags.FAILED_COMMIT_VALIDATION_MESSAGE))
      .buildAndShow();
    } else {
      toReturn = MessagePresenterProvider.getBuilder(TRANSLATOR.getTranslation(
          Tags.PRE_COMMIT_VALIDATION), DialogType.WARNING)
          .setMessage(TRANSLATOR.getTranslation(Tags.FAILED_COMMIT_VALIDATION_MESSAGE))
          .setOkButtonName(Tags.COMMIT_ANYWAY)
          .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CANCEL))
          .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
    }

    return toReturn;
  }

  /**
   * Check all staged files without missed or removed files.
   * <br>
   * If problems are detected, are presented for a user confirmation if the commit should be canceled or performed.
   * 
   * @see GitChangeType
   * 
   * @return <code>true</code> if the commit resources are valid to perform a commit.
   */
  public boolean checkCommitValid() {
    boolean performCommit = true;
    if(isPreCommitValidationEnabled()) {
      if(!hasUncommitedChanges(false)) {     
        performCommit = false;
        MessagePresenterProvider
        .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_COMMIT_VALIDATION), DialogType.ERROR)
        .setOkButtonVisible(false)
        .setMessage(TRANSLATOR.getTranslation(Tags.COMMIT_VALIDATION_UNSTAGED_FILES))
        .buildAndShow();
      }
      if(performCommit && !validateFilesBeforeCommit(
          FileStatusUtil.getFilesStatuesURL(
              GitAccess.getInstance().getStagedFiles().stream().filter(
                  file -> file.getChangeType() != GitChangeType.REMOVED && 
                  file.getChangeType() != GitChangeType.MISSING)
              .collect(Collectors.toList())
              , false)
          )) {
        performCommit = showCommitFilesProblems();
      }
    }

    return performCommit;
  }

  /**
   * @return <code>true</code> if the pre push validation is enabled.
   */
  public boolean isPrePushValidationEnabled() {
    return OPTIONS_MANAGER.isMainFilesValidatedBeforePush();
  }

  /**
   * Check all main files to test if the project is valid or no.
   * <br>
   * If problems are detected, are presented for a user confirmation if the push should be canceled or performed.
   * 
   * @see GitChangeType
   * 
   * @return <code>true</code> if the project is valid and the push should be performed.
   */
  public boolean checkPushValid() {
    boolean performPush = true;
    final PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    if(isPrePushValidationEnabled() && pluginWorkspace instanceof StandalonePluginWorkspace) {
      final StandalonePluginWorkspace standalonePluginWorkspace = (StandalonePluginWorkspace)pluginWorkspace;
      performPush = validateProject(standalonePluginWorkspace);
    }   

    return performPush;
  }

  /**
   * Validate the current project opened in Git Staging.
   *
   * @param standalonePluginWorkspace The Standalone Plugin Workspace.
   * 
   * @return <code>true</code> if the push operation could be performed, <code>false</code> otherwise.
   */
  private boolean validateProject(final StandalonePluginWorkspace standalonePluginWorkspace) {
    boolean performPush = true; 
    final URL currentProjectURL = standalonePluginWorkspace.getProjectManager().getCurrentProjectURL(); 
    Optional<RevCommit> stash = Optional.empty();
    try {
      if(currentProjectURL == null ||!RepoUtil.isEqualsWithCurrentRepo(new File(currentProjectURL.toURI()))) {
        performPush = treatNotSameProjectCase(standalonePluginWorkspace);
      } 
      
      // treat case with validation could not be performed because there are uncommited changes.
      if(performPush && hasUncommitedChanges(true)) { 
        performPush = MessagePresenterProvider
            .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.WARNING)
            .setQuestionMessage(TRANSLATOR.getTranslation(Tags.PUSH_VALIDATION_UNCOMMITED_CHANGES))
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.STASH))
            .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
        if(performPush) {
          stash = Optional.ofNullable(GitAccess.getInstance().createStash(true, 
              TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION) + ": " + new Date()));
        }
      }
      
      if(performPush && !validateMainFilesBeforePush(CollectionsUtil.toList(
          OxygenAPIWrapper.getInstance().getMainFileResourcesIterator()))) {
        performPush = showPushFilesProblems(stash.isPresent()? 
            TRANSLATOR.getTranslation(Tags.PUSH_VALIDATION_FAILED_WITH_STASH) :
            TRANSLATOR.getTranslation(Tags.PUSH_VALIDATION_FAILED) );
        stash = Optional.empty(); // don't apply the stash if problems were found
      }
    } catch (URISyntaxException e) {
      standalonePluginWorkspace.showErrorMessage(e.getMessage());
      performPush = false;
    } finally {
      removeStashIfNeeded(stash);
    }
    
    return performPush;
  }

  /**
   * This case occurs when the project loaded "Project" View is not the same with project loaded in Git Staging.
   * 
   * @param standalonePluginWorkspace The Standalone Plugin Workspace.
   * 
   * @return <code>true</code> if the push can be performed.
   */
  private boolean treatNotSameProjectCase(final StandalonePluginWorkspace standalonePluginWorkspace) {
    boolean performPush = MessagePresenterProvider 
        .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.WARNING)
        .setQuestionMessage(TRANSLATOR.getTranslation(Tags.NOT_SAME_PROJECT_MESSAGE))
        .setOkButtonName(TRANSLATOR.getTranslation(Tags.LOAD))
        .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
    if(performPush) {
      final Optional<File> currentProjectXprFile = Optional.ofNullable(
          ProjectHelper.getInstance().findXPRFromCurrentGitProject());
      performPush = currentProjectXprFile.isPresent();
      if(performPush) {
        standalonePluginWorkspace.getProjectManager().loadProject(currentProjectXprFile.get());
      } else {
        standalonePluginWorkspace.showErrorMessage(TRANSLATOR.getTranslation(Tags.NO_XPR_FILE_FOUND_MESSAGE));
      }
    }
    return performPush;
  }

  /**
   * This method remove the stash(if this exists) created to store uncommited changes.
   * 
   * @param stash The created stash or empty @Optional if no stash were created.
   */
  private void removeStashIfNeeded(final Optional<RevCommit> stash) {
    stash.ifPresent(st -> 
    {
      try {
        GitAccess.getInstance().popStash(st.toObjectId().getName());
      } catch (GitAPIException e) {
        LOGGER.error(e.getMessage(), e);
      }
    });
  }

  /**
   * Show problems that occurs on last commit validation.
   * 
   * @param message Message to presents the problems.
   * 
   * @return <code>true</code> if the commit can be performed.
   */
  public boolean showPushFilesProblems(final String message) {
    final List<DocumentPositionedInfo> problems = Arrays.asList(
        pushMainFilesValidator.getCollector().getAll());
    RESULT_MANAGER.setResults(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), 
        problems, ResultType.PROBLEM);
    boolean toReturn = false;
    if(OPTIONS_MANAGER.isPushRejectedOnValidationProblems()) {
      MessagePresenterProvider.getBuilder(TRANSLATOR.getTranslation(
          Tags.PRE_PUSH_VALIDATION), DialogType.ERROR)
      .setOkButtonVisible(false)
      .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
      .setMessage(message)
      .buildAndShow();
    } else {
      toReturn = MessagePresenterProvider.getBuilder(TRANSLATOR.getTranslation(
          Tags.PRE_PUSH_VALIDATION), DialogType.WARNING)
          .setMessage(message)
          .setOkButtonName(TRANSLATOR.getTranslation(Tags.PUSH_ANYWAY))
          .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CANCEL))
          .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
    }

    return toReturn;
  }
  
  /**
   * @param includeStagedFiles <code>true</code> if the staged files should be included.
   * 
   * @return <code>true</code> if there are uncommited changes without ".xpr" files.
   */
  private boolean hasUncommitedChanges(final boolean includeStagedFiles) {
    final List<FileStatus> unstagedFiles = GitAccess.getInstance().getUnstagedFiles();
    FileStatusUtil.removeUnreachableFiles(unstagedFiles);
    boolean toReturn = !unstagedFiles.isEmpty();
    if(includeStagedFiles && !toReturn) {
      final List<FileStatus> stagedFiles = GitAccess.getInstance().getStagedFiles();
      FileStatusUtil.removeUnreachableFiles(stagedFiles);
      toReturn = !stagedFiles.isEmpty();
    }
    return toReturn;
  }
  
}
