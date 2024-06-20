package com.oxygenxml.git.service;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.GPGPassphraseDialog;

/**
 * GPG-keys credentials provider.
 */
public class GPGCapableCredentialsProvider extends CredentialsProvider {

  /**
   *  Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GPGCapableCredentialsProvider.class); 
  
  /**
   * The passphrase to be used.
   */
  private String passphrase;
  
  /**
   * Constructor.
   * 
   * @param passphrase Passphrase.
   */
  public GPGCapableCredentialsProvider(String passphrase) {
    super();
    this.passphrase = passphrase;
  }

  /**
   * @see org.eclipse.jgit.transport.CredentialsProvider.isInteractive()
   */
  @Override
  public boolean isInteractive() {
    return false;
  }

  /**
   * @see org.eclipse.jgit.transport.CredentialsProvider.supports(CredentialItem... items)
   */
  @Override
  public boolean supports(CredentialItem... items) {
    for (CredentialItem i : items) {
      if (i instanceof CredentialItem.Password
          || i instanceof CredentialItem.StringType && i.getPromptText().startsWith("Passphrase")) {
        continue;
      }
      return false;
    }
    return true;
  }

  /**
   * @see com.oxygenxml.git.service.GPGCapableCredentialsProvider.get(URIish uri, CredentialItem... items)
   */
  @Override
  public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
    Boolean isRequestSuccessful = null;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("GPGP credential provider, uri: {}", uri);
    }
    for (CredentialItem item : items) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Item class: {}, is secure value: {}.", item.getClass(), item.isValueSecure());
        LOGGER.debug("Message: |{}|", item.getPromptText());
      }
      
      if ((item instanceof CredentialItem.StringType || item instanceof CredentialItem.Password) 
          && item.getPromptText().startsWith("Passphrase")) {
        isRequestSuccessful = treatPassphrase(item);
      }

      if (isRequestSuccessful != null) {
        break;
      }
    }
    
    return isRequestSuccessful != null && isRequestSuccessful;
  }
  
  /**
   * Treat passphrase.
   * 
   * @param item The credential item.
   * 
   * @return <code>true</code> if the request was successful and values were supplied; 
   * <code>false</code> if the user canceled the request and did not supply all requested values.
   */
  private boolean treatPassphrase(CredentialItem item) {
    LOGGER.debug("GPG passphrase required.");
    
    if (!validPassphrase(passphrase)) {
      // We don't have a phrase from options. Ask the user.
      LOGGER.debug("Ask for new GPG passphrase...");
      passphrase = new GPGPassphraseDialog(Translator.getInstance().getTranslation(Tags.ENTER_GPG_PASSPHRASE) + ".").getPassphrase();
    }
    
    if (validPassphrase(passphrase)) {
      if (item instanceof CredentialItem.StringType) {
        ((CredentialItem.StringType) item).setValue(passphrase);
      } else if (item instanceof CredentialItem.Password) {
        ((CredentialItem.Password) item).setValue(passphrase.toCharArray());
      }
      // true tells the engine that we supplied the value.
      // The engine will look inside the given item for the response.
      return true;
    } else {
      // The user canceled the dialog.
      return false;
    }
  }

  /**
   * @param passphrase Pass phrase.
   * 
   * @return <code>true</code> if the phrase is not null and not empty.
   */
  private static final boolean validPassphrase(String passphrase) {
    return passphrase != null && passphrase.length() > 0;
  }
}
