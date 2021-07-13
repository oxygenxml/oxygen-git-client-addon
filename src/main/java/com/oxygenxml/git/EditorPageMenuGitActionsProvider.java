package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;

import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.history.HistoryController;

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
   * 
   * @param historyController History controller.
   */
  public EditorPageMenuGitActionsProvider(HistoryController historyController) {
    this.historyCtrl = historyController;
    
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    if (pluginWS != null) {
      pluginWS.addViewComponentCustomizer(
          viewInfo -> {
            if (OxygenGitPluginExtension.GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
              gitHistoryViewInfo = viewInfo;
            }
          });
    }
  }

  /**
   * Get the Git-specific action for the current editor page.
   * 
   * @param editorURL Editor URL
   * 
   * @return the Git-specific action for the current editor page.
   */
  public List<AbstractAction> getActionsForCurrentEditorPage(URL editorURL) {
	  List<AbstractAction> actions = new ArrayList<>();
	  File file = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().locateFile(editorURL);

	  if (file == null) {
		  return Collections.emptyList();
	  }

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
