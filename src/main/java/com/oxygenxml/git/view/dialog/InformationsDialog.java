package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Shows an information dialog
 * 
 * @author Tudosie Razvan
 *
 */
@SuppressWarnings("java:S110")
public class InformationsDialog extends OKCancelDialog {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(InformationsDialog.class.getName());

  /**
   * The preferred width of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_WIDTH = 300;

  /**
   * The preferred eight of the scroll pane for the files list.
   */
  private static final int MESSAGE_SCROLLPANE_PREFERRED_HEIGHT = 100;

  /**
   * Minimum width.
   */
  private static final int WARN_MESSAGE_DLG_MINIMUM_WIDTH = 300;

  /**
   * Minimum height.
   */
  private static final int WARN_MESSAGE_DLG_MINIMUM_HEIGHT = 150;

  /**
   * Document with custom wrapping.
   */
  private static class CustomWrapDocument extends DefaultStyledDocument {
    /**
     * Maximum number of characters without a newline.
     */
    private static final int MAX_NO_OF_CHARS_PER_LINE = 100;

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      StringBuilder sb = new StringBuilder();
      int charsSinceLastNewline = 0;
      char[] charArray = str.toCharArray();
      for (char ch : charArray) {
        if (charsSinceLastNewline >= MAX_NO_OF_CHARS_PER_LINE) {
          if (Character.isWhitespace(ch)) {
            sb.append('\n');
            charsSinceLastNewline = 0;
          } else {
            sb.append(ch);
          }
        } else {
          if (ch == '\n') {
            charsSinceLastNewline = 0;
          }
          sb.append(ch);
        }
        charsSinceLastNewline++;
      }
      super.insertString(offs, sb.toString(), a);
    }
  }

  /**
   * Constructor.
   * 
   * @param iconPath         Icon path.
   * @param title            Dialog title.
   * @param message      A message that needs to be displayed
   * @param descriptionMessage          An description of the message. May be <code>null</code>.
   * @param lastMessage  A question message connected to the presented information. May be <code>null</code>.
   * @param okButtonName     Text to be written on the button in case the answer to the question is affirmative
   * @param cancelButtonName Text to be written on the button in case the answer to the question is negative
   */
  private InformationsDialog(
      String iconPath,
      String title,
      String message,
      List<String> descriptionMessage,
      String lastMessage,
      String okButtonName,
      String cancelButtonName) {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? 
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        title,
        true);
    
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    // Icon
    JLabel iconLabel = new JLabel();
    URL iconURL =  PluginWorkspaceProvider.class.getResource(iconPath);
    if (iconURL != null) {
      ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
      Icon icon = (Icon) imageUtilities.loadIcon(iconURL);
      iconLabel.setIcon(icon);
    }
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        10);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    panel.add(iconLabel, gbc);
    
    // DescriptionMessage
    if (descriptionMessage != null) {
      JPanel descriptionPanel=new JPanel(new GridBagLayout());
      GridBagConstraints descriptionConstraints = new GridBagConstraints();
      descriptionConstraints.anchor = GridBagConstraints.NORTHWEST;
      descriptionConstraints.weightx = 1;
      descriptionConstraints.weighty = 1;
      descriptionConstraints.gridx = 0;
      descriptionConstraints.gridy = 0;
      descriptionConstraints.fill = GridBagConstraints.HORIZONTAL;
      descriptionPanel.add(new JLabel("<html>"+descriptionMessage.get(0)+"</html>"),descriptionConstraints);
      
      descriptionConstraints.gridy++;
      descriptionConstraints.insets=new Insets(12,0,0,0);
      for(int i=1; i<descriptionMessage.size(); i++)
      {
        descriptionPanel.add(new JLabel("<html>"+descriptionMessage.get(i)+"</html>"),descriptionConstraints);
        if(i==1)
        {
          descriptionConstraints.insets=new Insets(4,0,0,0);
        }
        descriptionConstraints.gridy++;
      }
      
      
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(descriptionPanel, gbc);
      
      gbc.gridy++;
    }
    
    // Message
    if (message != null) {
      
     
      JTextArea messageArea=new JTextArea();
      messageArea.setText(message);
      messageArea.setLineWrap(true);
      messageArea.setEditable(false);             
      JScrollPane scollPane = new JScrollPane(messageArea);
      scollPane.setPreferredSize(new Dimension(MESSAGE_SCROLLPANE_PREFERRED_WIDTH, MESSAGE_SCROLLPANE_PREFERRED_HEIGHT));
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      gbc.insets = new Insets( -5, 
        0,
        0, 
        0);
      panel.add(scollPane, gbc);
      
      gbc.gridy++;
      gbc.insets = new Insets(
          UIConstants.COMPONENT_TOP_PADDING, 
          UIConstants.COMPONENT_LEFT_PADDING,
          UIConstants.COMPONENT_BOTTOM_PADDING, 
          10);
    }
    
    if (lastMessage == null) {
      getCancelButton().setVisible(false);
    } else {
      gbc.gridy++;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridx = 1;
      gbc.gridheight = 1;
      panel.add(new JLabel("<html>"+lastMessage+"</html>"), gbc);
    }
    
    getContentPane().add(panel);
    setResizable(false);
    pack();
    
    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  }

  /**
   * Shows an information
   * 
   * @param title Dialog title.
   * @param name  of files/data that needs to be shown The information message to
   *              be presented.
   * @return The option chosen by the user. {@link #RESULT_OK} or
   *         {@link #RESULT_CANCEL}
   */
  public static void showInformationMessage(String title, String message,List<String> description, String lastMessage) {
    InformationsDialog dialog = new InformationsDialog(Icons.INFO_ICON, title, message, description, lastMessage, null, null);
    dialog.setResizable(true);
    dialog.setMinimumSize(new Dimension(WARN_MESSAGE_DLG_MINIMUM_WIDTH,
     WARN_MESSAGE_DLG_MINIMUM_HEIGHT));
    dialog.setVisible(true);

  }
}
