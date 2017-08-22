package com.oxygenxml.git.translator;

/**
 * Interface used for internationalization.
 * 
 * @author Beniamin Savu
 *
 */
public interface Translator {

	/**
	 * Get the translation from the given key;
	 * 
	 * @param key
	 *          - the key.
	 * @return the translation.
	 */
	public String getTraslation(String key);
}