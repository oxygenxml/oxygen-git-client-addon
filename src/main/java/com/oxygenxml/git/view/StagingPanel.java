package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.Subject;

/**
 * Main panel containing all the other panels.
 * 
 * @author intern2
 *
 */
public class StagingPanel extends JPanel implements Observer<PushPullEvent>{

	private ToolbarPanel toolbarPanel;
	private WorkingCopySelectionPanel workingCopySelectionPanel;
	private UnstagedChangesPanel unstagedChangesPanel;
	private UnstagedChangesPanel stagedChangesPanel;
	private CommitPanel commitPanel;
	private JSplitPane splitPane;
	private List<Subject<PushPullEvent>> subjects = new ArrayList<Subject<PushPullEvent>>();
	private List<Observer<PushPullEvent>> observers = new ArrayList<Observer<PushPullEvent>>();
	

	public StagingPanel(WorkingCopySelectionPanel workingCopySelectionPanel, UnstagedChangesPanel unstagedChangesPanel,
			UnstagedChangesPanel stagedChangesPanel, CommitPanel commitPanel, ToolbarPanel toolbarPanel) {
		this.toolbarPanel = toolbarPanel;
		this.workingCopySelectionPanel = workingCopySelectionPanel;
		this.unstagedChangesPanel = unstagedChangesPanel;
		this.stagedChangesPanel = stagedChangesPanel;
		this.commitPanel = commitPanel;
	}

	public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
		return workingCopySelectionPanel;
	}

	public void setWorkingCopySelectionPanel(WorkingCopySelectionPanel workingCopySelectionPanel) {
		this.workingCopySelectionPanel = workingCopySelectionPanel;
	}

	public UnstagedChangesPanel getUnstagedChangesPanel() {
		return unstagedChangesPanel;
	}

	public void setUnstagedChangesPanel(UnstagedChangesPanel unstagedChangesPanel) {
		this.unstagedChangesPanel = unstagedChangesPanel;
	}

	public UnstagedChangesPanel getStagedChangesPanel() {
		return stagedChangesPanel;
	}

	public void setStagedChangesPanel(UnstagedChangesPanel stagedChangesPanel) {
		this.stagedChangesPanel = stagedChangesPanel;
	}

	public CommitPanel getCommitPanel() {
		return commitPanel;
	}

	public void setCommitPanel(CommitPanel commitPanel) {
		this.commitPanel = commitPanel;
	}

	public ToolbarPanel getToolbarPanel() {
		return toolbarPanel;
	}

	public void setToolbarPanel(ToolbarPanel toolbarPanel) {
		this.toolbarPanel = toolbarPanel;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addToolbatPanel(gbc);
		addWorkingCopySelectionPanel(gbc);
		addUnstagedChangesPanel(gbc);
		addStagedChangesPanel(gbc);
		addCommitPanel(gbc);

		toolbarPanel.createGUI();
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();
		commitPanel.createGUI();
		workingCopySelectionPanel.createGUI();

		addSplitPanel(gbc);
	}

	private void addSplitPanel(GridBagConstraints gbc) {
		
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

	private void addUnstagedChangesPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(unstagedChangesPanel, gbc);

	}

	private void addStagedChangesPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(stagedChangesPanel, gbc);
	}

	private void addCommitPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(commitPanel, gbc);
	}

	public void stateChanged(PushPullEvent pushPullEvent) {
		if(pushPullEvent.getActionStatus() == ActionStatus.STARTED){
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

	public void setSplit(JSplitPane splitPane) {
		this.splitPane = splitPane;
	}

}
