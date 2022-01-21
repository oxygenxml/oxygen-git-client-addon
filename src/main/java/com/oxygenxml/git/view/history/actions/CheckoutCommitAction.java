package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.CheckoutCommitDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Action used to checkout a commit or a tag.
 * 
 * @author alex_smarandache
 *
 */
public class CheckoutCommitAction extends AbstractAction {

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER =  LoggerFactory.getLogger(CheckoutCommitAction.class);

	/**
	 * The commit to checkout.
	 */
	private final RevCommit commit;

	/**
	 * The ID of the commit to checkout.
	 */
	private final String commitID;


	/**
	 * Constructor.
	 * 
	 * @param commit The commit to checkout.
	 */
	public CheckoutCommitAction(RevCommit commit) {
		super(Translator.getInstance().getTranslation(Tags.CHECKOUT) + "...");
		this.commit = commit;
		this.commitID = null;
	}


	/**
	 * Constructor.
	 * 
	 * @param commitID The ID of the commit to checkout.
	 */
	public CheckoutCommitAction(String commitID) {
		super(Translator.getInstance().getTranslation(Tags.CHECKOUT) + "...");
		this.commit = null;
		this.commitID = commitID;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		boolean hasUncommitedChanges = false;
		try {
			hasUncommitedChanges = GitAccess.getInstance().getGit().status().call().hasUncommittedChanges();
		} catch (NoWorkTreeException | GitAPIException e1) {
			LOGGER.error(e1.getMessage(), e1);
		}
		if(!hasUncommitedChanges) {
			if(commit != null ) {
				new CheckoutCommitDialog(commit);
			} else {
				new CheckoutCommitDialog(commitID);
			}
		} else {
			String msg = Translator.getInstance().getTranslation(Tags.UNCOMMITED_CHANGES_WHEN_CHECKOUT_COMMIT);
			PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(msg);
		}
	}


}
