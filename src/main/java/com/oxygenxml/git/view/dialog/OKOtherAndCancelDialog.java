package com.oxygenxml.git.view.dialog;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PlatformDetectionUtil;

import ro.sync.exml.workspace.api.standalone.ui.Button;

/**
 * A dialog with two options buttons and a cancel button.
 * 
 * @author Alex_Smarandache
 */
public class OKOtherAndCancelDialog extends JDialog {

  /**
   * The button for the OK option.
   */
  private final Button okButton = new Button(Translator.getInstance().getTranslation(Tags.OK));

  /**
   * The button for the "Other" option.
   */
  private final Button otherButton = new Button(Translator.getInstance().getTranslation(Tags.OTHER));

  /**
   * The "Cancel" button.
   */
  private final Button cancelButton = new Button(Translator.getInstance().getTranslation(Tags.CANCEL));
  
  /**
   * The dimension for MacOS devices.
   */
  private static final Dimension MAC_OS_DIMENSION = new Dimension(80, 26);

  /**
   * The dimension for Windows devices.
   */
  private static final Dimension WIN_OS_DIMENSION = new Dimension(75, 23);

  /**
   * The dimension for Other platforms devices.
   */
  private static final Dimension OTHER_OS_DIMENSION = new Dimension(85, 23);
  
  /**
   * The MacOS width displacement.
   */
  private static final int MAC_OS_WIDTH_DISPLACEMENT = 15;
  
  /**
   * The MacOS height displacement.
   */
  private static final int MAC_OS_HEIGHT_DISPLACEMENT = 4;
  
  /**
   * The answer for current dialog.
   */
  private int answer = RESULT_CANCEL;

  /**
   * The panel with the content for dialog.
   */
  private final JPanel contentPanel = new JPanel(new GridBagLayout());

  /**
   * Size of the buttons.
   */
  private final Dimension buttonsSize;
  
  /**
   * The result for cancel button.
   */
  public static final int RESULT_CANCEL = 0;

  /**
   * The result for OK button.
   */
  public static final int RESULT_OK = 1;

  /**
   * The result for Other button.
   */
  public static final int RESULT_OTHER = 2;


  /**
   * The constructor.
   *
   * @param parentFrame  The parent frame.
   * @param title        The title of dialog.
   * @param modal        <code>true</code> if the dialog is modal.
   */
  public OKOtherAndCancelDialog(Frame parentFrame, String title, boolean modal) {
    super(parentFrame, title, modal ? Dialog.DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);

    if (PlatformDetectionUtil.isWin()) {
      buttonsSize = WIN_OS_DIMENSION;
    } else if (PlatformDetectionUtil.isMacOS()) {
      buttonsSize = MAC_OS_DIMENSION;
    } else {
      buttonsSize = OTHER_OS_DIMENSION;
    }

    //The OK button
    getRootPane().setDefaultButton(okButton);

    //Listener for ESC pressed
    Action cancelAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(cancelButton.isEnabled()) {
          answer = RESULT_CANCEL;
          dispose();
        }
      }
    };
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelAction);
    getRootPane().getActionMap().put(cancelAction, cancelAction);

    int bw = PlatformDetectionUtil.isMacOS() ? UIConstants.DLG_MARGIN_GAP_MAC 
        : UIConstants.DLG_MARGIN_GAP_WIN;

    JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(bw, bw, bw, bw));
    GridBagConstraints gridBagConstr = new GridBagConstraints();
    gridBagConstr.gridx = 0;
    gridBagConstr.gridy = 0;
    gridBagConstr.fill = GridBagConstraints.BOTH;
    gridBagConstr.weightx = 1;
    gridBagConstr.weighty = 1;
    gridBagConstr.anchor = GridBagConstraints.WEST;
    gridBagConstr.gridwidth = 1;
    gridBagConstr.gridheight = 1;
    mainPanel.add(contentPanel, gridBagConstr);

    // Buttons panel
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

    okButton.addActionListener(e -> {
      answer = RESULT_OK;
      dispose();
    });
    
    otherButton.addActionListener(e -> {
      answer = RESULT_OTHER;
      dispose();
    });

    cancelButton.addActionListener(e -> {
      answer = RESULT_CANCEL;
      dispose();
    });

    buttonsPanel.add(Box.createHorizontalGlue());
    if (PlatformDetectionUtil.isMacOS()) {
      buttonsPanel.add(cancelButton);
      buttonsPanel.add(Box.createHorizontalStrut(UIConstants.HGAP_MAC));
      buttonsPanel.add(otherButton);
      buttonsPanel.add(Box.createHorizontalStrut(UIConstants.HGAP_MAC));
      buttonsPanel.add(okButton);
    } else {
      buttonsPanel.add(okButton);
      buttonsPanel.add(Box.createHorizontalStrut(UIConstants.HGAP_WIN));
      buttonsPanel.add(otherButton);
      buttonsPanel.add(Box.createHorizontalStrut(UIConstants.HGAP_WIN));
      buttonsPanel.add(cancelButton);
    }

    gridBagConstr.gridx = 0;
    gridBagConstr.gridy++;
    gridBagConstr.gridwidth = 1;
    gridBagConstr.gridheight = 1;
    gridBagConstr.weightx = 1;
    gridBagConstr.weighty = 0;
    gridBagConstr.anchor = GridBagConstraints.EAST;
    gridBagConstr.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstr.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, 0, 0, 0);
    mainPanel.add(buttonsPanel, gridBagConstr);

    setContentPane(mainPanel);

    setResizable(false);
  }


  /**
   * Sets the text for a button.
   * If possible it imposes a preferred size.
   *
   * @param button The button to set text for.
   * @param text The button text.
   */
  public void setButtonText(JButton button, String text) {
    if (text != null && text.length() > 0) {
      int textWidth = button.getFontMetrics(button.getFont()).stringWidth(text);
      int textHeight = button.getFontMetrics(button.getFont()).getHeight();
      int allocW = buttonsSize.width - button.getMargin().left - button.getMargin().right;
      int allocH = buttonsSize.height - button.getMargin().top - button.getMargin().bottom;
      if (PlatformDetectionUtil.isMacOS()) {
        allocW -= MAC_OS_WIDTH_DISPLACEMENT;
        allocH -= MAC_OS_HEIGHT_DISPLACEMENT;
      }
      if (textWidth <= allocW - MAC_OS_HEIGHT_DISPLACEMENT && textHeight <= allocH) {
        button.setPreferredSize(buttonsSize);
      } else {
        button.setPreferredSize(null);
      }
      button.setText(text);
    }
  }


  /**
   * @return ok button.
   */
  public Button getOKButton() {
    return okButton;
  }


  /**
   * @return button for second option.
   */
  public Button getOtherButton() {
    return otherButton;
  }


  /**
   * @return cancel button.
   * */
  public Button getCancelButton() {
    return cancelButton;
  }


  /**
   * @return the dialog answer.
   */
  public int getResult() {
    return answer;
  }


  /**
   * @return the panel content.
   */
  public JPanel getContentPanel() {
    return contentPanel;
  }

}
