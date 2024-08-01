package com.oxygenxml.git;

import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.auth.ResolvingProxyDataFactory;
import com.oxygenxml.git.auth.login.LoginMediator;
import com.oxygenxml.git.auth.sshagent.GitClientSshdSessionFactory;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.editorvars.GitEditorVariablesResolver;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitListeners;
import com.oxygenxml.git.service.RemoteRepositoryChangeWatcher;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.exceptions.IndexLockExistsException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.lfs.LFSSupport;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.GitAddonSystemProperties;
import com.oxygenxml.git.utils.LoggingUtil;
import com.oxygenxml.git.validation.ValidationManager;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.actions.GitActionsMenuBar;
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.branches.BranchCheckoutMediator;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.dialog.DetachedHeadDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.OperationUtil;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.progress.OperationProgressManager;
import com.oxygenxml.git.view.refresh.PanelsRefreshSupport;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.actions.MenusAndToolbarsContributorCustomizer;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Plugin extension - workspace access extension.
 * 
 * @author Beniamin Savu
 */
public class OxygenGitPluginExtension implements WorkspaceAccessPluginExtension, HistoryController, BranchManagementViewPresenter {

  /**
   * Treats different events provided by Git operations.
   * 
   * @author alex_smarandache
   */
	private final class GitOperationEventListener extends GitEventAdapter {
    @Override
    public void operationAboutToStart(GitEventInfo info) {
      if(info.getGitOperation() == GitOperation.PUSH 
          || info.getGitOperation() == GitOperation.PULL
          || info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
        LoginMediator.getInstance().reset();
      }
    
    }

    @Override
    public void operationSuccessfullyEnded(GitEventInfo info) {
    	final GitOperation operation = info.getGitOperation();
    	if (GIT_OPERATIONS_WITH_REQUIRED_REFRESH.contains(operation)) {
    		gitRefreshSupport.call();
    		if (operation == GitOperation.CHECKOUT || operation == GitOperation.MERGE) { // this operation need a super-refresh
    			try {
    				FileUtil.refreshProjectView();
    			} catch (NoRepositorySelected e) {
    				LOGGER.debug(e.getMessage(), e);
    			}
    		} else if (operation == GitOperation.OPEN_WORKING_COPY && GitAccess.getInstance().getBranchInfo().isDetached()) {
    		  treatDetachedHead((WorkingCopyGitEventInfo) info);
    		}
    				
    	}
    }

    @Override
    public void operationFailed(GitEventInfo info, Throwable t) {
    	final GitOperation operation = info.getGitOperation();
    	if (operation == GitOperation.CONTINUE_REBASE || operation == GitOperation.RESET_TO_COMMIT) {
    		gitRefreshSupport.call();
    	}
    }
    
    /**
     * Treat detached HEAD.
     * 
     * @param wcEventInfo event info.
     */
    private void treatDetachedHead(WorkingCopyGitEventInfo wcEventInfo) {
      if (wcEventInfo.isWorkingCopySubmodule()) {
        return;
      }

      Repository repo = null;
      try {
        repo = GitAccess.getInstance().getRepository();
      } catch (NoRepositorySelected e) {
        LOGGER.error(e.getMessage(), e);
      }

      if (repo != null && repo.getRepositoryState() != RepositoryState.REBASING_MERGE) {
        String commitFullID = GitAccess.getInstance().getBranchInfo().getBranchName();
        try (RevWalk revWalk = new RevWalk(repo)) {
          RevCommit commit = revWalk.parseCommit(repo.resolve(commitFullID));
          DetachedHeadDialog dlg = new DetachedHeadDialog(commit);
          dlg.setVisible(true);
        } catch (RevisionSyntaxException | IOException e) {
          LOGGER.debug(e.getMessage(), e);
        }
      }
    }
  }

  /**
	 * i18n
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(OxygenGitPluginExtension.class);

	/**
	 * ID of the Git staging view. Defined in plugin.xml.
	 */
	public static final String GIT_STAGING_VIEW = "GitStagingView";

	/**
	 * ID of the Git History view.
	 */
	public static final String GIT_HISTORY_VIEW = "GitHistoryView";

	/**
	 * ID of the Git Branch view.
	 */
	public static final String GIT_BRANCH_VIEW = "GitBranchView";

	/**
	 * Refresh support.
	 */
	private PanelsRefreshSupport gitRefreshSupport;

	/**
	 * Manages Push/Pull actions.
	 */
	private GitController gitController = new GitController();
	
