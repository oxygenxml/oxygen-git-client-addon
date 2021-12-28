package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;

/**
 * Action for submodule.
 * 
 * @author alex_smarandache
 *
 */
public class SubmodulesAction extends AbstractAction {
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Constructor.
	 */
	public SubmodulesAction() {
		super(TRANSLATOR.getTranslation(Tags.SUBMODULE));
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		new SubmoduleSelectDialog();
	}
}
