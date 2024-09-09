package com.oxygenxml.git.validation.gitoperation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.oxygenxml.git.ProjectHelper;
import com.oxygenxml.git.ProjectHelper.LoadProjectOperationStatus;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RepoNotInitializedException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.validation.internal.IPreOperationValidation;
import com.oxygenxml.git.validation.internal.IValidator;
import com.oxygenxml.git.validation.internal.ValidationListenersManager;
import com.oxygenxml.git.validation.internal.ValidationOperationInfo;
import com.oxygenxml.git.validation.internal.ValidationOperationType;
import com.oxygenxml.git.validation.internal.exception.MainFilesNotAvailableException;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.results.ResultsManager.ResultType;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Implements a pre-push validation.
 * 
 * @author alex_smarandache
 *
 */
public class PrePushValidation implements IPreOperationValidation {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PrePushValidation.class);

  /**
   * Notifies listeners for commit operation status.
   */
  private final Optional<ValidationListenersManager> listenersManager;

  /**
   * The push main files validator.
   */
  private Optional<IValidator> pushMainFilesValidator;

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
   * The util access class.
   */
  private final UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
  

  /**
   * Constructor.
   * 
   * @param commitFilesValidator  The push main files validator. If <code>null</code> this validation will be disabled.
   * @param listenersManager      Notifies listeners for commit operation status.
   */
  public PrePushValidation(@Nullable final IValidator pushMainFilesValidator, 
      @Nullable final ValidationListenersManager listenersManager
      ) {
    this.listenersManager = Optional.ofNullable(listenersManager);
    this.pushMainFilesValidator = Optional.ofNullable(pushMainFilesValidator);
  }

  @Override
  public boolean isEnabled() {
    boolean isEnabled = pushMainFilesValidator.isPresent() 
        && OPTIONS_MANAGER.isMainFilesValidatedBeforePush() 
        && pushMainFilesValidator.get().isAvailable();
    if(isEnabled) {
      try {
        isEnabled = GitAccess.getInstance().getPushesAhead() > 0;
      } catch (RepoNotInitializedException e) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }
        isEnabled = false;
      }
    }
    return isEnabled; 
  }

  @Override
  public boolean checkValid() {
    boolean performPush = true;
    final PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    if(isEnabled() && pluginWorkspace instanceof StandalonePluginWorkspace) {
      final StandalonePluginWorkspace standalonePluginWorkspace = (StandalonePluginWorkspace)pluginWorkspace;
      listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutStartOperation(
          new ValidationOperationInfo(ValidationOperationType.PRE_PUSH_VALIDATION)));
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
    final URL currentProjectURL = standalonePluginWorkspace.getProjectManager().getCurrentProjectURL(); 
    boolean canPerformPush = true; 
  
    Optional<RevCommit> stash = Optional.empty();
    final File currentProjectFile = currentProjectURL != null ? 
        utilAccess.locateFile(currentProjectURL) : null;
    try {
      if(currentProjectFile == null || !RepoUtil.isEqualsWithCurrentRepo(currentProjectFile)) {
        canPerformPush = treatNotSameProjectCase();
      } 
      List<URL> mainFiles = new ArrayList<>(); 
      
      if(canPerformPush) {
        mainFiles = computeMainFiles(); 
        canPerformPush = !mainFiles.isEmpty();
      }

      // treat case with validation could not be performed because there are uncommited changes.
      if(canPerformPush && GitValidationUtil.hasUncommitedChanges(true)) { 
        canPerformPush = MessagePresenterProvider
            .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.WARNING)
            .setQuestionMessage(TRANSLATOR.getTranslation(Tags.PUSH_VALIDATION_UNCOMMITED_CHANGES))
            .setOkButtonName(TRANSLATOR.getTranslation(Tags.STASH_AND_CONTINUE))
            .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
        if(canPerformPush) {
          stash = Optional.ofNullable(GitAccess.getInstance().createStash(true, 
              TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION) + ": " + new Date()));
        }
      }

      if(canPerformPush) {
        if(!validateMainFilesBeforePush(mainFiles)) {
          canPerformPush = showPushFilesProblems(MessageFormat.format(TRANSLATOR.getTranslation(
              stash.isPresent()? Tags.PUSH_VALIDATION_FAILED_WITH_STASH : Tags.PUSH_VALIDATION_FAILED), 
              TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION)));

          stash = Optional.empty(); // don't apply the stash if problems were found
        }
        listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutFinishedOperation(
            new ValidationOperationInfo(ValidationOperationType.PRE_PUSH_VALIDATION)));
      } else {
        listenersManager.ifPresent(listeners -> listeners.notifyListenersAboutCanceledOperation(
            new ValidationOperationInfo(ValidationOperationType.PRE_PUSH_VALIDATION)));
      }

    } finally {
      removeStashIfNeeded(stash);
    }

    return canPerformPush;
  }

  /**
   * If the current project has Main files, this method will only collect then into the given list.
   * Else, a dialog box will be displayed to treat some exception that occur.
   * 
   * @return The list with current project's Main files or an empty list if there are no Main files.
   */
  private List<URL> computeMainFiles() {
    List<URL> mainFiles = Collections.emptyList();
    try {
      mainFiles = getMainFiles();
      final List<File> notFoundMainFiles = mainFiles.stream().map(utilAccess::locateFile)
          .filter(file -> !file.exists()).collect(Collectors.toList());
      if(!notFoundMainFiles.isEmpty()) {
        final Map<String, String> notFoundFilesWithTooltips = new HashMap<>();
        for(File file : notFoundMainFiles) {
          notFoundFilesWithTooltips.put(file.getName(), file.getAbsolutePath());
        }
        MessagePresenterProvider
        .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.ERROR)
        .setMessage(TRANSLATOR.getTranslation(Tags.PRE_PUSH_MAIN_FILES_NOT_FOUND_MESSAGE))
        .setOkButtonVisible(false)
        .setTargetResourcesWithTooltips(notFoundFilesWithTooltips)
        .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
        .buildAndShow();
        mainFiles = Collections.emptyList();
      }
    } catch (MainFilesNotAvailableException e) {
      MessagePresenterProvider
      .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.ERROR)
      .setMessage(e.getMessage())
      .setOkButtonVisible(false)
      .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
      .buildAndShow();
    }
    
    return mainFiles;
  }

  /**
   * Get the Main files for current project.
   * 
   * @return The list of Main files for current project.
   * 
   * @throws MainFilesNotAvailableException  When the project has no Main files.
   */
  @NonNull
  private List<URL> getMainFiles() throws MainFilesNotAvailableException {
    final List<URL> mainFiles = ImmutableList.copyOf(
        ((StandalonePluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace())
        .getProjectManager().getMainFileResourcesIterator());
    if(mainFiles.isEmpty()) {
      throw new MainFilesNotAvailableException();
    }
    return mainFiles;
  }

  /**
   * This case occurs when the project loaded "Project" View is not the same with project loaded in Git Staging.
   * 
   * @return <code>true</code> if the push can be performed.
   */
  private boolean treatNotSameProjectCase() {
    boolean canPerformPush = MessagePresenterProvider 
        .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.WARNING)
        .setQuestionMessage(TRANSLATOR.getTranslation(Tags.NOT_SAME_PROJECT_MESSAGE))
        .setOkButtonName(TRANSLATOR.getTranslation(Tags.LOAD))
        .buildAndShow().getResult() == OKCancelDialog.RESULT_OK;
    if(canPerformPush) {
      try {
        final LoadProjectOperationStatus projectLoadResult = tryToLoadProject();
        if(projectLoadResult == LoadProjectOperationStatus.PROJECT_NOT_FOUND) {
          MessagePresenterProvider 
          .getBuilder(TRANSLATOR.getTranslation(Tags.PRE_PUSH_VALIDATION), DialogType.ERROR)
          .setMessage(TRANSLATOR.getTranslation(Tags.NO_XPR_FILE_FOUND_MESSAGE))
          .setOkButtonVisible(false)
          .setCancelButtonName(Tags.CLOSE)
          .buildAndShow();
          canPerformPush = false;
        } else if(projectLoadResult == LoadProjectOperationStatus.CANCELED_BY_USER) {
          canPerformPush = false;
        }
      } catch (NoRepositorySelected e) {
        LOGGER.error(e.getMessage(), e); // should not happen
      }
    }
    
    return canPerformPush;
  }

  /**
   * Try to load an Oxygen project from the current repository.
   * 
   * @return The result of this load operation.
   * 
   * @throws NoRepositorySelected When no repository is selected.
   */
  private LoadProjectOperationStatus tryToLoadProject() throws NoRepositorySelected {
  LoadProjectOperationStatus projectLoadResult = LoadProjectOperationStatus.PROJECT_NOT_FOUND;
    try {
      File repoDir = GitAccess.getInstance().getRepository().getDirectory().getParentFile();
      projectLoadResult = ProjectHelper.getInstance().openOxygenProjectFromLoadedRepository(repoDir);
    } catch(MalformedURLException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return projectLoadResult;
  }

  /**
   * This method remove the stash(if this exists) created to store uncommited changes.
   * 
   * @param stash The created stash or empty @Optional if no stash were created.
   */
  private void removeStashIfNeeded(final Optional<RevCommit> stash) {
    stash.ifPresent(st -> {
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
        pushMainFilesValidator.get().getCollector().getAll()); // NOSONAR Already verified before call this method in @isAvailable.
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
   * Validate main files before to push.
   * 
   * @param files The files to be validated.
   *  
   * @return <code>true</code> if no problems are found.
   */
  private boolean validateMainFilesBeforePush(final List<URL> files) {
    boolean toReturn = true;
    if(pushMainFilesValidator.isPresent()) {
      pushMainFilesValidator.get().validate(files);
      toReturn = pushMainFilesValidator.get().getCollector().isEmpty();
    }
    return toReturn;
  }

}