	private static final Set<GitOperation> GIT_OPERATIONS_WITH_REQUIRED_REFRESH = ImmutableSet.of(
	    GitOperation.CHECKOUT, 
	    GitOperation.CONTINUE_REBASE, 
	    GitOperation.RESET_TO_COMMIT, 
	    GitOperation.OPEN_WORKING_COPY, 
	    GitOperation.MERGE, 
	    GitOperation.COMMIT, 
	    GitOperation.REVERT_COMMIT, 
	    GitOperation.STASH_CREATE,
	    GitOperation.STASH_APPLY,
	    GitOperation.STASH_DROP,
	    GitOperation.UPDATE_CONFIG_FILE,
	    GitOperation.STASH_POP,
	    GitOperation.CHECKOUT_FILE,
	    GitOperation.CHECKOUT_COMMIT,
	    GitOperation.CREATE_TAG,
	    GitOperation.DELETE_TAG
	);
	
	/**
	 * Window listener used to call the refresh command when the Oxygen window is activated
	 */
	private WindowAdapter panelRefreshWindowListener = new WindowAdapter() {

		private boolean refresh = false;

		@Override
		public void windowActivated(WindowEvent e) {
			// Reset when you leave oxygen. Subsequent calls will recompute it.
			gitController.getGitAccess().getStatusCache().resetCache();
			super.windowActivated(e);
			final boolean isStagingPanelShowing = stagingPanel != null && stagingPanel.isShowing();
			final boolean isHistoryPanelShowing = historyView != null && historyView.isShowing();
			final boolean isBranchesPanelShowing = branchManagementPanel != null && branchManagementPanel.isShowing();
			if (refresh && (isStagingPanelShowing || isHistoryPanelShowing || isBranchesPanelShowing)) {
				gitRefreshSupport.call();
			}
			refresh = false;
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			super.windowDeactivated(e);
			SwingUtilities.invokeLater(() -> {
				Object focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				if (focusedWindow == null) {
					refresh = true;
				}
			});
		}
	};

	/**
	 * Staging panel.
	 */
	private StagingPanel stagingPanel;

	/**
	 * Plugin workspace access.
	 */
	private StandalonePluginWorkspace pluginWorkspaceAccess;

	/**
	 * History view.
	 */
	private HistoryPanel historyView;

	/**
	 * Branch management panel.
	 */
	private BranchManagementPanel branchManagementPanel;
	
	/**
	 * The menu bar that contributes the Git actions
	 */
	private GitActionsMenuBar menuBar;
	
	/**
	 * Customize the menus and add some Git actions.
	 */
	private MenusAndToolbarsContributorCustomizer menusAndToolbarsCustomizer = new MenusAndToolbarsContributorCustomizer() {
		EditorPageMenuGitActionsProvider editorPageActionsProvider = 
				new EditorPageMenuGitActionsProvider(OxygenGitPluginExtension.this);
		@Override
		public void customizeAuthorPopUpMenu(JPopupMenu popUp, AuthorAccess authorAccess) {
			URL editorURL = authorAccess.getEditorAccess().getEditorLocation();
			List<AbstractAction> actions = editorPageActionsProvider.getActionsForCurrentEditorPage(editorURL);
			if (!actions.isEmpty()) {
				UIUtil.addGitActions(popUp, actions);
			}
		}
		@Override
		public void customizeTextPopUpMenu(JPopupMenu popUp, WSTextEditorPage textPage) {
			URL editorURL = textPage.getParentEditor().getEditorLocation();
			List<AbstractAction> actions = editorPageActionsProvider.getActionsForCurrentEditorPage(editorURL);
			if (!actions.isEmpty()) {
				UIUtil.addGitActions(popUp, actions);
			}
		}
	};

