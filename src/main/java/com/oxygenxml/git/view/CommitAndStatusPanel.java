package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.ibm.icu.text.MessageFormat;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.PanelRefresh.RepositoryStatus;
import com.oxygenxml.git.utils.UndoSupportInstaller;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitCommandState;
import com.oxygenxml.git.view.event.GitEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.Subject;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Panel to insert the commit message and commit the staged files. 
 */
public class CommitAndStatusPanel extends JPanel implements Subject<PushPullEvent> {
  
  /**
   * Commit action.
   */
  private final class CommitAction extends AbstractAction {
    /**
     * Timer for updating cursor and status.
     */
    private Timer cursorTimer = new Timer(
        1000,
        e -> SwingUtilities.invokeLater(() -> {
          setStatusMessage(translator.getTranslation(Tags.COMMITTING) + "...");
          CommitAndStatusPanel.this.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }));
    
    /**
     * Constructor.
     */
    public CommitAction() {
      super(translator.getTranslation(Tags.COMMIT_BUTTON_TEXT));
    }

    /**
     * Action performed.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      GitOperationScheduler.getInstance().schedule(() -> {
        try {
          cursorTimer.stop();
          cursorTimer.start();
         
          RepositoryState repoState = getRepoState();
          if (// EXM-43923: Faster evaluation. Only seldom ask for the conflicting files,
              // which actually calls git.status(), operation that is slow
              repoState == RepositoryState.MERGING 
              || repoState == RepositoryState.REBASING_MERGE && !gitAccess.getConflictingFiles().isEmpty()) {
            cursorTimer.stop();
            SwingUtilities.invokeLater(() -> setStatusMessage(""));
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(translator.getTranslation(Tags.COMMIT_WITH_CONFLICTS));
          } else {
            SwingUtilities.invokeLater(() -> commitButton.setEnabled(false));
            gitAccess.commit(commitMessageArea.getText());
            optionsManager.saveCommitMessage(commitMessageArea.getText());

            previousMessages.removeAllItems();
            previousMessages.addItem(getCommitMessageHistoryHint());
            for (String previouslyCommitMessage : optionsManager.getPreviouslyCommitedMessages()) {
              previousMessages.addItem(previouslyCommitMessage);
            }
            
            PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.UPDATE_COUNT, null);
            notifyObservers(pushPullEvent);
            
            SwingUtilities.invokeLater(() -> {
              commitMessageArea.setText("");
              setStatusMessage(translator.getTranslation(Tags.COMMIT_SUCCESS));
              previousMessages.setSelectedItem(getCommitMessageHistoryHint());
            });
          }
        } finally {
          cursorTimer.stop();
          SwingUtilities.invokeLater(() -> CommitAndStatusPanel.this.getParent().setCursor(Cursor.getDefaultCursor()));
        }
      });
    }
  }
  
  /**
   * Commit shortcut key.
   */
  private static final String COMMIT_SHORTCUT = "commitShortcut";
  /**
   * Options manager.
   */
  private static final OptionsManager optionsManager = OptionsManager.getInstance();
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(CommitAndStatusPanel.class);
  /**
   * Text area for the commit message.
   */
	private JTextArea commitMessageArea;
	/**
	 * The button that commits the staged files.
	 */
	private JButton commitButton;
	/**
	 * Git access.
	 */
	private GitAccess gitAccess = GitAccess.getInstance();
	/**
	 * Previous messages history.
	 */
	private JComboBox<String> previousMessages;
	/**
	 * Messages of interest.
	 */
	private JLabel statusLabel;
  /**
	 * Will be notified after the commit.
	 */
	private Observer<PushPullEvent> observer;
	/**
	 * Translation support.
	 */
	private Translator translator = Translator.getInstance();
	/**
	 * Task for updating the state of the commit button and the message area.
	 */
	private SwingWorker<Void, Void> commitButtonAndMessageUpdateTask;
	/**
	 * Commit action.
	 */
	private CommitAction commitAction = new CommitAction();
	
	/**
	 * Timer for the task that updates the state of the commit button and message area.
	 */
	private Timer commitButtonAndMessageUpdateTaskTimer = new Timer(
      300,
      e -> {
        if (commitButtonAndMessageUpdateTask != null) {
          GitOperationScheduler.getInstance().schedule(commitButtonAndMessageUpdateTask);
        } 
      });

