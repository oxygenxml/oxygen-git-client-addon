package com.oxygenxml.git;

import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.auth.ResolvingProxyDataFactory;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.editorvars.GitEditorVariablesResolver;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RemoteRepositoryChangeWatcher;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.GitAddonSystemProperties;
import com.oxygenxml.git.utils.Log4jUtil;
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.dialog.DetachedHeadDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.refresh.PanelRefresh;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
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
   * i18n
   */
  private static Translator translator = Translator.getInstance();

  /**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(OxygenGitPluginExtension.class);

	/**
	 * ID of the Git staging view. Defined in plugin.xml.
	 */
	static final String GIT_STAGING_VIEW = "GitStagingView";
	
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
	private PanelRefresh gitRefreshSupport;
	
	/**
	 * Manages Push/Pull actions.
	 */
	private GitController gitController;
	
	/**
	 * Window listener used to call the refresh command when the Oxygen window is activated
	 */
  private WindowAdapter panelRefreshWindowListener = new WindowAdapter() {

    private boolean refresh = false;

    @Override
    public void windowActivated(WindowEvent e) {
      super.windowActivated(e);
      boolean isStagingPanelShowing = stagingPanel != null && stagingPanel.isShowing();
      if (isStagingPanelShowing && refresh) {
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
	  OptionsManager.getInstance().loadOptions(pluginWS.getOptionsStorage());
	  pluginWorkspaceAccess = pluginWS;
	  gitController = new GitController(GitAccess.getInstance());
		try {
		  // Uncomment this to start with fresh options. For testing purposes
//			PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null); NOSONAR

		  if (!"true".equals(System.getProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS))) {
  		  org.eclipse.jgit.transport.SshSessionFactory.setInstance(
  		      new org.eclipse.jgit.transport.sshd.SshdSessionFactory(null, new ResolvingProxyDataFactory()));
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
              installCursorUpdater(viewInfo.getComponent());
              customizeGitStagingView(viewInfo);
          	} else if (GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
          	  customizeHistoryView(viewInfo);
          	} else if(GIT_BRANCH_VIEW.equals(viewInfo.getViewID())) {
          	  customizeBranchView(viewInfo);
          	}
          });

			
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
			
			Log4jUtil.setupLog4JLogger();
			
		} catch (Throwable t) { // NOSONAR
			// Catch Throwable - Runtime exceptions shouldn't affect Oxygen.
			pluginWorkspaceAccess.showErrorMessage(t.getMessage());
			logger.fatal(t, t);
		}
		
		RemoteRepositoryChangeWatcher watcher = RemoteRepositoryChangeWatcher.createWatcher(pluginWorkspaceAccess, gitController);
		gitRefreshSupport = new PanelRefresh(watcher);
	  
		UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
    utilAccess.addCustomEditorVariablesResolver(new GitEditorVariablesResolver(gitController));
	}

	/**
	 * When Git operations are running set a working cursor.
	 * 
	 * @param stagingViewComponent The staging view component on which to update the cursor. 
	 */
  private void installCursorUpdater(JComponent stagingViewComponent) {
    gitController.addGitListener(
        // Set the proper cursor when operations start and end.
        new GitEventAdapter() {
          private Timer cursorTimer = new Timer(
              1000,
              e -> SwingUtilities.invokeLater(
                  () -> stagingViewComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
                  ));
          {
            cursorTimer.setRepeats(false); // NOSONAR java:S1171
          }

          @Override
          public void operationAboutToStart(GitEventInfo info) {
            cursorTimer.restart();
          }
          @Override
          public void operationSuccessfullyEnded(GitEventInfo info) {
            cursorTimer.stop();
            SwingUtilities.invokeLater(() -> stagingViewComponent.setCursor(Cursor.getDefaultCursor()));
          }
          @Override
          public void operationFailed(GitEventInfo info, Throwable t) {
            cursorTimer.stop();
            SwingUtilities.invokeLater(() -> stagingViewComponent.setCursor(Cursor.getDefaultCursor()));
          }
        });
  }

	/**
	 * Customize the Git Staging view.
	 * 
	 * @param viewInfo View information.
	 */
	private void customizeGitStagingView(ViewInfo viewInfo) {
    boolean shouldRecreateStagingPanel = stagingPanel == null;
    if (shouldRecreateStagingPanel) {
      stagingPanel = new StagingPanel(
          gitRefreshSupport,
          gitController,
          OxygenGitPluginExtension.this,
          OxygenGitPluginExtension.this);
      gitRefreshSupport.setStagingPanel(stagingPanel);
    }
    viewInfo.setComponent(stagingPanel);
    
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.CHECKOUT
            || operation == GitOperation.CONTINUE_REBASE 
            || operation == GitOperation.RESET_TO_COMMIT
            || operation == GitOperation.OPEN_WORKING_COPY
            || operation == GitOperation.MERGE
            || operation == GitOperation.REVERT_COMMIT 
            || operation == GitOperation.STASH_CREATE
            || operation == GitOperation.STASH_DROP
            || operation == GitOperation.STASH_APPLY
            || operation == GitOperation.STASH_POP
            || operation == GitOperation.CHECKOUT_FILE
            || operation == GitOperation.TAG_COMMIT
            || operation == GitOperation.TAG_DELETE) {
          gitRefreshSupport.call();
          
          if (operation == GitOperation.CHECKOUT
              || operation == GitOperation.MERGE) {
            try {
              FileUtil.refreshProjectView();
            } catch (NoRepositorySelected e) {
              logger.debug(e, e);
            }
          } else if (operation == GitOperation.OPEN_WORKING_COPY
              && GitAccess.getInstance().getBranchInfo().isDetached()) {
            treatDetachedHead((WorkingCopyGitEventInfo) info);
          }
        }
      }
      
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.CONTINUE_REBASE || operation == GitOperation.RESET_TO_COMMIT) {
          gitRefreshSupport.call();
        }
      }
    });
    
    gitRefreshSupport.call();
    
    viewInfo.setIcon(Icons.getIcon(Icons.GIT_ICON));
    viewInfo.setTitle(translator.getTranslation(Tags.GIT_STAGING));
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
      logger.error(e, e);
    }

    if (repo != null && repo.getRepositoryState() != RepositoryState.REBASING_MERGE) {
      String commitFullID = GitAccess.getInstance().getBranchInfo().getBranchName();
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commit = revWalk.parseCommit(repo.resolve(commitFullID));
        DetachedHeadDialog dlg = new DetachedHeadDialog(commit);
        dlg.setVisible(true);
      } catch (RevisionSyntaxException | IOException e) {
        logger.debug(e, e);
      }
    }
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
	  pluginWorkspaceAccess.showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_BRANCH_VIEW, true);
	  branchManagementPanel.showBranches();
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

}