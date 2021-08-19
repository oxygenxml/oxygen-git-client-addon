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

  /**
   * The JEditorPane that contains the text of the tooltip.
   */
  protected JEditorPane tooltipText;
  /**
   * The JScrollPane to make theToolTip scrollable.
   */
  protected JScrollPane scrollpane;
  /*
   * The swing associated component.
   */
  protected JComponent comp;
  /**
   * The super ToolTip.
   */
  private final JToolTip toolTip;
  /*
   * The maximum height of the ToolTip.
   */
  protected static final int MAXIMUM_HEIGHT = 400;
  /*
   * The maximum width of the ToolTip.
   */
  protected static final int MAXIMUM_WIDTH = 500;
  /*
   * The size of redundant height.
   */
  private static final int HEIGHT_DELTA = 20;
   

  /** Creates a tool tip. */
  public JScrollableToolTip() {
    this(null);
  }
  
  /**
   * @param comp the associated swing component.
   */
  public JScrollableToolTip(final JComponent comp) {
    this.comp = comp;
    setLayout(new BorderLayout());
    toolTip = UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
    tooltipText = new JEditorPane();
    tooltipText.setEditable(false);
    tooltipText.setContentType("text/html");
    tooltipText.setBackground(super.getBackground());
    tooltipText.setFont(super.getFont());
    scrollpane = new JScrollPane(tooltipText);
    scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    setPreferredSize(new Dimension(200, 200));
    add(scrollpane);
    if(comp != null){
      comp.addMouseWheelListener(this);
    }
  }

  /**
   * The move for mouse wheel.
   */
  public void mouseWheelMoved(final MouseWheelEvent e) {
    scrollpane.dispatchEvent(e);
    MouseEvent e2 = new MouseEvent(comp, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false);
    comp.dispatchEvent(e2);
  }

  @Override
  public void setTipText(final String text) {
    String oldValue = this.tooltipText.getText();
    toolTip.setTipText(text);
    int width = toolTip.getPreferredSize().width;
    int height = toolTip.getPreferredSize().height;
    width = Math.min(width, MAXIMUM_WIDTH);
    height = (height > MAXIMUM_HEIGHT ) ? MAXIMUM_HEIGHT : height - HEIGHT_DELTA; 
    setPreferredSize(new Dimension(width, height));
    this.tooltipText.setText(text);
    this.tooltipText.setCaretPosition(0);
    firePropertyChange("tiptext", oldValue, text);
  }
  
  @Override
  public String getTipText() {
    return tooltipText == null ? "" : tooltipText.getText();
  }

  @Override
  protected String paramString() {
    String tipTextString = (tooltipText.getText() != null ? tooltipText.getText() : "");

    return super.paramString() +
            ",tipText=" + tipTextString;
  }
  
}
