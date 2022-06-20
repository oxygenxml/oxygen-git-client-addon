package com.oxygenxml.git.view.history;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.RevCommitUtilBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusOverDiffEntry;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.history.actions.CheckoutCommitAction;
import com.oxygenxml.git.view.history.actions.CreateBranchFromCommitAction;
import com.oxygenxml.git.view.history.actions.CreateTagAction;
import com.oxygenxml.git.view.history.actions.ResetBranchToCommitAction;
import com.oxygenxml.git.view.history.actions.RevertCommitAction;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Presents contextual actions over a resource in the history view.
 * 
 * @author alex_jitianu
 */
public class HistoryViewContextualMenuPresenter {
  /**
   * i18n
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  /**
   * Exception message prefix.
   */
  private static final String UNABLE_TO_OPEN_REVISION = "Unable to open revision. ";
  /**
   * Exception message prefix.
   */
  private static final String UNABLE_TO_COMPARE = "Unable to compare. ";
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(HistoryViewContextualMenuPresenter.class);
  /**
   * Executes GIT commands (stage, unstage, discard, etc).
   */
  protected GitControllerBase gitCtrl;
  
  /**
   * Constructor.
   * 
   * @param gitCtrl Executes GIT commands (stage, unstage, discard, etc).
   */
  public HistoryViewContextualMenuPresenter(GitControllerBase gitCtrl) {
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
  public void populateContextualActionsHistoryContext(
      JPopupMenu jPopupMenu,
      String filePath,
      CommitCharacteristics... commitCharacteristics) throws IOException, GitAPIException {
    
    if (commitCharacteristics != null && commitCharacteristics.length > 0) {
      if (commitCharacteristics.length == 1) {
        populateActions4SingleSelection(jPopupMenu, filePath, commitCharacteristics[0]);
      } else if (filePath != null) {
        populateActions4MultipleSelection(jPopupMenu, filePath, commitCharacteristics);
      }
    }
  }
  
  /**
   * We have multiple revisions selected for a given path.
   * 
   * @param jPopupMenu Menu to populate.
   * @param filePath Selected path.
   * @param commitCharacteristics Revisions.
   */
  private void populateActions4MultipleSelection(JPopupMenu jPopupMenu, String filePath,
      CommitCharacteristics... commitCharacteristics) {
    // Add open actions.
    String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(filePath);
    String actionName = MessageFormat.format(TRANSLATOR.getTranslation(Tags.OPEN_FILE), fileName);

    Action open = new AbstractAction(actionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (CommitCharacteristics commitCharacteristic : commitCharacteristics) {
          try {
            Optional<FileStatus> fileStatus = getFileStatus(filePath, commitCharacteristic);
            checkIfValidForOpen(filePath, commitCharacteristic, fileStatus);

            Optional<URL> fileURL = getFileURL(commitCharacteristic.getCommitId(), fileStatus.get());
            fileURL.ifPresent(url -> PluginWorkspaceProvider.getPluginWorkspace().open(url));
          } catch (IOException | GitAPIException | NoRepositorySelected e1) {
            LOGGER.debug(e1.getMessage(), e1);
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_OPEN_REVISION + e1.getMessage());
          }
        }
      }
    };    

