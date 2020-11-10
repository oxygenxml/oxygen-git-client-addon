package com.oxygenxml.git.view;

import java.awt.Cursor;
import java.awt.Dimension;
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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RepoNotInitializedException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.PanelRefresh.RepositoryStatus;
import com.oxygenxml.git.utils.UndoSupportInstaller;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.Subject;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Panel to insert the commit message and commit the staged files. 
 */
public class CommitAndStatusPanel extends JPanel implements Subject<PushPullEvent> {
  
  /**
   * Max number of characters for the previous messages. 
   */
  private static final int PREV_MESS_MAX_WIDTH = 100;
  
  /**
   * Commit action.
   */
  private final class CommitAction extends AbstractAction {
    /**
     * <code>true</code> if the commit is in progress. <code>false</code> if it has ended.
     */
    private AtomicBoolean commitinProgress = new AtomicBoolean(false);
    /**
     * Timer for updating cursor and status.
     */
    private Timer cursorTimer = new Timer(
        1000,
        e -> SwingUtilities.invokeLater(() -> {
          if (commitinProgress.compareAndSet(true, true)) {
            // Commit process still running. Present a hint.
            setStatusMessage(translator.getTranslation(Tags.COMMITTING) + "...");
            CommitAndStatusPanel.this.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          }
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
        RepositoryState repoState = getRepoState();
        if (// EXM-43923: Faster evaluation. Only seldom ask for the conflicting files,
            // which actually calls git.status(), operation that is slow
            repoState == RepositoryState.MERGING 
            || repoState == RepositoryState.REBASING_MERGE && !gitAccess.getConflictingFiles().isEmpty()) {
          SwingUtilities.invokeLater(() -> setStatusMessage(""));
          PluginWorkspaceProvider.getPluginWorkspace().showInformationMessage(
              translator.getTranslation(Tags.COMMIT_WITH_CONFLICTS));
        } else {
          if (commitMessageArea.getText().trim().isEmpty()) {
            int userAnswer = FileStatusDialog.showWarningMessageWithConfirmation(
                translator.getTranslation(Tags.NO_COMMIT_MESSAGE_TITLE),
                translator.getTranslation(Tags.NO_COMMIT_MESSAGE_DIALOG), 
                translator.getTranslation(Tags.COMMIT_ANYWAY),
                translator.getTranslation(Tags.CANCEL));
            if (userAnswer == 1) {
              executeCommit();
            }else {
              commitMessageArea.requestFocus();
            }
          } else {
            executeCommit();
          }
        }
      });
    }
    
    /**
     * @return repository state.
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
     * Commit the staged files.
     */
    private void executeCommit() {
      boolean commitSuccessful = false;
      try {
        // Prepare the flag for the timer. Unconditionally because the timer is not yet running.
        stopTimer();
        commitinProgress.set(true);
        cursorTimer.start();

        SwingUtilities.invokeLater(() -> commitButton.setEnabled(false));
        
        gitAccess.commit(commitMessageArea.getText(), amendLastCommitToggle.isSelected());
        
        commitSuccessful = true;
      } catch (GitAPIException e1) {
        logger.debug(e1, e1);
        
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(
            "Commit failed.  " + e1.getMessage());
      } finally {
        stopTimer();
        handleCommitEnded(commitSuccessful);
        SwingUtilities.invokeLater(() -> CommitAndStatusPanel.this.getParent().setCursor(Cursor.getDefaultCursor()));
        
        if (commitSuccessful && autoPushWhenCommittingToggle.isSelected()) {
          pushPullController.push();
        }
      }
    }

    /**
     * Stops the timer that presents the commit in progress label.
     */
    private void stopTimer() {
      commitinProgress.getAndSet(false);
      cursorTimer.stop();
    }
    
