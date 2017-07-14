package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

public class GitWindow extends JFrame{

	private StagingPanel stagingPanel;

	public GitWindow(StagingPanel stagingPanel) {
		super();
		this.stagingPanel = stagingPanel;
	}

	public void createGUI() {
		this.setTitle("Git");
		stagingPanel.createGUI();
		this.getContentPane().add(stagingPanel, BorderLayout.CENTER);
		
		this.pack();
		
		this.setMinimumSize(new Dimension(600,700));
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
	}

}
