package com.oxygenxml.git.view.dialog.internal;

import java.util.List;

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
   * Files that relate to the message.
   */
  List<String> targetFiles;
  
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
}