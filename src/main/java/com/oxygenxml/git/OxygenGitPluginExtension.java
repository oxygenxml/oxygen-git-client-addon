package com.oxygenxml.git;

import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.auth.ResolvingProxyDataFactory;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitAddonSystemProperties;
import com.oxygenxml.git.utils.Log4jUtil;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitCommandState;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.historycomponents.HistoryController;
import com.oxygenxml.git.view.historycomponents.HistoryPanel;
import com.oxygenxml.git.watcher.RepositoryChangeWatcher;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.actions.MenusAndToolbarsContributorCustomizer;

/**
 * Plugin extension - workspace access extension.
 * 
 * @author Beniamin Savu
 */
public class OxygenGitPluginExtension implements WorkspaceAccessPluginExtension, HistoryController {

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
	 * Refresh support.
	 */
	final PanelRefresh gitRefreshSupport = new PanelRefresh();
	
	/**
	 * Manages Push/Pull actions.
	 */
	private PushPullController pushPullController;
	
	/**
	 * Window listener used to call the refresh command when the Oxygen window is activated
	 */
  private WindowAdapter panelRefreshWindowListener = new WindowAdapter() {

    private boolean refresh = false;

    @Override
    public void windowActivated(WindowEvent e) {
      super.windowActivated(e);
      if (refresh && stagingPanel != null && stagingPanel.isShowing()) {
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
	 * @see WorkspaceAccessPluginExtension#applicationStarted(StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(final StandalonePluginWorkspace pluginWS) {
	  pluginWorkspaceAccess = pluginWS;
	  pushPullController = PushPullController.createPushPullController();
		try {
		  // Uncomment this to start with fresh options. For testing purposes
//			PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption("GIT_PLUGIN_OPTIONS", null); NOSONAR

		  if (!"true".equals(System.getProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS))) {
  		  org.eclipse.jgit.transport.SshSessionFactory.setInstance(
  		      new org.eclipse.jgit.transport.sshd.SshdSessionFactory(null, new ResolvingProxyDataFactory()));
		  }
		  
		  AuthenticationInterceptor.install();

			GitController gitCtrl = new GitController();
			
			// Add Git actions to the contextual menu of the Project view
			ProjectMenuGitActionsProvider projectMenuGitActionsProvider = new ProjectMenuGitActionsProvider(
          pluginWorkspaceAccess,
          gitCtrl,
          OxygenGitPluginExtension.this);
      ProjectViewManager.addPopUpMenuCustomizer(projectMenuGitActionsProvider);
      
      // Add Git actions to the contexual menu of the current editor page
      EditorPageMenuGitActionsProvider editorPageActionsProvider = 
          new EditorPageMenuGitActionsProvider(OxygenGitPluginExtension.this);
      pluginWorkspaceAccess.addMenusAndToolbarsContributorCustomizer(new MenusAndToolbarsContributorCustomizer() {
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
      });

			pluginWorkspaceAccess.addViewComponentCustomizer(
			    viewInfo -> {
            // The constants' values are defined in plugin.xml
            if (GIT_STAGING_VIEW.equals(viewInfo.getViewID())) {
              customizeGitStagingView(gitCtrl, viewInfo);
          	} else if (GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
          	  customizeHistoryView(gitCtrl, viewInfo);
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
		
		RepositoryChangeWatcher.createWatcher(pluginWorkspaceAccess, pushPullController);
	}
	
	/**
	 * Customize the Git Staging view.
	 * 
	 * @param gitCtrl  Git controller.
	 * @param viewInfo View information.
	 */
	private void customizeGitStagingView(GitController gitCtrl, ViewInfo viewInfo) {
    boolean shouldRecreateStagingPanel = stagingPanel == null;
    if (shouldRecreateStagingPanel) {
      stagingPanel = new StagingPanel(gitRefreshSupport, gitCtrl, OxygenGitPluginExtension.this, pushPullController);
      gitRefreshSupport.setPanel(stagingPanel);
    }
    viewInfo.setComponent(stagingPanel);
    
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryIsAboutToOpen(File repo) {
        SwingUtilities.invokeLater(() -> viewInfo.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
      }
      
      @Override
      public void repositoryChanged() {
        SwingUtilities.invokeLater(() -> viewInfo.getComponent().setCursor(Cursor.getDefaultCursor()));
      }
      
      @Override
      public void repositoryOpeningFailed(File repo, Throwable ex) {
        SwingUtilities.invokeLater(() -> viewInfo.getComponent().setCursor(Cursor.getDefaultCursor()));
      }
     
      private Timer cursorTimer = new Timer(
          1000,
          e -> SwingUtilities.invokeLater(() -> viewInfo.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))));
      @Override
      public void stateChanged(com.oxygenxml.git.view.event.GitEvent changeEvent) {
        GitCommand cmd = changeEvent.getGitCommand();
        GitCommandState cmdState = changeEvent.getGitComandState();
        if (cmdState == GitCommandState.STARTED) {
          cursorTimer.restart();
        } else if (cmdState == GitCommandState.SUCCESSFULLY_ENDED || cmdState == GitCommandState.FAILED) {
          cursorTimer.stop();
          SwingUtilities.invokeLater(() -> viewInfo.getComponent().setCursor(Cursor.getDefaultCursor()));
        
          if (cmd == GitCommand.CONTINUE_REBASE) {
            gitRefreshSupport.call();
          }
        }
      }
      
      @Override
      public void branchChanged(String oldBranch, String newBranch) {
        gitRefreshSupport.call();
        try {
          FileHelper.refreshProjectView();
        } catch (NoRepositorySelected e) {
          logger.debug(e, e);
        }
      }
    });
    
    gitRefreshSupport.call();
    
    viewInfo.setIcon(Icons.getIcon(Icons.GIT_ICON));
    viewInfo.setTitle(Translator.getInstance().getTranslation(Tags.GIT_STAGING));
  }
	
	/**
	 * Customize the history view.
	 * 
	 * @param gitCtrl   Git controller.
	 * @param viewInfo  View information.
	 */
  private void customizeHistoryView(GitController gitCtrl, ViewInfo viewInfo) {
    if (historyView == null) {
      historyView = new HistoryPanel(gitCtrl);
    }
    viewInfo.setComponent(historyView);
    
    viewInfo.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
    viewInfo.setTitle(Translator.getInstance().getTranslation(Tags.GIT_HISTORY));
  }



	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */
	@Override
	public boolean applicationClosing() {
		OptionsManager.getInstance().saveOptions();
		
		// EXM-42867: wait for the refresh to execute
		gitRefreshSupport.shutdown();
		
		GitAccess.getInstance().closeRepo();
		
		// Close application.
		return true;
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