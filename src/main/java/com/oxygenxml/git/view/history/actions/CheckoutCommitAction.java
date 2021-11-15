package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.CheckoutCommitDialog;

/**
 * Action used to checkout a commit or a tag.
 * 
 * @author alex_smarandache
 *
 */
public class CheckoutCommitAction extends AbstractAction {
    
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
		if(commit != null ) {
			new CheckoutCommitDialog(commit);
		} else {
			new CheckoutCommitDialog(commitID);
		}
	}

	
}