	/**
	 * @see WorkspaceAccessPluginExtension#applicationStarted(StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(final StandalonePluginWorkspace pluginWS) {
	  this.pluginWorkspaceAccess = pluginWS;

	  OptionsManager.getInstance().loadOptions(pluginWS.getOptionsStorage());
	  ProjectHelper.getInstance().installProjectChangeListener(pluginWS.getProjectManager(), () -> stagingPanel);
	  
	  GitListeners.getInstance().addGitPriorityListener(new GitEventAdapter() {
	    @Override
      public void operationAboutToStart(GitEventInfo info) throws IndexLockExistsException {
	      if(info.getGitOperation() != GitOperation.OPEN_WORKING_COPY) {
	        try {
	          String repoDir = GitAccess.getInstance().getRepository().getDirectory().getAbsolutePath();
	          File lockFile = new File(repoDir, "index.lock"); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false positive
	          if (lockFile.exists()) {
	            throw new IndexLockExistsException();
	          }
	        } catch (NoRepositorySelected e) {
	          LOGGER.error(e.getMessage(), e);
	        }
	      }
	    }
	    
	    @Override
	    public void operationFailed(GitEventInfo gitEvent, Throwable t) {
	      if (t instanceof IndexLockExistsException) {
	        try {
	          String repoDir = GitAccess.getInstance().getRepository().getDirectory().getAbsolutePath();
	          String message = MessageFormat.format(
	              Translator.getInstance().getTranslation(Tags.LOCK_FAILED_EXPLANATION),
	              new File(repoDir, "index.lock").getAbsolutePath());
	          pluginWS.showErrorMessage(message);
	        } catch (NoRepositorySelected e) {
	          LOGGER.error(e.getMessage(), e);
	        }
	      }
	    }
	  });
	  
	  gitController.addGitListener(new GitEventAdapter() {
	    @Override
	    public void operationSuccessfullyEnded(GitEventInfo info) {
	      if(info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
	        try {
	          final File wc = GitAccess.getInstance().getWorkingCopy();
	          final String absolutePath = wc.getAbsolutePath();
	          OptionsManager.getInstance().addRepository(absolutePath);
	          OptionsManager.getInstance().saveSelectedRepository(absolutePath);
	        } catch (NoRepositorySelected e) {
	          LOGGER.error(e.getMessage(), e);
	        }

	        OptionsManager.getInstance().setCurrentBranch(gitController.getGitAccess().getBranchInfo().getBranchName()); // reset branch
	      } else if(info.getGitOperation() == GitOperation.PULL) {
	        EditorContentReloader.reloadCurrentEditor(pluginWS);
	      }
	    }
	  });

	  gitController.setBranchesCheckoutMediator(new BranchCheckoutMediator(gitController));
	  gitController.getGitAccess().installOperationProgressSupport(new OperationProgressManager(gitController));
	  LFSSupport.install(gitController);

	  final UtilAccess utilAccess = pluginWS.getUtilAccess();
	  utilAccess.addCustomEditorVariablesResolver(new GitEditorVariablesResolver(gitController));

	  gitRefreshSupport = new PanelsRefreshSupport(
	      RemoteRepositoryChangeWatcher.createWatcher(pluginWS, gitController),
	      () -> menuBar);

	  final GitActionsManager gitActionsManager = new GitActionsManager(gitController, this, this, gitRefreshSupport);
	  menuBar = new GitActionsMenuBar(gitActionsManager);
	  pluginWS.addMenuBarCustomizer(menuBar);

	  try {
	    // Uncomment this to start with fresh options. For testing purposes
	    // PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null); NOSONAR

	    if (!"true".equals(System.getProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS))) {
	      org.eclipse.jgit.transport.SshSessionFactory.setInstance(
	          new GitClientSshdSessionFactory(new ResolvingProxyDataFactory()));
	    } 

	    AuthenticationInterceptor.install();

	    BlameManager.getInstance().install(gitController);

	    // Add Git actions to the contextual menu of the Project view
	    ProjectMenuGitActionsProvider projectMenuGitActionsProvider = new ProjectMenuGitActionsProvider(
	        pluginWorkspaceAccess,
	        gitController,
	        OxygenGitPluginExtension.this);
	    ProjectViewManager.addPopUpMenuCustomizer(projectMenuGitActionsProvider);

	    // Add Git actions to the contextual menu of the current editor page
	    pluginWorkspaceAccess.addMenusAndToolbarsContributorCustomizer(menusAndToolbarsCustomizer);

	    // Customize the contributed side-views
	    pluginWorkspaceAccess.addViewComponentCustomizer(
	        viewInfo -> {

	          // The constants' values are defined in plugin.xml
	          if (GIT_STAGING_VIEW.equals(viewInfo.getViewID())) {
	            customizeGitStagingView(viewInfo, gitActionsManager);
	          } else if (GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
	            customizeHistoryView(viewInfo);
	          } else if(GIT_BRANCH_VIEW.equals(viewInfo.getViewID())) {
	            customizeBranchView(viewInfo);
	          }
	        });

	    // Listens on the save event in the Oxygen editor and invalidates the cache.
	    GitAccess.getInstance().getStatusCache().installEditorsHook(pluginWS);

	    // Present the view to the user if it is the first run of the plugin
	    final JFrame parentFrame = (JFrame) pluginWorkspaceAccess.getParentFrame();
	    parentFrame.addComponentListener(new ComponentAdapter() {
	      @Override
	      public void componentShown(ComponentEvent e) {
	        String key = "view.presented.on.first.run";
	        String firstRun = pluginWorkspaceAccess.getOptionsStorage().getOption(key, null);
	        if (firstRun == null) {
	          // This is the first run of the plugin.
	          pluginWorkspaceAccess.showView(GIT_STAGING_VIEW, false);
	          pluginWorkspaceAccess.getOptionsStorage().setOption(key, "true");
	        }
	      }
	    });

	    // Call the refresh command when the Oxygen window is activated
	    parentFrame.addWindowListener(panelRefreshWindowListener);

	    LoggingUtil.setupLogger();

	  } catch (Throwable t) { // NOSONAR
	    // Catch Throwable - Runtime exceptions shouldn't affect Oxygen.
	    pluginWorkspaceAccess.showErrorMessage(t.getMessage());
	    LOGGER.error(t.getMessage(), t);
	  }
	  
	}

	/**
	 * Customize the Git Staging view.
	 * 
	 * @param viewInfo View information.
	 */
	private void customizeGitStagingView(final ViewInfo viewInfo, final GitActionsManager gitActionsManager) {
		boolean shouldRecreateStagingPanel = Objects.isNull(stagingPanel);
		if (shouldRecreateStagingPanel) {
			stagingPanel = new StagingPanel(
					gitRefreshSupport,
					gitController,
					OxygenGitPluginExtension.this,
					gitActionsManager);
			OperationUtil.installMouseBusyCursor(gitController, stagingPanel); 
			OperationUtil.installMouseBusyCursor(ValidationManager.getInstance(), stagingPanel); 
			gitRefreshSupport.setStagingPanel(stagingPanel);
			installGitOperationsListener();
		}
		
		viewInfo.setComponent(stagingPanel);
	
		gitRefreshSupport.call();

		viewInfo.setIcon(Icons.getIcon(Icons.GIT_ICON));
		viewInfo.setTitle(translator.getTranslation(Tags.GIT_STAGING));
	}

