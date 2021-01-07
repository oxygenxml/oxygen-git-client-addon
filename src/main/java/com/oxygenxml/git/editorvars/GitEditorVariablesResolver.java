package com.oxygenxml.git.editorvars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.util.EditorVariableDescription;
import ro.sync.exml.workspace.api.util.EditorVariablesResolver;

/**
 * Resolver for Git-related editor variables.
 * 
 * @author Bogdan Draghici
 *
 */
public class GitEditorVariablesResolver extends EditorVariablesResolver {
  /**
   * Translator instance.
   */
  private static Translator translator = Translator.getInstance();
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitEditorVariablesResolver.class);
  /**
   * Editor variable name to resolved value.
   */
  private final Map<String, String> editorVarsCache = new HashMap<>();
  
  /**
   * Git event listener.
   */
  private GitEventAdapter gitEventListener = new GitEventAdapter() {
    @Override
    public void operationSuccessfullyEnded(GitEventInfo info) {
      if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
        editorVarsCache.clear();
      } else if (info.getGitOperation() == GitOperation.CHECKOUT) {
        editorVarsCache.remove(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR);
        editorVarsCache.remove(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR);
      }
    }
  };
  /**
   * Git operations controller.
   */
  private GitControllerBase gitController;
  
  /**
   * Constructor.
   * 
   * @param ctrl High level Git operations.
   */
  public GitEditorVariablesResolver(GitControllerBase ctrl) {
    this.gitController = ctrl;
    ctrl.addGitListener(gitEventListener);
  }
  
  /**
   * @see ro.sync.exml.workspace.api.util.EditorVariablesResolver.resolveEditorVariables(String, String)
   */
  @Override
  public String resolveEditorVariables(String contentWithEditorVariables, String currentEditedFileURL) {
    // Branch names (short or full)
    if (contentWithEditorVariables.contains(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR)) {
      contentWithEditorVariables = resolveShortBranchName(contentWithEditorVariables);
    }
    if (contentWithEditorVariables.contains(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR)) {
      contentWithEditorVariables = resolveFullBranchName(contentWithEditorVariables);
    }

    try {
        File workingCopy = null;

        // Working copy name
        if (contentWithEditorVariables.contains(GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR)) {
          String wcName = editorVarsCache.get(GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR);
          if (wcName == null) {
            workingCopy = gitController.getGitAccess().getWorkingCopy();
            wcName = workingCopy.getName();
            editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR, wcName);
          }
          contentWithEditorVariables = contentWithEditorVariables.replace(
              GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR,
              wcName);
        }

        // Working copy path
        if (contentWithEditorVariables.contains(GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR)) {
          String wcPath = editorVarsCache.get(GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR);
          if (wcPath == null) {
            if (workingCopy == null) {
              workingCopy = gitController.getGitAccess().getWorkingCopy();
            }
            wcPath = workingCopy.getAbsolutePath();
            editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR, wcPath);
          }
          contentWithEditorVariables = contentWithEditorVariables.replace(
              GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR,
              wcPath);
        }
    } catch (NoRepositorySelected e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }
    return contentWithEditorVariables;
  }

  /**
   * Resolve full branch name editor variable.
   * 
   * @param contentWithEditorVariables The original content.
   * 
   * @return the updated content.
   */
  private String resolveFullBranchName(String contentWithEditorVariables) {
    try {
      String branch = editorVarsCache.get(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR);
      if (branch == null) {
        branch = gitController.getGitAccess().getRepository().getFullBranch();
        editorVarsCache.put(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR, branch);
      }
      contentWithEditorVariables = contentWithEditorVariables.replace(
          GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
          branch);
    } catch (NoRepositorySelected | IOException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }
    return contentWithEditorVariables;
  }

  /**
   * Resolve short branch name editor variable.
   * 
   * @param contentWithEditorVariables The original content.
   * 
   * @return the updated content.
   */
  private String resolveShortBranchName(String contentWithEditorVariables) {
    String branch = editorVarsCache.get(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR);
    if (branch == null) {
      branch = gitController.getGitAccess().getBranchInfo().getBranchName();
      editorVarsCache.put(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR, branch);
    }
    return contentWithEditorVariables.replace(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
        branch);
  }

  /**
   * @see ro.sync.exml.workspace.api.util.EditorVariablesResolver.getCustomResolverEditorVariableDescriptions()
   */
  @Override
  public List<EditorVariableDescription> getCustomResolverEditorVariableDescriptions() {
    List<EditorVariableDescription> list = new ArrayList<>();
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
            translator.getTranslation(Tags.SHORT_BRANCH_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
            translator.getTranslation(Tags.FULL_BRANCH_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR,
            translator.getTranslation(Tags.WORKING_COPY_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR,
            translator.getTranslation(Tags.WORKING_COPY_PATH_DESCRIPTION)));
    return list;
  }
  
  public Map<String, String> getEditorVarsCacheFromTests() {
    return editorVarsCache;
  }
  
  public GitEventAdapter getGitEventListenerFromTests() {
    return gitEventListener;
  }
}
