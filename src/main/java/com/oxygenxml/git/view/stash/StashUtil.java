package com.oxygenxml.git.view.stash;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Contains useful methods for stash.
 *
 * @author Alex_Smarandache
 */
public class StashUtil {

  /**
   * The git access.
   */
  private static final GitAccess GIT_ACCESS = GitAccess.getInstance();

  /**
   * The translator for the messages that are displayed in this panel
   */
  private static final Translator TRANSLATOR = Translator.getInstance();


  /**
   * The hidden constructor.
   */
  private StashUtil() {
    // Nothing.
  }


  /**
   * Tries to create a new stash.
   *
   * <code>True</code> if the stash was created.
   * 
   * @return <code>true</code> if the stash entry was created.
   */
  public static boolean stashChanges() {
    boolean successfullyCreated = false;
    SimpleDateFormat commitDateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);

    if (GIT_ACCESS.getConflictingFiles().isEmpty()) {
      StashChangesDialog stashDialog = new StashChangesDialog();
      stashDialog.setVisible(true);
      if (stashDialog.getResult() == OKCancelDialog.RESULT_OK) {
        String description = stashDialog.getStashMessage();

        if (description.isEmpty()) {
          description = "WIP on "
              + GIT_ACCESS.getBranchInfo().getBranchName()
              + " ["
              + commitDateFormat.format(new Date())
              + "]";
        }

        successfullyCreated = GIT_ACCESS.createStash(stashDialog.shouldIncludeUntracked(), description) != null;
      }
    } else {
      PluginWorkspaceProvider.getPluginWorkspace()
              .showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
    }

    return successfullyCreated;
  }


  /**
   * Drop one stash after user confirmation.
   * <br>
   * If the stash cannot be dropped, one message will be displayed.
   * <br>
   * If the user doesn't confirm the drop operation, <code>False</code> will be returned.
   *
   * @param stashToBeDropped Index of stash to be dropped.
   *
   *
   * @return <code>True</code> if the stash was dropped.
   */
  public static boolean dropStash(int stashToBeDropped) {
    boolean wasDropped = false;

    int answer = FileStatusDialog.showWarningMessageWithConfirmation(
            TRANSLATOR.getTranslation(Tags.DELETE_STASH),
            TRANSLATOR.getTranslation(Tags.STASH_DELETE_CONFIRMATION),
            TRANSLATOR.getTranslation(Tags.YES),
            TRANSLATOR.getTranslation(Tags.NO));
    if (OKCancelDialog.RESULT_OK == answer) {
      try {
        GIT_ACCESS.dropStash(stashToBeDropped);
        wasDropped = true;
      } catch (GitAPIException e) {
        FileStatusDialog.showErrorMessage(
                TRANSLATOR.getTranslation(Tags.DELETE_STASH),
                null,
                TRANSLATOR.getTranslation(Tags.STASH_CANNOT_BE_DELETED) + e.getMessage()
        );
      }
    }

    return wasDropped;
  }


  /**
   * Clear all stashes after user confirmation.
   * <br>
   * If the stashes cannot be dropped, one message will be displayed.
   * <br>
   * If the user doesn't confirm the clear operation, <code>False</code> will be returned.
   *
   * @return <code>True</code> if the all stashes were dropped.
   */
  public static boolean clearStashes() {
    boolean wereDroppedAllStashes = false;

    int answer = FileStatusDialog.showWarningMessageWithConfirmation(
            TRANSLATOR.getTranslation(Tags.DELETE_ALL_STASHES),
            TRANSLATOR.getTranslation(Tags.CONFIRMATION_CLEAR_STASHES_MESSAGE),
            TRANSLATOR.getTranslation(Tags.YES),
            TRANSLATOR.getTranslation(Tags.NO));
    if (OKCancelDialog.RESULT_OK == answer) {
      try {
        GIT_ACCESS.dropAllStashes();
        wereDroppedAllStashes = true;
      } catch (GitAPIException e) {
        FileStatusDialog.showErrorMessage(
                TRANSLATOR.getTranslation(Tags.DELETE_ALL_STASHES),
                null,
                TRANSLATOR.getTranslation(Tags.STASH_CANNOT_BE_DELETED)
        );
      }
    }

    return wereDroppedAllStashes;
  }

}
