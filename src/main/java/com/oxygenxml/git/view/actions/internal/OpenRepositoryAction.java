package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.io.File;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.event.GitController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Open a repository from disk.
 * 
 * @author Alex_Smarandache
 *
 */
public class OpenRepositoryAction extends AlwaysEnabledAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	/**
	 * The Git Controller.
	 */
	private final GitController gitController;
	
	
	/**
	 * Constructor.
	 * 
	 * @param gitController The Git Controller.
	 */
	public OpenRepositoryAction(GitController gitController) {
		super(TRANSLATOR.getTranslation(Tags.OPEN_REPOSITORY) + "...");
		this.gitController = gitController;
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.FILE_CHOOSER_ICON));
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		File directory = PluginWorkspaceProvider.getPluginWorkspace().chooseDirectory();
		if(directory != null) {
			final String directoryPath = directory.getAbsolutePath();
			if (FileUtil.isGitRepository(directoryPath)) {
				// adds the directory path to the combo box if it doesn't already exist
				OptionsManager.getInstance().addRepository(directoryPath);
				gitController.getGitAccess().setRepositoryAsync(directoryPath);
			} else {
				PluginWorkspaceProvider.getPluginWorkspace()
				.showInformationMessage(TRANSLATOR.getTranslation(Tags.WORKINGCOPY_NOT_GIT_DIRECTORY));
			}
		}
		
	}

}
