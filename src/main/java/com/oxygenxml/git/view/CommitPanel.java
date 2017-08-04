package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.options.Options;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;
import com.oxygenxml.git.view.event.Subject;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class CommitPanel extends JPanel implements Observer<ChangeEvent>, Subject<PushPullEvent> {

	private StageController stageController;
	private JLabel label;
	private JTextArea commitMessage;
	private JButton commitButton;
	private GitAccess gitAccess;
	private JComboBox<String> previouslyMessages;
	private JLabel statusLabel;
	private Observer<PushPullEvent> observer;
	private volatile int messagesActive = 0;

	public CommitPanel(GitAccess gitAccess, StageController observer) {
		this.gitAccess = gitAccess;
		this.stageController = observer;
	}

	public JButton getCommitButton() {
		return commitButton;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());
		stageController.registerObserver(this);

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addPreviouslyMessagesComboBox(gbc);
		addCommitMessageTextArea(gbc);
		addStatusLabel(gbc);
		addCommitButton(gbc);

		addCommitButtonListener();
		addPreviouslyMessagesComboBoxListener();
	}

	private void addPreviouslyMessagesComboBoxListener() {
		previouslyMessages.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (!previouslyMessages.getSelectedItem().equals("Previously Commit Messages")) {
						commitMessage.setText((String) previouslyMessages.getSelectedItem());
						previouslyMessages.setEditable(true);
						previouslyMessages.setSelectedItem("Previously Commit Messages");
						previouslyMessages.setEditable(false);
					}
				}
			}
		});

	}

	private void addCommitButtonListener() {
		commitButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ChangeEvent changeEvent = new ChangeEvent(StageState.COMMITED, StageState.STAGED, gitAccess.getStagedFile());
				stageController.stateChanged(changeEvent);
				gitAccess.commit(commitMessage.getText());
				OptionsManager.getInstance().saveCommitMessage(commitMessage.getText());
				previouslyMessages.removeAllItems();
				for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
					previouslyMessages.addItem(previouslyCommitMessage);
				}
				commitMessage.setText("");
				commitButton.setEnabled(false);
				// ((StandalonePluginWorkspace)
				// PluginWorkspaceProvider.getPluginWorkspace())
				// .showInformationMessage("Commit successful");
				String message = "Commit successful";
				PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.UPDATE_COUNT, message);
				notifyObservers(pushPullEvent);
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

	private void addPreviouslyMessagesComboBox(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		previouslyMessages = new JComboBox<String>();
		for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
			previouslyMessages.addItem(previouslyCommitMessage);
		}
		previouslyMessages.setEditable(true);
		previouslyMessages.setSelectedItem("Previously Commit Messages");
		previouslyMessages.setEditable(false);
		this.add(previouslyMessages, gbc);
	}

	private void addCommitMessageTextArea(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		commitMessage = new JTextArea();
		commitMessage.setLineWrap(true);
		// Around 3 lines of text.
		int fontH = commitMessage.getFontMetrics(commitMessage.getFont()).getHeight();
		commitMessage.setPreferredSize(new Dimension(200, 30 * fontH));

		JScrollPane scrollPane = new JScrollPane(commitMessage);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new Dimension(10, 3 * fontH));
		this.add(scrollPane, gbc);
	}

	private void addStatusLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 0;
		gbc.weighty = 0;
		statusLabel = new JLabel();
		this.add(statusLabel, gbc);
	}

	private void addCommitButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0;
		commitButton = new JButton("Commit");
		if (gitAccess.getStagedFile().size() > 0) {
			commitButton.setEnabled(true);
		} else {
			commitButton.setEnabled(false);
		}
		this.add(commitButton, gbc);
	}

	public void stateChanged(ChangeEvent changeEvent) {
		if (gitAccess.getStagedFile().size() > 0) {
			commitButton.setEnabled(true);
		} else {
			commitButton.setEnabled(false);
		}
	}

	private void notifyObservers(PushPullEvent pushPullEvent) {
		observer.stateChanged(pushPullEvent);
	}

	public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	public void removeObserver(Observer<PushPullEvent> obj) {
		observer = null;
	}

	public void setStatus(final String message) {
		new Thread(new Runnable() {

			public void run() {
				try {
					synchronized (this) {
						messagesActive++;
						statusLabel.setText(message);
					}
					TimeUnit.SECONDS.sleep(5);
					synchronized (this) {
						messagesActive--;
						if (messagesActive == 0) {
							statusLabel.setText("");
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
