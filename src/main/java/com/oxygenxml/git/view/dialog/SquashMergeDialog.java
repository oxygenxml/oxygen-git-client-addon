package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoChangesInSquashedCommitException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.util.ExceptionHandlerUtil;
import com.oxygenxml.git.view.util.HiDPIUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * The dialog that performs the Squash and merge action.
 * 
 * @author alex_smarandache
 *
 */
public class SquashMergeDialog extends OKCancelDialog {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SquashMergeDialog.class);
  
  /**
   * Preferred height for this dialog.
   */
  private static final int HEIGHT = 275;
  
  /**
   * Preferred width for this dialog.
   */
  private static final int WIDTH = 600;
  
  /**
   * The top inset.
   */
  private static final int TOP_INSET = 11;

	/**
	 * Translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Squash info.
	 */
	private String selectedBranch;
	
	/**
	 * Merge info.
	 */
	private final JTextArea mergeInfo = UIUtil.createMessageArea("");
	
	/**
	 * The text area for commit message.
	 */
	private final JTextArea commitMessageTextArea = new JTextArea();
	
	/**
	 * The default squash commit message.
	 */
	private static final String DEFAULT_SQUASH_COMMIT_MESSAGE = "Squashed commit of the following:\n";
	

	
	/**
	 * Constructor.
	 * 
	 */
	public SquashMergeDialog() {
		super(
				(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.SQUASH_MERGE),
				true);
		
		createGUI();
		setPreferredSize(HiDPIUtil.getHiDPIDimension(new Dimension(WIDTH, HEIGHT)));
		
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
	}

	/**
	 * @param squashCommitMessage The commit message.
	 * 
	 * @return <code>true</code> if a squash commit can be created.
	 */
  private boolean checkIfASquashCommitCanBeCreated(final String squashCommitMessage) {
    return squashCommitMessage != null &&  !squashCommitMessage.isEmpty() &&
        !squashCommitMessage.equals(DEFAULT_SQUASH_COMMIT_MESSAGE);
  }
	
	/**
	 * Adds to the dialog the labels and the text fields.
	 */
	public void createGUI() {
		final JPanel panel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		gbc.gridwidth = 1;
		panel.add(mergeInfo, gbc);
		
		gbc.gridy ++;
		gbc.insets = new Insets(TOP_INSET, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		panel.add(new JLabel(TRANSLATOR.getTranslation(Tags.COMMIT_MESSAGE_LABEL) + ":"), gbc);

		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy ++;
		panel.add(new JScrollPane(commitMessageTextArea), gbc);
    
		this.add(panel, BorderLayout.CENTER);

		setOkButtonText(TRANSLATOR.getTranslation(Tags.SQUASH_AND_COMMIT));	    
	}
  
  @Override
  protected void doOK() {
    try {
      GitAccess.getInstance().squashAndMergeBranch(selectedBranch, commitMessageTextArea.getText());
    } catch (GitAPIException | IOException | NoRepositorySelected | NoChangesInSquashedCommitException e) {
      LOGGER.error(e.getMessage(), e);
      ExceptionHandlerUtil.handleMergeException(e);
    }
    super.doOK();
  }
  
  /**
   * Show the dialog and initialize components.
   *  
   * @param currentBranch          The current branch.
   * @param selectedBranch         The selected branch by user.
   * @param sourceCommitObjectId   The source commit object id.
   * 
   * @throws NoChangesInSquashedCommitException When a squash command cannot be applied because between branches.
   * 
   */
  public void performSquashMerge(final String currentBranch, final String selectedBranch, 
      final ObjectId sourceCommitObjectId) throws NoChangesInSquashedCommitException {
    mergeInfo.setText(MessageFormat.format(
        TRANSLATOR.getTranslation(Tags.SQUASH_MERGE_INFO),
        TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."),
        TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")));
    this.selectedBranch = selectedBranch;
    
    // This message computation should not affect the dialog
    String squashCommitMessage = null;
    try {
      squashCommitMessage = RepoUtil.computeSquashMessage(sourceCommitObjectId, 
          GitAccess.getInstance().getRepository());
    } catch (Exception expection) {
      squashCommitMessage = "";
    }
    
    if(!checkIfASquashCommitCanBeCreated(squashCommitMessage)) {
      throw new NoChangesInSquashedCommitException(MessageFormat.format(
          Translator.getInstance().getTranslation(Tags.SQUASH_NO_COMMITS_DETECTED_MESSAGE),
          TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")));
    }
   
    commitMessageTextArea.setText(squashCommitMessage);
   
    this.repaint();
    this.pack();
    this.setVisible(true);
  }

	
}
