package com.oxygenxml.git.view.dialog;

import com.oxygenxml.git.service.annotation.UsedForTests;
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
   * The dialog presenter.
   */
  private static IDialogPresenter internalDialogPresenter = new MessageDialogPresenter();

  /**
   * Hidden constructor.
   */
  private MessagePresenterProvider() {
    // not needed
  }

  /**
   * Get the presenter.
   *
   * @return Dialog presenter.
   */
  public static IDialogPresenter getPresenter() {
    return internalDialogPresenter;
  }

  /**
   * Set the new presenter for message dialog.
   * 
   * @param newPresenter The new dialog presenter. 
   */
  @UsedForTests
  public static void setPresenter(final IDialogPresenter newPresenter) {
    internalDialogPresenter = newPresenter;
  }
  
}
