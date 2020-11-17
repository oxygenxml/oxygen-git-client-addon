package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitController;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.Subject;
import com.oxygenxml.git.view.history.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Main panel containing all the other panels. It also creates them
 * 
 * @author Beniamin Savu
 *
 */
public class StagingPanel extends JPanel implements Observer<PushPullEvent> {

  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(StagingPanel.class);

  /**
   * <code>true</code> if focus gained.
   */
	boolean focusGained = false;
	
	/**
	 * The tool bar panel used for the push and pull
	 */
	private ToolbarPanel toolbarPanel;

	/**
	 * The working copy panel used for selecting and adding a working copy
	 */
	private WorkingCopySelectionPanel workingCopySelectionPanel;

	/**
	 * The unsatging area
	 */
	private ChangesPanel unstagedChangesPanel;

	/**
	 * The staging area
	 */
	private ChangesPanel stagedChangesPanel;
	
	/**
	 * Conflict buttons panel (continue rebase, abort rebase, abort merge, etc).
	 */
	private ConflictButtonsPanel conflictButtonsPanel;

	/**
	 * The commit panel
	 */
	private CommitAndStatusPanel commitPanel;

	/**
	 * List of listeners for this panel
	 */
	private List<Subject<PushPullEvent>> subjects = new ArrayList<>();

	/**
	 * Main panel refresh
	 */
	private GitRefreshSupport refreshSupport;

	/**
	 * Staging controller.
	 */
	private GitController stageController;
	
	/** 
	 * Manages Push/Pull actions.
	 */
  private PushPullController pushPullController;
  
  /**
   * Plugin workspace access.
   */
  private StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();

  /**
   * Constructor.
   * 
   * @param refreshSupport   Refresh support.        
   * @param stageController  Staging controller.
   * @param historyController History related interaction.
   * @param pushPullController High level push and pull support.
   */
  public StagingPanel(
      GitRefreshSupport refreshSupport, 
      GitController stageController, 
      HistoryController historyController,
      BranchManagementViewPresenter branchManagementView,
      PushPullController pushPullController) {
		this.refreshSupport = refreshSupport;
		this.stageController = stageController;
		this.pushPullController = pushPullController;
		
		createGUI(historyController, branchManagementView);
	}
  
  /**
   * Create toolbar. <br><br>
   * 
   * Not created from 99% of the test cases.
   * 
   * @param historyController History controller.
   * @param branchManagementViewPresenter Branch management interface.
   * @param gitController Git operations controller.
   */
  protected ToolbarPanel createToolbar(HistoryController historyController, BranchManagementViewPresenter branchManagementViewPresenter, GitController gitController) {
    return new ToolbarPanel(pushPullController, refreshSupport, historyController, branchManagementViewPresenter, gitController);
  }

	/**
	 * Create the GUI.
	 * 
	 * @param historyController History related interaction.
	 */
	private void createGUI(HistoryController historyController, BranchManagementViewPresenter branchManagementViewPresenter) {
		this.setLayout(new GridBagLayout());

		// Creates the panels objects that will be in the staging panel
		unstagedChangesPanel = new ChangesPanel(stageController, historyController, false);
		stagedChangesPanel = new ChangesPanel(stageController, historyController, true);
		workingCopySelectionPanel = new WorkingCopySelectionPanel(stageController);
		commitPanel = new CommitAndStatusPanel(pushPullController, stageController);
		toolbarPanel = createToolbar(historyController, branchManagementViewPresenter, stageController);
		conflictButtonsPanel = new ConflictButtonsPanel(stageController);
		
		// adds the unstaged and the staged panels to a split pane
		JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
		splitPane.add(unstagedChangesPanel);
		splitPane.add(stagedChangesPanel);
		splitPane.add(commitPanel);
		splitPane.setDividerSize(10);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);

		// adds the panels to the staging panel using gird bag constraints
		GridBagConstraints gbc = new GridBagConstraints();
		if (toolbarPanel != null) {
		  addToolbatPanel(gbc);
		}
		addWorkingCopySelectionPanel(gbc);
		addRebasePanel(gbc);
		addSplitPanel(gbc, splitPane);

		// creates the actual GUI for each panel
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();

		registerSubject(pushPullController);
		registerSubject(commitPanel);

		addRefreshF5();
		
		// Listens on the save event in the Oxygen editor and updates the unstaged resources area
		pluginWS.addEditorChangeListener(
		    new WSEditorChangeListener() {
		      @Override
		      public void editorOpened(final URL editorLocation) {
		        addEditorSaveHook(editorLocation);
		      }
		    },
		    PluginWorkspace.MAIN_EDITING_AREA);

