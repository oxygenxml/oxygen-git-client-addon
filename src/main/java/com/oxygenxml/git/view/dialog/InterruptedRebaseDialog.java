package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.HiDPIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.ui.hidpi.RetinaDetector;

/**
 * Dialog shown when an operation is tried to be performed
 * while the repo has a rebasing in progress.
 */
public class InterruptedRebaseDialog extends JDialog {
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
  public InterruptedRebaseDialog() {
    super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        translator.getTranslation(Tags.REBASE_IN_PROGRESS),
        true);
    
    JFrame parentFrame = (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame();
    setIconImage(parentFrame.getIconImage());
    
    createGUI();

    setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(parentFrame);
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
    Button continueRebaseButton = new Button("Continue rebase");
    continueRebaseButton.addActionListener(createContinueActionListener());
    buttonsPanel.add(continueRebaseButton);
    Button abortRebaseButton = new Button("Abort rebase");
    abortRebaseButton.addActionListener(createAbortRebaseActionListener());
    buttonsPanel.add(abortRebaseButton);
    Button cancelButton = new Button("Cancel");
    cancelButton.addActionListener(createCancelActionListener());
    buttonsPanel.add(cancelButton);
    mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
    
    pack();
  }
  
  /**
   * @return "Continue rebase" action listener.
   */
  private ActionListener createContinueActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
        GitAccess.getInstance().continueRebase();
      }
    };
  }
  
  /**
   * @return "Abort rebase" action listener.
   */
  private ActionListener createAbortRebaseActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
        GitAccess.getInstance().abortRebase();
      }
    };
  }
  
  /**
   * @return Cancel action listener.
   */
  private ActionListener createCancelActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    };
  }
  
  /**
   * @see java.awt.Window#pack()
   */
  @Override
  public void pack() {
    if (!dlgHiDPIUpdated && RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
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
    if (RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
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
    if (visible) {
      if (!dlgHiDPIUpdated) {
        if (RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
          HiDPIUtil.updateComponentsForHiDPI(getContentPane(), true);
          dlgHiDPIUpdated = true;
        }
      }
    }
    super.setVisible(visible);
  }
  
}
