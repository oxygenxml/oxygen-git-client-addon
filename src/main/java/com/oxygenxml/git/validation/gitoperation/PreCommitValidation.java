package com.oxygenxml.git.validation.gitoperation;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.annotations.Nullable;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.validation.internal.IPreOperationValidation;
import com.oxygenxml.git.validation.internal.IValidator;
import com.oxygenxml.git.validation.internal.ValidationListenersManager;
import com.oxygenxml.git.validation.internal.ValidationOperationInfo;
import com.oxygenxml.git.validation.internal.ValidationOperationType;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.results.ResultsManager.ResultType;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Implements a pre-commit validation.
 * 
 * @author alex_smarandache
 *
 */
public class PreCommitValidation implements IPreOperationValidation {

  /**
   * Notifies listeners for commit operation status.
   */
  private final Optional<ValidationListenersManager> listenersManager;

  /**
   * The commit files validator.
   */
  private final Optional<IValidator> commitFilesValidator;
  
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
   * Constructor.
   * 
   * @param commitFilesValidator  The commit files validator. If <code>null</code> this validation will be disabled.
   * @param listenersManager      Notifies listeners for commit operation status.
   */
  public PreCommitValidation(@Nullable final IValidator commitFilesValidator, 
      @Nullable final ValidationListenersManager listenersManager
      ) {
    this.listenersManager = Optional.ofNullable(listenersManager);
    this.commitFilesValidator = Optional.ofNullable(commitFilesValidator);
  }

  @Override
  public boolean isEnabled() {
    return commitFilesValidator.isPresent() && OPTIONS_MANAGER.isFilesValidatedBeforeCommit() 
        && commitFilesValidator.get().isAvailable();
  }

  @Override
  public boolean checkValid() {
    boolean performCommit = true;
    if(isEnabled()) {
      performCommit = validateFilesBeforeCommit();
    }
    
    return performCommit;
  }

  /**
   * Validates files to be commited.
   * 
   * @return <code>true</code> if the commit could be performed.
   */
  private boolean validateFilesBeforeCommit() {
    boolean performCommit = true;
    listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutStartOperation(
        new ValidationOperationInfo(ValidationOperationType.PRE_COMMIT_VALIDATION)));
    
    if(GitValidationUtil.hasUncommitedChanges(false)) {     
      performCommit = false;
      MessagePresenterProvider
      .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_COMMIT_VALIDATION), DialogType.ERROR)
      .setOkButtonVisible(false)
      .setMessage(TRANSLATOR.getTranslation(Tags.COMMIT_VALIDATION_UNSTAGED_FILES))
      .buildAndShow();
    }
    
    if(performCommit) {
      if(!validateFilesBeforeCommit(
          FileStatusUtil.getFilesStatuesURL(
              GitAccess.getInstance().getStagedFiles().stream().filter(
                  file -> file.getChangeType() != GitChangeType.REMOVED && 
                  file.getChangeType() != GitChangeType.MISSING)
              .collect(Collectors.toList())
              , false)
          )) {
        performCommit = showCommitFilesProblems();
      }
      listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutFinishedOperation(
          new ValidationOperationInfo(ValidationOperationType.PRE_COMMIT_VALIDATION)));
    } else {
      listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutCanceledOperation(
          new ValidationOperationInfo(ValidationOperationType.PRE_COMMIT_VALIDATION)));
    }
    
    return performCommit;
  }

  /**
   * Validate files before to commit.
   * 
   * @param files The files to be validated.
   *  
   * @return <code>true</code> if no problems are found.
   */
  private boolean validateFilesBeforeCommit(final List<URL> files) {
    boolean toReturn = true;
    if(commitFilesValidator.isPresent()) {
      commitFilesValidator.get().validate(files);
      toReturn = commitFilesValidator.get().getCollector().isEmpty();
    }
    return toReturn;
  }
  
  /**
   * Show problems that occurs on last commit validation.
   * 
   * @return <code>true</code> if the commit can be performed.
   */
  private boolean showCommitFilesProblems() {
    final List<DocumentPositionedInfo> problems = Arrays.asList(
        commitFilesValidator.get().getCollector().getAll()); // NOSONAR Already verified before call this method in @isAvailable.
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
}
