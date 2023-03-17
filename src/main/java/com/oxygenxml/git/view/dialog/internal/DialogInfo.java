package com.oxygenxml.git.view.dialog.internal;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contains necessary information for a @MessageDialog construction.
 * 
 * @author alex_smarandache
 *
 */
 public class DialogInfo {
 
  /**
   * The dialog title dialog.
   */
  String title;
  
  /**
   * Icon path for dialog.
   */
  String iconPath;
  
  /**
   * Contains the target files and their tooltips.
   * <br>
   * <code>Key:</code> The relative path of the file.
   * <code>Value:</code> The tooltip for the file.
   */
  Map<String, String> targetFilesWithTooltips;
  
  /**
   * The dialog message.
   */
  String message;
  
  /**
   * The question message.
   */
  String questionMessage;
  
  /**
   * Text for "Ok" button.
   */
  String okButtonName;
  
  /**
   * Text for "Cancel" button.
   */
  String cancelButtonName;
  
  /**
   * <code>True</code> if "Ok" button should be visible.
   */
  boolean showOkButton = true;
  
  /**
   * <code>True</code> if "Cancel" button should be visible.
   */
  boolean showCancelButton = true;

  @Override
  public String toString() {
    final StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("title = ").append(title).append('\n')
    .append("iconPath = ").append(iconPath).append('\n')
    .append("targetFiles = ").append(targetFilesWithTooltips != null ?
        targetFilesWithTooltips.keySet().stream().sorted().collect(Collectors.toList()) : null).append('\n')
    .append("message = ").append(message).append('\n')
    .append("questionMessage = ").append(questionMessage).append('\n')
    .append("okButtonName = ").append(okButtonName).append('\n')
    .append("cancelButtonName = ").append(cancelButtonName).append('\n')
    .append("showOkButton = ").append(showOkButton).append('\n')
    .append("showCancelButton = ").append(showCancelButton);
    
    return strBuilder.toString();
  }
   
}