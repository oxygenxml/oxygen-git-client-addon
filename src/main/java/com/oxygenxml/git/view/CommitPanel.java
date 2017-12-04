package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.ScrollPaneConstants;

import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PanelRefresh.RepositoryStatus;
import com.oxygenxml.git.utils.UndoSupportInstaller;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.FileState;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.Subject;

import ro.sync.ui.Icons;

public class CommitPanel extends JPanel implements Observer<ChangeEvent>, Subject<PushPullEvent> {

	private StageController stageController;
	private JTextArea commitMessage;
	private JButton commitButton;
	private GitAccess gitAccess;
	private JComboBox<String> previousMessages;
	private JLabel statusLabel;
	private Observer<PushPullEvent> observer;
	private Translator translator;

	public CommitPanel(GitAccess gitAccess, StageController observer, Translator translator) {
		this.gitAccess = gitAccess;
		this.stageController = observer;
		this.translator = translator;
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
		this.setPreferredSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_HEIGHT));
		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_HEIGHT));
	}

	private void addPreviouslyMessagesComboBoxListener() {
		previousMessages.addItemListener(new ItemListener() {
		  @Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED
				    && !previousMessages.getSelectedItem().equals(
				        translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE))) {
						commitMessage.setText((String) previousMessages.getSelectedItem());
						previousMessages.setEditable(true);
						previousMessages.setSelectedItem(translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE));
						previousMessages.setEditable(false);
				}
			}
		});

	}

	private void addCommitButtonListener() {
		commitButton.addActionListener(new ActionListener() {

			@Override
      public void actionPerformed(ActionEvent e) {
				String message = "";
				if (!gitAccess.getConflictingFiles().isEmpty()) {
					message = translator.getTranslation(Tags.COMMIT_WITH_CONFLICTS);
				} else {
					message = translator.getTranslation(Tags.COMMIT_SUCCESS);
					ChangeEvent changeEvent = new ChangeEvent(FileState.COMMITED, FileState.STAGED, gitAccess.getStagedFile());
					stageController.stateChanged(changeEvent);
					gitAccess.commit(commitMessage.getText());
					OptionsManager.getInstance().saveCommitMessage(commitMessage.getText());
					previousMessages.removeAllItems();
					for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
						previousMessages.addItem(previouslyCommitMessage);
					}

					commitButton.setEnabled(false);
				}
				commitMessage.setText("");
				PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.UPDATE_COUNT, message);
				notifyObservers(pushPullEvent);
			}
		});
	}

	private void addLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		this.add(new JLabel(translator.getTranslation(Tags.COMMIT_MESSAGE_LABEL)), gbc);
	}

	private void addPreviouslyMessagesComboBox(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		previousMessages = new JComboBox<String>();
		PreviousMessagesToolTipRenderer renderer = new PreviousMessagesToolTipRenderer();
		previousMessages.setRenderer(renderer);

		for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
			previousMessages.addItem(previouslyCommitMessage);
		}
		previousMessages.setEditable(true);
		previousMessages.setSelectedItem(translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE));
		previousMessages.setEditable(false);

		int height = (int) previousMessages.getPreferredSize().getHeight();
		previousMessages.setMinimumSize(new Dimension(10, height));

		this.add(previousMessages, gbc);
	}

	private void addCommitMessageTextArea(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		commitMessage = new JTextArea();
		commitMessage.setLineWrap(true);
		// Around 3 lines of text.
		int fontH = commitMessage.getFontMetrics(commitMessage.getFont()).getHeight();
		commitMessage.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(commitMessage);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new Dimension(10, 3 * fontH));

		UndoSupportInstaller.installUndoManager(commitMessage);
		this.add(scrollPane, gbc);
	}

	private void addStatusLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
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
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0;
		commitButton = new JButton(translator.getTranslation(Tags.COMMIT_BUTTON_TEXT));
		toggleCommitButton();
		this.add(commitButton, gbc);
	}

	@Override
  public void stateChanged(ChangeEvent changeEvent) {
		toggleCommitButton();
	}

	private void toggleCommitButton() {
	  boolean enable = false;
	  try {
	    if (gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING_RESOLVED
	        && gitAccess.getStagedFile().isEmpty() && gitAccess.getUnstagedFiles().isEmpty()) {
	      enable = true;
	      commitMessage.setText(translator.getTranslation(Tags.CONCLUDE_MERGE_MESSAGE));
	    } else if (!gitAccess.getStagedFile().isEmpty()) {
	      enable = true;
	    }
	  } catch (NoRepositorySelected e) {
	    // Remains disabled
	  }
	  commitButton.setEnabled(enable);

	}

	private void notifyObservers(PushPullEvent pushPullEvent) {
		observer.stateChanged(pushPullEvent);
	}

	@Override
  public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	@Override
  public void removeObserver(Observer<PushPullEvent> obj) {
		observer = null;
	}

	/**
	 * Update status.
	 * 
	 * @param the current status.
	 */
	public void setStatus(final String status) {
		if (RepositoryStatus.UNAVAILABLE.equals(status)) {
			statusLabel.setText(translator.getTranslation(Tags.CANNOT_REACH_HOST));
			statusLabel.setIcon(Icons.getIcon(ImageConstants.VALIDATION_ERROR));
		} else if (RepositoryStatus.AVAILABLE.equals(status)) {
		  statusLabel.setText(null);
		  statusLabel.setIcon(null);
		} else {
		  statusLabel.setIcon(null);
      statusLabel.setText(status);
		}
	}

	public void clearCommitMessage() {
		commitMessage.setText(null);
	}

	/**
	 * Renderer for the combo box presenting the previous commit messages. 
	 */
	private static final class PreviousMessagesToolTipRenderer extends DefaultListCellRenderer {

	  /**
	   * Maximum tooltip width.
	   */
		private static final int MAX_TOOLTIP_WIDTH = 700;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			JToolTip createToolTip = comp.createToolTip();
			Font font = createToolTip.getFont();
			FontMetrics fontMetrics = getFontMetrics(font);
			int length = fontMetrics.stringWidth((String) value);
			if (length < MAX_TOOLTIP_WIDTH) {
				comp.setToolTipText("<html><p width=\"" + length + "\">" + value + "</p></html>");
			} else {
				comp.setToolTipText("<html><p width=\"" + MAX_TOOLTIP_WIDTH + "\">" + value + "</p></html>");
			}
			return comp;
		}
	}

}
