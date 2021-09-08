package com.oxygenxml.git.view.stash;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

import java.text.SimpleDateFormat;
import java.util.Date;

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
}
