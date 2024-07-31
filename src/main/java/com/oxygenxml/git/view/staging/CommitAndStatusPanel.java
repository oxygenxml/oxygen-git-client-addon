package com.oxygenxml.git.view.staging;

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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
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

import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.exceptions.IndexLockExistsException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RepoNotInitializedException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PlatformDetectionUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.RepositoryStatusInfo;
import com.oxygenxml.git.utils.RepositoryStatusInfo.RepositoryStatus;
import com.oxygenxml.git.validation.ValidationManager;
import com.oxygenxml.git.view.UndoRedoSupportInstaller;
import com.oxygenxml.git.view.components.ApplicationLabel;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Panel to insert the commit message and commit the staged files. 
 */
public class CommitAndStatusPanel extends JPanel {
  /**
   * The minimum number of available lines for the commit message.
   */
  private static final int COMMIT_MSG_MIN_NO_OF_AVAILABLE_LINES = 3;
  /**
   * The pixel distance between label's icon and text.
   */
  private static final int LABEL_ICON_TO_TEXT_DISTANCE = 4;
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
        RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
        if (// EXM-43923: Faster evaluation. Only seldom ask for the conflicting files,
            // which actually calls git.status(), operation that is slow
            repoState == RepositoryState.MERGING 
            || repoState == RepositoryState.REVERTING
            || repoState == RepositoryState.REBASING_MERGE && !gitAccess.getConflictingFiles().isEmpty()) {
          SwingUtilities.invokeLater(() -> setStatusMessage(""));
          PluginWorkspaceProvider.getPluginWorkspace().showInformationMessage(
              translator.getTranslation(Tags.COMMIT_WITH_CONFLICTS));
        } else {
          if (commitMessageArea.getText().trim().isEmpty()) {
            final int userAnswer = MessagePresenterProvider.getBuilder(
                translator.getTranslation(Tags.NO_COMMIT_MESSAGE_TITLE), DialogType.WARNING)
                .setQuestionMessage(translator.getTranslation(Tags.NO_COMMIT_MESSAGE_DIALOG))
                .setOkButtonName(translator.getTranslation(Tags.COMMIT))
                .setCancelButtonName(translator.getTranslation(Tags.CANCEL))
                .buildAndShow().getResult();
            if (userAnswer == 1) {
              executeCommit();
            } else {
              commitMessageArea.requestFocus();
            }
          } else {
            executeCommit();
          }
        }
      });
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
        final boolean isPreCommitEnaled = ValidationManager.getInstance().isPreCommitValidationEnabled();  
        if(!isPreCommitEnaled || ValidationManager.getInstance().checkCommitValid()) {
          gitAccess.commit(commitMessageArea.getText(), amendLastCommitToggle.isSelected());
          commitSuccessful = true;
        } else {
          SwingUtilities.invokeLater(() -> commitButton.setEnabled(true));
        }
        
      } catch (CanceledException | IndexLockExistsException e) {
        toggleCommitButtonAndUpdateMessageArea(false);
      } catch (GitAPIException | JGitInternalException e) {
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Commit failed. Reason: " + e.getMessage());
      } finally {
        stopTimer();
        handleCommitEnded(commitSuccessful);
        
        ValidationManager validationManager = ValidationManager.getInstance();
        if (commitSuccessful 
            && autoPushWhenCommittingToggle.isSelected() 
            && (!validationManager.isPrePushValidationEnabled() || validationManager.checkPushValid())) {
          gitController.push();
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
  private static OptionsManager optionsManager = OptionsManager.getInstance();
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CommitAndStatusPanel.class);
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
	 * Git controller.
	 */
  private GitController gitController;

	/**
	 * Constructor.
	 * 
	 * @param gitController Git controller.
	 */
	public CommitAndStatusPanel(GitController gitController) {
	  this.gitController = gitController;
	  
	  // By default a swing timer is on repeat.
	  commitButtonAndMessageUpdateTaskTimer.setRepeats(false);
	  
    createGUI();
	  
    gitController.addGitListener(new GitEventAdapter() {
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
            LOGGER.debug(e.getMessage(), e);
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

		this.setPreferredSize(new Dimension(getPreferredSize().width, UIConstants.COMMIT_PANEL_PREF_HEIGHT));
		this.setMinimumSize(new Dimension(getMinimumSize().width, UIConstants.COMMIT_PANEL_MIN_HEIGHT));
	}
	
	/**
   * Add "Commit" label.
   * 
   * @param gbc Grid bad constraints.
   */
  private void addLabel(GridBagConstraints gbc) {
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING,
        0,
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridwidth = 1;
    this.add(new JLabel(translator.getTranslation(Tags.COMMIT_MESSAGE_LABEL) + ":"), gbc);
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
        0, 
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
      @Override
      public void itemStateChanged(ItemEvent ev) {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
          treatAmendEnabled();
        } else {
          commitMessageArea.setText(previousText);
          toggleCommitButtonAndUpdateMessageArea(false);
          commitButton.setText(translator.getTranslation(Tags.COMMIT));
        }
      }

      /**
       * Amend was enabled. Treat the event.
       */
      private void treatAmendEnabled() {
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
      }
      
      /**
       * Prepare amend.
       */
      private void prepareAmend() {
        RevCommit latestCommitOnBranch = null;
        try {
          latestCommitOnBranch = GitAccess.getInstance().getLatestCommitOnCurrentBranch();
        } catch (GitAPIException | IOException | NoRepositorySelected e) {
          LOGGER.error(e.getMessage(), e);
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
   * 
   * @param gbc Grid bad constraints.
   */
	private void addCommitMessageTextArea(GridBagConstraints gbc) {
		gbc.insets = new Insets(
		    0,
		    UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING,
				UIConstants.COMPONENT_RIGHT_PADDING);
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
		scrollPane.setMinimumSize(new Dimension(
		    UIUtil.DUMMY_MIN_WIDTH,
		    COMMIT_MSG_MIN_NO_OF_AVAILABLE_LINES * fontH));
		UIUtil.setDefaultScrollPaneBorder(scrollPane);

		UndoRedoSupportInstaller.installManager(commitMessageArea);
		
		this.add(scrollPane, gbc);
	}

	/**
	 * Add status label.
	 * 
	 * @param gbc Grid bad constraints.
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
		statusLabel = new ApplicationLabel() {
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
    if (statusLabel == null || !statusLabel.isShowing()) {
      return;
    }
    
    String text = statusLabel.getText();
    if (text != null && !text.isEmpty()) {
      String toolTipText = statusLabel.getToolTipText();
      boolean hasTooltip = toolTipText != null && !toolTipText.isEmpty();
      if (hasTooltip) {
        toolTipText = toolTipText.replace(text, "").trim();
      }

      Icon icon = statusLabel.getIcon();
      int iconWidth = icon != null ? icon.getIconWidth() : 0;
      FontMetrics fontMetrics = statusLabel.getFontMetrics(statusLabel.getFont());
      int availableWidth = statusLabel.getSize().width - iconWidth - LABEL_ICON_TO_TEXT_DISTANCE;
      if (fontMetrics.stringWidth(text) > availableWidth) {
        statusLabel.setToolTipText(hasTooltip ? text + "\n\n" + toolTipText : text);
      } else if (hasTooltip) {
        statusLabel.setToolTipText(toolTipText);
      }
    }
  }

	/**
	 * Add commit button.
	 * 
	 * @param gbc Grid bad constraints.
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
		
		// Add shortcut to the Commit action
		KeyStroke commitKeyStroke = KeyStroke.getKeyStroke(
		    KeyEvent.VK_ENTER,
		    PlatformDetectionUtil.isMacOS() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
    commitMessageArea.getInputMap(JComponent.WHEN_FOCUSED).put(commitKeyStroke, COMMIT_SHORTCUT);
    commitMessageArea.getActionMap().put(COMMIT_SHORTCUT, commitAction);
    commitAction.putValue(Action.ACCELERATOR_KEY, commitKeyStroke);
    commitButton.setToolTipText(
        translator.getTranslation(Tags.COMMIT)
            + " "
            + "(" 
            + (PlatformDetectionUtil.isMacOS() ? UIUtil.getKeyModifiersSymbol(commitKeyStroke.getModifiers(), false) 
                : InputEvent.getModifiersExText(commitKeyStroke.getModifiers())) 
            + "+"
            + KeyEvent.getKeyText(commitKeyStroke.getKeyCode())
            + ")");
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
	    } catch (NoRepositorySelected e) {
	      // Remains disabled
	    }
	  }
	}

	/**
	 * Update status.
	 * 
	 * @param statusInfo The current status.
	 */
	public void setRepoStatus(final RepositoryStatusInfo statusInfo) {
	  RepositoryStatus repoStatus = statusInfo.getRepoStatus();
	  SwingUtilities.invokeLater(() -> {
	    if (RepositoryStatus.UNAVAILABLE == repoStatus) {
	      String extraInfo = statusInfo.getExtraInfo();
	      String text = translator.getTranslation(Tags.CANNOT_REACH_HOST);
	      if (extraInfo != null && !extraInfo.isEmpty()) {
	        text +=  ". " + translator.getTranslation(Tags.HOVER_FOR_DETAILS);
	      }
        statusLabel.setText(text);
	      statusLabel.setIcon(Icons.getIcon(Icons.VALIDATION_ERROR));
        statusLabel.setToolTipText(extraInfo);
	    } else if (RepositoryStatus.AVAILABLE == repoStatus) {
	      statusLabel.setText(null);
	      statusLabel.setIcon(null);
	      statusLabel.setToolTipText(null);
	    }
	  });
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
