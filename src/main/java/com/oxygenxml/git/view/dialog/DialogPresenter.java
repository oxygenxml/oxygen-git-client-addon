package com.oxygenxml.git.view.dialog;

import java.util.List;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.view.dialog.internal.IDialogPresenter;
import com.oxygenxml.git.view.dialog.internal.MessageDialogPresenter;

/**
 * Used to show dialogs.
 * 
 * @author alex_smarandache
 *
 */
public class DialogPresenter {

  /**
   * The internal @IDialogPresenter which gives the implementation to presents methods.
   */
  private IDialogPresenter internalDialogPresenter;
  
  /**
   * The hidden constructor.
   */
  private DialogPresenter() {
    internalDialogPresenter = new MessageDialogPresenter();
  }
  
  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class DialogPresenterHelper {
      static final DialogPresenter INSTANCE = new DialogPresenter();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  public static DialogPresenter getInstance() {
      return DialogPresenterHelper.INSTANCE;
  }
  
  /**
   * @param newPresenter The new dialog presenter.
   */
  public void setPresenter(@NonNull final IDialogPresenter newPresenter) {
    if(newPresenter != null) {
      this.internalDialogPresenter = newPresenter;
    }
  }
  
  /**
   * Presents an error to the user about the files' status.
   * 
   * @param title    Title of the Dialog
   * @param files    Files that relate to the message.
   * @param message  The message.
   */
  public void showErrorMessage(
      final String title, 
      final List<String> files, 
      final String message) {
   internalDialogPresenter.showErrorMessage(title, files, message);
  }
  
  /**
   * Presents a warning to the user about the files' status.
   * 
   * @param title         Title of the Dialog
   * @param files         Files that relate to the message.
   * @param message       The message.
   */
  public void showWarningMessage(
      final String title, 
      final List<String> files, 
      final String message) {
    internalDialogPresenter.showWarningMessage(title, files, message);
  }
  
  /**
   * Presents a warning message where the user also has to confirm something.
   * 
   * @param title             Title of the Dialog
   * @param message           The message.
   * @param okButtonLabel     The label of the OK button.
   * @param cancelButtonLabel The label of the cancel dialog.
   * 
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public int showWarningMessageWithConfirmation(
      final String title,
      final String message,
      final String okButtonLabel,
      final String cancelButtonLabel) {
    return internalDialogPresenter.showWarningMessageWithConfirmation(title, message, okButtonLabel, cancelButtonLabel);
  }
  
  /**
   * Shows pull status and conflicting files and asks the user a question.
   * 
   * @param title            Dialog title.
   * @param files            Files that relate to the message.
   * @param message          Message shown
   * @param questionMessage  A question message connected to the presented information.
   * @param okButtonName     The name given to the button for answering affirmative to the question.
   * @param cancelButtonName The name given to the button for answering negative to the question.
   * 
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public int showQuestionMessage(
      final String title, 
      final List<String> files, 
      final String message,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    return internalDialogPresenter.showQuestionMessage(title, files, message, questionMessage, okButtonName, cancelButtonName);
  }

  /**
   * Shows a warning and asks the user a question.
   * 
   * @param title            Dialog title.
   * @param questionMessage  The warning and question message to be presented.
   * @param okButtonName     The name given to the button for answering affirmative to the question.
   * @param cancelButtonName The name given to the button for answering negative to the question.
   * 
   * @return The option chosen by the user. {@link #RESULT_OK} or {@link #RESULT_CANCEL}
   */
  public int showQuestionMessage(
      final String title,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    return internalDialogPresenter.showInformationMessage(title, questionMessage, okButtonName, cancelButtonName);
  }
  
  /**
   * Shows an information and gives the user 2 options.
   * 
   * @param title              Dialog title.
   * @param informationMessage The information message to be presented.
   * @param okButtonName       The name given to the button for answering
   *                           affirmative to the question.
   * @param cancelButtonName   The name given to the button for answering negative
   *                           to the question.
   *                           
   * @return The option chosen by the user. {@link #RESULT_OK} or
   *         {@link #RESULT_CANCEL}
   */
  public int showInformationMessage(
      final String title, 
      final String informationMessage, 
      final String okButtonName, 
      final String cancelButtonName) {
    return internalDialogPresenter.showInformationMessage(title, informationMessage, okButtonName, cancelButtonName);
  }
  
}
