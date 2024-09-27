package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog;
import com.oxygenxml.git.view.remotes.RemotesViewUtil;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Action to set remote for current branch.
 * 
 * @author alex_smarandache
 *
 */
public class SetRemoteAction extends BaseGitAbstractAction {

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	/**
	 * Constructor.
	 */
	public SetRemoteAction() {
		super(TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH) +  "...");
	}

	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		setRemote();
	}

	
	/**
	 * Tries to set the remote for current branch.
	 */
	private void setRemote() {
		CurrentBranchRemotesDialog dialog = new CurrentBranchRemotesDialog();
		if(dialog.getStatusResult() == RemotesViewUtil.STATUS_REMOTE_NOT_EXISTS) {
			OKCancelDialog addRemoteDialog = new AddRemoteDialog();
			addRemoteDialog.setVisible(true);
			if(addRemoteDialog.getResult() == OKCancelDialog.RESULT_OK) {
				setRemote();
			}
		} else if(dialog.getStatusResult() == RemotesViewUtil.STATUS_BRANCHES_NOT_EXIST) {
		  MessagePresenterProvider.getBuilder(
          TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH), DialogType.ERROR)
          .setMessage(TRANSLATOR.getTranslation(Tags.NO_BRANCHES_FOUNDED))
          .setCancelButtonVisible(false)
          .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
          .buildAndShow();  
		}
	}

}
