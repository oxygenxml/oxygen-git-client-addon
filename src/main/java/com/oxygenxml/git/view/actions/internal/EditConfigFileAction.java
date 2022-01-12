package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Action to open the git config file.
 * 
 * @author Alex_Smarandache
 *
 */
public class EditConfigFileAction extends BaseGitAbstractAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(EditConfigFileAction.class);

	
	
    /**
     * Constructor.
     */
	public EditConfigFileAction() {
		super(TRANSLATOR.getTranslation(Tags.EDIT_CONFIG_FILE));
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			PluginWorkspaceProvider.getPluginWorkspace().open((
					new File(GitAccess.getInstance().getConfigFilePath()).toURI().toURL()), 
					null, "text/plain");
		} catch (MalformedURLException | NoRepositorySelected exc) {
			LOGGER.error(exc, exc);
		} 
	}
	
}
