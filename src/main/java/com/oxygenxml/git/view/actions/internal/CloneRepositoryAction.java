package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;

/**
 * Action to clone a repository.
 * 
 * @author alex_smarandache
 *
 */
public class CloneRepositoryAction extends AlwaysEnabledAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	/**
	 * Constructor.
	 */
	public CloneRepositoryAction() {
		super(TRANSLATOR.getTranslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP) + "...");
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON));
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		new CloneRepositoryDialog();
	}
	
}