    /**
     * A commit ended. Update the view.
     * 
     * @param commitSuccessful <code>true</code> if the commit was successful. <code>false</code> if it failed.
     */
    private void handleCommitEnded(boolean commitSuccessful) {
      if (commitSuccessful) {
        optionsManager.saveCommitMessage(commitMessageArea.getText());

        previousMessages.setMenuActions(Collections.emptyList());
        for (String message : optionsManager.getPreviouslyCommitedMessages()) {
          String shortCommitMessage = message.length() <= PREV_MESS_MAX_WIDTH ? message 
              : message.substring(0, PREV_MESS_MAX_WIDTH) + "...";
          AbstractAction action = new AbstractAction(shortCommitMessage) {
            @Override
            public void actionPerformed(ActionEvent e) {
              commitMessageArea.setText(message);
            }
          };
          JMenuItem menuItem = previousMessages.add(action);
          menuItem.setToolTipText(message.length() > PREV_MESS_MAX_WIDTH ? message : null);
        }
        
        PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.UPDATE_COUNT, null);
        observers.forEach((Observer<PushPullEvent> observer) -> observer.stateChanged(pushPullEvent));

        SwingUtilities.invokeLater(() -> {
          setStatusMessage(amendLastCommitToggle.isSelected() ? translator.getTranslation(Tags.AMENDED_SUCCESSFULLY)
              : translator.getTranslation(Tags.COMMIT_SUCCESS));
          amendLastCommitToggle.setSelected(false);
          commitButton.setText(translator.getTranslation(Tags.COMMIT));
          commitMessageArea.setText("");
        });
      } else {
        SwingUtilities.invokeLater(() -> setStatusMessage(""));
      }
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
	private SplitMenuButton previousMessages;
	/**
   * Amend last commit.
   */
  private JideToggleButton amendLastCommitToggle;
	/**
	 * Automatically push when committing.
	 */
	private JideToggleButton autoPushWhenCommittingToggle;
	/**
	 * Messages of interest.
	 */
	private JLabel statusLabel;
  /**
	 * Will be notified after the commit.
	 */
	private Collection<Observer<PushPullEvent>> observers;
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
	 * Push / pull controller.
	 */
  private PushPullController pushPullController;

