package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.validation.ValidationManager;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;

/**
 * Action to push.
 * 
 * @author alex_smarandache
 *
 */
public class PushAction extends BaseGitAbstractAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PushAction.class);

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
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.GIT_PUSH_ICON));
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		try { 
			if (GitAccess.getInstance().getRepository() != null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Push Button Clicked");
				}

				GitOperationScheduler.getInstance().schedule(() -> {
				  if(!ValidationManager.getInstance().isPrePushValidationEnabled() 
				      || ValidationManager.getInstance().checkPushValid()) {
				    Optional<IGitViewProgressMonitor> progressMonitor = Optional.of(
				        new GitOperationProgressMonitor(
				            new ProgressDialog(
				                TRANSLATOR.getTranslation(Tags.PUSH),
				                false)));
            gitController.push(progressMonitor);
				  }
				});
				
			}
		} catch (NoRepositorySelected e1) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(e1.getMessage(), e1);
			}
		}

	}

}
