package com.oxygenxml.git.auth;

import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.dialog.SSHPassphraseDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * A checker that is able to handle SSH-based requests.
 */
public class SSHCapableUserCredentialsProvider extends ResetableUserCredentialsProvider {
  /**
   *  Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SSHCapableUserCredentialsProvider.class); 
	/**
	 * The pass phase to be used for SSH connections.
	 */
	private String passphrase;
	/**
	 * <code>true</code> if the pass phase was requested (for SSH).
	 */
	private boolean passphaseRequested = false;
	
	/**
	 * Constructor.
	 * 
	 * @param username User name.
	 * @param password Password.
	 * @param passphrase SSH passphrase.
	 * @param host The host name.
	 */
	public SSHCapableUserCredentialsProvider(String username, String password, String passphrase, String host) {
		super(username, password != null ? password : "", host);
		this.passphrase = passphrase;
	}
	
	/**
	 * @see org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider#get(org.eclipse.jgit.transport.URIish, org.eclipse.jgit.transport.CredentialItem[])
	 */
	@Override
	public boolean get(URIish uri, CredentialItem... items) {
	  Boolean isRequestSuccessful = null;
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Credential query, uri: {}", uri);
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

	    if (item instanceof CredentialItem.YesNoType) {
	      if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("YesNoType");
	      }

	      // Present the question to the user.
	      boolean userResponse = askUser(item.getPromptText());
	      ((CredentialItem.YesNoType) item).setValue(userResponse);

	      // true tells the engine that we supplied the value.
	      // The engine will look inside the given item for the response.
	      isRequestSuccessful = true;
	    }
	    
	    if (isRequestSuccessful != null) {
	      break;
	    }
	  }
		
		return isRequestSuccessful == null ? super.get(uri, items) : isRequestSuccessful;
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
    LOGGER.debug("Passphrase required.");
    
    // A not so great method to check that the pass phrase is requested.
    passphaseRequested = true;
    
    if (!validPassphrase(passphrase)) {
      // We don't have a phrase from options. Ask the user.
      LOGGER.debug("Ask for new passphrase...");
      passphrase = new SSHPassphraseDialog(translator.getTranslation(Tags.ENTER_SSH_PASS_PHRASE) + ".").getPassphrase();
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
	
	/**
   * Presents the message to the user and returns the user's answer.
   * 
   * @param promptText THe message.
   * 
   * @return <code>true</code> if the user agrees with the message, <code>false</code> otherwise.
   */
  private boolean askUser(String promptText) {
    OptionsManager optionsManager = OptionsManager.getInstance();
    Boolean response = optionsManager.getSshPromptAnswer(promptText);
    
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Look in cache for answer to: {}, got: {}.", promptText, response);
    }
    
    if (response == null) {
      // Ask the user.
      String[] options = new String[] { "   Yes   ", "   No   " };
      int[] optonsId = new int[] { 0, 1 };
      int result = PluginWorkspaceProvider.getPluginWorkspace()
          .showConfirmDialog("Connection", promptText, options, optonsId);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Asked the user, answer: {}", response);
      }

      if (result == 0) {
        // true tells the engine that we supplied the value.
        response = Boolean.TRUE;
      } else {
        response = Boolean.FALSE;
      }
      
      optionsManager.saveSshPrompt(promptText, response);
    }
    
    return response;
    
  }
	
	/**
	 * @return <code>true</code> if the pass phase was requested (for SSH) and provided by the user.
	 */
	public boolean isPassphaseRequested() {
    return passphaseRequested;
  }
}
