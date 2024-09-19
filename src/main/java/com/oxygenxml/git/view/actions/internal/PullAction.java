package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;

/**
 * Pull action.
 * 
 * @author alex_smarandache
 *
 */
public class PullAction extends BaseGitAbstractAction {
	
	/**
	 * Logger for logging.
	 */
  private static final Logger LOGGER = LoggerFactory.getLogger(PullAction.class);
	
	/**
	 * Property for pull type.
	 */
	public static final String PULL_TYPE_ACTION_PROP = "pullType";

	/**
	 * The pull type.
	 */
    private final PullType pullType;
    
    /**
     * The Git Controller.
     */
    private final transient GitController gitController;
    
    /**
     * The progress monitor of the action.
     */
    private final IGitViewProgressMonitor progressMonitor;
   
    
	
    /**
     * Constructor.
     * 
     * @param gitController   Git Controller.
     * @param name            Action name.
     * @param pullType        The pull type.
     * @param progressMonitor The progress monitor of the action.
     */
    public PullAction(final GitController gitController, final String name, final PullType pullType, final IGitViewProgressMonitor progressMonitor) {
      super(name);
      this.pullType = pullType;
      this.progressMonitor = progressMonitor;
      putValue(PULL_TYPE_ACTION_PROP, pullType);
      this.gitController = gitController;
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        if (GitAccess.getInstance().getRepository() != null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pull action invoked");
          }
          gitController.pull(pullType, progressMonitor);
          OptionsManager.getInstance().saveDefaultPullType(pullType);
        }
      } catch (NoRepositorySelected e1) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e1.getMessage(), e1);
        }
      }
    }
    
    
  }
