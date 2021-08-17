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
import javax.swing.ScrollPaneConstants;

import com.oxygenxml.git.view.util.UIUtil;

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
  protected static final int MAXIMUM_HEIGHT = 165;
  protected static final int MAXIMUM_WIDTH = 465;
  
  Dimension dimension  = new Dimension(200, 200); 

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
    scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    setPreferredSize(dimension);
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
    JToolTip toolTip =  UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
    toolTip.setTipText(tipText);
    int width = toolTip.getPreferredSize().width;
    int height = toolTip.getPreferredSize().height;
    width = (width > MAXIMUM_WIDTH) ? MAXIMUM_WIDTH : width;
    height = (height > MAXIMUM_HEIGHT ) ? MAXIMUM_HEIGHT : height; 
    setPreferredSize(new Dimension(width, height));
    this.tipText.setText(tipText);
    this.tipText.setCaretPosition(0);
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
