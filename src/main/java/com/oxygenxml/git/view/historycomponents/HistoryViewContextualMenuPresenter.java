package com.oxygenxml.git.view.historycomponents;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.RevCommitUtilBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.event.GitController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Presents contextual actions over a resource in the history view.
 * 
 * @author alex_jitianu
 */
public class HistoryViewContextualMenuPresenter {
  /**
   * Exception message prefix.
   */
  private static final String UNABLE_TO_COMPARE = "Unable to compare: ";
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(HistoryViewContextualMenuPresenter.class);
  /**
   * Executes GIT commands (stage, unstage, discard, etc).
   */
  protected GitController gitCtrl;
  
  /**
   * Constructor.
   * 
   * @param gitCtrl Executes GIT commands (stage, unstage, discard, etc).
   */
  public HistoryViewContextualMenuPresenter(GitController gitCtrl) {
    this.gitCtrl = gitCtrl;
  }
  
  /**
   * Contributes the contextual actions for the given file, at the given revision/commit.
   * 
   * @param jPopupMenu            Contextual menu in which to put the actions.
   * @param filePath              File path.
   * @param commitCharacteristics Revision/commit data.
   * 
   * @throws IOException If it fails.
   * @throws GitAPIException If it fails.
   */
  public void populateContextualActions(
      JPopupMenu jPopupMenu,
      String filePath,
      CommitCharacteristics commitCharacteristics) throws IOException, GitAPIException {
    List<FileStatus> changes = RevCommitUtil.getChangedFiles(commitCharacteristics.getCommitId());
    Optional<FileStatus> fileStatusOptional = changes.stream().filter(f -> filePath.equals(f.getFileLocation())).findFirst();
    if (fileStatusOptional.isPresent()) {
      populateContextualActions(jPopupMenu, fileStatusOptional.get(), commitCharacteristics, true);
    }
  }


