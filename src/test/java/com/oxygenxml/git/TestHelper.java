package com.oxygenxml.git;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.extensions.jfcunit.WindowMonitor;
import junit.extensions.jfcunit.finder.ComponentFinder;

/**
 * Helper that contains generic methods for tests.
 * 
 * @author alex_smarandache
 */
public final class TestHelper {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestHelper.class);
  
  /**
   * Hidden constructor.
   */
  private TestHelper() {
    // not needed
  }
  
  /**
   * Sleep
   * 
   * @param time
   */
  public static void sleep(int time) {
    try {
      Thread.sleep(time); // NOSONAR
    } catch (InterruptedException e) {}
  }
  
  /**
   * Searches for the first button with the specified text in the container.
   * 
   * @param parent  The parent container.
   * @param index   The index of the button in the list of all buttons having that text.
   * @return        The button, or null if there is no button having that text.
   */
  public static JButton findFirstButton(Container parent, String text){
    JButton result = null;
    
    // Gets all the buttons.
    ComponentFinder cf = new ComponentFinder(JButton.class);
    List<Component> allButtons = cf.findAll(parent);
    
    // Selects the one with the given text.
    for (Iterator<Component> iterator = allButtons.iterator(); iterator.hasNext();) {
      JButton button = (JButton) iterator.next();
      boolean equals = button.getText() != null && button.getText().equals(text);
      if(equals){
        result = button;
        break;
      }
    }
    
    return result;      
  }
  
  /**
   * Searches for a visible dialog with the specified text in the title.
   * 
   * @param title The title of the dialog.
   * 
   * @return The dialog, or null if there is no dialog having that title.
   */
  public static JDialog findDialog(String title){

    final JDialog dialogToReturn[] = new JDialog[1];
    try {
      Awaitility.await().atMost(1250, TimeUnit.MILLISECONDS).until(() -> {
        
        // Get the opened windows
        final Window[] windows = WindowMonitor.getWindows();
        if (windows != null && windows.length > 0) {
          for (Window window : windows) { 
            if (window.isActive() && window instanceof JDialog) {
              JDialog dialog = (JDialog) window;
              String dialogTitle = dialog.getTitle();
              if (dialogTitle != null) {
                // If the dialog title is the same or starts with the given title
                // return this dialog
                if (title.equals(dialogTitle) || dialogTitle.startsWith(title)) {
                  dialogToReturn[0] = dialog;
                }
              }
            }
          }
        }                
        return Objects.nonNull(dialogToReturn[0]);
      });

    } catch(Exception e) {
      e.printStackTrace();
    }

    if(Objects.isNull(dialogToReturn[0])) {
      LOGGER.warn("Cannot find the dialog using the search string '" + title + "' - throttling..");
    }

    return dialogToReturn[0];
  }

}
