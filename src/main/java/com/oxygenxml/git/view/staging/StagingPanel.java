package com.oxygenxml.git.view.staging;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;


/**
 * Main panel containing all the other panels. It also creates them
 * 
 * @author Beniamin Savu
 *
 */
public class StagingPanel extends JPanel {

	/**
	 * Divider size.
	 */
	private static final int DIVIDER_SIZE = 10;

	/**
	 * Left and right component inset.
	 */
	private static final int HORIZONTAL_INSET = 5;

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(StagingPanel.class);

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
	 * Main panel refresh
	 */
	private final GitRefreshSupport refreshSupport;

	/**
	 * Git controller.
	 */
	private final GitController gitController;

	/**
	 * The git actions manager.
	 */
	private final GitActionsManager gitActionsManager;

	/**
	 * The branch selection panel.
	 */
	private BranchSelectionCombo branchSelectionCombo;

	/**
	 * Constructor.
	 * 
	 * @param refreshSupport   Refresh support.        
	 * @param gitCtrl  Git controller.
	 * @param historyController History related interaction.
	 */
	public StagingPanel(
			GitRefreshSupport refreshSupport, 
			GitController gitCtrl, 
			HistoryController historyController,
			GitActionsManager gitActionsManager) {
		this.refreshSupport = refreshSupport;
		this.gitController = gitCtrl;

		this.gitActionsManager = gitActionsManager; 

		createGUI(historyController);

		gitCtrl.addGitListener(new GitEventListener() {
			@Override
			public void operationSuccessfullyEnded(GitEventInfo info) {
				if (info.getGitOperation() == GitOperation.PULL || info.getGitOperation() == GitOperation.PUSH) {
					handlePushPullEvent((PushPullEvent) info, false);
				}
			}
			@Override
			public void operationFailed(GitEventInfo info, Throwable t) {
				if (info.getGitOperation() == GitOperation.PULL || info.getGitOperation() == GitOperation.PUSH) {
					handlePushPullEvent((PushPullEvent) info, false);
				}
			}

			@Override
			public void operationAboutToStart(GitEventInfo info) {
				if (info.getGitOperation() == GitOperation.PULL || info.getGitOperation() == GitOperation.PUSH) {
					handlePushPullEvent((PushPullEvent) info, true);
				}
			}
		});
	}

	/**
	 * Create toolbar. <br><br>
	 * 
	 * Not created from 99% of the test cases.
	 * 
	 * @param historyController History controller.
	 * @param branchManagementViewPresenter Branch management interface.
	 * 
	 * @return the toolbar.
	 */
	protected ToolbarPanel createToolbar(GitActionsManager gitActionsManager) {
		return new ToolbarPanel(gitController, gitActionsManager);
	}

	/**
	 * Create the GUI.
	 * 
	 * @param historyController History related interaction.
	 */
	private void createGUI(HistoryController historyController) {
		this.setLayout(new GridBagLayout());

		// Creates the panels objects that will be in the staging panel
		unstagedChangesPanel = new ChangesPanel(gitController, historyController, false);
		stagedChangesPanel = new ChangesPanel(gitController, historyController, true);
		workingCopySelectionPanel = new WorkingCopySelectionPanel(gitController, false);
		branchSelectionCombo = new BranchSelectionCombo(gitController);
		commitPanel = new CommitAndStatusPanel(gitController);
		toolbarPanel = createToolbar(gitActionsManager);
		conflictButtonsPanel = new ConflictButtonsPanel(gitController);

		// adds the unstaged and the staged panels to a split pane
		JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
		splitPane.add(unstagedChangesPanel);
		splitPane.add(stagedChangesPanel);
		splitPane.add(commitPanel);
		splitPane.setDividerSize(DIVIDER_SIZE);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);