    // Add Compare action.
    if (commitCharacteristics.length == 2) {
      CommitCharacteristics c1 = commitCharacteristics[0];
      CommitCharacteristics c2 = commitCharacteristics[1];
      addCompareWithEachOtherAction(jPopupMenu, filePath, c1, c2);
    }
    jPopupMenu.add(open);
  }

  /**
   * Adds the action that compares two revisions of the same file.
   * 
   * @param jPopupMenu Popup menu.
   * @param filePath File path.
   * @param commit1 First revision.
   * @param commit2 Second revision.
   */
  private void addCompareWithEachOtherAction(
      JPopupMenu jPopupMenu,
      String filePath,
      CommitCharacteristics commit1,
      CommitCharacteristics commit2) {

    // Create action
    Action compareWithEachOther = 
        new AbstractAction(TRANSLATOR.getTranslation(Tags.COMPARE_WITH_EACH_OTHER)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Optional<FileStatus> fileStatus1 = getFileStatus(filePath, commit1);
          Optional<FileStatus> fileStatus2 = getFileStatus(filePath, commit2);

          checkIfValidForOpen(filePath, commit1, fileStatus1);
          checkIfValidForOpen(filePath, commit2, fileStatus2);

          DiffPresenter.showTwoWayDiff(
              commit1.getCommitId(),
              filePath, 
              commit2.getCommitId(),
              filePath);
        } catch (IOException | GitAPIException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
        }
      }
    };

    jPopupMenu.add(compareWithEachOther);
  }
  
  /**
   * Checks if there are any errors with the given path.
   * 
   * @param filePath File path.
   * @param commit     Revision information.
   * @param fileStatus File information at the given revision.
   * 
   * @throws IOException
   */
  private void checkIfValidForOpen(
      String filePath, 
      CommitCharacteristics commit, 
      Optional<FileStatus> fileStatus) throws IOException {
    if (!fileStatus.isPresent()) {
      String error = MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.FILE_NOT_PRESENT_IN_REVISION),
          filePath,
          commit.getCommitAbbreviatedId());
      throw new IOException(error);
    } else if (fileStatus.get().getChangeType() == GitChangeType.REMOVED) {
      String error = MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.FILE_WAS_REMOVED_IN_REVISION),
          filePath,
          commit.getCommitAbbreviatedId());
      throw new IOException(error);
    }
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
  private void populateActions4SingleSelection(
      JPopupMenu jPopupMenu,
      String filePath,
      CommitCharacteristics commitCharacteristics) throws IOException, GitAPIException {
    if (filePath != null) {
      Optional<FileStatus> fileStatusOptional = getFileStatus(filePath, commitCharacteristics);
      fileStatusOptional.ifPresent(fileStatus -> populateContextActionsForFile(jPopupMenu, fileStatus, commitCharacteristics, true));
    }

    if (filePath != null) {
      jPopupMenu.addSeparator();
    }
    String commitId = commitCharacteristics.getCommitId();
    if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(commitId)) {
      jPopupMenu.add(new CreateBranchFromCommitAction(commitId));
      jPopupMenu.add(new CreateTagAction(commitId));
      jPopupMenu.add(new CheckoutCommitAction(commitCharacteristics.getPlotCommit()));
      jPopupMenu.addSeparator();
      jPopupMenu.add(new RevertCommitAction(commitCharacteristics));
      jPopupMenu.add(new ResetBranchToCommitAction(commitCharacteristics));
    }
  }

  /**
   * Searches for the file path in the given commit.
   * 
   * @param filePath File path to search.
   * @param commitCharacteristics A commit info.
   * 
   * @return An optional file info if the file path is found inside the commit.
   * 
   * @throws IOException Problems trying to iterate over the repository.
   * @throws GitAPIException Problems trying to iterate over the repository.
   */
  public Optional<FileStatus> getFileStatus(String filePath, CommitCharacteristics commitCharacteristics)
      throws IOException, GitAPIException {
    List<FileStatus> changes = RevCommitUtil.getChangedFiles(commitCharacteristics.getCommitId());
    Optional<FileStatus> fileStatusOptional = changes.stream().filter(f -> filePath.equals(f.getFileLocation())).findFirst();
    if (!fileStatusOptional.isPresent()) {
      // Perhaps the file was renamed at some point.
      String oldFilePath = RevCommitUtil.getOldPathStartingFromHead(
          GitAccess.getInstance().getGit(), 
          commitCharacteristics.getCommitId(), 
          filePath);
      
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("new " + filePath + " old " + oldFilePath);
      }
      
      fileStatusOptional = changes.stream().filter(f -> oldFilePath.equals(f.getFileLocation())).findFirst();
    }
    return fileStatusOptional;
  }


  /**
   * Contributes the contextual actions for the given commit.
   * 
   * @param jPopupMenu            Contextual menu.
   * @param fileStatus            File path do diff.
   * @param commitCharacteristics Current commit data.
   * @param addFileName           <code>true</code> to add the name of the file to the action's name.
   */
  void populateContextActionsForFile(
      JPopupMenu jPopupMenu,  
      FileStatus fileStatus,
      CommitCharacteristics commitCharacteristics,
      boolean addFileName) {
    List<Action> contextualActions = getFileContextualActions(fileStatus, commitCharacteristics, addFileName);
    contextualActions.forEach(action -> {
      if(action == null) {
        jPopupMenu.addSeparator();
      } else {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
        jPopupMenu.add(menuItem);
      }
    });  
  }

  /**
   * Contributes the contextual actions for the given file and commit.
   * 
   * @param fileStatus            The file.
   * @param commitCharacteristics Commit information.
   * @param addFileName           <code>true</code> to add the name of the file to the action's name.
   * 
   * @return A list with actions that can be executed over the given file. Never null.
   */
  public List<Action> getFileContextualActions(
      FileStatus fileStatus,
      CommitCharacteristics commitCharacteristics,
      boolean addFileName) {
    
    List<Action> actions = new LinkedList<>();
    
    String fileStatusLocation = fileStatus.getFileLocation();
    GitChangeType fileStatusChangeType = fileStatus.getChangeType();
    String currentCommitID = commitCharacteristics.getCommitId();
    if (fileStatusChangeType != GitChangeType.REMOVED && fileStatusChangeType != GitChangeType.MISSING) {
      if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(currentCommitID)) {
        createCompareActionsForCommit(actions, commitCharacteristics, addFileName, fileStatus);
      } else if (fileStatusChangeType != GitChangeType.ADD && fileStatusChangeType != GitChangeType.UNTRACKED) {
        // Uncommitted changes. Compare between local and HEAD.
        actions.add(new AbstractAction(TRANSLATOR.getTranslation(Tags.OPEN_IN_COMPARE)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            DiffPresenter.showDiff(fileStatus, gitCtrl);
          }
        });
      }
    }

    if(!actions.isEmpty()) {
      actions.add(null);
    }

    actions.add(createOpenFileAction(currentCommitID, fileStatus, addFileName));

    if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(currentCommitID)
        && fileStatusChangeType != GitChangeType.REMOVED) {
      actions.add(createOpenWorkingCopyFileAction(fileStatus, currentCommitID, addFileName));
    }
    
    if(fileStatusChangeType != GitChangeType.REMOVED
        && fileStatusChangeType != GitChangeType.UNTRACKED
        && !GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(currentCommitID)
        && existsLocalFile(fileStatusLocation)) {
      actions.add(null);
      actions.add(createCheckoutFileAction(currentCommitID, fileStatus, addFileName)); 
    }
   
    return actions;
  }

  /**
   * Creates the DIFF actions for an actual commit from history.
   * 
   * @param actions               List where to put the actions.
   * @param commitCharacteristics Commit data.
   * @param addFileName           <code>true</code> to append the name of the file to the actions' name.
   * @param fileStatus            File to diff.
   */
  private void createCompareActionsForCommit(
      List<Action> actions, 
      CommitCharacteristics commitCharacteristics, 
      boolean addFileName,
      FileStatus fileStatus) {
    String filePath = fileStatus.getFileLocation();
    
    if (fileStatus instanceof FileStatusOverDiffEntry
        && fileStatus.getChangeType() == GitChangeType.RENAME) {
      actions.add(new AbstractAction(getCompareWithPreviousVersionActionName(filePath, addFileName)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            DiffPresenter.showTwoWayDiff(((FileStatusOverDiffEntry) fileStatus));
          } catch (MalformedURLException e1) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
            LOGGER.error(e1.getMessage(), e1);
          }
        }
      });
    } else if (fileStatus.getChangeType() != GitChangeType.ADD) {
      addCompareWithParentsAction(actions, commitCharacteristics, addFileName, filePath);
    }
    
    addCompareWithWorkingTreeAction(actions, commitCharacteristics, addFileName, filePath);

  }

  /**
   * Iterate over the parent revisions and add one Compare action for each.
   * 
   * @param actions List where to put the actions.
   * @param commitCharacteristics Revision information.
   * @param addFileName <code>true</code> to add the name of the file to the action name.
   * @param filePath File location relative to the WC root.
   */
  private void addCompareWithParentsAction(
      List<Action> actions,
      CommitCharacteristics commitCharacteristics,
      boolean addFileName,
      String filePath) {
    List<String> parents = commitCharacteristics.getParentCommitId();
    if (parents != null && !parents.isEmpty()) {
      try {
        RevCommit[] parentsRevCommits = RevCommitUtil.getParents(
            GitAccess.getInstance().getRepository(),
            commitCharacteristics.getCommitId());
        boolean addParentID = parents.size() > 1;
        for (RevCommit parentID : parentsRevCommits) {
          actions.add(createCompareWithPrevVersionAction(
              filePath, 
              commitCharacteristics.getCommitId(),
              filePath,
              parentID, 
              addParentID, 
              addFileName));
        }
      } catch (IOException | NoRepositorySelected e2) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e2.getMessage());
        LOGGER.error(e2.getMessage(), e2);
      }
    }
  }

  /**
   * Compare the current revision with the working tree version.
   * 
   * @param actions List where to put the actions.
   * @param commitCharacteristics Revision information.
   * @param addFileName <code>true</code> to also add the name of the file to the action name.
   * @param filePath Path of the resource to compare. Relative to the WC root.
   */
  private void addCompareWithWorkingTreeAction(
      List<Action> actions, 
      CommitCharacteristics commitCharacteristics, 
      boolean addFileName,
      String filePath) {
    String actionName = TRANSLATOR.getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION);
    if (addFileName) {
      actionName = MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.COMPARE_FILE_WITH_WORKING_TREE_VERSION),
          PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(filePath));
    }
    actions.add(new AbstractAction(actionName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          DiffPresenter.showTwoWayDiffWithLocal(filePath, commitCharacteristics.getCommitId());
        } catch (FileNotFoundException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
          LOGGER.debug(e1.getMessage(), e1);
        } catch (NoRepositorySelected | IOException | GitAPIException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_COMPARE + e1.getMessage());
          LOGGER.error(e1.getMessage(), e1);
        }
      }
    });
  }

  /**
   * Creates an action that invokes Oxygen's DIFF. It compares the current version with the previous.
   * 
   * @param filePath                File to compare. Path relative to the working tree.
   * @param commitID                The current commit id. First version to compare.
   * @param parentFilePath          The parent file path.
   * @param parentRevCommit         The parent revision. Second version to compare.
   * @param addParentIDInActionName <code>true</code> to put the ID of the parent version in the action's name.
   * @param addFileName             <code>true</code> to add the file name to the action's name. 
   * 
   * @return The action that invokes the DIFF.
   */
  private Action createCompareWithPrevVersionAction(
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
          LOGGER.debug(e1.getMessage(), e1);
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
   * @return The name of the compare action.
   */
  private String getCompareWithPreviousVersionActionName(String filePath, boolean addFileName) {
    String actionName = TRANSLATOR.getTranslation(Tags.COMPARE_WITH_PREVIOUS_VERSION);
    if (addFileName) {
      actionName = MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.COMPARE_FILE_WITH_PREVIOUS_VERSION),
          PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(filePath));
    }
    return actionName;
  }
  
  
  /**
   * Used to verify if a file exists on local disk.
   * 
   * @param filePath the file path
   * 
   * @return <code>true</code> if the file exists 
   */
  private boolean existsLocalFile(String filePath) {
    String selectedRepository = "";
    boolean toReturn = true;
    try {
      selectedRepository = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
      File file = new File(selectedRepository, filePath); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false pozitive
      toReturn = file.exists();
    } catch (NoRepositorySelected e) {
     toReturn = false;
    }
    return toReturn;
  }

  
  /**
   * Creates an action revert a file for a file.
   * 
   * @author Alex_Smarandache
   * 
   * @param commitId         The commit ID.
   * @param fileStatus       The File to be reverted.
   * @param addFileName      <code>true</code> to append the name of the file to the name of the action.
   * 
   * @return The action that will open the file when invoked.
   */
  private Action createCheckoutFileAction(String commitId, FileStatus fileStatus, boolean addFileName) {
    return new AbstractAction(getCheckoutFileActionName(fileStatus, addFileName)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        GitOperationScheduler.getInstance().schedule(() -> GitAccess.getInstance().checkoutCommitForFile(fileStatus.getFileLocation(), commitId));
      }
    };
  }
  
  
  /**
   * Creates an action to open a working copy file.
   * 
   * @author Alex_Smarandache
   * 
   * @param fileStatus  File path, relative to the working copy.
   * @param commitID    Current commit ID.
   * @param addFileName <code>true</code> to append the name of the file to the name of the action.
   * 
   * @return The action that will open the file when invoked.
   */
  protected Action createOpenWorkingCopyFileAction(
      FileStatus fileStatus,
      String commitID,
      boolean addFileName) {
    return new AbstractAction(getOpenFileWorkingCopyActionName(fileStatus, addFileName)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          String localFilePath = RevCommitUtil.getNewPathInWorkingCopy(
              GitAccess.getInstance().getGit(), 
              fileStatus.getFileLocation(), 
              commitID);
          URL fileURL = FileUtil.getFileURL(localFilePath);
                 
          boolean isProjectExt = false;
          int index = localFilePath.lastIndexOf('.');
          if (index != -1) {
            String ext = localFilePath.substring(index + 1);
            isProjectExt = "xpr".equals(ext);
          }
          
          PluginWorkspaceProvider.getPluginWorkspace().open(
              fileURL,
              isProjectExt ? EditorPageConstants.PAGE_TEXT : null,
              isProjectExt ? "text/xml" : null);
        } catch (FileNotFoundException ex) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_OPEN_REVISION + ex.getMessage());
          LOGGER.debug(ex.getMessage(), ex);
        } catch (NoRepositorySelected | IOException | GitAPIException ex) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_OPEN_REVISION + ex.getMessage());
          LOGGER.error(ex.getMessage(), ex);
        }
      }
     };
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
  private Action createOpenFileAction(String revisionID, FileStatus fileStatus, boolean addFileName) {
    String tooltipText = addFileName ? null : TRANSLATOR.getTranslation(Tags.HISTORY_RESOURCE_OPEN_ACTION_TOOLTIP);
    AbstractAction openFileAction = new AbstractAction(getOpenFileActionName(fileStatus, addFileName)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Optional<URL> fileURL = getFileURL(revisionID, fileStatus);
          fileURL.ifPresent(url -> PluginWorkspaceProvider.getPluginWorkspace().open(url));
        } catch (NoRepositorySelected | IOException e1) {
          LOGGER.error(e1.getMessage(), e1);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(UNABLE_TO_OPEN_REVISION + e1.getMessage());
        } 
      }
    };
    openFileAction.putValue(Action.SHORT_DESCRIPTION, tooltipText);
    return openFileAction;
  }

  
  /**
   * Builds the name for the action that opens the work copy for a file.
   *
   * @author Alex_Smarandache
   * 
   * @param fileStatus File info.
   * @param addFileName <code>true</code> to put the file name in the action name too.
   * 
   * @return The name of the open file action.
   */
  private String getOpenFileWorkingCopyActionName(FileStatus fileStatus, boolean addFileName) {
    String actionName = TRANSLATOR.getTranslation(Tags.OPEN_WORKING_COPY);
    if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(fileStatus.getFileLocation());
      actionName = MessageFormat.format(TRANSLATOR.getTranslation(Tags.OPEN_WORKING_COPY_VERSION), fileName);
    }
    return actionName;
  }
  
  
  /**
   * Builds the name for the action that revert changes for a file.
   * 
   * @param fileStatus File info.
   * @param addFileName <code>true</code> to put the file name in the action name too.
   * 
   * @return The name of the revert file action.
   */
  private String getCheckoutFileActionName(FileStatus fileStatus, boolean addFileName) {

    String actionName = Translator.getInstance().getTranslation(Tags.RESET_FILE_TO_THIS_COMMIT);
    if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(fileStatus.getFileLocation());
      actionName = MessageFormat.format(Translator.getInstance().getTranslation(Tags.RESET_FILE_X_TO_THIS_COMMIT), fileName);
    }
    return actionName;
  }
  
  
  
  /**
   * Builds the name for the action that opens a file.
   * 
   * @param fileStatus File info.
   * @param addFileName <code>true</code> to put the file name in the action name too.
   * 
   * @return The name of the open file action.
   */
  private String getOpenFileActionName(FileStatus fileStatus, boolean addFileName) {
    String actionName = TRANSLATOR.getTranslation(Tags.OPEN);
    if (fileStatus.getChangeType() == GitChangeType.REMOVED || fileStatus.getChangeType() == GitChangeType.MISSING) {
      // A removed file. We can only present the previous version.
      actionName = TRANSLATOR.getTranslation(Tags.OPEN_PREVIOUS_VERSION);
    } else if (addFileName) {
      String fileName = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName(fileStatus.getFileLocation());
      actionName = MessageFormat.format(TRANSLATOR.getTranslation(Tags.OPEN_THIS_VERSION_OF_FILENAME), fileName);
    }
    return actionName;
  }
  
  /**
   * Builds an URL that identifies a file at a specific revision.
   * 
   * @param revisionID Revision ID.
   * @param fileStatus FIle info.
   * 
   * @return The URL, if one was built.
   * 
   * @throws NoRepositorySelected No repository is loaded.
   * @throws IOException Problems identifying the revision.
   */
  private Optional<URL> getFileURL(String revisionID, FileStatus fileStatus)
      throws NoRepositorySelected, IOException {
    URL fileURL = null;
    String fileStatusLocation = fileStatus.getFileLocation();
    if (fileStatus.getChangeType() == GitChangeType.REMOVED) {
      Repository repository = GitAccess.getInstance().getRepository();
      RevCommit[] parentsRevCommits = RevCommitUtil.getParents(repository, revisionID);
      
      // If it's a merge, we look for the one parent with the actual file in it.
      Optional<RevCommit> previousVersionCommit = Arrays.stream(parentsRevCommits).filter(revCommit -> {
        try {
          return RevCommitUtil.getObjectID(repository, revCommit.getId().getName(), fileStatusLocation) != null;
        } catch (IOException e1) {
          // Unable to find a parent with the given path.
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to open file because of " + e1.getMessage());
        }
        return false;
      }).findFirst();
      
      if (previousVersionCommit.isPresent()) {
        fileURL = GitRevisionURLHandler.encodeURL(
            previousVersionCommit.get().getId().getName(),
            fileStatusLocation);  
      }
    } else if (fileStatus.getChangeType() == GitChangeType.MISSING) {
      fileURL = GitRevisionURLHandler.encodeURL(VersionIdentifier.INDEX_OR_LAST_COMMIT, fileStatusLocation);
    } else if (!GitAccess.UNCOMMITED_CHANGES.getCommitId().equals(revisionID)) {
      fileURL = GitRevisionURLHandler.encodeURL(revisionID, fileStatusLocation);
    } else {
      fileURL = FileUtil.getFileURL(fileStatusLocation);
    }
    
    return Optional.ofNullable(fileURL);
  }
}
