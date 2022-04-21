package com.oxygenxml.git.view.dialog;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.view.dialog.internal.IDialogPresenter;
import com.oxygenxml.git.view.dialog.internal.MessageDialogPresenter;

/**
 * Used to provide a dialog presenter.
 * 
 * @author alex_smarandache
 *
 */
public class MessagePresenterProvider {

  /**
   * The internal @IDialogPresenter which gives the implementation to presents methods.
   */
  private IDialogPresenter internalDialogPresenter;
  
  /**
   * The hidden constructor.
   */
  private MessagePresenterProvider() {
    internalDialogPresenter = new MessageDialogPresenter();
  }
  
  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class DialogPresenterHelper {
      static final MessagePresenterProvider INSTANCE = new MessagePresenterProvider();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  public static MessagePresenterProvider getInstance() {
      return DialogPresenterHelper.INSTANCE;
  }
  
  /**
   * @return The dialog presenter.
   */
  public IDialogPresenter getPresenter() {
    return internalDialogPresenter;
  }
  
  /**
   * @param newPresenter The new dialog presenter.
   */
  public void setPresenter(@NonNull final IDialogPresenter newPresenter) {
    if(newPresenter != null) {
      this.internalDialogPresenter = newPresenter;
    }
  }
  
}