	/**
	 * Install a listener for git operations.
	 */
  private void installGitOperationsListener() {
    gitController.addGitListener(new GitOperationEventListener());
  }
  
	/**
	 * Customize the history view.
	 * 
	 * @param viewInfo  View information.
	 */
	private void customizeHistoryView(ViewInfo viewInfo) {
		if (historyView == null) {
			historyView = new HistoryPanel(gitController);
			gitRefreshSupport.setHistoryPanel(historyView);
		}
		viewInfo.setComponent(historyView);

		viewInfo.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
		viewInfo.setTitle(translator.getTranslation(Tags.GIT_HISTORY));
	}

	/**
	 * Customize the branch management view.
	 * 
	 * @param viewInfo View information.
	 */
	private void customizeBranchView(ViewInfo viewInfo) {
		if(branchManagementPanel == null) {
			branchManagementPanel = new BranchManagementPanel(gitController);
			gitRefreshSupport.setBranchPanel(branchManagementPanel);
		}

		viewInfo.setComponent(branchManagementPanel);
		viewInfo.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
		viewInfo.setTitle(translator.getTranslation((Tags.BRANCH_MANAGER_TITLE)));
	}

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */
	@Override
	public boolean applicationClosing() {
		// EXM-42867: wait for the refresh to execute
		gitRefreshSupport.shutdown();

		GitAccess.getInstance().closeRepo();

		// Close application.
		return true;
	}

	@Override
	public void showGitBranchManager() {
	  if(branchManagementPanel.isShowing()) {
      branchManagementPanel.showBranches();
    } else {
      // EXM-49642
      // No need to call @BranchManagementPanel::showBranches because the installed @HierarchyListener will detect that the view has been displayed 
      // and will call the method
      pluginWorkspaceAccess.showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_BRANCH_VIEW, true);
    }
	}

	@Override
	public boolean isGitBranchManagerViewShowing() {
		return pluginWorkspaceAccess.isViewShowing(com.oxygenxml.git.OxygenGitPluginExtension.GIT_BRANCH_VIEW);
	}

	@Override
	public void showRepositoryHistory() {
		pluginWorkspaceAccess.showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_HISTORY_VIEW, true);
		historyView.showRepositoryHistory();    
	}

	@Override
	public void showResourceHistory(String path) {
		pluginWorkspaceAccess.showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_HISTORY_VIEW, true);
		historyView.showHistory(path);    
	}

	@Override
	public void showCommit(String filePath, RevCommit activeRevCommit) {
		pluginWorkspaceAccess.showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_HISTORY_VIEW, false);
		historyView.showCommit(filePath, activeRevCommit);
	}

	@Override
	public boolean isHistoryShowing() {
		return pluginWorkspaceAccess.isViewShowing(com.oxygenxml.git.OxygenGitPluginExtension.GIT_HISTORY_VIEW);
	}
	
	/**
	 * Setter for staging panel.
	 * 
	 * @param stagingPanel The new staging panel.
	 */
	@TestOnly
	public void setStagingPanel(final StagingPanel stagingPanel) {
    this.stagingPanel = stagingPanel;
  }
	
	/**
   * Setter for refresh support panel.
   * 
   * @param gitRefreshSupport The new refresh support panel.
   */
	@TestOnly
	public void setGitRefreshSupport(final PanelsRefreshSupport gitRefreshSupport) {
    this.gitRefreshSupport = gitRefreshSupport;
  }

}