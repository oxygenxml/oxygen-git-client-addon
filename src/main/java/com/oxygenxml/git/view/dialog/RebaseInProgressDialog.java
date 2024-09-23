package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;

/**
 * Dialog shown when an operation is tried to be performed
 * while the repo has a rebasing in progress.
 */
@SuppressWarnings("java:S110")
public class RebaseInProgressDialog extends JDialog {
  /**
   * i18n
   */
  private static Translator translator = Translator.getInstance();

  /**
   * Constructor.
   */
  public RebaseInProgressDialog() {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        translator.getTranslation(Tags.REBASE_IN_PROGRESS),
        true);
    
    JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
    
    if (parentFrame != null) {
      setIconImage(parentFrame.getIconImage());
    }
    
    createGUI();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    if (parentFrame != null) {
      setLocationRelativeTo(parentFrame);
    }
    setSize(new Dimension(475, getPreferredSize().height));
    setResizable(false);
    
  }
  
  /**
   * Create GUI.
   */
  private void createGUI() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    getContentPane().add(mainPanel);
    
    JTextArea messageArea = new JTextArea(translator.getTranslation(Tags.INTERRUPTED_REBASE));
    messageArea.setEditable(false);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setBackground(mainPanel.getBackground());
    messageArea.setBorder(BorderFactory.createEmptyBorder(10, 7, 0, 7));
    mainPanel.add(messageArea, BorderLayout.NORTH);
    
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 7));
    Button continueRebaseButton = new Button(translator.getTranslation(Tags.CONTINUE_REBASE));
    continueRebaseButton.addActionListener(createContinueRebaseActionListener());
    buttonsPanel.add(continueRebaseButton);
    Button abortRebaseButton = new Button(translator.getTranslation(Tags.ABORT_REBASE));
    abortRebaseButton.addActionListener(createAbortRebaseActionListener());
    buttonsPanel.add(abortRebaseButton);
    Button cancelButton = new Button(translator.getTranslation(Tags.CANCEL));
    cancelButton.addActionListener(createCancelActionListener());
    buttonsPanel.add(cancelButton);
    mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
    
    pack();
  }
  
  /**
   * @return "Continue rebase" action listener.
   */
  private ActionListener createContinueRebaseActionListener() {
    return e -> {
      setVisible(false);
      GitAccess.getInstance().continueRebase(Optional.of(new GitOperationProgressMonitor(new ProgressDialog(Translator.getInstance().getTranslation(Tags.CONTINUE_REBASE), true))));
    };
  }
  
  /**
   * @return "Abort rebase" action listener.
   */
  private ActionListener createAbortRebaseActionListener() {
    return e -> {
      setVisible(false);
      GitAccess.getInstance().abortRebase(Optional.of(new GitOperationProgressMonitor(new ProgressDialog(Translator.getInstance().getTranslation(Tags.ABORT_REBASE), true))));
    };
  }
  
  /**
   * @return Cancel action listener.
   */
  private ActionListener createCancelActionListener() {
    return e -> setVisible(false);
  }
}
