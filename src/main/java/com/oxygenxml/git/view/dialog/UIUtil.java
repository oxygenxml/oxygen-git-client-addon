package com.oxygenxml.git.view.dialog;

import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.oxygenxml.git.utils.UndoSupportInstaller;

/**
 * Utility class for UI-related issues. 
 */
public class UIUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(UIUtil.class.getName());
  
  /**
   * Hidden constructor.
   */
  private UIUtil() {
    // Nothing
  }
  
  /**
   * This method first tries to create an undoable text field,
   * that also has a contextual menu with editing action. If this is not possible,
   * a standard Java text field is created.
   * 
   * @return the text field.
   */
  public static JTextField createTextField() {
    JTextField textField = null;
    try {
      Class<?> textFieldClass= Class.forName("ro.sync.exml.workspace.api.standalone.ui.TextField");
      textField = (JTextField) textFieldClass.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
      textField = new JTextField();
      UndoSupportInstaller.installUndoManager(textField);
      if (logger.isDebugEnabled()) {
        logger.debug(e1, e1);
      }
    }
    return textField;
  }

}
