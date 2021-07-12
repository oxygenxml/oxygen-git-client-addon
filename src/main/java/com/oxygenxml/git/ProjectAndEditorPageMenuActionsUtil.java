package com.oxygenxml.git;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Utility class related to the Project and editor page menu actions.
 */
public class ProjectAndEditorPageMenuActionsUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(ProjectAndEditorPageMenuActionsUtil.class.getName());
  
  /**
   * i18n
   */
  private static Translator translator = Translator.getInstance();
  
  /**
   * Hidden constructor.
   */
  private ProjectAndEditorPageMenuActionsUtil() {
    // Avoid instantiation
  }

  /**
   * Show blame for selected resource.
   * 
   * @param file                   The current file.
   * @param historyCtrl            History controller.
   * @param viewsToUpdateCursorFor A list of views for which to update the cursor (for example, make it busy).
   */
  public static void showBlame(
      File file,
      HistoryController historyCtrl,
      List<ViewInfo> viewsToUpdateCursorFor) {
    PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
    try {
      UIUtil.setBusyCursor(true, viewsToUpdateCursorFor);
      
      String repository = RepoUtil.getRepositoryForFile(file);
      if (repository != null) {
        RepoUtil.updateCurrentRepository(repository);
        
        URL fileURL = file.toURI().toURL();
        WSEditor editor = pluginWS.getEditorAccess(fileURL, PluginWorkspace.MAIN_EDITING_AREA);
        if (editor == null || !editor.isModified()) {
          doBlame(file, historyCtrl, repository);
        } else if (editor.isModified()) {
          // Ask for save
          int response = pluginWS.showConfirmDialog(
              translator.getTranslation(Tags.SHOW_BLAME),
              MessageFormat.format(
                  translator.getTranslation(Tags.THIS_OPERATION_REQUIRES_SAVING),
                  fileURL.toExternalForm()
              ),
              new String[] {
                  "   " + translator.getTranslation(Tags.YES) + "   ",
                  "   " + translator.getTranslation(Tags.NO) + "   "
              },
              new int[] { 0, 1 });
          if (response == 0) {
            editor.save();
            doBlame(file, historyCtrl, repository);
          }
        }
      }
    } catch (IOException e) {
      pluginWS.showErrorMessage("Repository opening failed due to: " + e.getMessage());
      logger.error(e, e);
    } finally {
      UIUtil.setBusyCursor(false, viewsToUpdateCursorFor);
    }
  }

  /**
   * Do blame.
   * 
   * @param file         The file.
   * @param historyCtrl  History-related stuff controller.
   * @param repository   Repository.
   */
  private static void doBlame(File file, HistoryController historyCtrl, String repository) {
    try { //NOSONAR: keep the try here
      String relativeFilePath = RepoUtil.getFilePathRelativeToRepo(file, repository);
      BlameManager.getInstance().doBlame(
          FileHelper.rewriteSeparator(relativeFilePath),
          historyCtrl);
    } catch (IOException | GitAPIException e1) {
      logger.error(e1, e1);
    }
  }
  
  /**
   * Show history for selected resource.
   * 
   * @param file                   The current file.
   * @param historyCtrl            History controller.
   * @param viewsToUpdateCursorFor A list of views for which to update the cursor (for example, make it busy).
   */
  public static void showHistory(
      File file,
      HistoryController historyCtrl,
      List<ViewInfo> viewsToUpdateCursorFor) {
    try {
      UIUtil.setBusyCursor(true, viewsToUpdateCursorFor);
      
      String repository = RepoUtil.getRepositoryForFile(file);
      if (repository != null) {
        RepoUtil.updateCurrentRepository(repository);
        
        String relativeFilePath = RepoUtil.getFilePathRelativeToRepo(file, repository);
        if (relativeFilePath.isEmpty()) {
          historyCtrl.showRepositoryHistory();
        } else {
          historyCtrl.showResourceHistory(FileHelper.rewriteSeparator(relativeFilePath));
        }
      }
    } catch (IOException e) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Repository opening failed due to: " + e.getMessage());
      
      logger.error(e, e);
    } finally {
      UIUtil.setBusyCursor(false, viewsToUpdateCursorFor);
    }
  }
  
}
