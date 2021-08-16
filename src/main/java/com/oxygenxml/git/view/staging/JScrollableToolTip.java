package com.oxygenxml.git.view.staging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;

/**
 * A tip tool that is scrollable.
 * 
 * @author Alex_Smarandache
 *
 */
public class JScrollableToolTip extends JToolTip implements MouseWheelListener {

  private JTextArea tipArea;
  private JScrollPane scrollpane;
  private JComponent comp;

  /** Creates a tool tip. */
  public JScrollableToolTip(final int width, final int height) {
    this(width, height, null);
  }

  public JScrollableToolTip(final int width, final int height, final JComponent comp) {
    this.comp = comp;
    setPreferredSize(new Dimension(width, height));
    setLayout(new BorderLayout());
    tipArea = new JTextArea();
    tipArea.setLineWrap(true);
    tipArea.setWrapStyleWord(true);
    tipArea.setEditable(false);
    tipArea.setBackground(super.getBackground());
    tipArea.setFont(super.getFont());
    scrollpane = new JScrollPane(tipArea);
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
    String oldValue = this.tipArea.getText();
    tipArea.setText(tipText);
    tipArea.setCaretPosition(0);
    firePropertyChange("tiptext", oldValue, tipText);
  }

  @Override
  public String getTipText() {
    return tipArea == null ? "" : tipArea.getText();
  }

  @Override
  protected String paramString() {
    String tipTextString = (tipArea.getText() != null ? tipArea.getText() : "");

    return super.paramString() +
            ",tipText=" + tipTextString;
  }


}
