package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;


import com.oxygenxml.git.options.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Asks user for permissions and caches the response.  
 */
public class SshPrompt {
  /**
   *  Logger for logging.
   */
  private static Logger logger = Logger.getLogger(SshPrompt.class);
  
  /**
   * Presents the message to the user and returns the user's answer.
   * 
   * @param promptText THe message.
   * 
   * @return <code>true</code> if the user agrees with the message, <code>false</code> otherwise.
   */
  public static boolean askUser(String promptText) {
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

}
