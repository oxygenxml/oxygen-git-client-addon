package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;

/**
 * Action to show branches.
 * 
 * @author alex_smarandache
 *
 */
public class ShowBranchesAction extends GitAbstractAction {

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
	  branchManagementViewPresenter.showGitBranchManager();
	}	

}
