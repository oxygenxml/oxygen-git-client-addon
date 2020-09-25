package com.oxygenxml.git.editorvars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

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
  private static final Translator translator = Translator.getInstance();
  /**
   * GitAccess instance.
   */
  private static final GitAccess gitAccess = GitAccess.getInstance();
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitEditorVariablesResolver.class);
  
  /**
   * @see ro.sync.exml.workspace.api.util.EditorVariablesResolver.resolveEditorVariables(String, String)
   */
  @Override
  public String resolveEditorVariables(String contentWithEditorVariables, String currentEditedFileURL) {
    // Local branch short name / full name vars
    contentWithEditorVariables = contentWithEditorVariables.replace(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
        gitAccess.getBranchInfo().getBranchName());

    try {
      contentWithEditorVariables = contentWithEditorVariables.replace(
          GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
          gitAccess.getRepository().getFullBranch());
    } catch (NoRepositorySelected | IOException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }

    // Working-copy-related vars
    try {
      File workingCopy = gitAccess.getWorkingCopy();
      contentWithEditorVariables = contentWithEditorVariables.replace(
          GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR,
          workingCopy.getName());
      contentWithEditorVariables = contentWithEditorVariables.replace(
          GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR,
          workingCopy.getAbsolutePath());
    } catch (NoRepositorySelected e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }
    return contentWithEditorVariables;
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
}
