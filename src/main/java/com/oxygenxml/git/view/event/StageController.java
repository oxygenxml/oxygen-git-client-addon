package com.oxygenxml.git.view.event;

import java.awt.Component;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * 
 * Executes Git commands. A higher level wrapper over the GitAccess.
 *  
 * @author Beniamin Savu
 */
public class StageController {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitAccess.class);
  
  /**
   * Translator for the UI.
   */
  private Translator translator = Translator.getInstance();

	/**
	 * the git API
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * Executes the given action on the given files.
	 * 
	 * @param filesStatuses The files to be processed. 
	 * @param action        The action that is executed: stage, unstage, discard, resolve, etc.
	 */
	public void doGitCommand(List<FileStatus> filesStatuses, GitCommand action) {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Do action " + action + " on " + filesStatuses);
	  }
	  
	  switch (action) {
	    case STAGE:
	      gitAccess.addAll(filesStatuses);
	      break;
	    case UNSTAGE:
        gitAccess.resetAll(filesStatuses);
        break;
	    case DISCARD:
	      discard(filesStatuses);
	      break;
	    case RESOLVE_USING_MINE:
	      if (shouldContinueResolvingConflictUsingMineOrTheirs(GitCommand.RESOLVE_USING_MINE)) {
	        resolveUsingMine(filesStatuses);
	      }
	      break;
	    case RESOLVE_USING_THEIRS:
	      if (shouldContinueResolvingConflictUsingMineOrTheirs(GitCommand.RESOLVE_USING_THEIRS)) {
	        resolveUsingTheirs(filesStatuses);
	      }
	      break;
	    default:
	      break;
	  }
	}

	/**
	 * Should continue resolving a conflict using 'mine' or 'theirs'.
	 * 
	 * @param cmd GitCommand.RESOLVE_USING_MINE or GitCommand.RESOLVE_USING_THEIRS.
	 * 
	 * @return <code>true</code> to continue resolving the conflict using 'mine' or 'theirs'.
	 */
  private boolean shouldContinueResolvingConflictUsingMineOrTheirs(GitCommand cmd) {
    boolean shouldContinue = false;
    try {
      RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
      if (repositoryState != RepositoryState.REBASING_MERGE
          // When having a conflict while rebasing, 'mine' and 'theirs' are switched.
          // Tell this to the user and ask if they are OK with their choice.
          || isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(cmd)) {
        shouldContinue = true;
      }
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
    return shouldContinue;
  }
	
	/**
	 * Ask the user if they are OK with continuing the conflict resolving.
	 * When having a conflict while rebasing, 'mine' and 'theirs' are reversed.
   * Tell this to the user and ask if they are OK with their choice.
   * 
   * @param cmd GitCommand.RESOLVE_USING_MINE or GitCommand.RESOLVE_USING_THEIRS.
	 * 
	 * @return <code>true</code> to continue.
	 */
	private boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
	  boolean isResolveUsingMine = cmd == GitCommand.RESOLVE_USING_MINE;
    String actionName = isResolveUsingMine ? translator.getTranslation(Tags.RESOLVE_USING_MINE)
	      : translator.getTranslation(Tags.RESOLVE_USING_THEIRS);
	  String side = isResolveUsingMine ? translator.getTranslation(Tags.MINE)
	      : translator.getTranslation(Tags.THEIRS);
	  String branch = isResolveUsingMine ? translator.getTranslation(Tags.THE_UPSTREAM_BRANCH)
        : translator.getTranslation(Tags.THE_WORKING_BRANCH);
    int result = JOptionPane.showConfirmDialog(
        (Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        MessageFormat.format(
            translator.getTranslation(Tags.CONTINUE_RESOLVING_REBASE_CONFLICT_USING_MINE_OR_THEIRS),
            side,
            branch),
        actionName,
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
    return result == JOptionPane.YES_OPTION;
	}

	/**
	 * Resolve using 'Mine'.
	 * 
	 * @param filesStatuses The resources to resolve.
	 */
  private void resolveUsingMine(List<FileStatus> filesStatuses) {
    discard(filesStatuses);
    gitAccess.addAll(filesStatuses);
  }

	/**
	 * Resolve using 'Theirs'.
	 * 
	 * @param filesStatuses The resources to resolve.
	 */
  private void resolveUsingTheirs(List<FileStatus> filesStatuses) {
    for (FileStatus file : filesStatuses) {
      gitAccess.replaceWithRemoteContent(file.getFileLocation());
    }
    gitAccess.addAll(filesStatuses);
  }

	/**
	 * Discard files.
	 * 
	 * @param filesStatuses The resources to discard.
	 */
  private void discard(List<FileStatus> filesStatuses) {
    gitAccess.resetAll(filesStatuses);
    List<String> paths = new LinkedList<>();
    for (FileStatus file : filesStatuses) {
      if (file.getChangeType() != GitChangeType.SUBMODULE) {
        paths.add(file.getFileLocation());
      }
    }
    gitAccess.restoreLastCommitFile(paths);
  }
  
}
