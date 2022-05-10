package com.oxygenxml.git.view.dialog;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.dialog.internal.MessageDialogBuilder;

/**
 * Used to provide a dialog presenter.
 *
 * @author alex_smarandache
 *
 */
public class MessagePresenterProvider {

  /**
   * The dialog builder.
   */
  private static MessageDialogBuilder imposedBuilder;

  /**
   * Hidden constructor.
   */
  private MessagePresenterProvider() {
    // not needed
  }
  
  /**
   * Provide a builder for @MessageDialog.
   * 
   * @param title The title of the initial dialog.
   * @param type  The type of new initial dialog.
   * 
   * @return the existing builder or a new instance if no builder is set.
   */
  public static MessageDialogBuilder getBuilder(@NonNull final String title,
      final @NonNull DialogType type) { 
    return imposedBuilder != null ? 
        imposedBuilder : new MessageDialogBuilder(title, type);
  }

  /**
   * Set the new builder for dialogs.
   * 
   * @param newBuilder The new dialogs builder. 
   */
  @TestOnly
  public static void setBuilder(final MessageDialogBuilder newBuilder) {
    imposedBuilder = newBuilder;
  }
  
}
