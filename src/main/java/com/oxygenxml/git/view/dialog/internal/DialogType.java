package com.oxygenxml.git.view.dialog.internal;

import com.oxygenxml.git.constants.Icons;

/**
 * Enumeration for dialog types.
 * 
 * @author alex_smarandache
 *
 */
public enum DialogType {
  
  /**
   * Error dialog type.
   */
  ERROR(Icons.ERROR_ICON),
  
  /**
   * Warning dialog type.
   */
  WARNING(Icons.WARNING_ICON),
  
  /**
   * Question dialog type.
   */
  QUESTION(Icons.QUESTION_ICON),
  
  /**
   * Info dialog type.
   */
  INFO(Icons.INFO_ICON);
  
  
  /**
   * The path for a icon of that type.
   */
  private final String iconPath;
  
  /**
   * Hidden Constructor.
   *
   * @param iconPath The path for a icon of that type.
   */
  private DialogType(final String iconPath) {
    this.iconPath = iconPath;
  }
  
  /**
   * @return icon path for this type.
   */
  public String getIconPath() {
    return iconPath;
  }
  
}
