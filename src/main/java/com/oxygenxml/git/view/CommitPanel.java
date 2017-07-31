package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;

public class CommitPanel extends JPanel {

	private StageController stageController;
	private JLabel label;
	private JTextArea commitMessage;
	private JButton commitButton;
	private GitAccess gitAccess;

	public CommitPanel(GitAccess gitAccess, StageController observer) {
		this.gitAccess = gitAccess;
		this.stageController = observer;
	}

	public JButton getCommitButton() {
		return commitButton;
	}

	public void setCommitButton(JButton commitButton) {
		this.commitButton = commitButton;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addCommitMessageTextArea(gbc);
		addCommitButton(gbc);

		addCommitButtonListener();
	}

	private void addCommitButtonListener() {
		commitButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ChangeEvent changeEvent = new ChangeEvent(StageState.COMMITED, StageState.STAGED, gitAccess.getStagedFile());
				stageController.stateChanged(changeEvent);
				gitAccess.commit(commitMessage.getText());
				commitMessage.setText("");
				// TODO Give the parent.
				JOptionPane.showMessageDialog(null, "Commit successful");
			}
		});
	}

	private void addLabel(GridBagConstraints gbc) { 
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		label = new JLabel("Message for the commit: ");
		this.add(label, gbc);
	}

	private void addCommitMessageTextArea(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		commitMessage = new JTextArea();
		commitMessage.setLineWrap(true);
		// Around 3 lines of text.
		int fontH = commitMessage.getFontMetrics(commitMessage.getFont()).getHeight();
		commitMessage.setPreferredSize(new Dimension(200, 30 * fontH));

		JScrollPane scrollPane = new JScrollPane(commitMessage);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(200, 3 * fontH));
		this.add(scrollPane, gbc);
	}

	private void addCommitButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		commitButton = new JButton("Commit");
		this.add(commitButton, gbc);
	}

}
