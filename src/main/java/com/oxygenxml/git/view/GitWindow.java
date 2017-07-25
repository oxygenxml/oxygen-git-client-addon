package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class GitWindow extends JFrame{

	private StagingPanel stagingPanel;

	public GitWindow(StagingPanel stagingPanel) {
		super();
		this.stagingPanel = stagingPanel;
	}

	public void createGUI() {
		this.setTitle("Git");
		this.getContentPane().add(stagingPanel, BorderLayout.CENTER);
		
		this.pack();
		
		this.setMinimumSize(new Dimension(600,700));
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
	}

	public JPanel getMainPanel() {
		return stagingPanel;
	}
	


}
