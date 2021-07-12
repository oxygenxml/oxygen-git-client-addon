package com.oxygenxml.git.view.util;

import javax.swing.Icon;

/**
 * Rendering info.
 */
public class RenderingInfo {
  /**
   * Icon.
   */
  private Icon icon;
  /**
   * Tootlip text.
   */
  private String tooltip;
  
  /**
   * Constructor.
   * 
   * @param icon     Icon.
   * @param tooltip  Tooltip text.
   */
  public RenderingInfo(Icon icon, String tooltip) {
    this.icon = icon;
    this.tooltip = tooltip;
  }
  
  /**
   * @return the icon
   */
  public Icon getIcon() {
    return icon;
  }
  
  /**
   * @return the tooltip
   */
  public String getTooltip() {
    return tooltip;
  }
}