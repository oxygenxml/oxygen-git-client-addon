package com.oxygenxml.git.view.dialog.internal;

import java.util.List;

import org.eclipse.jgit.annotations.NonNull;

/**
 * Builder for @MessageDialog.
 * 
 * @author alex_smarandache
 *
 */
public class MessageDialogBuilder {
 
  /**
   * Contains informations about the future dialog that will be created.
   */
  protected DialogInfo dialogInfo;
  
  
  /**
   * Constructor.
   * 
   * @param title The title of the dialog.
   * @param type  The type of this dialog.
   */
  public MessageDialogBuilder(@NonNull final String title, @NonNull DialogType type) {
    dialogInfo = new DialogInfo();
    dialogInfo.title = title;
    dialogInfo.iconPath = type.getIconPath();
  }
  
  /**
   * Set title for the new dialog.
   * 
   * @param title The new dialog title.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setTitle(@NonNull final String title) {
    dialogInfo.title = title;
    return this;
  }
  
  /**
   * @param type The new type of the new dialog.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setType(@NonNull final DialogType type) {
    dialogInfo.iconPath = type.getIconPath();
    return this;
  }
  
  /**
   * Set message for this dialog.
   * 
   * @param message The dialog message.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setMessage(final String message) {
    dialogInfo.message = message;
    return this;
  }

  /**
   * Set the new target files to be presented.
   * 
   * @param targetFiles The new files.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setTargetFiles(final List<String> targetFiles) {
    dialogInfo.targetFiles = targetFiles;
    return this;
  }

  /**
   * Set the question message.
   * 
   * @param questionMessage The new question message.
   *
   * @return This dialog builder.
   */
  public MessageDialogBuilder setQuestionMessage(String questionMessage) {
    dialogInfo.questionMessage = questionMessage;
    return this;
  }

  /**
   * Set name for ok button.
   * 
   * @param okButtonName The new name for ok button.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setOkButtonName(final String okButtonName) {
    dialogInfo.okButtonName = okButtonName;
    return this;
  }

  /**
   * Set name for cancel button.
   * 
   * @param cancelButtonName The new name for cancel button.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setCancelButtonName(final String cancelButtonName) {
    dialogInfo.cancelButtonName = cancelButtonName;
    return this;
  }

  /**
   * By default the value is <code>true</code>.
   * 
   * @param isOkButtonVisible <code>true</code> if the ok button should be visible.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setOkButtonVisible(final boolean isOkButtonVisible) {
    dialogInfo.showOkButton = isOkButtonVisible;
    return this;
  }

  /**
   * By default the value is <code>true</code>.
   * 
   * @param isCancelButtonVisible <code>true</code> if the cancel button should be visible.
   * 
   * @return This dialog builder.
   */
  public MessageDialogBuilder setCancelButtonVisible(final boolean isCancelButtonVisible) {
    dialogInfo.showCancelButton = isCancelButtonVisible;
    return this;
  }
  
  /**
   * Build a dialog with the set properties.
   * 
   * @return The created dialog.
   */
  public MessageDialog build() {
    return new MessageDialog(dialogInfo);
  }
  
  /**
   * Build a dialog with the set properties and show it.
   * 
   * @return The created dialog.
   */
  public MessageDialog buildAndShow() {
    final MessageDialog dialog = new MessageDialog(dialogInfo);
    dialog.setVisible(true);
    return dialog;
  }
  
}