package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.stash.StashUtil;

/**
 * Action to stash changes.
 * 
 * @author Alex_Smarandache
 *
 */
public class StashChangesAction extends AbstractAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	
	
	/**
	 * Constructor.
	 */
	public StashChangesAction() {
		super(TRANSLATOR.getTranslation(Tags.STASH_CHANGES) + "...");
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		StashUtil.stashChanges();
	}

}
