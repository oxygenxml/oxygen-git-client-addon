package com.oxygenxml.git.view.staging;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
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
  private static final Logger logger = Logger.getLogger(OpenAction.class.getName());
  
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
        logger.error(ex, ex);
      }
    }
    
    if (submodule != null) {
      try {
        GitAccess.getInstance().setSubmodule(submodule.getFileLocation());
      } catch (IOException | GitAPIException ex) {
        logger.error(ex, ex);
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
    URL fileURL = null;
    String fileLocation = file.getFileLocation();
    if (file.getChangeType() == GitChangeType.ADD || file.getChangeType() == GitChangeType.CHANGED) {
      // A file from the INDEX. We need a special URL to access it.
      fileURL = GitRevisionURLHandler.encodeURL(
          VersionIdentifier.INDEX_OR_LAST_COMMIT,
          fileLocation);
    } else {
      // We must open a local copy.
      fileURL = FileUtil.getFileURL(fileLocation);
    }

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