		// Detect focus transitions between the view and the outside.
		installFocusListener(this, createFocusListener());
	}

	/**
	 * @return The focus listener.
	 */
  private FocusAdapter createFocusListener() {
    return new FocusAdapter() {
			boolean inTheView = false;

			@Override
			public void focusGained(final FocusEvent e) {
				focusGained = true;
				// The focus is somewhere in the view.
				if (!inTheView) {
				  // EXM-40880: Invoke later so that the focus event gets processed.
				  SwingUtilities.invokeLater(() -> refreshSupport.call());
				}
				inTheView = true;
			}

			@Override
			public void focusLost(FocusEvent e) {
				focusGained = false;
				
				// The focus might still be somewhere in the view.
				if(e.getOppositeComponent() != null){
					Window windowAncestor = SwingUtilities.getWindowAncestor(e.getOppositeComponent());
					if (windowAncestor != null) {
					  boolean contains = windowAncestor.toString().contains("MainFrame");
					  if(contains && !SwingUtilities.isDescendingFrom(e.getOppositeComponent(), StagingPanel.this)){
					    inTheView = false;
					  } else {
					    inTheView = true;
					  }
					}
				} else {
					inTheView = true;
				}
			}
		};
  }
	
  /**
   * Adds a hook to refresh the models if the editor is part of the Git working copy.
   * 
   * @param gitAccess Git access.
   * @param editorLocation Editor to check.
   */
  private void addEditorSaveHook(final URL editorLocation) {
    WSEditor editorAccess = pluginWS.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
    if (editorAccess != null) {
      editorAccess.addEditorListener(new WSEditorListener() {
        @Override
        public void editorSaved(int operationType) {
          GitOperationScheduler.getInstance().schedule(() -> treatEditorSavedEvent(editorLocation));
        }
      });
    }
  }
	
  /**
   * Treat editor saved event.
   * 
   * @param editorLocation Editor URL.
   */
  private void treatEditorSavedEvent(final URL editorLocation) {
    File locateFile = null;
    if ("file".equals(editorLocation.getProtocol())) {
      locateFile = pluginWS.getUtilAccess().locateFile(editorLocation);
      if (locateFile != null) {
        String fileInWorkPath = locateFile.toString();
        fileInWorkPath = FileHelper.rewriteSeparator(fileInWorkPath);

        try {
          String selectedRepositoryPath = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
          selectedRepositoryPath = FileHelper.rewriteSeparator(selectedRepositoryPath);

          if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Notify " + fileInWorkPath);
              logger.debug("WC " + selectedRepositoryPath);
            }

            Collection<String> affectedFiles = Arrays.asList(fileInWorkPath.substring(selectedRepositoryPath.length () + 1));
            FileGitEventInfo changeEvent = new FileGitEventInfo(GitOperation.UNSTAGE, affectedFiles);
            SwingUtilities.invokeLater(() -> unstagedChangesPanel.fileStatesChanged(changeEvent));
          }
        } catch (NoRepositorySelected e) {
          logger.debug(e, e);
        }
      }
    }
  }

	/**
	 * Add rebase panel.
	 * 
	 * @param gbc Constraints.
	 */
  private void addRebasePanel(GridBagConstraints gbc) {
    gbc.gridx = 0;
		gbc.gridy ++;
		gbc.insets = new Insets(10, 2, 10, 0);
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 1;
    gbc.weighty = 0;
		add(conflictButtonsPanel, gbc);
  }

	/**
	 * Adds the refresh call on the F5 keyboard button
	 */
	private void addRefreshF5() {
		Action action = new AbstractAction() {

			@Override
      public void actionPerformed(ActionEvent e) {
				refreshSupport.call();
			}
		};
		this.getActionMap().put("Refresh", action);
		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "Refresh");
	}

	/**
	 * Adds a focus listener on the component and its descendents.
	 * 
	 * @param c
	 *          The component.
	 * @param focusListener
	 *          Focus Listener.
	 */
	private void installFocusListener(Component c, FocusListener focusListener) {
		c.addFocusListener(focusListener);

		if (c instanceof Container) {
			Container container = (Container) c;
			int componentCount = container.getComponentCount();
			for (int i = 0; i < componentCount; i++) {
				Component child = container.getComponent(i);
				installFocusListener(child, focusListener);
			}
		}
	}

	/**
	 * Adds the given split pane to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 * @param splitPane
	 *          - the splitPane to add
	 */
	private void addSplitPanel(GridBagConstraints gbc, Component splitPane) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(splitPane, gbc);
	}

	/**
	 * Adds the tool bar to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addToolbatPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(toolbarPanel, gbc);
	}

	/**
	 * Adds the working copy area to the panel
	 * 
	 * @param gbc-
	 *          the constraints used for this component
	 */
	private void addWorkingCopySelectionPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(workingCopySelectionPanel, gbc);
	}

	/**
	 * State changed. React.
	 */
	@Override
	public void stateChanged(PushPullEvent pushPullEvent) {
	  SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
	      ActionStatus pushPullActionStatus = pushPullEvent.getActionStatus();
	      switch (pushPullActionStatus) {
	        case STARTED:
	          treatPushPullStarted(pushPullEvent);
	          break;
	        case FINISHED:
	          treatPushPullFinished(pushPullEvent);
	          break;
	        case UPDATE_COUNT:
	          if (toolbarPanel != null) {
	            toolbarPanel.refresh();
	          }
	          break;
	        case PULL_MERGE_CONFLICT_GENERATED:
	        case PULL_REBASE_CONFLICT_GENERATED:
	          conflictButtonsPanel.setVisible(true);
	          break;
	        default:
	          break;
	      }
	    }

	    /**
	     * Push/pull finished. Treat the event.
	     * 
	     * @param pushPullEventThe event.
	     */
	    private void treatPushPullFinished(PushPullEvent pushPullEvent) {
	      commitPanel.setStatusMessage(pushPullEvent.getMessage());
	      commitPanel.reset();
	      commitPanel.toggleCommitButtonAndUpdateMessageArea(false);
	      workingCopySelectionPanel.getBrowseButton().setEnabled(true);
	      workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(true);

	      // Update models.
	      GitStatus status = GitAccess.getInstance().getStatus();
	      unstagedChangesPanel.update(status.getUnstagedFiles());
	      stagedChangesPanel.update(status.getStagedFiles());

	      if (toolbarPanel != null) {
	        toolbarPanel.updateButtonState(true);
	        toolbarPanel.refresh();
	      }
	    }

	    /**
	     * Push/pull started. Treat the event.
	     * 
	     * @param pushPullEventThe event.
	     */
	    private void treatPushPullStarted(PushPullEvent pushPullEvent) {
	      commitPanel.setStatusMessage(pushPullEvent.getMessage());
	      commitPanel.reset();
	      workingCopySelectionPanel.getBrowseButton().setEnabled(false);
	      workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(false);

	      if (toolbarPanel != null) {
	        toolbarPanel.updateButtonState(false);
	      }

	      commitPanel.getCommitButton().setEnabled(false);
	    }
	  });
	}

	/**
	 * Register a subject for the observer pattern.
	 * 
	 * @param subject the subject to register.
	 */
	public void registerSubject(Subject<PushPullEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	/**
	 * @return <code>true</code> if panel has focus.
	 */
	@Override
  public boolean hasFocus() {
		return focusGained;
	}
	
	/**
   * @return the stageController
   */
  public GitController getStageController() {
    return stageController;
  }
  
  /**
   * @return The controller for Push/Pull events.
   */
  public PushPullController getPushPullController() {
    return pushPullController;
  }

  /**
   * Update rebase panel visibility based on repo state.
   */
  public void updateConflictButtonsPanelBasedOnRepoState() {
    conflictButtonsPanel.updateBasedOnRepoState();
  }
  

  /**
   * @return the unstaged resources panel.
   */
  public ChangesPanel getUnstagedChangesPanel() {
    return unstagedChangesPanel;
  }

  /**
   * @return The staged resources panel.
   */
  public ChangesPanel getStagedChangesPanel() {
    return stagedChangesPanel;
  }

  /**
   * @return The commit panel.
   */
  public CommitAndStatusPanel getCommitPanel() {
    return commitPanel;
  }

  /**
   * @return  The tool bar panel used for the push and pull
   */
  public ToolbarPanel getToolbarPanel() {
    return toolbarPanel;
  }
  
  void setToolbarPanelFromTests(ToolbarPanel toolbarPanel) {
    this.toolbarPanel = toolbarPanel;
  }
  
  public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
    return workingCopySelectionPanel;
  }
  
  /**
   * !!!!!!! FOR TESTS !!!!!!
   * 
   * @return The conflict buttons panel.
   */
  public ConflictButtonsPanel getConflictButtonsPanel() {
    return conflictButtonsPanel;
  }
  
  /**
   * Load working copies.
   */
  public void loadWorkingCopies() {
    workingCopySelectionPanel.loadEntries();
  }
}
