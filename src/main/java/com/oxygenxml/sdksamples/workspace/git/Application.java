package com.oxygenxml.sdksamples.workspace.git;


import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.view.CommitPanel;
import com.oxygenxml.sdksamples.workspace.git.view.GitWindow;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;
import com.oxygenxml.sdksamples.workspace.git.view.ToolbarPanel;
import com.oxygenxml.sdksamples.workspace.git.view.UnstagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.WorkingCopySelectionPanel;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageController;

public class Application {

	public static void main(String[] args) {
		new Application().start();

	}

	private void start() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		createAndShowFrame();
	}

	private void createAndShowFrame() {
		GitAccess gitAccess = new GitAccess();
		StageController observer = new StageController(gitAccess);

		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, false);
		UnstagedChangesPanel stagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, true);
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess);
		CommitPanel commitPanel = new CommitPanel(gitAccess, observer);
		ToolbarPanel toolbarPanel = new ToolbarPanel(gitAccess);

		StagingPanel stagingPanel = new StagingPanel(workingCopySelectionPanel, unstagedChangesPanel, stagedChangesPanel,
				commitPanel, toolbarPanel);

		GitWindow gitWindow = new GitWindow(stagingPanel);
		gitWindow.createGUI();
	}

}
