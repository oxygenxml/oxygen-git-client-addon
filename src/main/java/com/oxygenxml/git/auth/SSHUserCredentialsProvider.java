package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.options.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * A checker that handles SSH related questions.
 */
public class SSHUserCredentialsProvider extends ResetableUserCredentialsProvider {
  /**
   *  Logger for logging.
   */
  private static Logger logger = Logger.getLogger(SSHUserCredentialsProvider.class); 
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
	 * @param passphrase SSH pass phase.
	 * @param host The host name.
	 */
	public SSHUserCredentialsProvider(String username, String password, String passphrase, String host) {
		super(username, password, host);
		this.passphrase = passphrase;
	}

	/**
	 * @see org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider#get(org.eclipse.jgit.transport.URIish, org.eclipse.jgit.transport.CredentialItem[])
	 */
	@Override
	public boolean get(URIish uri, CredentialItem... items) {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Credential query, uri " + uri);
	  }
	  for (CredentialItem item : items) {
	    // TODO Should handle item.isValueSecure()
	    if (logger.isDebugEnabled()) {
	      logger.debug(item.getClass() + ", secure value: " + item.isValueSecure());
	      logger.debug("Message: |" + item.getPromptText() + "|");
	    }

	    if (item instanceof CredentialItem.StringType
	        && item.getPromptText().startsWith("Passphrase")) {
	      // A not so great method to check that the pass phrase is requested.
	      passphaseRequested = true;

	      ((CredentialItem.StringType) item).setValue(passphrase);
	      // true tells the engine that we supplied the value.
	      // The engine will look inside the given item for the response.
	      return true;
	    }

	    if (item instanceof CredentialItem.YesNoType) {
	      if (logger.isDebugEnabled()) {
	        logger.debug("YesNoType");
	      }

	      // Present the question to the user.
	      boolean userResponse = askUser(item.getPromptText());
	      ((CredentialItem.YesNoType) item).setValue(userResponse);

	      // true tells the engine that we supplied the value.
	      // The engine will look inside the given item for the response.
	      return true;
	    }
	  }
		
		return super.get(uri, items);
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
    
    if (logger.isDebugEnabled()) {
      logger.debug("Look in cache for answer to: " + promptText + ", got " + response);
    }
    
    if (response == null) {
      // Ask the user.
      String[] options = new String[] { "   Yes   ", "   No   " };
      int[] optonsId = new int[] { 0, 1 };
      int result = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .showConfirmDialog("Connection", promptText, options, optonsId);

      if (logger.isDebugEnabled()) {
        logger.debug("Asked the user, answer: " + response);
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
	 * @return <code>true</code> if the pass phase was requested (for SSH).
	 */
	public boolean isPassphaseRequested() {
    return passphaseRequested;
  }
}
