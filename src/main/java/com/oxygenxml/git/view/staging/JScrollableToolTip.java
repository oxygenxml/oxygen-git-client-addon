package com.oxygenxml.git.view.staging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;

/**
 * A tip tool that is scrollable.
 * 
 * @author Alex_Smarandache
 *
 */
public class JScrollableToolTip extends JToolTip implements MouseWheelListener {

  protected JEditorPane tipText;
  protected JScrollPane scrollpane;
  protected JComponent comp;
  
  Dimension smallText  = new Dimension(200, 75); 
  Dimension mediumText = new Dimension(300, 150); 
  Dimension largeText  = new Dimension(400, 300); 

  /** Creates a tool tip. */
  public JScrollableToolTip(final int width, final int height) {
    this(width, height, null);
  }

  public JScrollableToolTip(final int width, final int height, final JComponent comp) {
    this.comp = comp;
    setLayout(new BorderLayout());
    tipText = new JEditorPane();
    tipText.setEditable(false);
    tipText.setContentType("text/html");
    tipText.setBackground(super.getBackground());
    tipText.setFont(super.getFont());
    scrollpane = new JScrollPane(tipText);
    add(scrollpane);
    if(comp != null){
      comp.addMouseWheelListener(this);
    }
  }

  public void mouseWheelMoved(final MouseWheelEvent e) {
    scrollpane.dispatchEvent(e);
    MouseEvent e2 = new MouseEvent(comp, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false);
    comp.dispatchEvent(e2);
  }

  @Override
  public void setTipText(final String tipText) {
    String oldValue = this.tipText.getText();
    this.tipText.setText(tipText);
    if(tipText.length() <= 150) {
      setPreferredSize(smallText);
    } else if (tipText.length() <= 500) {
      setPreferredSize(mediumText);
    } else {
      setPreferredSize(largeText);
    }
    firePropertyChange("tiptext", oldValue, tipText);
  }
  
  @Override
  public String getTipText() {
    return tipText == null ? "" : tipText.getText();
  }

  @Override
  protected String paramString() {
    String tipTextString = (tipText.getText() != null ? tipText.getText() : "");

    return super.paramString() +
            ",tipText=" + tipTextString;
  }
  
}
