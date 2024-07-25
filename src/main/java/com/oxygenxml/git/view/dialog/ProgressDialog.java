package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.oxygenxml.git.view.actions.IProgressUpdater;
import com.oxygenxml.git.view.dialog.internal.OnDialogCancel;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * This class represents a progress dialog.
 * 
 * @author alex_smarandache
 */
@SuppressWarnings("java:S110")
public class ProgressDialog extends OKCancelDialog implements IProgressUpdater {

  /**
   * The progress bar component.
   */
  private JProgressBar progressBar;
  
  /**
   * The component to display the note label.
   */
  private JLabel noteLabel;
  
  /**
   * <code>true</code> if the operation is canceled.
   */
  private boolean isCancelled;
  
  /**
   * <code>true</code> if the operation is completed.
   */
  private boolean isCompleted = false;
  
  /**
   * The listener to be called when operation is canceled.
   */
  private OnDialogCancel cancelListener;

  /**
   * Constructor.
   * 
   * @param dialogTitle The title of the dialog.
   */
  public ProgressDialog(String dialogTitle) {
    super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(), "", true);
    setTitle(dialogTitle);
    initUI();
  }

  /**
   * Initialize the UI components. Remove all the old components is the method is called more times.
   */
  public void initUI() {
    this.getContentPane().removeAll();
    isCompleted = false;
    isCancelled = false;
    cancelListener = null;
    
    noteLabel = new JLabel(" ");

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
    gbc.insets = new Insets(10, 5, 10, 5);
    panel.add(noteLabel, gbc);

    add(panel);

    getOkButton().setVisible(false);

    setMinimumSize(new Dimension(400, 150));
    setResizable(false);
    pack();
  }

  /**
   * @param text Set the new note to be displayed.
   */
  @Override
  public void setNote(String text) {
    noteLabel.setText(text);
  }

  /**
   * Cancel the dialog.
   */
  @Override
  protected void doCancel() {
    isCancelled = true;
    Optional.ofNullable(cancelListener).ifPresent(OnDialogCancel::doOnCancel);
  }

  /**
   * @return <code>true</code> if the dialog is canceled.
   */
  @Override
  public boolean isCancelled() {
    return isCancelled;
  }
  
  /**
   * @param cancelListener The new listener to be called on cancel operation.
   */
  public void setCancelListener(OnDialogCancel cancelListener) {
    this.cancelListener = cancelListener;
  }

  /**
   * Mark the operation as completed.
   */
  @Override
  public void markAsCompleted() {
    this.isCompleted = true;
    SwingUtilities.invokeLater(() -> setVisible(false));
    
  }

  @Override
  public void setVisible(boolean visible) {
    if(!visible && !isCompleted) {
      isCancelled = true;
    }
    
    super.setVisible(visible);
  }
  /**
   *  @return <code>true</code> if the operation is completed.
   */
  @Override
  public boolean isCompleted() {
    return isCompleted;
  }
  
  /**
   * @param millis These milliseconds are used to not show the progress dialog for quickly operations.
   */
  public void show(long millis) {
    Executors.newSingleThreadScheduledExecutor().schedule(() -> {
      SwingUtilities.invokeLater(() -> {
        if(!isCancelled && !isCompleted) {
          setVisible(true);
        }
      });
    }, millis, TimeUnit.MILLISECONDS);
  }

}