package com.oxygenxml.git.view.dialog.internal;

import java.awt.Dimension;
import java.util.List;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.internal.MessageDialog.MessageDialogBuilder;

/**
 * Class to presents the @MessageDialog in a lot of different ways.
 * 
 * @author alex_smarandache
 *
 */
public class MessageDialogPresenter implements IDialogPresenter {

  @Override
  public void showErrorMessage(
      final String title, 
      final List<String> files, 
      final String message) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.ERROR_ICON)
        .setMessage(message)
        .setCancelButtonVisible(false)
        .setTargetFiles(files)
        .build();
    
    dialog.setResizable(files != null && !files.isEmpty());
    dialog.setMinimumSize(new Dimension(MessageDialog.WARN_MESSAGE_DLG_MINIMUM_WIDTH, MessageDialog.WARN_MESSAGE_DLG_MINIMUM_HEIGHT));
    dialog.getOkButton().setText(Translator.getInstance().getTranslation(Tags.CLOSE));
    dialog.pack();
    dialog.setVisible(true);
  }
  
  @Override
  public void showWarningMessage(
      final String title, 
      final List<String> files, 
      final String message) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.WARNING_ICON)
        .setMessage(message)
        .setCancelButtonVisible(false)
        .setTargetFiles(files)
        .build();
   
    dialog.setResizable(files != null && !files.isEmpty());
    dialog.setMinimumSize(new Dimension(MessageDialog.WARN_MESSAGE_DLG_MINIMUM_WIDTH, MessageDialog.WARN_MESSAGE_DLG_MINIMUM_HEIGHT));
    dialog.pack();
    dialog.setVisible(true);
  }
  
  @Override
  public int showWarningMessageWithConfirmation(
      final String title,
      final String message,
      final String okButtonLabel,
      final String cancelButtonLabel) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.WARNING_ICON)
        .setQuestionMessage(message)
        .setOkButtonName(okButtonLabel)
        .setCancelButtonName(cancelButtonLabel)
        .build();
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
  @Override
  public int showQuestionMessage(
      final String title, 
      final List<String> files, 
      final String message,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.QUESTION_ICON)
        .setQuestionMessage(questionMessage)
        .setOkButtonName(okButtonName)
        .setCancelButtonName(cancelButtonName)
        .setMessage(message)
        .setTargetFiles(files)
        .build();
    dialog.setVisible(true);  
    return dialog.getResult();
  }

  @Override
  public int showQuestionMessage(
      final String title,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.QUESTION_ICON)
        .setQuestionMessage(questionMessage)
        .setOkButtonName(okButtonName)
        .setCancelButtonName(cancelButtonName)
        .build();
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
  @Override
  public int showInformationMessage(
      final String title, 
      final String informationMessage, 
      final String okButtonName, 
      final String cancelButtonName) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.INFO_ICON)
        .setMessage(informationMessage)
        .setOkButtonName(okButtonName)
        .setCancelButtonName(cancelButtonName)
        .build();
        
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
  @Override
  public int showInformationMessage(
      final String title, 
      final String informationMessage, 
      final String okButtonName, 
      final String cancelButtonName,
      final boolean isOkButtonVisible,
      final boolean isCancelButtonVisible) {
    final MessageDialog dialog = new MessageDialogBuilder(title)
        .setIconPath(Icons.INFO_ICON)
        .setMessage(informationMessage)
        .setOkButtonName(okButtonName)
        .setOkButtonVisible(isOkButtonVisible)
        .setCancelButtonVisible(isCancelButtonVisible)
        .setCancelButtonName(cancelButtonName)
        .build();
    dialog.setVisible(true);
    return dialog.getResult();
  }
  
}
