package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;

/**
 * Action for submodule.
 * 
 * @author alex_smarandache
 *
 */
public class SubmodulesAction extends GitAbstractAction {
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Constructor.
	 */
	public SubmodulesAction() {
		super(TRANSLATOR.getTranslation(Tags.SUBMODULE));
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.GIT_SUBMODULE_ICON));
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		new SubmoduleSelectDialog();
	}
}
