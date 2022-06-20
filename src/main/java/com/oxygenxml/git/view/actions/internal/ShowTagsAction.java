package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.tags.TagsDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Action to show tags from current repository.
 * 
 * @author Alex_Smarandache
 *
 */
public class ShowTagsAction extends BaseGitAbstractAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	
	
	/**
	 * Constructor.
	 */
	public ShowTagsAction() {
		super(TRANSLATOR.getTranslation(Tags.SHOW_TAGS) + "...");
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.TAG));
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			final TagsDialog dialog = new TagsDialog();
			dialog.setVisible(true);
		} catch (GitAPIException | IOException | NoRepositorySelected ex) {
			PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
		}

	}

}
