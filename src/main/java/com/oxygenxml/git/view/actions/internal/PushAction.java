package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitController;

/**
 * Action to push.
 * 
 * @author alex_smarandache
 *
 */
public class PushAction extends AbstractAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(PushAction.class);

	/**
	 * The Git Controller.
	 */
	private final GitController gitController;



	/**
	 * Constructor.
	 * 
	 * @param gitController The Git Controller.
	 */
	public PushAction(final GitController gitController) {
		super(TRANSLATOR.getTranslation(Tags.PUSH));
		this.gitController = gitController;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		try { 
			if (GitAccess.getInstance().getRepository() != null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Push Button Clicked");
				}

				gitController.push();

			}
		} catch (NoRepositorySelected e1) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(e1, e1);
			}
		}

	}

}