	/**
	 * Constructor.
	 */
	public CommitAndStatusPanel() {
	  createGUI();
	  
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        Repository repository;
        try {
          repository = gitAccess.getRepository();
          if (repository != null) {
            // When a new working copy is selected clear the commit text area
            reset();

            // checks what buttons to keep active and what buttons to deactivate
            toggleCommitButtonAndUpdateMessageArea(false);
          }
        } catch (NoRepositorySelected e) {
          logger.debug(e, e);
        }
      }
      
      @Override
      public void stateChanged(GitEvent changeEvent) {
        GitCommand cmd = changeEvent.getGitCommand();
        GitCommandState cmdState = changeEvent.getGitComandState();
        toggleCommitButtonAndUpdateMessageArea(
            cmd == GitCommand.STAGE && cmdState == GitCommandState.SUCCESSFULLY_ENDED);
      }
    });
  }

	/**
	 * Create GUI.
	 */
	private void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addPreviouslyMessagesComboBox(gbc);
		addCommitMessageTextArea(gbc);
		addStatusLabel(gbc);
		addCommitButton(gbc);

		this.setPreferredSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_MIN_HEIGHT));
	}
	
	/**
	 * @return repo state.
	 */
  private RepositoryState getRepoState() {
    RepositoryState repoState = null;
    try {
      repoState = gitAccess.getRepository().getRepositoryState();
    } catch (NoRepositorySelected e1) {
      logger.debug(e1, e1);
    }
    return repoState;
  }

  /**
   * Add "Commit" label. 
   */
	private void addLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				0, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		this.add(new JLabel(translator.getTranslation(Tags.COMMIT_MESSAGE_LABEL)), gbc);
	}

	/**
	 * Add previous commit messages combo. 
	 */
	private void addPreviouslyMessagesComboBox(GridBagConstraints gbc) {
	  gbc.insets = new Insets(
	      UIConstants.COMPONENT_TOP_PADDING, 
	      UIConstants.COMPONENT_LEFT_PADDING,
	      3, 
	      UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		previousMessages = new JComboBox<>();
		PreviousMessagesToolTipRenderer renderer = new PreviousMessagesToolTipRenderer();
		previousMessages.setRenderer(renderer);

		// Add the hint first.
		previousMessages.addItem(getCommitMessageHistoryHint());
		for (String previouslyCommitMessage : optionsManager.getPreviouslyCommitedMessages()) {
			previousMessages.addItem(previouslyCommitMessage);
		}
		
		previousMessages.setSelectedItem(getCommitMessageHistoryHint());
		
		previousMessages.addItemListener(
		    new ItemListener() { // NOSONAR
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED
            && !previousMessages.getSelectedItem().equals(getCommitMessageHistoryHint())) {
            commitMessageArea.setText((String) previousMessages.getSelectedItem());
        }
      }
    });

		int height = (int) previousMessages.getPreferredSize().getHeight();
		previousMessages.setMinimumSize(new Dimension(10, height));

		this.add(previousMessages, gbc);
	}

	/**
	 * @return The message that instructs the user to select a previously used message.
	 */
  private String getCommitMessageHistoryHint() {
    return "<" + translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE) + ">";
  }

  /**
   * Add commit message text area.
   */
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
		commitMessageArea = new JTextArea();
		commitMessageArea.setLineWrap(true);
		// Around 3 lines of text.
		int fontH = commitMessageArea.getFontMetrics(commitMessageArea.getFont()).getHeight();
		commitMessageArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(commitMessageArea);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new Dimension(10, 3 * fontH));

		UndoSupportInstaller.installUndoManager(commitMessageArea);
		
		KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
		commitMessageArea.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, COMMIT_SHORTCUT);
		commitMessageArea.getActionMap().put(COMMIT_SHORTCUT, commitAction);
		
		this.add(scrollPane, gbc);
	}

	/**
	 * Add status label.
	 */
	private void addStatusLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		statusLabel = new JLabel() {
		  @Override
		  public void setText(String text) {
		    super.setText(text);
		    setStatusLabelTooltip();
		  }
		};
		statusLabel.addComponentListener(new ComponentAdapter() {
		  @Override
		  public void componentResized(ComponentEvent e) {
		    super.componentResized(e);
		    setStatusLabelTooltip();
		  }
		});
		this.add(statusLabel, gbc);
	}
	
	/**
	 * Set a tooltip for the status label.
	 */
	private void setStatusLabelTooltip() {
	  if (statusLabel != null && statusLabel.isShowing()) {
	    String text = statusLabel.getText();
	    if (text != null && !text.isEmpty()) {
	      FontMetrics fontMetrics = getFontMetrics(getFont());
	      if (fontMetrics.stringWidth(text) > statusLabel.getSize().width) {
	        statusLabel.setToolTipText(text);
	      } else {
	        statusLabel.setToolTipText(null);
	      }
	    }
	  }
	}

	/**
	 * Add commit button.
	 */
	private void addCommitButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		commitButton = new JButton(commitAction);
		this.add(commitButton, gbc);
	}

  /**
   * Checks if the commit button should be enabled.
   * 
   * @param forceEnable <code>true</code> to make the button enable without any additional checks.
   */
  void toggleCommitButtonAndUpdateMessageArea(boolean forceEnable) {
    boolean enable = false;
    if (forceEnable) {
      enable = true;
    } else {
      try {
        Repository repo = gitAccess.getRepository();
        final RepositoryState repositoryState = repo.getRepositoryState();
        final String mergeMessage = MessageFormat.format(
            translator.getTranslation(Tags.COMMIT_TO_MERGE),
            gitAccess.getBranchInfo().getBranchName(),
            repo.getConfig().getString("remote", "origin", "url"));
        if (repositoryState == RepositoryState.MERGING_RESOLVED
            && mergeMessage.equals(commitMessageArea.getText())) {
          commitMessageArea.setText("");
        } else {
          // Possible time consuming operations.
          commitButtonAndMessageUpdateTask = new SwingWorker<Void, Void>() {
            boolean enable = false;
            String message = null;
            @Override
            protected Void doInBackground() throws Exception {
              GitStatus status = gitAccess.getStatus();
              if (repositoryState == RepositoryState.MERGING_RESOLVED
                  && status.getStagedFiles().isEmpty()
                  && status.getUnstagedFiles().isEmpty()) {
                enable = true;
                message = mergeMessage;
              } else if (!status.getStagedFiles().isEmpty()) {
                enable = true;
              }
              return null;
            }

            @Override
            protected void done() {
              if (message != null) {
                commitMessageArea.setText(message);
              }
              commitButton.setEnabled(enable);
            }
          };

          commitButtonAndMessageUpdateTaskTimer.restart();

        }
      } catch (NoRepositorySelected e) {
        // Remains disabled
      }
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
	public void setRepoStatus(final RepositoryStatus status) {
		if (RepositoryStatus.UNAVAILABLE == status) {
			statusLabel.setText(translator.getTranslation(Tags.CANNOT_REACH_HOST));
		  statusLabel.setIcon(Icons.getIcon(Icons.VALIDATION_ERROR));
		} else if (RepositoryStatus.AVAILABLE == status) {
		  statusLabel.setText(null);
		  statusLabel.setIcon(null);
		}
	}
	
	/**
   * Update the status.
   * 
   * @param message New status.
   */
  public void setStatusMessage(String message) {
    statusLabel.setIcon(null);
    statusLabel.setText(message);
  }

	/**
	 * Resets the panel. Clears any selection done by the user or inserted text.
	 */
	public void reset() {
	  previousMessages.setSelectedItem(getCommitMessageHistoryHint());
		commitMessageArea.setText(null);
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
			if (value != null) {
			  JToolTip createToolTip = comp.createToolTip();
			  Font font = createToolTip.getFont();
			  FontMetrics fontMetrics = getFontMetrics(font);
			  int length = fontMetrics.stringWidth((String) value);
			  if (length < MAX_TOOLTIP_WIDTH) {
			    comp.setToolTipText("<html><p width=\"" + length + "\">" + value + "</p></html>");
			  } else {
			    comp.setToolTipText("<html><p width=\"" + MAX_TOOLTIP_WIDTH + "\">" + value + "</p></html>");
			  }
			}
			return comp;
		}
	}
	
	/**
	 * @return the commit message text area.
	 */
	public JTextArea getCommitMessageArea() {
    return commitMessageArea;
  }

  /**
   * @return the status label
   */
  public JLabel getStatusLabel() {
    return statusLabel;
  }
  
  /**
   * @return the commit button.
   */
  public JButton getCommitButton() {
    return commitButton;
  }
}