		// adds the panels to the staging panel using gird bag constraints
		GridBagConstraints gbc = new GridBagConstraints();
		if (toolbarPanel != null) {
			addToolbarPanel(gbc);
		}
		addWorkingCopySelectionPanel(gbc);
		addBranchesCombo(gbc);
		addConflictButtonsPanel(gbc);
		addSplitPanel(gbc, splitPane);

		// creates the actual GUI for each panel
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();

		addRefreshF5();

		// Listens on the save event in the Oxygen editor and updates the unstaged resources area
		PluginWorkspaceProvider.getPluginWorkspace().addEditorChangeListener(
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
				if (!e.isTemporary()) {
					focusGained = true;
					if (!inTheView) {
						// EXM-40880: Invoke later so that the focus event gets processed.
						SwingUtilities.invokeLater(refreshSupport::call);
					}
					inTheView = true;
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					focusGained = false;
					// The focus might still be somewhere in the view.
					Component opposite = e.getOppositeComponent();
					if (opposite != null) {
						Window windowAncestor = SwingUtilities.getWindowAncestor(opposite);
						if (windowAncestor != null) {
							boolean contains = windowAncestor.toString().contains("MainFrame");
							inTheView = !contains || SwingUtilities.isDescendingFrom(opposite, StagingPanel.this);
						}
					} else {
						inTheView = true;
					}
				}
			}
		};
	}

	/**
	 * Add branches combo.
	 * 
	 * @param gbc Grid bag constraints.
	 */
	private void addBranchesCombo(GridBagConstraints gbc) {
	  gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING + HORIZONTAL_INSET,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridwidth = 1;
    this.add(new JLabel(Translator.getInstance().getTranslation(Tags.BRANCH) + ":"), gbc);

    gbc.insets = new Insets(0, UIConstants.COMPONENT_LEFT_PADDING, 0, HORIZONTAL_INSET);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    gbc.weightx = 1;
    gbc.weighty = 0;
    this.add(branchSelectionCombo, gbc);
    
  }

	/**
	 * Adds a hook to refresh the models if the editor is part of the Git working copy.
	 * 
	 * @param editorLocation Editor to check.
	 */
	private void addEditorSaveHook(final URL editorLocation) {
		WSEditor editorAccess = PluginWorkspaceProvider.getPluginWorkspace().getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
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
			locateFile = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().locateFile(editorLocation);
			if (locateFile != null) {
				String fileInWorkPath = locateFile.toString();
				fileInWorkPath = FileUtil.rewriteSeparator(fileInWorkPath);

				try {
					String selectedRepositoryPath = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
					selectedRepositoryPath = FileUtil.rewriteSeparator(selectedRepositoryPath);

					if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
					  if(gitActionsManager != null) {
              gitActionsManager.refreshActionsStates();
              
            }
					  SwingUtilities.invokeLater(() -> updateToolbarsButtonsStates());
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Notify " + fileInWorkPath);
							LOGGER.debug("WC " + selectedRepositoryPath);
						}

						Collection<String> affectedFiles = Collections.singletonList(fileInWorkPath.substring(selectedRepositoryPath.length() + 1));
						FileGitEventInfo changeEvent = new FileGitEventInfo(GitOperation.UNSTAGE, affectedFiles);
						SwingUtilities.invokeLater(() -> unstagedChangesPanel.fileStatesChanged(changeEvent));
						
					}
				} catch (NoRepositorySelected e) {
					LOGGER.debug(e, e);
				}
			}
		}
	}

	/**
	 * Add rebase panel.
	 * 
	 * @param gbc Constraints.
	 */
	private void addConflictButtonsPanel(GridBagConstraints gbc) {
		gbc.gridx = 0;
		gbc.gridy ++;
		gbc.insets = new Insets(10, 2, 10, 0); // NOSONAR
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
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
		gbc.insets = new Insets(0, HORIZONTAL_INSET, 0, HORIZONTAL_INSET);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		this.add(splitPane, gbc);
	}

	/**
	 * Adds the tool bar to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addToolbarPanel(GridBagConstraints gbc) {
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(0, HORIZONTAL_INSET, 0, HORIZONTAL_INSET);
		this.add(toolbarPanel, gbc);
	}

	/**
	 * Adds the working copy area to the panel
	 * 
	 * @param gbc The constraints used for this component
	 */
	private void addWorkingCopySelectionPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(
				UIConstants.COMPONENT_TOP_PADDING,
				UIConstants.COMPONENT_LEFT_PADDING + HORIZONTAL_INSET,
				UIConstants.COMPONENT_BOTTOM_PADDING,
				UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		this.add(new JLabel(Translator.getInstance().getTranslation(Tags.WORKING_COPY_LABEL)), gbc);

		gbc.insets = new Insets(0, 0, 0, HORIZONTAL_INSET);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx++;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(workingCopySelectionPanel, gbc);
	}

	/**
	 * State changed. React.
	 * 
	 * @param pushPullEvent Change event.
	 * @param started <code>true</code> if the task just started. <code>false</code> if it ended.
	 */
	public void handlePushPullEvent(PushPullEvent pushPullEvent, boolean started) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (started) {
					treatPushPullStarted(pushPullEvent);
				} else {
					treatPushPullFinished(pushPullEvent);
					if (pushPullEvent.hasConficts()) {
						conflictButtonsPanel.setVisible(true);
					}
				}
			}

			/**
			 * Push/pull finished. Treat the event.
			 * 
			 * @param pushPullEvent The event.
			 */
			private void treatPushPullFinished(PushPullEvent pushPullEvent) {
				commitPanel.setStatusMessage(pushPullEvent.getMessage());
				commitPanel.reset();
				commitPanel.toggleCommitButtonAndUpdateMessageArea(false);
				workingCopySelectionPanel.getBrowseButton().setEnabled(true);
				workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(true);
				branchSelectionCombo.setEnabled(true);

				// Update models.
				GitStatus status = GitAccess.getInstance().getStatus();
				unstagedChangesPanel.update(status.getUnstagedFiles());
				stagedChangesPanel.update(status.getStagedFiles());
				
				branchSelectionCombo.refresh();

				gitActionsManager.refreshActionsStates();
			}

			/**
			 * Push/pull started. Treat the event.
			 * 
			 * @param pushPullEvent The event.
			 */
			private void treatPushPullStarted(PushPullEvent pushPullEvent) {
				commitPanel.setStatusMessage(pushPullEvent.getMessage());
				commitPanel.reset();
				workingCopySelectionPanel.getBrowseButton().setEnabled(false);
				workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(false);
				branchSelectionCombo.setEnabled(false);

				if (toolbarPanel != null) {
					toolbarPanel.setButtonsEnabledState(false);
				}

				commitPanel.getCommitButton().setEnabled(false);
			}
		});
	}

	/**
	 * @return <code>true</code> if panel has focus.
	 */
	@Override
	public boolean hasFocus() {
		return focusGained;
	}

	/**
	 * @return the Git controller.
	 */
	public GitControllerBase getGitController() {
		return gitController;
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

	/**
	 * @param toolbarPanel The new toolbar panel.
	 */
	void setToolbarPanelFromTests(ToolbarPanel toolbarPanel) {
		this.toolbarPanel = toolbarPanel;
	}

	/**
	 * @return The WC Selection Panel.
	 */
	public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
		return workingCopySelectionPanel;
	}
	
	/**
	 * Update states for toolar buttons.
	 */
	public void updateToolbarsButtonsStates() {
	  // null from tests
    Optional.ofNullable(toolbarPanel).ifPresent(ToolbarPanel::updateButtonsStates);
	}

	/**
	 * @return The branches combo.
	 */
	public BranchSelectionCombo getBranchesCombo() {
	  return branchSelectionCombo;
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
	 * @return The manager responsible with git action.
	 */
	public GitActionsManager getGitActionsManager() {
    return gitActionsManager;
  }

}
