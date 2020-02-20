package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Show blame.
 */
public class ShowBlameForUnstagedResourceAction extends AbstractAction {
  
  /**
   * The translator used for the contextual menu names
   */
  private static final Translator translator = Translator.getInstance();
  
  /**
   * Selected resources provider.
   */
  private SelectedResourcesProvider selResProvider;

  /**
   * History controller.
   */
  private HistoryController historyController;
  
  /**
   * Constructor.
   * 
   * @param historyController  History controller.
   * @param selResProvider     Selected resources provider.
   */
  public ShowBlameForUnstagedResourceAction(HistoryController historyController, SelectedResourcesProvider selResProvider) {
    super(translator.getTranslation(Tags.SHOW_BLAME));
    this.historyController = historyController;
    this.selResProvider = selResProvider;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
    if (!allSelectedResources.isEmpty()) {
      try {
        String filePath = allSelectedResources.get(0).getFileLocation();
        File file = new File(GitAccess.getInstance().getWorkingCopy(), filePath);
        URL fileURL = file.toURI().toURL();
        WSEditor editor = pluginWS.getEditorAccess(fileURL, PluginWorkspace.MAIN_EDITING_AREA);
        if (editor == null || !editor.isModified()) {
          tryBlame(filePath);
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
            tryBlame(filePath);
          }
        }
      } catch (NoRepositorySelected | MalformedURLException ex) {
        pluginWS.showErrorMessage(ex.getMessage());
      } 
    }
  }
  
  /**
   * Try blame.
   * 
   * @param filePath File path.
   */
  private void tryBlame(String filePath) {
    try {
      BlameManager.getInstance().doBlame(
          filePath, 
          historyController);
    } catch (IOException | GitAPIException ex) {
      ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showErrorMessage(ex.getMessage());
    }
  }

}