	/**
	 * Constructor.
	 * 
	 * @param pushPullController Push / pull controller.
	 */
	public CommitAndStatusPanel(PushPullController pushPullController) {
	  this.pushPullController = pushPullController;
	  this.observers = new HashSet<>();
	  
	  // By default a swing timer is on repeat.
	  commitButtonAndMessageUpdateTaskTimer.setRepeats(false);
	  
    createGUI();
	  
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation gitOperation = info.getGitOperation();
        if (gitOperation == GitOperation.OPEN_WORKING_COPY) {
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
        } else if (gitOperation == GitOperation.CHECKOUT) {
          reset();
        } else {
          if (gitOperation == GitOperation.MERGE_RESTART) {
            commitMessageArea.setText(null);
          }
          toggleCommitButtonAndUpdateMessageArea(gitOperation == GitOperation.STAGE);
        }
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
		addCommitToolbar(gbc);
		addCommitMessageTextArea(gbc);
		addStatusLabel(gbc);
		addCommitButton(gbc);

		this.setPreferredSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.COMMIT_PANEL_MIN_HEIGHT));
	}
	
	/**
   * Add "Commit" label. 
   */
  private void addLabel(GridBagConstraints gbc) {
    gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
        0, UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridwidth = 1;
    this.add(new JLabel(translator.getTranslation(Tags.COMMIT_MESSAGE_LABEL)), gbc);
  }

	/**
	 * Add commit toolbar.
	 * 
	 * @param gbc Constraints.
	 */
  private void addCommitToolbar(GridBagConstraints gbc) {
    JToolBar commitToolbar = new JToolBar();
		commitToolbar.setOpaque(false);
		commitToolbar.setFloatable(false);
		
		addPreviouslyMessagesComboBox(commitToolbar);
		addAutoPushOnCommitToggle(commitToolbar);
		addAmendLastCommitToggle(commitToolbar);
		
		gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        3, 
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridwidth = 1;
    this.add(commitToolbar, gbc);
  }
  
  /**
   * Add the toggle that allows amending the last commit.
   * 
   * @param toolbar The toolbar to which to add.
   */
  private void addAmendLastCommitToggle(JToolBar toolbar) {
    amendLastCommitToggle = new JideToggleButton(Icons.getIcon(Icons.AMEND_COMMIT));
    amendLastCommitToggle.setFocusPainted(false);
    amendLastCommitToggle.setToolTipText(translator.getTranslation(Tags.AMEND_LAST_COMMIT));
    amendLastCommitToggle.addItemListener(new ItemListener() {
      String previousText = "";
      public void itemStateChanged(ItemEvent ev) {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
          previousText = commitMessageArea.getText();
          try {
            if (gitAccess.getPushesAhead() == 0) {
              int result = PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
                  translator.getTranslation(Tags.AMEND_LAST_COMMIT),
                  translator.getTranslation(Tags.AMEND_PUSHED_COMMIT_WARNING),
                  new String[] {
                      "   " + translator.getTranslation(Tags.YES) + "   ",
                      "   " + translator.getTranslation(Tags.NO) + "   " },
                  new int[] {1, 0});
              if (result == 1) {
                prepareAmend();
              } else {
                amendLastCommitToggle.setSelected(false);
              }
            } else {
              prepareAmend();
            }
          } catch (RepoNotInitializedException e) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage());
          }
        } else {
          commitMessageArea.setText(previousText);
          toggleCommitButtonAndUpdateMessageArea(false);
          commitButton.setText(translator.getTranslation(Tags.COMMIT));
        }
      }
      
      /**
       * Prepare amend.
       */
      private void prepareAmend() {
        RevCommit latestCommitOnBranch = null;
        try {
          latestCommitOnBranch = GitAccess.getInstance().getLatestCommitOnCurrentBranch();
        } catch (GitAPIException | IOException e) {
          logger.debug(e, e);
        }
        if (latestCommitOnBranch != null) {
          String text = latestCommitOnBranch.getFullMessage();
          commitMessageArea.setText(text);
          toggleCommitButtonAndUpdateMessageArea(false);
          commitButton.setText(translator.getTranslation(Tags.AMEND_LAST_COMMIT));
        }
      }
      
    });
    toolbar.add(amendLastCommitToggle);
  }
	
	/**
	 * Add the toggle that controls whether or not to automatically push on commit.
	 * 
	 * @param toolbar The toolbar to which to add.
	 */
  private void addAutoPushOnCommitToggle(JToolBar toolbar) {
    autoPushWhenCommittingToggle = new JideToggleButton(Icons.getIcon(Icons.AUTO_PUSH_ON_COMMIT));
    autoPushWhenCommittingToggle.setFocusPainted(false);
    autoPushWhenCommittingToggle.setToolTipText(translator.getTranslation(Tags.PUSH_WHEN_COMMITTING));
    autoPushWhenCommittingToggle.setSelected(OptionsManager.getInstance().isAutoPushWhenCommitting());
    autoPushWhenCommittingToggle.addItemListener(
        ev -> OptionsManager.getInstance().setAutoPushWhenCommitting(
            ev.getStateChange()==ItemEvent.SELECTED));
    toolbar.add(autoPushWhenCommittingToggle);
  }

	/**
	 * Add previous commit messages combo. 
	 * 
	 * @param toolbar The toolbar to which to add.
	 */
	private void addPreviouslyMessagesComboBox(JToolBar toolbar) {
		previousMessages = new SplitMenuButton(
		    null,
		    Icons.getIcon(Icons.PREV_COMMIT_MESSAGES),
		    false,
		    false,
		    true,
		    false);
		previousMessages.setToolTipText(translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE));
		translator.getTranslation(Tags.COMMIT_COMBOBOX_DISPLAY_MESSAGE);
		
		for (String commitMessage : optionsManager.getPreviouslyCommitedMessages()) {
		  String shortCommitMessage = commitMessage.length() <= PREV_MESS_MAX_WIDTH ? commitMessage 
		      : commitMessage.substring(0, PREV_MESS_MAX_WIDTH) + "...";
			AbstractAction action = new AbstractAction(shortCommitMessage) {
        @Override
        public void actionPerformed(ActionEvent e) {
          commitMessageArea.setText(commitMessage);
        }
      };
      JMenuItem menuItem = previousMessages.add(action);
      menuItem.setToolTipText(commitMessage.length() > PREV_MESS_MAX_WIDTH ? commitMessage : null);
		}
		
		toolbar.add(previousMessages);
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
    if (forceEnable) {
      commitButton.setEnabled(true);
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
          commitButton.setEnabled(false);
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
              } else if (!status.getStagedFiles().isEmpty() || amendLastCommitToggle.isSelected()) {
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
  }

	@Override
  public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null) {
			throw new NullPointerException("Null Observer");
		}
		observers.add(observer);
	}

	@Override
  public void removeObserver(Observer<PushPullEvent> obj) {
		observers.remove(obj);
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
		commitMessageArea.setText(null);
		amendLastCommitToggle.setSelected(false);
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
  
  /**
   * @return The "Auto push when committing" toggle button.
   */
  public JideToggleButton getAutoPushWhenCommittingToggle() {
    return autoPushWhenCommittingToggle;
  }
  
  /**
   * @return The "Amend last commit" toggle button.
   */
  public JideToggleButton getAmendLastCommitToggle() {
    return amendLastCommitToggle;
  }
}
