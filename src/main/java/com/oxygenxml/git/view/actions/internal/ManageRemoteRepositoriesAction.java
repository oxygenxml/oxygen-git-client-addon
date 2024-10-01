package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.remotes.RemotesRepositoryDialog;

/**
 * Action to manage the remote repositories from git config.
 * 
 * @author alex_smarandache
 *
 */
public class ManageRemoteRepositoriesAction extends GitAbstractAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageRemoteRepositoriesAction.class);
	
	
	
	/**
	 * Constructor.
	 */
	public ManageRemoteRepositoriesAction() {
		super(TRANSLATOR.getTranslation(Tags.MANAGE_REMOTE_REPOSITORIES) +  "...");
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.REMOTE));
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (GitAccess.getInstance().getRepository() != null) {
				new RemotesRepositoryDialog().configureRemotes();
			}
		} catch (NoRepositorySelected e1) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(e1.getMessage(), e1);
			}
		}
	}

}
