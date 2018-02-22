package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class ProgressDialog extends OKCancelDialog {

	private JProgressBar progressBar;
	private JLabel noteLabel;
	private boolean isCanceled;
	private Translator translator;

	public ProgressDialog(JFrame parentFrame) {
		super(parentFrame, "", true);
		translator = Translator.getInstance();
		setTitle(translator.getTranslation(Tags.CLONE_PROGRESS_DIALOG_TITLE));
		
		noteLabel = new JLabel();

		progressBar = new JProgressBar();
		progressBar.setStringPainted(false);
		progressBar.setIndeterminate(true);
		progressBar.setPreferredSize(new Dimension(250, progressBar.getPreferredSize().height));

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

		setMinimumSize(new Dimension(370, 150));
		setResizable(false);
		pack();
		setLocationRelativeTo(parentFrame);

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