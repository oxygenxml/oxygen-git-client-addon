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
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommand;
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
  public static final String NO_REPOSITORY = "[No repository]";

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
	 * The commit panel
	 */
	private CommitPanel commitPanel;

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
	private StageController stageController;
	
	/**
	 * Manages Push/Pull actions.
	 */
  private PushPullController pushPullController;

  /**
   * Constructor.
   * 
   * @param refreshSupport   Refresh support.        
   * @param stageController  Staging controller.
   */
  public StagingPanel(
      GitRefreshSupport refreshSupport, 
      StageController stageController) {
		this.refreshSupport = refreshSupport;
		this.stageController = stageController;
		
		createGUI();
	}

  /**
   * @return the working copy selection panel.
   */
	public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
		return workingCopySelectionPanel;
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
	public CommitPanel getCommitPanel() {
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

	/**
	 * Create the GUI.
	 */
	public void createGUI() {
		this.setLayout(new GridBagLayout());

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
		splitPane.setDividerSize(15);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);

		// adds the panels to the staging panel using gird bag constraints
		GridBagConstraints gbc = new GridBagConstraints();
		addToolbatPanel(gbc);
		addWorkingCopySelectionPanel(gbc);
		addSplitPanel(gbc, splitPane);

		// creates the actual GUI for each panel
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
						addEditorSaveHook(editorLocation);
					}

					/**
					 * Adds a hook to refresh the models if the editor is part of the Git working copy.
					 * 
					 * @param gitAccess Git access.
					 * @param editorLocation Editor to check.
					 */
          private void addEditorSaveHook(final URL editorLocation) {
            WSEditor editorAccess = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess != null) {
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
                          if (logger.isDebugEnabled()) {
                            logger.debug("Notify " + fileInWorkPath);
                            logger.debug("WC " + selectedRepositoryPath);
                          }

                          Collection<String> affectedFiles = Arrays.asList(fileInWorkPath.substring(selectedRepositoryPath.length () + 1));
                          ChangeEvent changeEvent = new ChangeEvent(GitCommand.UNSTAGE, affectedFiles);
                          unstagedChangesPanel.stateChanged(changeEvent);
                        }
                      } catch (NoRepositorySelected e) {
                        logger.debug(e, e);
                      }
                    }
                  }
                }
              });
            }
          }
				}, PluginWorkspace.MAIN_EDITING_AREA);

		// Detect focus transitions between the view and the outside.
		installFocusListener(this, new FocusAdapter() {
			boolean inTheView = false;

			@Override
			public void focusGained(final FocusEvent e) {
				focusGained = true;
				// The focus is somewhere in he view.
				if (!inTheView) {
				  // EXM-40880: Invoke later so that the focus event gets processed.
				  SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              refreshSupport.call();
            }
          });
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
		});
	}

	/**
	 * Create the push/pull controller.
	 * 
	 * @return the controller.
	 */
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
	  SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (pushPullEvent.getActionStatus() == ActionStatus.STARTED) {
          commitPanel.setStatus(pushPullEvent.getMessage());
          commitPanel.reset();
          workingCopySelectionPanel.getBrowseButton().setEnabled(false);
          workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(false);
          
          toolbarPanel.updateButtonState(false);

          commitPanel.getCommitButton().setEnabled(false);
        } else if (pushPullEvent.getActionStatus() == ActionStatus.FINISHED) {
          commitPanel.setStatus(pushPullEvent.getMessage());
          commitPanel.reset();
          if (!GitAccess.getInstance().getStagedFiles().isEmpty()) {
            commitPanel.getCommitButton().setEnabled(true);
          }
          workingCopySelectionPanel.getBrowseButton().setEnabled(true);
          workingCopySelectionPanel.getWorkingCopyCombo().setEnabled(true);
          
          // Update models.
          String rootFolder = NO_REPOSITORY;
          try {
            rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
          } catch (NoRepositorySelected e) {
            // Never happens.
            logger.error(e, e);
          }
          
          GitStatus status = GitAccess.getInstance().getStatus();
          unstagedChangesPanel.update(rootFolder, status.getUnstagedFiles());
          stagedChangesPanel.update(rootFolder, status.getStagedFiles());
          
          toolbarPanel.updateButtonState(true);
          
          toolbarPanel.updateStatus();
        } else if (pushPullEvent.getActionStatus() == ActionStatus.UPDATE_COUNT) {
          commitPanel.setStatus(pushPullEvent.getMessage());
          toolbarPanel.updateStatus();
        }
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
