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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
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
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.event.ActionStatus;
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
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator;

	/**
	 * Main panel refresh
	 */
	private GitRefreshSupport refresh;

	private StageController stageController;

  public StagingPanel(Translator translator, GitRefreshSupport refresh, StageController stageController) {
		this.translator = translator;
		this.refresh = refresh;
		this.stageController = stageController;
		createGUI();
		
		GitAccess.getInstance().addGitListener(new GitEventListener() {
      @Override
      public void repositoryChanged() {
        GitAccess gitAccess = GitAccess.getInstance();
        Repository repository;
        try {
          repository = gitAccess.getRepository();
          if (repository != null) {
            String path = gitAccess.getWorkingCopy().getAbsolutePath();

            List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
            List<FileStatus> stagedFiles = gitAccess.getStagedFile();

            // generate content for FLAT_VIEW
            getUnstagedChangesPanel().updateFlatView(unstagedFiles);
            getStagedChangesPanel().updateFlatView(stagedFiles);

            // generate content for TREE_VIEW
            getUnstagedChangesPanel().createTreeView(path, unstagedFiles);
            getStagedChangesPanel().createTreeView(path, stagedFiles);

            // whan a new working copy is selected clear the commit text area
            getCommitPanel().clearCommitMessage();

            // checks what buttons to keep active and what buttons to deactivate
            if (!gitAccess.getStagedFile().isEmpty()) {
              getCommitPanel().getCommitButton().setEnabled(true);
            } else {
              getCommitPanel().getCommitButton().setEnabled(false);
            }
            getUnstagedChangesPanel().getStageSelectedButton().setEnabled(false);
            getStagedChangesPanel().getStageSelectedButton().setEnabled(false);
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
		PushPullController pushPullController = new PushPullController(gitAccess, translator);

		// Creates the panels objects that will be in the staging panel
		unstagedChangesPanel = new ChangesPanel(gitAccess, stageController, false, translator);
		stagedChangesPanel = new ChangesPanel(gitAccess, stageController, true, translator);
		workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess, translator);
		commitPanel = new CommitPanel(gitAccess, stageController, translator);
		toolbarPanel = new ToolbarPanel(pushPullController, translator, refresh);
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
						WSEditor editorAccess = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
						editorAccess.addEditorListener(new WSEditorListener() {
							@Override
							public void editorSaved(int operationType) {
								String fileInWorkPath = editorLocation.getFile().substring(1);
								try {
									fileInWorkPath = URLDecoder.decode(fileInWorkPath, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									if (logger.isDebugEnabled()) {
									  logger.debug(e, e);
									}
								}
								String selectedRepositoryPath = OptionsManager.getInstance().getSelectedRepository();
								selectedRepositoryPath = selectedRepositoryPath.replace("\\", "/");
								if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
									List<FileStatus> newFiles = gitAccess.getUnstagedFiles();
									unstagedChangesPanel.updateFlatView(newFiles);
									unstagedChangesPanel.createTreeView(OptionsManager.getInstance().getSelectedRepository(), newFiles);
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
					refresh.call();
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

	/**
	 * Adds the refresh call on the F5 keyboard button
	 */
	private void addRefreshF5() {
		Action action = new AbstractAction() {

			@Override
      public void actionPerformed(ActionEvent e) {
				refresh.call();
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
			workingCopySelectionPanel.getWorkingCopySelector().setEnabled(false);
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
			workingCopySelectionPanel.getWorkingCopySelector().setEnabled(true);
			toolbarPanel.getPushButton().setEnabled(true);
			toolbarPanel.getPullButton().setEnabled(true);
			toolbarPanel.getCloneRepositoryButton().setEnabled(true);
			unstagedChangesPanel.updateFlatView(GitAccess.getInstance().getUnstagedFiles());
			unstagedChangesPanel.createTreeView(OptionsManager.getInstance().getSelectedRepository(),
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
}
