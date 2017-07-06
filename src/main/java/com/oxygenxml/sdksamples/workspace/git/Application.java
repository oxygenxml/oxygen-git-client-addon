package com.oxygenxml.sdksamples.workspace.git;

import com.oxygenxml.sdksamples.workspace.git.view.CommitPanel;
import com.oxygenxml.sdksamples.workspace.git.view.GitWindow;
import com.oxygenxml.sdksamples.workspace.git.view.StagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;
import com.oxygenxml.sdksamples.workspace.git.view.UnstagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.WorkingCopySelectionPanel;

public class Application {
	public static void main(String[] args) {
		new Application().start();
	}

	private void start() {
		createAndShowFrame();
		
	}

	private void createAndShowFrame() {
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel();
		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel();
		StagedChangesPanel stagedChangesPanel = new StagedChangesPanel();
		CommitPanel commitPanel = new CommitPanel();
		
		StagingPanel stagingPanel = new StagingPanel(workingCopySelectionPanel, unstagedChangesPanel, stagedChangesPanel, commitPanel);
		
		GitWindow gitWindow = new GitWindow(stagingPanel);
		
	}
}
