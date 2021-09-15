package com.oxygenxml.git.service;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.PushPullEvent;

import junit.extensions.jfcunit.finder.ComponentFinder;

/**
 * Test utility methods.
 * 
 * @author alex_jitianu
 */
public class TestUtil {
  /**
   * Utility class.
   */
  private TestUtil() {}
  
  /**
   * Adds a listener that will collect push events.
   * 
   * @param pc Git controller.
   * @param b Event collector.
   */
  public static void collectPushPullEvents(GitController pc, final StringBuilder b) {
    pc.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: STARTED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: FINISHED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info instanceof PushPullEvent) {
          b.append("Status: FAILED, message: ").append(((PushPullEvent) info).getMessage()).append("\n");
        }
      }
    });
  }
  
  /**
   * Changes and commits one file.
   * 
   * @return The created revision.
   * @throws Exception If it fails.
   */
  public static final RevCommit commitOneFile(Repository repository, String fileName, String fileContent) throws Exception {
    try (Git git = new Git(repository)) {
      PrintWriter out = new PrintWriter(new File(repository.getWorkTree(), fileName));
      out.println(fileContent);
      out.close();
      git.add().addFilepattern(fileName).call();
      return git.commit().setMessage("New file: " + fileName).call();
    }
  }
  
  /**
   * Search for a button that contains a specific text
   * 
   * @param container The container with components
   * @param buttonText The text that button should contain
   * 
   * @return a JButton or null if not found
   */
  public static JButton findButton(Container container, String buttonText) {
    Component[] components = container.getComponents();

    for (Component component : components) {
      
      if (component instanceof JButton) {
        if( ((JButton) component).getText().contains(buttonText) ) {
          return (JButton) component;
        }
      }

      if (component instanceof Container) {
        JButton buttonSearched = findButton((Container) component, buttonText);
        if (buttonSearched != null) {
          return buttonSearched;
        }
      }
    }
    return null;
  }
  
  /**
   * Searches for the first text field.
   * 
   * @param parent  The parent container.
   * @param index   The index of the button in the list of all buttons having that text.
   * @return        The text field, or null if there is no button having that text.
   */
  public static JTextField findFirstTextField(Container parent) {
    JTextField result = null;

    ComponentFinder cf = new ComponentFinder(JTextField.class);
    @SuppressWarnings("unchecked")
    List<Component> allTextFields = cf.findAll(parent);

    // Selects the one with the given text.
    for (Iterator<Component> iterator = allTextFields.iterator(); iterator.hasNext();) {
      JTextField textField = (JTextField) iterator.next();
      boolean equals = textField.getText() != null ;
      if (equals) {
        result = textField;
        break;
      }
    }

    return result;
  }
  
  
  /**
   * Gets the content from a given URL.
   * 
   * @param url The URL from where to read.
   * 
   * @return The content, never <code>null</code>.
   */
  @SuppressWarnings("unused")
  public final static String read(URL url) throws IOException {
    String result = null;
    try (
        // Java will try to automatically close each of the declared resources
        InputStream openedStream = url.openStream();
        InputStreamReader inputStreamReader = new InputStreamReader(openedStream, "UTF-8")) {
      result = read(inputStreamReader);
    } catch (IOException e) {
      if (result == null) {
        throw e;
      } else {
        // Just some info about this error, the method will return the result.
        e.printStackTrace();
      }
    }
    return result != null ? result.trim() : null;
  }
  
  /**
   * Reads all the content from a given reader.
   * 
   * @param isr The reader.
   *
   * @return The content.
   *
   * @throws IOException If cannot read.
   */
  private static String read(InputStreamReader isr) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    char[] buf = new char[1024];
    int length = -1;
    while ((length = isr.read(buf)) != -1) {
      stringBuilder.append(buf, 0, length);
    }
    return stringBuilder.toString();
  }
}
