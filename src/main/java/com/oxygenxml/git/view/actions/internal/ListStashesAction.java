package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.stash.ListStashesDialog;


/**
 * Action to list stashes from current repository.
 * 
 * @author Alex_Smarandache
 *
 */
public class ListStashesAction extends BaseGitAbstractAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();



	/**
	 * Constructor.
	 */
	public ListStashesAction() {
		super(TRANSLATOR.getTranslation(Tags.LIST_STASHES) + "...");
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		ListStashesDialog stashesDialog = new ListStashesDialog();
		stashesDialog.setVisible(true);
	}
}
