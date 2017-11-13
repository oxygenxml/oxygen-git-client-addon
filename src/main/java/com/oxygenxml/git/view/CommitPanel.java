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
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;
import com.oxygenxml.git.view.event.Subject;

import ro.sync.ui.Icons;

public class CommitPanel extends JPanel implements Observer<ChangeEvent>, Subject<PushPullEvent> {

	private StageController stageController;
	private JLabel label;
	private JTextArea commitMessage;
	private JButton commitButton;
	private GitAccess gitAccess;
	private JComboBox<String> previouslyMessages;
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
		this.setPreferredSize(new Dimension(Constants.PANEL_WIDTH, Constants.COMMIT_PANEL_HEIGHT));
		this.setMinimumSize(new Dimension(Constants.PANEL_WIDTH, Constants.COMMIT_PANEL_HEIGHT));
	}

	private void addPreviouslyMessagesComboBoxListener() {
		previouslyMessages.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {

					if (!previouslyMessages.getSelectedItem()
							.equals(translator.getTraslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE))) {
						commitMessage.setText((String) previouslyMessages.getSelectedItem());
						previouslyMessages.setEditable(true);
						previouslyMessages.setSelectedItem(translator.getTraslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE));
						previouslyMessages.setEditable(false);
					}
				}
			}
		});

	}

	private void addCommitButtonListener() {
		commitButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String message = "";
				if (gitAccess.getConflictingFiles().size() > 0) {
					message = translator.getTraslation(Tags.COMMIT_WITH_CONFLICTS);
				} else {
					message = translator.getTraslation(Tags.COMMIT_SUCCESS);
					ChangeEvent changeEvent = new ChangeEvent(StageState.COMMITED, StageState.STAGED, gitAccess.getStagedFile());
					stageController.stateChanged(changeEvent);
					gitAccess.commit(commitMessage.getText());
					OptionsManager.getInstance().saveCommitMessage(commitMessage.getText());
					previouslyMessages.removeAllItems();
					for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
						previouslyMessages.addItem(previouslyCommitMessage);
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
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		label = new JLabel(translator.getTraslation(Tags.COMMIT_MESSAGE_LABEL));
		this.add(label, gbc);
	}

	private void addPreviouslyMessagesComboBox(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		previouslyMessages = new JComboBox<String>();
		PreviouslyMessagesToolTipRenderer renderer = new PreviouslyMessagesToolTipRenderer();
		previouslyMessages.setRenderer(renderer);

		for (String previouslyCommitMessage : OptionsManager.getInstance().getPreviouslyCommitedMessages()) {
			previouslyMessages.addItem(previouslyCommitMessage);
		}
		previouslyMessages.setEditable(true);
		previouslyMessages.setSelectedItem(translator.getTraslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE));
		previouslyMessages.setEditable(false);

		int height = (int) previouslyMessages.getPreferredSize().getHeight();
		previouslyMessages.setMinimumSize(new Dimension(10, height));

		this.add(previouslyMessages, gbc);
	}

	private void addCommitMessageTextArea(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
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
		// commitMessage.setPreferredSize(new Dimension(200, 30 * fontH));
		commitMessage.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(commitMessage);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new Dimension(10, 3 * fontH));

		final UndoManager undoManager = new UndoManager();
		Document doc = commitMessage.getDocument();
		// Listen for undo and redo events
		doc.addUndoableEditListener(undoManager);

		// Create an undo action and add it to the text component
		commitMessage.getActionMap().put("Undo", new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent evt) {
				if (undoManager.canUndo()) {
					undoManager.undo();
				}
			}
		});

		// Bind the undo action to ctl-Z
		commitMessage.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

		// Create a redo action and add it to the text component
		commitMessage.getActionMap().put("Redo", new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent evt) {
				if (undoManager.canRedo()) {
					undoManager.redo();
				}
			}
		});

		// Bind the redo action to ctl-Y
		commitMessage.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
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
		commitButton = new JButton(translator.getTraslation(Tags.COMMIT_BUTTON_TEXT));
		toggleCommitButton();
		this.add(commitButton, gbc);
	}

	public void stateChanged(ChangeEvent changeEvent) {
		toggleCommitButton();
	}

	private void toggleCommitButton() {
		try {
			if (gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING_RESOLVED
					&& gitAccess.getStagedFile().size() == 0 && gitAccess.getUnstagedFiles().size() == 0) {
				commitButton.setEnabled(true);
				commitMessage.setText(translator.getTraslation(Tags.CONCLUDE_MERGE_MESSAGE));
			} else if (gitAccess.getStagedFile().size() > 0) {
				commitButton.setEnabled(true);
			} else {
				commitButton.setEnabled(false);
			}
		} catch (NoRepositorySelected e) {
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

	/**
	 * Update status.
	 * 
	 * @param message The message to present in the status field.
	 */
	public void setStatus(final String message) {
	  // TODO Create constants for the messages.
		if ("unavailable".equals(message)) {
			statusLabel.setText(translator.getTraslation(Tags.CANNOT_REACH_HOST));
			statusLabel.setIcon(Icons.getIcon(ImageConstants.VALIDATION_ERROR));
		} else if ("available".equals(message)) {
		  statusLabel.setText(null);
		  statusLabel.setIcon(null);
		} else {
		  statusLabel.setIcon(null);
      statusLabel.setText(message);
		}
	}

	public void clearCommitMessage() {
		commitMessage.setText(null);
	}

	class MyCompoundEdit extends CompoundEdit {
		boolean isUnDone = false;

		public int getLength() {
			return edits.size();
		}

		public void undo() throws CannotUndoException {
			super.undo();
			isUnDone = true;
		}

		public void redo() throws CannotUndoException {
			super.redo();
			isUnDone = false;
		}

		public boolean canUndo() {
			return edits.size() > 0 && !isUnDone;
		}

		public boolean canRedo() {
			return edits.size() > 0 && isUnDone;
		}

	}

	class UndoManager extends AbstractUndoableEdit implements UndoableEditListener {
		String lastEditName = null;
		int lastOffset = -1;
		ArrayList<MyCompoundEdit> edits = new ArrayList<MyCompoundEdit>();
		MyCompoundEdit current;
		int pointer = -1;

		public void undoableEditHappened(UndoableEditEvent e) {
			UndoableEdit edit = e.getEdit();
			if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
				try {
					AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) edit;
					int start = event.getOffset();
					int len = event.getLength();
					String text = "";
					if ("addition".equals(edit.getPresentationName())) {
						text = event.getDocument().getText(start, len);
					}
					boolean isNeedStart = false;
					if (current == null) {
						isNeedStart = true;
					} else if (text.contains("\n") && !"deletion".equals(edit.getPresentationName())) {
						isNeedStart = true;
					} else if (lastEditName == null || !lastEditName.equals(edit.getPresentationName())) {
						isNeedStart = true;
					} else if (Math.abs(lastOffset - start) > 1) {
						isNeedStart = true;
					}

					while (pointer < edits.size() - 1) {
						edits.remove(edits.size() - 1);
						isNeedStart = true;
					}
					if (isNeedStart) {
						createCompoundEdit();
					}

					current.addEdit(edit);
					lastEditName = edit.getPresentationName();
					lastOffset = start;

				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		}

		public void createCompoundEdit() {
			if (current == null) {
				current = new MyCompoundEdit();
			} else if (current.getLength() > 0) {
				current = new MyCompoundEdit();
			}

			edits.add(current);
			pointer++;
		}

		public void undo() throws CannotUndoException {
			if (!canUndo()) {
				throw new CannotUndoException();
			}

			MyCompoundEdit u = edits.get(pointer);
			u.undo();
			pointer--;

		}

		public void redo() throws CannotUndoException {
			if (!canRedo()) {
				throw new CannotUndoException();
			}

			pointer++;
			MyCompoundEdit u = edits.get(pointer);
			u.redo();

		}

		public boolean canUndo() {
			return pointer >= 0;
		}

		public boolean canRedo() {
			return edits.size() > 0 && pointer < edits.size() - 1;
		}

	}

	class PreviouslyMessagesToolTipRenderer extends DefaultListCellRenderer {

		private final int MAX_TOOL_TIP_WIDTH = 700;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			JToolTip createToolTip = comp.createToolTip();
			Font font = createToolTip.getFont();
			FontMetrics fontMetrics = getFontMetrics(font);
			int length = fontMetrics.stringWidth((String) value);
			if (length < MAX_TOOL_TIP_WIDTH) {
				comp.setToolTipText("<html><p width=\"" + length + "\">" + value + "</p></html>");
			} else {
				comp.setToolTipText("<html><p width=\"" + MAX_TOOL_TIP_WIDTH + "\">" + value + "</p></html>");
			}
			return comp;
		}
	}

}
