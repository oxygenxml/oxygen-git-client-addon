package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.remotes.RemotesRepositoryDialog;

/**
 * Action to show branches.
 * 
 * @author alex_smarandache
 *
 */
public class ShowBranchesAction extends AbstractAction {

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LogManager.getLogger(RemotesRepositoryDialog.class);

	/**
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Branches presenter.
	 */
	private final transient BranchManagementViewPresenter branchManagementViewPresenter;



	/**
	 * Branches presenter.
	 * 
	 * @param branchManagementViewPresenter
	 */
	public ShowBranchesAction(final BranchManagementViewPresenter branchManagementViewPresenter) {
		super(TRANSLATOR.getTranslation(Tags.SHOW_BRANCHES));
		this.branchManagementViewPresenter = branchManagementViewPresenter;
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.GIT_BRANCH_ICON));
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (GitAccess.getInstance().getRepository() != null) {
				branchManagementViewPresenter.showGitBranchManager();
			}
		} catch (NoRepositorySelected e1) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(e1, e1);
			}
		}

	}	

}
