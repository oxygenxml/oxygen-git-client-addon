package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.HiDPIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;

/**
 * Dialog shown when an operation is tried to be performed
 * while the repo has a rebasing in progress.
 */
public class RebaseInProgressDialog extends JDialog {
  /**
   * i18n
   */
  private static Translator translator = Translator.getInstance();
  /**
   * <code>true</code> if the dialog has been uupdated in regards to HiDPI dimensions and stuff.
   */
  private boolean dlgHiDPIUpdated;

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
      GitAccess.getInstance().continueRebase();
    };
  }
  
  /**
   * @return "Abort rebase" action listener.
   */
  private ActionListener createAbortRebaseActionListener() {
    return e -> {
      setVisible(false);
      GitAccess.getInstance().abortRebase();
    };
  }
  
  /**
   * @return Cancel action listener.
   */
  private ActionListener createCancelActionListener() {
    return e -> setVisible(false);
  }
  
  /**
   * @see java.awt.Window#pack()
   */
  @Override
  public void pack() {
    if (!dlgHiDPIUpdated && HiDPIUtil.isRetinaNoImplicitSupport()) {
      HiDPIUtil.updateComponentsForHiDPI(getContentPane(), true);
      dlgHiDPIUpdated = true;
    }
    super.pack();
  }
  
  /**
   * @see java.awt.Window#setSize(int, int)
   */
  @Override
  public void setSize(int width, int height) {
    Dimension newDim = getHiDPIAwareDimension(new Dimension(width, height));
    super.setSize(newDim.width, newDim.height);
  }
  
  /**
   * @see java.awt.Component#setPreferredSize(java.awt.Dimension)
   */
  @Override
  public void setPreferredSize(Dimension preferredSize) {
    super.setPreferredSize(getHiDPIAwareDimension(preferredSize));
  }
  
  /**
   * Get the high DPI aware dimension.
   * 
   * @param initialDimension The initial dimension.
   * 
   * @return The computed dimension if we have a high DPI screen with no implicit support form the OS (e.g. Windows),
   *     or the initial dimension otherwise. 
   */
  private Dimension getHiDPIAwareDimension(Dimension initialDimension) {
    Dimension dim = initialDimension;
    if (HiDPIUtil.isRetinaNoImplicitSupport()) {
      if (!dlgHiDPIUpdated) {
        HiDPIUtil.updateComponentsForHiDPI(getContentPane(), true);
        dlgHiDPIUpdated = true;
      }
      dim = HiDPIUtil.computeHiDPIDimensionForNoImplicitSupport(initialDimension, getPreferredSize());
    }
    return dim;
  }
  
  /**
   * @see java.awt.Dialog#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    if (visible
        && !dlgHiDPIUpdated
        && HiDPIUtil.isRetinaNoImplicitSupport()) {
      HiDPIUtil.updateComponentsForHiDPI(getContentPane(), true);
      dlgHiDPIUpdated = true;
    }
    super.setVisible(visible);
  }
  
}
