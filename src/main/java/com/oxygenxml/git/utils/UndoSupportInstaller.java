package com.oxygenxml.git.utils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.log4j.Logger;

import ro.sync.util.PlatformDetector;

/**
 * Installs the UNDO/REDO support on a text component. 
 * @author alex_jitianu
 */
public class UndoSupportInstaller {
  
  /**
   * Hidden constructor. 
   */
  private UndoSupportInstaller() {
    // Nothing
  }

  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(UndoSupportInstaller.MyCompoundEdit.class.getName());

  private static class MyCompoundEdit extends CompoundEdit {
  	boolean isUnDone = false;
  
  	public int getLength() {
  		return edits.size();
  	}
  
  	@Override
  	public void undo() {
  		super.undo();
  		isUnDone = true;
  	}
  
  	@Override
  	public void redo() {
  		super.redo();
  		isUnDone = false;
  	}
  
  	@Override
  	public boolean canUndo() {
  		return !edits.isEmpty() && !isUnDone;
  	}
  
  	@Override
  	public boolean canRedo() {
  		return !edits.isEmpty() && isUnDone;
  	}
  
  }

  private static class UndoManager extends AbstractUndoableEdit implements UndoableEditListener {
  	String lastEditName = null;
  	int lastOffset = -1;
  	ArrayList<MyCompoundEdit> edits = new ArrayList<MyCompoundEdit>();
  	MyCompoundEdit current;
  	int pointer = -1;
  	@Override
  	public void undoableEditHappened(UndoableEditEvent e) {
  		UndoableEdit edit = e.getEdit();
  		if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
  			try {
  				AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) edit;
  				int start = event.getOffset();
  				int len = event.getLength();
  				String text = "";
  				if ("addition".equals(edit.getPresentationName())) {
  					text = event.getDocument().getText(start, len);
  				}
  				boolean isNeedStart = false;
  				if (current == null
  				    || lastEditName == null 
  				    || !lastEditName.equals(edit.getPresentationName())
  				    || text.contains("\n") && !"deletion".equals(edit.getPresentationName())
  				    || Math.abs(lastOffset - start) > 1) {
  					isNeedStart = true;
  				}
  
  				while (pointer < edits.size() - 1) {
  					edits.remove(edits.size() - 1);
  					isNeedStart = true;
  				}
  				if (isNeedStart) {
  					createCompoundEdit();
  				}
  
  				current.addEdit(edit);
  				lastEditName = edit.getPresentationName();
  				lastOffset = start;
  
  			} catch (BadLocationException e1) {
  				if (logger.isDebugEnabled()) {
  				  logger.debug(e1, e1);
  				}
  			}
  		}
  	}
  
  	public void createCompoundEdit() {
  		if (current == null || current.getLength() > 0) {
  			current = new MyCompoundEdit();
  		}
  
  		edits.add(current);
  		pointer++;
  	}
  
  	@Override
  	public void undo() {
  		if (!canUndo()) {
  			throw new CannotUndoException();
  		}
  
  		MyCompoundEdit u = edits.get(pointer);
  		u.undo();
  		pointer--;
  
  	}
  
  	@Override
  	public void redo() {
  		if (!canRedo()) {
  			throw new CannotUndoException();
  		}
  
  		pointer++;
  		MyCompoundEdit u = edits.get(pointer);
  		u.redo();
  
  	}
  
  	@Override
  	public boolean canUndo() {
  		return pointer >= 0;
  	}
  
  	@Override
  	public boolean canRedo() {
  		return !edits.isEmpty() && pointer < edits.size() - 1;
  	}
  
  }

  public static void installUndoManager(JTextComponent commitMessage) {
    final UndoManager undoManager = new UndoManager();
  	Document doc = commitMessage.getDocument();
  	// Listen for undo and redo events
  	doc.addUndoableEditListener(undoManager);
  
  	// Create an undo action and add it to the text component
  	commitMessage.getActionMap().put("Undo", new AbstractAction("Undo") {
  		public void actionPerformed(ActionEvent evt) {
  			if (undoManager.canUndo()) {
  				undoManager.undo();
  			}
  		}
  	});
  
  	// Bind the undo action to ctl-Z
  	int modifier = PlatformDetector.isMacOS() ? KeyEvent.META_DOWN_MASK:KeyEvent.CTRL_DOWN_MASK;
  	
  	commitMessage.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier), "Undo");
  
  	// Create a redo action and add it to the text component
  	commitMessage.getActionMap().put("Redo", new AbstractAction("Redo") {
  		public void actionPerformed(ActionEvent evt) {
  			if (undoManager.canRedo()) {
  				undoManager.redo();
  			}
  		}
  	});
  
  	// Bind the redo action to ctl-Y
  	commitMessage.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier), "Redo");
  }

}
