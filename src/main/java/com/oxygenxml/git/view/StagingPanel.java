package com.oxygenxml.git.view;


import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.Subject;

/**
 * Main panel containing all the other panels.
 * 
 * @author intern2
 *
 */
public class StagingPanel extends JPanel implements Observer<PushPullEvent> {

	private ToolbarPanel toolbarPanel;
	private WorkingCopySelectionPanel workingCopySelectionPanel;
	private UnstagedChangesPanel unstagedChangesPanel;
	private UnstagedChangesPanel stagedChangesPanel;
	private CommitPanel commitPanel;
	private List<Subject<PushPullEvent>> subjects = new ArrayList<Subject<PushPullEvent>>();

	public StagingPanel() {
		createGUI();
	}

	public UnstagedChangesPanel getUnstagedChangesPanel() {
		return unstagedChangesPanel;
	}

	public UnstagedChangesPanel getStagedChangesPanel() {
		return stagedChangesPanel;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		final GitAccess gitAccess = GitAccess.getInstance();
		StageController observer = new StageController(gitAccess);
		PushPullController pushPullController = new PushPullController(gitAccess);

		unstagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, false);
		stagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, true);
		JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
		splitPane.add(unstagedChangesPanel);
		splitPane.add(stagedChangesPanel);
		splitPane.setDividerSize(5);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(true);
		splitPane.setBorder(null);

		workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess);
		commitPanel = new CommitPanel(gitAccess, observer);
		toolbarPanel = new ToolbarPanel(pushPullController);

		GridBagConstraints gbc = new GridBagConstraints();

		addToolbatPanel(gbc);
		addWorkingCopySelectionPanel(gbc);
		addSplitPanel(gbc, splitPane);
		addCommitPanel(gbc);

		toolbarPanel.createGUI();
		commitPanel.createGUI();
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();
		workingCopySelectionPanel.createGUI();

		registerSubject(pushPullController);
		
		// Detect focus transitions between the view and the outside.
    installFocusListener(this, new FocusAdapter() {
      boolean inTheView = false;
      @Override
      public void focusGained(FocusEvent e) {
        // The focus is somewhere in he view.
        if (!inTheView) {
          // TODO The focus was lost but now is back.
          unstagedChangesPanel.updateFlatView(gitAccess.getUnstagedFiles());
          stagedChangesPanel.updateFlatView(gitAccess.getStagedFile());
        }
        inTheView = true;
      }
      
      @Override
      public void focusLost(FocusEvent e) {
        // The focus might still be somewhere in the view.
        if (e.getOppositeComponent() != null) {
          inTheView = SwingUtilities.isDescendingFrom((Component) e.getOppositeComponent(), StagingPanel.this);
        } else {
          inTheView = false;
        }
      }
    });
	}

	/**
	 * Adds a focus listener on the component and its descendents.
	 * 
	 * @param c The component.
	 * @param focusListener Focus Listener.
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

	private void addCommitPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(commitPanel, gbc);
	}

	public void stateChanged(PushPullEvent pushPullEvent) {
		if (pushPullEvent.getActionStatus() == ActionStatus.STARTED) {
			workingCopySelectionPanel.getBrowseButton().setEnabled(false);
			workingCopySelectionPanel.getWorkingCopySelector().setEnabled(false);
			toolbarPanel.getPushButton().setEnabled(false);
			toolbarPanel.getPullButton().setEnabled(false);
			commitPanel.getCommitButton().setEnabled(false);
		} else {
			workingCopySelectionPanel.getBrowseButton().setEnabled(true);
			workingCopySelectionPanel.getWorkingCopySelector().setEnabled(true);
			toolbarPanel.getPushButton().setEnabled(true);
			toolbarPanel.getPullButton().setEnabled(true);
			commitPanel.getCommitButton().setEnabled(true);
			unstagedChangesPanel.updateFlatView(GitAccess.getInstance().getUnstagedFiles());
			unstagedChangesPanel.createTreeView(OptionsManager.getInstance().getSelectedRepository(), GitAccess.getInstance().getUnstagedFiles());
		}
	}

	public void registerSubject(Subject<PushPullEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	public void unregisterSubject(Subject<PushPullEvent> subject) {
		subjects.remove(subject);

		subject.removeObserver(this);
	}

}
