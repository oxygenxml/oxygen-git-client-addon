package com.oxygenxml.sdksamples.workspace.git;


import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.view.CommitPanel;
import com.oxygenxml.sdksamples.workspace.git.view.GitWindow;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;
import com.oxygenxml.sdksamples.workspace.git.view.ToolbarPanel;
import com.oxygenxml.sdksamples.workspace.git.view.UnstagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.WorkingCopySelectionPanel;
import com.oxygenxml.sdksamples.workspace.git.view.event.PushPullController;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageController;
import com.sun.xml.bind.v2.runtime.output.StAXExStreamWriterOutput;

import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class Application {

	private StagingPanel stagingPanel;
	
	public void start() {

		/*try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}*/

		createAndShowFrame();
	}

	private void createAndShowFrame() {
		GitAccess gitAccess = new GitAccess();
		StageController observer = new StageController(gitAccess);
		PushPullController pushPullController = new PushPullController(gitAccess);

		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, false);
		UnstagedChangesPanel stagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, true);
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess);
		CommitPanel commitPanel = new CommitPanel(gitAccess, observer);
		ToolbarPanel toolbarPanel = new ToolbarPanel(pushPullController);

		StagingPanel stagingPanel = new StagingPanel(workingCopySelectionPanel, unstagedChangesPanel, stagedChangesPanel,
				commitPanel, toolbarPanel);
		stagingPanel.registerSubject(pushPullController);
		stagingPanel.createGUI();
		
		this.stagingPanel = stagingPanel;
		//GitWindow gitWindow = new GitWindow(stagingPanel);
		
		//gitWindow.createGUI();
		//this.gitWIndow = gitWindow;
	}
	
	public JPanel getGitWindow(){
		return stagingPanel;
	}

}
