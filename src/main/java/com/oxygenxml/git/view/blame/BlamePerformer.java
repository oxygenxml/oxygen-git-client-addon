package com.oxygenxml.git.view.blame;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.utils.Equaler;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

/**
 * Computes the blame information and installs highlights on the tet page. 
 */
public class BlamePerformer {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(BlamePerformer.class);
  /**
   * The current active commit. The one that contained the caret line. 
   */
  protected RevCommit activeRevCommit;
  /**
   * Timer to sync the caret with a revision in the history view.
   */
  private  Timer caretSyncTimer = new Timer();
  /**
   * Task to sync the caret with a revision in the history view.
   */
  private  TimerTask caretSyncTask;
  /**
   * Revision ID to Painter mapping.
   */
  private  Map<String, HighlightPainter> painters = new HashMap<>();
  
  /**
   * Highlight to Revision mapping.
   */
  private  Map<Object, RevCommit> highlightsToRevCommits = new HashMap<>();
  
  /**
   * Line to Revision mapping.
   */
  private  Map<Integer, RevCommit> lineIndicesToRevCommits = new HashMap<>();
  /**
   * The bind text page.
   */
  private WSTextEditorPage textpage;
  /**
   * Focus listener added on the text page.
   */
  private FocusAdapter focusListener;
  /**
   * Caret listener added on the text page.
   */
  private CaretListener caretListener;
  /**
   * Random number generator.
   */
  private Random rand = new SecureRandom();
  
  /**
   * Computes the blame for the given resource and adds highlights on the editor.
   * 
   * @param repository The repository that contains the file.
   * @param filePath File for which to compute the path.
   * @param editor Editor that presents the file.
   * @param historyController Interface to history support.
   * 
   * @throws GitAPIException Git related issues.
   */
  public  void doit(
      Repository repository, 
      String filePath, 
      final WSEditor editor, 
      HistoryController historyController) throws GitAPIException {
    
    // Currently we only support text page highlights.
    editor.changePage(EditorPageConstants.PAGE_TEXT);
    
    WSEditorPage currentPage = editor.getCurrentPage();
    if (currentPage instanceof WSTextEditorPage) {
      doBlame(repository, filePath, historyController, (WSTextEditorPage) currentPage);
    }
    
    // Present the history for the given resource.
    historyController.showResourceHistory(filePath);
  }

  /**
   * Computes the blame for the given resource and adds highlights on the editor.
   * 
   * @param repository The repository that contains the file.
   * @param filePath File for which to compute the path.
   * @param historyController Interface to history support.
   * @param currentPage Editor that presents the file.
   * 
   * @throws GitAPIException if it fails.
   */
  private void doBlame(
      Repository repository,
      String filePath, 
      HistoryController historyController, 
      WSTextEditorPage currentPage) throws GitAPIException {
    BlameCommand blamer = new BlameCommand(repository);
    
    // This is how you do it on a specific commit. If left out, it's performed on the WC instance.
//    ObjectId commitID = repository.resolve("HEAD~~"); NOSONAR
//    blamer.setStartCommit(commitID); NOSONAR
    blamer.setFilePath(filePath);
    BlameResult blame = blamer.call();
    textpage = currentPage;
    JTextArea textArea = (JTextArea) textpage.getTextComponent();
    Highlighter highlighter = textArea.getHighlighter();
    
    // Add highlights for each interval.
    int lines = blame.getResultContents().size();
    for (int i = 0; i < lines; i++) {
      RevCommit commit = blame.getSourceCommit(i);
      
      lineIndicesToRevCommits.put(i, commit);

      if (commit != null) {
        try {
          int offsetOfLineStart = textpage.getOffsetOfLineStart(i + 1);
          int offsetOfLineEnd = textpage.getOffsetOfLineEnd(i + 1);

          Object addHighlight = highlighter.addHighlight(offsetOfLineStart, offsetOfLineEnd, getPainter(commit, textpage));
          highlightsToRevCommits.put(addHighlight, commit);
        } catch (BadLocationException e) {
          LOGGER.error(e, e);
        }
      }
    }
    
    installSyncListeners(filePath, historyController, textArea);
  }

  /**
   * Installs various listeners to synchronized between caret and revision.
   * 
   * @param filePath File for which to compute the path.
   * @param historyController Interface to history support.
   * @param textArea Text page that presents the file.
   */
  private void installSyncListeners(String filePath, HistoryController historyController, JTextArea textArea) {
    focusListener = new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        cancelCaretSyncTask();
      }
    };
    textArea.addFocusListener(focusListener);
    
    // When the caret changes we sync with the history view.
    caretListener = e -> {
      cancelCaretSyncTask();
      
      caretSyncTask = new TimerTask() {
        @Override
        public void run() {
          syncCaretWithHistory(filePath, historyController, textArea, e.getDot());
        }
      };
      
      caretSyncTimer.schedule(caretSyncTask, 400);
    };
    textArea.addCaretListener(caretListener);
    
    syncCaretWithHistory(filePath, historyController, textArea, textArea.getCaretPosition());
  }
  
  /**
   * Cancel the sync task, if any.
   */
  private void cancelCaretSyncTask() {
    if (caretSyncTask != null) {
      caretSyncTask.cancel();
    }
  }
  
  /**
   * Synchronizes the history view with the caret position.
   * 
   * @param filePath The file we show blame for.
   * @param historyController History controller.
   * @param textArea The text area presenting the content of the file.
   * @param caret Caret position.
   */
  private void syncCaretWithHistory(String filePath, HistoryController historyController, JTextArea textArea, int caret) {
    try {
      int line = textpage.getLineOfOffset(caret);
      
      RevCommit nextRevCommit = lineIndicesToRevCommits.get(line - 1);
      // The active highlight might have changed.
      
      if (!Equaler.verifyEquals(activeRevCommit , nextRevCommit)) {
        activeRevCommit = nextRevCommit;
        textArea.repaint();

        SwingUtilities.invokeLater(() -> {historyController.showCommit(filePath, activeRevCommit);});
        
      }
    } catch (BadLocationException e1) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e1, e1);
      }
    }
  }

  /**
   * Get a painter for the given commit.
   * 
   * @param rev Revision.
   * @param textpage Text page.
   * 
   * @return A painter for the given revision.
   */
  private  HighlightPainter getPainter(RevCommit rev, WSTextEditorPage textpage) {
    return painters.computeIfAbsent(rev.getId().name(), name1 -> {
      float r = rand.nextFloat();
      float g = rand.nextFloat();
      float b = rand.nextFloat();
      Color randomColor = new Color(r, g, b, (float) 0.4);
      return new CommitHighlightPainter(randomColor, textpage, lineIndicesToRevCommits, () -> activeRevCommit);
    });
  }

  /**
   * Clears all internal buffers and removes all listeners added on the editor.
   */
  public void dispose() {
    if (textpage != null) {
      cancelCaretSyncTask();
      
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info("Dispose " + textpage.getParentEditor().getEditorLocation());
      }
      
      JTextArea textArea = (JTextArea) textpage.getTextComponent();
      Highlighter highlighter = textArea.getHighlighter();
      
      Iterator<Object> iterator = highlightsToRevCommits.keySet().iterator();
      while (iterator.hasNext()) {
        Object h = iterator.next();
        highlighter.removeHighlight(h);
      }
      
      if (focusListener != null) {
        textArea.removeFocusListener(focusListener);
      }
      
      if (caretListener != null) {
        textArea.removeCaretListener(caretListener);
      }
    }
  }
}