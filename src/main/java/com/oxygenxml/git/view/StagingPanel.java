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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.Subject;

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
   * No repository.
   */
  private static final String NO_REPOSITORY = "[No repository]";

  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(StagingPanel.class);

	boolean gained = false;
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
	 * The commit panel
	 */
	private CommitPanel commitPanel;

	/**
	 * List of listeners for this panel
	 */
	private List<Subject<PushPullEvent>> subjects = new ArrayList<Subject<PushPullEvent>>();

	/**
	 * Main panel refresh
	 */
	private GitRefreshSupport refreshSupport;

	private StageController stageController;
	/**
	 * Manages Push/Pull actions.
	 */
  private PushPullController pushPullController;

  public StagingPanel(
      GitRefreshSupport refresh, 
      StageController stageController) {
		this.refreshSupport = refresh;
		this.stageController = stageController;
		
		createGUI();
		
		GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        GitAccess gitAccess = GitAccess.getInstance();
        Repository repository;
        try {
          repository = gitAccess.getRepository();
          if (repository != null) {
            
            String rootFolder = NO_REPOSITORY;
            try {
              rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
            } catch (NoRepositorySelected e) {
              // Never happens.
              logger.error(e, e);
            }
            
            List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
            List<FileStatus> stagedFiles = gitAccess.getStagedFile();

            // generate content for FLAT_VIEW
            getUnstagedChangesPanel().updateFlatView(unstagedFiles);
            getStagedChangesPanel().updateFlatView(stagedFiles);

            // generate content for TREE_VIEW
            getUnstagedChangesPanel().createTreeView(rootFolder, unstagedFiles);
            getStagedChangesPanel().createTreeView(rootFolder, stagedFiles);

            // whan a new working copy is selected clear the commit text area
            getCommitPanel().clearCommitMessage();

            // checks what buttons to keep active and what buttons to deactivate
            if (!gitAccess.getStagedFile().isEmpty()) {
              getCommitPanel().getCommitButton().setEnabled(true);
            } else {
              getCommitPanel().getCommitButton().setEnabled(false);
            }
            getUnstagedChangesPanel().getChangeSelectedButton().setEnabled(false);
            getStagedChangesPanel().getChangeSelectedButton().setEnabled(false);
          }
        } catch (NoRepositorySelected e) {
          logger.debug(e, e);
          
          // clear content from FLAT_VIEW
          getUnstagedChangesPanel().updateFlatView(new ArrayList<FileStatus>());
          getStagedChangesPanel().updateFlatView(new ArrayList<FileStatus>());

          // clear content from TREE_VIEW
          getUnstagedChangesPanel().createTreeView("", new ArrayList<FileStatus>());
          getStagedChangesPanel().createTreeView("", new ArrayList<FileStatus>());
        }
      }
      
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        commitPanel.stateChanged(changeEvent);
      }
    });
	}

	public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
		return workingCopySelectionPanel;
	}

	public ChangesPanel getUnstagedChangesPanel() {
		return unstagedChangesPanel;
	}

	public ChangesPanel getStagedChangesPanel() {
		return stagedChangesPanel;
	}

	public CommitPanel getCommitPanel() {
		return commitPanel;
	}

	public ToolbarPanel getToolbarPanel() {
		return toolbarPanel;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		final GitAccess gitAccess = GitAccess.getInstance();
		pushPullController = createPushPullController();

		// Creates the panels objects that will be in the staging panel
		unstagedChangesPanel = new ChangesPanel(stageController, false);
		stagedChangesPanel = new ChangesPanel(stageController, true);
		workingCopySelectionPanel = new WorkingCopySelectionPanel();
		commitPanel = new CommitPanel();
		toolbarPanel = new ToolbarPanel(pushPullController, refreshSupport);
		// adds the unstaged and the staged panels to a split pane
		JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
		splitPane.add(unstagedChangesPanel);
		splitPane.add(stagedChangesPanel);
		splitPane.add(commitPanel);
		splitPane.setDividerSize(5);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);

		// adds the panels to the staging panel using gird bag constraints
		GridBagConstraints gbc = new GridBagConstraints();
		addToolbatPanel(gbc);
		addWorkingCopySelectionPanel(gbc);
		addSplitPanel(gbc, splitPane);

		// creates the actual GUI for each panel
		commitPanel.createGUI();
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();

		registerSubject(pushPullController);
		registerSubject(commitPanel);

		addRefreshF5();
		
		// Listens on the save event in the Oxygen editor and updates the unstaging
		// area
		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.addEditorChangeListener(new WSEditorChangeListener() {
					@Override
					public void editorOpened(final URL editorLocation) {
						addEditorSaveHook(gitAccess, editorLocation);
					}

					/**
					 * Adds a hook to refresh the models if the editor is part of the Git working copy.
					 * 
					 * @param gitAccess Git access.
					 * @param editorLocation Editor to check.
					 */
          private void addEditorSaveHook(final GitAccess gitAccess, final URL editorLocation) {
            WSEditor editorAccess = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
						editorAccess.addEditorListener(new WSEditorListener() {
							@Override
							public void editorSaved(int operationType) {
							  File locateFile = null;
							  if ("file".equals(editorLocation.getProtocol())) {
							    locateFile = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().locateFile(editorLocation);
							    if (locateFile != null) {
							      String fileInWorkPath = locateFile.toString();
							      fileInWorkPath = FileHelper.rewriteSeparator(fileInWorkPath);

							      try {
							        String selectedRepositoryPath = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
							        selectedRepositoryPath = FileHelper.rewriteSeparator(selectedRepositoryPath);

							        if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
							          // TODO Sorin Do not recreate the models from scratch. Just fire an atomic 
							          // event, like fireTableRowsUpdated()
							          // TODO Sorin It makes sense to schedule this on the PanelRefresh, to avoid threading issues.
							          List<FileStatus> newFiles = gitAccess.getUnstagedFiles();
							          unstagedChangesPanel.updateFlatView(newFiles);
							          
							          String rootFolder = NO_REPOSITORY;
							          try {
							            rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
							          } catch (NoRepositorySelected e) {
							            // Never happens.
							            logger.error(e, e);
							          }
							          
							          unstagedChangesPanel.createTreeView(rootFolder, newFiles);
							        }
							      } catch (NoRepositorySelected e) {
							        logger.error(e, e);
							      }
							    }
							  }
							}
						});
          }
				}, PluginWorkspace.MAIN_EDITING_AREA);

		// Detect focus transitions between the view and the outside.
		installFocusListener(this, new FocusAdapter() {
			boolean inTheView = false;

			@Override
			public void focusGained(final FocusEvent e) {
				gained = true;
				// The focus is somewhere in he view.
				if (!inTheView) {
					refreshSupport.call();
				}

				inTheView = true;
			}

			@Override
			public void focusLost(FocusEvent e) {
				gained = false;
				
				// The focus might still be somewhere in the view.
				if(e.getOppositeComponent() != null){
					Window windowAncestor = SwingUtilities.getWindowAncestor(e.getOppositeComponent());
					boolean contains = windowAncestor.toString().contains("MainFrame");
					if(contains && !SwingUtilities.isDescendingFrom(e.getOppositeComponent(), StagingPanel.this)){
						inTheView = false;
					} else {
						inTheView = true;
					}
				} else {
					inTheView = true;
				}
			}
		});
	}

  protected PushPullController createPushPullController() {
    return new PushPullController();
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
		gbc.gridy = 2;
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
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(workingCopySelectionPanel, gbc);
	}

	@Override
  public void stateChanged(PushPullEvent pushPullEvent) {
		if (pushPullEvent.getActionStatus() == ActionStatus.STARTED) {
			commitPanel.setStatus(pushPullEvent.getMessage());
			commitPanel.clearCommitMessage();
			workingCopySelectionPanel.getBrowseButton().setEnabled(false);
			workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(false);
			toolbarPanel.getPushButton().setEnabled(false);
			toolbarPanel.getPullButton().setEnabled(false);
			toolbarPanel.getCloneRepositoryButton().setEnabled(false);
			commitPanel.getCommitButton().setEnabled(false);
		} else if (pushPullEvent.getActionStatus() == ActionStatus.FINISHED) {
			commitPanel.setStatus(pushPullEvent.getMessage());
			commitPanel.clearCommitMessage();
			if (!GitAccess.getInstance().getStagedFile().isEmpty()) {
				commitPanel.getCommitButton().setEnabled(true);
			}
			workingCopySelectionPanel.getBrowseButton().setEnabled(true);
			workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(true);
			toolbarPanel.getPushButton().setEnabled(true);
			toolbarPanel.getPullButton().setEnabled(true);
			toolbarPanel.getCloneRepositoryButton().setEnabled(true);
			unstagedChangesPanel.updateFlatView(GitAccess.getInstance().getUnstagedFiles());
			String rootFolder = NO_REPOSITORY;
      try {
        rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
      } catch (NoRepositorySelected e) {
        // Never happens.
        logger.error(e, e);
      }
      unstagedChangesPanel.createTreeView(rootFolder,
      		GitAccess.getInstance().getUnstagedFiles());
			toolbarPanel.setPushesAhead(GitAccess.getInstance().getPushesAhead());
			toolbarPanel.setPullsBehind(GitAccess.getInstance().getPullsBehind());
			toolbarPanel.updateInformationLabel();
		} else if (pushPullEvent.getActionStatus() == ActionStatus.UPDATE_COUNT) {
			commitPanel.setStatus(pushPullEvent.getMessage());
			toolbarPanel.setPushesAhead(GitAccess.getInstance().getPushesAhead());
			toolbarPanel.setPullsBehind(GitAccess.getInstance().getPullsBehind());
			toolbarPanel.updateInformationLabel();
		}
	}

	public void registerSubject(Subject<PushPullEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	public boolean isInFocus() {
		return gained;
	}
	
	/**
   * @return the stageController
   */
  public StageController getStageController() {
    return stageController;
  }
  
  /**
   * @return The controller for Push/Pull events.
   */
  public PushPullController getPushPullController() {
    return pushPullController;
  }
}
