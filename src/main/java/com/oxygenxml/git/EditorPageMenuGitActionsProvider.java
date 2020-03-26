package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Provides Git-specific actions for the contextual menu of the Project view.
 * 
 * @author sorin_carbunaru
 */
public class EditorPageMenuGitActionsProvider {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(EditorPageMenuGitActionsProvider.class.getName());
  
  /**
   * Translator.
   */
  private Translator translator = Translator.getInstance();
  
  /**
   * Git history view.
   */
  private ViewInfo gitHistoryViewInfo;

  /**
   * History controller.
   */
  private HistoryController historyCtrl;
  
  /**
   * Constructor.
   */
  public EditorPageMenuGitActionsProvider(HistoryController historyController) {
    this.historyCtrl = historyController;
    
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    pluginWS.addViewComponentCustomizer(
        viewInfo -> {
          if (OxygenGitPluginExtension.GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
            gitHistoryViewInfo = viewInfo;
          }
        });
  }

  /**
   * @return the Git-specific action for the current editor page.
   */
  public List<AbstractAction> getActionsForCurrentEditorPage(URL editorURL) {
    List<AbstractAction> actions = new ArrayList<>();

    if (!"file".equalsIgnoreCase(editorURL.getProtocol())) {
      return Collections.emptyList();
    }
    
    try {
      File file = new File(editorURL.toURI());
      boolean isFromGitRepo = FileHelper.isFromGitRepo(file);
      if (isFromGitRepo) {

        GitOperationScheduler gitOpScheduler = GitOperationScheduler.getInstance();
        AbstractAction showHistoryAction = new AbstractAction(translator.getTranslation(Tags.SHOW_HISTORY)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            gitOpScheduler.schedule(
                () -> ProjectAndEditorPageMenuActionsUtil.showHistory(
                    file,
                    historyCtrl,
                    getViewsToSetCursorOn()));
          }
        };
        AbstractAction showBlameAction = new AbstractAction(translator.getTranslation(Tags.SHOW_BLAME)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            gitOpScheduler.schedule(
                () -> ProjectAndEditorPageMenuActionsUtil.showBlame(
                    file,
                    historyCtrl,
                    getViewsToSetCursorOn()));
          }
        };

        showBlameAction.setEnabled(true);
        showHistoryAction.setEnabled(true);

        actions.add(showHistoryAction);
        actions.add(showBlameAction);
      }
    } catch (URISyntaxException e) {
      logger.error(e, e);
    }

    return actions;
  }
  
  /**
   * @return a list of views to shown a certain type of cursor on. Never <code>null</code>.
   */
  private List<ViewInfo> getViewsToSetCursorOn() {
    List<ViewInfo> views = new ArrayList<>();
    if (gitHistoryViewInfo != null) {
      views.add(gitHistoryViewInfo);
    }
    return views;
  }
  
}
