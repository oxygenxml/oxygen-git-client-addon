package com.oxygenxml.git.view.dialog.internal;

import java.util.List;


/**
 * A dialog presenter.
 * 
 * @author alex_smarandache
 *
 */
public interface IDialogPresenter {

  /**
   * Presents an error to the user.
   * 
   * @param title    Title of the Dialog
   * @param files    Files that relate to the message.
   * @param message  The message.
   */
  public default void showErrorMessage(
      final String title, 
      final List<String> files, 
      final String message) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Presents a warning to the user.
   * 
   * @param title         Title of the Dialog
   * @param files         Files that relate to the message.
   * @param message       The message.
   */
  public default void showWarningMessage(
      final String title, 
      final List<String> files, 
      final String message) {
    throw new UnsupportedOperationException();
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
  public default int showWarningMessageWithConfirmation(
      final String title,
      final String message,
      final String okButtonLabel,
      final String cancelButtonLabel) {
    throw new UnsupportedOperationException();
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
  public default int showQuestionMessage(
      final String title, 
      final List<String> files, 
      final String message,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    throw new UnsupportedOperationException();
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
  public default int showQuestionMessage(
      final String title,
      final String questionMessage,
      final String okButtonName,
      final String cancelButtonName) {
    throw new UnsupportedOperationException();
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
   * @return The option chosen by the user. {@link #RESULT_OK} or
   *         {@link #RESULT_CANCEL}
   */
  public default int showInformationMessage(
      final String title, 
      final String informationMessage, 
      final String okButtonName, 
      final String cancelButtonName) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Shows an information and gives the user 2 options.
   * 
   * @param title                  Dialog title.
   * @param informationMessage     The information message to be presented.
   * @param okButtonName           The name given to the button for answering
   *                               affirmative to the question.
   * @param cancelButtonName       The name given to the button for answering negative
   *                               to the question.
   * @param isOkButtonVisible      <code>true</code> if the ok button should be visible. 
   * @param iscancelButtonVisible  <code>true</code> if the cancel button should be visible. 
   *                         
   * @return The option chosen by the user. {@link #RESULT_OK} or
   *         {@link #RESULT_CANCEL}
   */
  public default int showInformationMessage(
      final String title, 
      final String informationMessage, 
      final String okButtonName, 
      final String cancelButtonName,
      final boolean isOkButtonVisible,
      final boolean isCancelButtonVisible) {
    throw new UnsupportedOperationException();
  }

}
