package com.oxygenxml.git.validation;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.results.ResultsManager.ResultType;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Manage the validations.
 * 
 * @author alex_smarandache
 *
 */
public class ValidationManager {

  /**
   * The commit files validatior.
   */
  private IValidator commitFilesValidator;
  
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
    commitFilesValidator = new FilesValidator(new ProblemsCollector());
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
   * @param filesValidator The new validator for files.
   */
  @TestOnly
  public void setFilesValidator(final IValidator filesValidator) {
    this.commitFilesValidator = filesValidator;
  }
  
  /**
   * @return <code>true</code> if the files can and should be validated before commit.
   */
  public boolean isPreCommitValidationEnabled() {
    return OPTIONS_MANAGER.isFilesValidatedBeforeCommit() && commitFilesValidator.isAvailable();
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
          Tags.PRE_COMMIT_VALIDATION), DialogType.ERROR)
      .setMessage(TRANSLATOR.getTranslation(Tags.FAILED_COMMIT_VALIDATION_MESSAGE))
      .setOkButtonName(Tags.COMMIT_ANYWAY)
      .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
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
    if(isPreCommitValidationEnabled() 
        && !validateFilesBeforeCommit(
        FileStatusUtil.getFilesStatuesURL(
            GitAccess.getInstance().getStagedFiles().stream().filter(
                file -> file.getChangeType() != GitChangeType.REMOVED && 
                file.getChangeType() != GitChangeType.MISSING)
            .collect(Collectors.toList()))
        )) {
      performCommit = showCommitFilesProblems();
    }
    
    return performCommit;
  }
  
}
