package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;

public class FilePanel extends JPanel {

	private JCheckBox fileNameCheckBox = new JCheckBox("File");
	private JButton stageButton = new JButton("Stage");

	public FilePanel(){
		
	}
	
	public FilePanel(String fileName) {
		fileNameCheckBox.setText(fileName);
		init();

	}

	private void init() {
		this.setBorder(BorderFactory.createTitledBorder("A panel"));
		this.setEnabled(true);
		this.setBackground(Color.WHITE);
		this.setLayout(new GridBagLayout());
		
		
		MouseAdapter select = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				
			}
		};
		
		this.addMouseListener(select);
		fileNameCheckBox.addMouseListener(select);

		GridBagConstraints gbc = new GridBagConstraints();
		addFileNameCheckBox(gbc);
		addStageButton(gbc);
	}

	private void addFileNameCheckBox(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		fileNameCheckBox.setBackground(Color.WHITE);
		this.add(fileNameCheckBox, gbc);
	}

	private void addStageButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		this.add(stageButton, gbc);
	}
}