  /**
   * Contributes the DIFF actions between the current revision and the previous ones on the contextual menu.
   * 
   * @param jPopupMenu            Contextual menu.
   * @param fileStatus            File path do diff.
   * @param commitCharacteristics Current commit data.
   * @param addFileName           <code>true</code> to add the name of the file to the action's name.
   */
  public void populateContextualActions(
      JPopupMenu jPopupMenu,  
      FileStatus fileStatus,
      CommitCharacteristics commitCharacteristics,
      boolean addFileName) {
    jPopupMenu.add(createOpenFileAction(commitCharacteristics.getCommitId(), fileStatus, addFileName));
    
    if (fileStatus.getChangeType() != GitChangeType.ADD && fileStatus.getChangeType() != GitChangeType.REMOVED) {
      if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(commitCharacteristics.getCommitId())) {
        createCompareActionsForCommit(jPopupMenu, commitCharacteristics, addFileName, fileStatus);
      } else {
        // Uncommitted changes. Compare between local and HEAD.
        jPopupMenu.add(new AbstractAction(Translator.getInstance().getTranslation(Tags.OPEN_IN_COMPARE)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            DiffPresenter.showDiff(fileStatus, gitCtrl);
          }
        });
      }
    }
  }

  /**
   * Creates the DIFF actions for an actual commit from history.
   * 
   * @param jPopupMenu            Menu where to add the actions.
   * @param commitCharacteristics Commit data.
   * @param addFileName           <code>true</code> to append the name of the file to the actions' name.
   * @param filePath              File to DIFF.
   */
  private void createCompareActionsForCommit(
      JPopupMenu jPopupMenu, 
      CommitCharacteristics commitCharacteristics, 
      boolean addFileName,
      FileStatus fileStatus) {
    String filePath = fileStatus.getFileLocation();
    // A revision.
    
    if (fileStatus instanceof FileStatusOverDiffEntry
        && fileStatus.getChangeType() == GitChangeType.RENAME) {
      jPopupMenu.add(new AbstractAction(getCompareWithPreviousVersionActionName(filePath, addFileName)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            DiffPresenter.showTwoWayDiff(((FileStatusOverDiffEntry) fileStatus));
          } catch (MalformedURLException e1) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
            LOGGER.error(e1, e1);
          }
        }
      });
    } else {
      addCompareWithParentsAction(jPopupMenu, commitCharacteristics, addFileName, filePath);
    }
    
    addCompareWithWorkingTreeAction(jPopupMenu, commitCharacteristics, addFileName, filePath);
  }

  /**
   * Iterate over the parent revisions and add one Compare action for each.
   * 
   * @param jPopupMenu Contextual menu.
   * @param commitCharacteristics Revision information.
   * @param addFileName <code>true</code> to add the name of the file to the action name.
   * @param filePath File location relative to the WC root.
   */
  private void addCompareWithParentsAction(JPopupMenu jPopupMenu, CommitCharacteristics commitCharacteristics, boolean addFileName,
      String filePath) {
    List<String> parents = commitCharacteristics.getParentCommitId();
    if (parents != null && !parents.isEmpty()) {
      try {
        RevCommit[] parentsRevCommits = RevCommitUtil.getParents(GitAccess.getInstance().getRepository(), commitCharacteristics.getCommitId());
        boolean addParentID = parents.size() > 1;
        for (RevCommit parentID : parentsRevCommits) {
          jPopupMenu.add(createCompareWithPrevVersionAction(
              filePath, 
              commitCharacteristics.getCommitId(),
              filePath,
              parentID, 
              addParentID, 
              addFileName));
        }
      } catch (IOException | NoRepositorySelected e2) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e2.getMessage());
        LOGGER.error(e2, e2);
      }
    }
  }

  /**
   * Compare the current revision with the working tree version.
   * 
   * @param jPopupMenu Contextual menu.
   * @param commitCharacteristics Revision information.
   * @param addFileName <code>true</code> to also add the name of the file to the action name.
   * @param filePath Path of the resource to compare. Relative to the WC root.
   */
  private void addCompareWithWorkingTreeAction(
      JPopupMenu jPopupMenu, 
      CommitCharacteristics commitCharacteristics, 
      boolean addFileName,
      String filePath) {
    // ========== Compare with working tree version ==========
    String actionName = Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION);
    if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(filePath);
      actionName = MessageFormat.format(Translator.getInstance().getTranslation(Tags.COMPARE_FILE_WITH_WORKING_TREE_VERSION), fileName);
    }
    jPopupMenu.add(new AbstractAction(actionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          DiffPresenter.showTwoWayDiffWithLocal(filePath, commitCharacteristics.getCommitId());
        } catch (NoRepositorySelected | IOException | GitAPIException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
          LOGGER.error(e1, e1);
        }
      }
    });
  }

  /**
   * Creates an action that invokes Oxygen's DIFF. It compares the current version with the previous.
   * 
   * @param filePath                File to compare. Path relative to the working tree.
   * @param commitID                The current commit id. First version to compare.
   * @param parentRevCommit         The parent revision. Second version to compare.
   * @param addParentIDInActionName <code>true</code> to put the ID of the parent version in the action's name.
   * @param addFileName             <code>true</code> to add the file name to the action's name. 
   * 
   * @return The action that invokes the DIFF.
   */
  private AbstractAction createCompareWithPrevVersionAction(
      String filePath,
      String commitID,
      String parentFilePath,
      RevCommit parentRevCommit,
      boolean addParentIDInActionName, 
      boolean addFileName) {
    
    // Compute action name
    String actionName = getCompareWithPreviousVersionActionName(filePath, addFileName);
    if (addParentIDInActionName) {
      actionName += " " + parentRevCommit.abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name();
    }
    
    // Create action
    return new AbstractAction(actionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          DiffPresenter.showTwoWayDiff(commitID, filePath, parentRevCommit.name(), parentFilePath);
        } catch (MalformedURLException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
          LOGGER.error(e1, e1);
        }
      }
    };
  }

  /**
   * Builds the name for the compare with previous version action.
   * 
   * @param filePath Local 
   * @param addFileName <code>true</code> to add the name of the file to the action's name.
   * 
   * @return The name of the comapre action.
   */
  private String getCompareWithPreviousVersionActionName(String filePath, boolean addFileName) {
    String actionName = Translator.getInstance().getTranslation(Tags.COMPARE_WITH_PREVIOUS_VERSION);
    if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(filePath);
      actionName = MessageFormat.format(Translator.getInstance().getTranslation(Tags.COMPARE_FILE_WITH_PREVIOUS_VERSION), fileName);
    }
    return actionName;
  }
  

  /**
   * Creates an action to open a file at a given revision.
   * 
   * @param revisionID Revision ID.
   * @param fileStatus File path, relative to the working copy.
   * @param addFileName <code>true</code> to append the name of the file to the name of the action.
   * 
   * @return The action that will open the file when invoked.
   */
  private AbstractAction createOpenFileAction(String revisionID, FileStatus fileStatus, boolean addFileName) {
    String actionName = Translator.getInstance().getTranslation(Tags.OPEN);
    if (fileStatus.getChangeType() == GitChangeType.REMOVED) {
      // A removed file. We can only present the previous version.
      actionName = Translator.getInstance().getTranslation(Tags.OPEN_PREVIOUS_VERSION);
    } else if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(fileStatus.getFileLocation());
      actionName = MessageFormat.format(Translator.getInstance().getTranslation(Tags.OPEN_FILE), fileName);
    }
    
    return new AbstractAction(actionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          URL fileURL = null;
          if (fileStatus.getChangeType() == GitChangeType.REMOVED) {
            Repository repository = GitAccess.getInstance().getRepository();
            RevCommit[] parentsRevCommits = RevCommitUtil.getParents(repository, revisionID);
            
            // If it's a merge, we look for the one parent with the actual file in it.
            Optional<RevCommit> findFirst = Arrays.asList(parentsRevCommits).stream().filter(p -> {
              try {
                return RevCommitUtil.getObjectID(repository, p.getId().getName(), fileStatus.getFileLocation()) != null;
              } catch (IOException e1) {
                // Unable to find a parent with the given path.
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to open file because of " + e1.getMessage());
              }
              
              return false;
            }).findFirst();
            
            if (findFirst.isPresent()) {
              fileURL = GitRevisionURLHandler.encodeURL(findFirst.get().getId().getName(), fileStatus.getFileLocation());  
            }
          } else if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(revisionID)) {
            fileURL = GitRevisionURLHandler.encodeURL(revisionID, fileStatus.getFileLocation());
          } else {
            fileURL = FileHelper.getFileURL(fileStatus.getFileLocation());
          }
          
          PluginWorkspaceProvider.getPluginWorkspace().open(fileURL);
        } catch (NoRepositorySelected | IOException e1) {
          LOGGER.error(e1, e1);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to open revision: " + e1.getMessage());
        } 
      }
    };
  }
}
