package com.oxygenxml.sdksamples.workspace.git.view;

import javax.swing.JFrame;

public class GitWindow extends JFrame{

	private StagingPanel stagingPanel;

	public GitWindow(StagingPanel stagingPanel) {
		super();
		this.stagingPanel = stagingPanel;
		init();
	}

	private void init() {
		this.setTitle("Git");
		this.add(stagingPanel);
		
		this.pack();
		
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(200, 200);
		this.setLocationRelativeTo(null);
	}

}
