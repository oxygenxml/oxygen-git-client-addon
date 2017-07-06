package com.oxygenxml.sdksamples.workspace.git.view;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WorkingCopySelectionPanel extends JPanel {
	
	private JLabel label;
	private JComboBox workingCopyelector;
	private JButton browse;
	
	public WorkingCopySelectionPanel(){
		init();
	}

	private void init() {
		label = new JLabel("Working copy");
		this.add(label);
	}
	
}
