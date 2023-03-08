package com.oxygenxml.git.editorvars;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
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
  private static final Translator TRANSLATOR = Translator.getInstance();
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GitEditorVariablesResolver.class);
  /**
   * Editor variable name to resolved value.
   */
  private final Map<String, String> editorVarsCache = new HashMap<>();
  
  /**
   * Git event listener.
   */
  private final GitEventAdapter gitEventListener = new GitEventAdapter() {
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
  private final GitControllerBase gitController;
  
  /**
   * Constructor.
   * <br>
   * Note: Make sure that the options all loaded in @OptionsManager before call the constructor 
   * for a good initialization if the repository is not opened yet.
   * 
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
    initCacheIfNeeded();
    contentWithEditorVariables = resolveBranchEditorVariables(contentWithEditorVariables);
    try {
      contentWithEditorVariables = resolveWorkingCopyEditorVariables(contentWithEditorVariables);
    } catch (NoRepositorySelected e) {
      LOGGER.error(e.getMessage(), e);
    }
    return contentWithEditorVariables;
  }

  /**
   * Used to set initial values for cache if the repository is not initialized.
   */
  private void initCacheIfNeeded() {
    if(editorVarsCache.isEmpty() && !GitAccess.getInstance().isRepoInitialized()) {
      final String selectedRepo = OptionsManager.getInstance().getSelectedRepository();
      if(selectedRepo != null && !selectedRepo.isEmpty()) {  
        try {
          final File gitDir = new File(selectedRepo);
          final URL gitProjectURL = gitDir.toURI().toURL();
          editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR, 
              gitProjectURL.toExternalForm());
          editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR, gitDir.getName());
          final File repoPath = new File(selectedRepo + "/.git");
          try(final Git git = Git.open(repoPath)) {
            final Repository repo = git.getRepository();
            final String fullNameBranch = repo.getFullBranch();
            final String shortNameBranch = repo.getBranch();
            editorVarsCache.put(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR, fullNameBranch); 
            editorVarsCache.put(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR, shortNameBranch); 
          } 
        } catch (IOException e) {
          LOGGER.error(e.getMessage(), e);      
        }
        editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR, selectedRepo); 
      }
    }    
  }

  /**
   * Resolve the working-copy-related editor variables.
   * 
   * @param contentWithEditorVariables The initial content.
   * 
   * @return the updated content.
   * 
   * @throws NoRepositorySelected when no repo is selected.
   */
  @SuppressWarnings("java:S3824") // Supress this because we don't want to compute the WC unnecessarily
  private String resolveWorkingCopyEditorVariables(String contentWithEditorVariables) throws NoRepositorySelected {
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

    // Working copy URL
    if (contentWithEditorVariables.contains(GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR)) {
      String wcURL = editorVarsCache.get(GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR);
      if (wcURL == null) {
        if (workingCopy == null) {
          workingCopy = gitController.getGitAccess().getWorkingCopy();
        }
        try {
          wcURL = workingCopy.getAbsoluteFile().toURI().toURL().toString();
          editorVarsCache.put(GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR, wcURL);
        } catch (MalformedURLException e) {
          LOGGER.error(e.getMessage(), e);
        }
      }
      if  (wcURL != null) {
        contentWithEditorVariables = contentWithEditorVariables.replace(
            GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR,
            wcURL);
      }
    } 
    return contentWithEditorVariables;
  }

  /**
   * Resolve the branch-related editor variables.
   * 
   * @param contentWithEditorVariables the initial content.
   * 
   * @return the content after expanding the editor variables.
   */
  private String resolveBranchEditorVariables(String contentWithEditorVariables) {
    if (contentWithEditorVariables.contains(GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR)) {
      contentWithEditorVariables = resolveShortBranchName(contentWithEditorVariables);
    }
    if (contentWithEditorVariables.contains(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR)) {
      try {
        contentWithEditorVariables = resolveFullBranchName(contentWithEditorVariables);
      } catch (IOException | NoRepositorySelected e) {
        LOGGER.error(e.getMessage(), e);
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
   * 
   * @throws NoRepositorySelected 
   * @throws IOException 
   */
  @SuppressWarnings("java:S3824")
  private String resolveFullBranchName(String contentWithEditorVariables) throws IOException, NoRepositorySelected {
    String branch = editorVarsCache.get(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR);
    if (branch == null) {
      branch = gitController.getGitAccess().getRepository().getFullBranch();
      editorVarsCache.put(GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR, branch);
    }
    contentWithEditorVariables = contentWithEditorVariables.replace(
        GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
        branch);
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
    String branch = editorVarsCache.computeIfAbsent(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
        k -> gitController.getGitAccess().getBranchInfo().getBranchName());
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
            TRANSLATOR.getTranslation(Tags.SHORT_BRANCH_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
            TRANSLATOR.getTranslation(Tags.FULL_BRANCH_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR,
            TRANSLATOR.getTranslation(Tags.WORKING_COPY_NAME_DESCRIPTION)));
    list.add(
        new EditorVariableDescription(
            GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR,
            TRANSLATOR.getTranslation(Tags.WORKING_COPY_PATH_DESCRIPTION)));
    list.add(
       new EditorVariableDescription(
            GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR,
            TRANSLATOR.getTranslation(Tags.WORKING_COPY_URL_DESCRIPTION)));
    return list;
  }
  
  public Map<String, String> getEditorVarsCacheFromTests() {
    return editorVarsCache;
  }
  
  public GitEventAdapter getGitEventListenerFromTests() {
    return gitEventListener;
  }
}
