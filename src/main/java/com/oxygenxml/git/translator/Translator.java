package com.oxygenxml.git.translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Translator for i18n.
 * 
 * @author sorin_carbunaru
 * @author alex_jitianu
 */
public class Translator {
  
  /**
   * The singleton instance.
   */
  private static Translator instance;
  
  /**
   * Hidden constructor.
   */
  private Translator() {
    // Nothing
  }
  
  /**
   * Get the singleton instance.
   * 
   * @return the singleton instance.
   */
  public static Translator getInstance() {
    if (instance == null) {
      instance = new Translator();
    }
    return instance;
  }

  /**
   * Get the translation for the given key.
   * 
   * @param key The key identifying the message.
   * 
   * @return the translation.
   */
	public String getTranslation(String key) {
		StandalonePluginWorkspace spw =
		    (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
		String translation = key;
		if (spw != null && spw.getResourceBundle() != null) {
		  translation = spw.getResourceBundle().getMessage(key);
		}
    return translation;
	}

}