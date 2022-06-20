package com.oxygenxml.git.view.staging.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * The "Open" action.
 */
public class OpenAction extends AbstractAction {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(OpenAction.class.getName());
  
  /**
   * The translator used for the contextual menu names
   */
  private static Translator translator = Translator.getInstance();

  /**
   * Selected resources provider.
   */
  private SelectedResourcesProvider selResProvider;
  
  /**
   * Constructor.
   * 
   * @param selResProvider Selected resources provider.
   */
  public OpenAction(SelectedResourcesProvider selResProvider) {
    super(translator.getTranslation(Tags.OPEN));
    this.selResProvider = selResProvider;
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
    FileStatus submodule = null;
    for (FileStatus file : allSelectedResources) {
      try {
        if (file.getChangeType() == GitChangeType.SUBMODULE) {
          // Open the submodule later, after processing all files.
          submodule = file;
        } else {
          openFile(file);
        }
      } catch (Exception ex) {
        LOGGER.error(ex.getMessage(), ex);
      }
    }
    
    if (submodule != null) {
      try {
        GitAccess.getInstance().setSubmodule(submodule.getFileLocation());
      } catch (IOException | GitAPIException ex) {
        LOGGER.error(ex.getMessage(), ex);
      }
    }
  }

  /**
   * Open a file inside an editor.
   * 
   * @param file File to open.
   * 
   * @throws MalformedURLException Unable to open.
   * @throws NoRepositorySelected Unable to open.
   */
  private void openFile(FileStatus file) throws MalformedURLException, NoRepositorySelected {
    URL fileURL = FileStatusUtil.computeFileStatusURL(file);
    final String fileLocation = file.getFileLocation();

    boolean isProjectExt = false;
    int index = fileLocation.lastIndexOf('.');
    if (index != -1) {
      String ext = fileLocation.substring(index + 1);
      isProjectExt = "xpr".equals(ext);
    }
    PluginWorkspaceProvider.getPluginWorkspace().open(
        fileURL,
        isProjectExt ? EditorPageConstants.PAGE_TEXT : null,
        isProjectExt ? "text/xml" : null);
  }
}
