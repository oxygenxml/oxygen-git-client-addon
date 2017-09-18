package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

public class ProgressDialog extends OKCancelDialog {

	private JProgressBar progressBar;
	private JLabel noteLabel;
	private boolean isCanceled;

	public ProgressDialog(JFrame parentFrame) {
		super(parentFrame, "Progress", true);

		noteLabel = new JLabel();

		progressBar = new JProgressBar();
		progressBar.setStringPainted(false);
		progressBar.setIndeterminate(true);

		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 15, 5, 15);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		panel.add(new JLabel(), gbc);

		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 5, 0, 5);
		panel.add(progressBar, gbc);

		gbc.gridy++;
		gbc.weightx = 0;
		gbc.insets = new Insets(0, 5, 10, 5);
		panel.add(noteLabel, gbc);

		add(panel);

		getOkButton().setVisible(false);
		
		pack();
		setLocationRelativeTo(parentFrame);
		setMinimumSize(new Dimension(370, 150));
		setResizable(false);

	}

	public void setNote(String text) {
		noteLabel.setText(text);
	}

	@Override
	protected void doCancel() {
		isCanceled = true;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

}