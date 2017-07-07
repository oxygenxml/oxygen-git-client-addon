package com.oxygenxml.sdksamples.workspace.git;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createAndShowFrame();

	}

	private void createAndShowFrame() {
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel();
		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel();
		StagedChangesPanel stagedChangesPanel = new StagedChangesPanel();
		CommitPanel commitPanel = new CommitPanel();

		StagingPanel stagingPanel = new StagingPanel(workingCopySelectionPanel, unstagedChangesPanel,
				stagedChangesPanel, commitPanel);

		GitWindow gitWindow = new GitWindow(stagingPanel);

	}
}
