package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * A checker that handles SSH related questions.
 */
public class SSHUserCredentialsProvider extends UsernamePasswordCredentialsProvider {
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
	 */
	public SSHUserCredentialsProvider(String username, String password, String passphrase) {
		super(username, password);
		this.passphrase = passphrase;
	}

	/**
	 * @see org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider#get(org.eclipse.jgit.transport.URIish, org.eclipse.jgit.transport.CredentialItem[])
	 */
	@Override
	public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
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

	      ((CredentialItem.StringType) item).setValue(new String(passphrase));
	      // true tells the engine that we supplied the value.
	      // The engine will look inside the given item for the response.
	      return true;
	    }

	    if (item instanceof CredentialItem.YesNoType) {
	      if (logger.isDebugEnabled()) {
	        logger.debug("YesNoType");
	      }

	      // Present the question to the user.
	      boolean userResponse = SshPrompt.askUser(item.getPromptText());
	      ((CredentialItem.YesNoType) item).setValue(userResponse);

	      // true tells the engine that we supplied the value.
	      // The engine will look inside the given item for the response.
	      return true;
	    }
	  }
		
		return super.get(uri, items);
	}
	
	/**
	 * @return <code>true</code> if the pass phase was requested (for SSH).
	 */
	public boolean isPassphaseRequested() {
    return passphaseRequested;
  }
}
