package com.oxygenxml.git.view.history;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Strategy to present repository history. 
 * 
 * @author alex_smarandache
 *
 */
public enum HistoryStrategy {
	
	/**
	 * Presents all branches, both local and remote.
	 */
	ALL_BRANCHES(Translator.getInstance().getTranslation(Tags.ALL_BRANCHES)),
	
	/**
	 * Presents all local branches.
	 */
	ALL_LOCAL_BRANCHES(Translator.getInstance().getTranslation(Tags.ALL_LOCAL_BRANCHES)),
	
	/**
	 * Presents the current branch, both local and remote.
	 */
	CURRENT_BRANCH(Translator.getInstance().getTranslation(Tags.CURRENT_BRANCH)),
	
	/**
	 * Presents only the current local branch.
	 */
	CURRENT_LOCAL_BRANCH(Translator.getInstance().getTranslation(Tags.CURRENT_LOCAL_BRANCH));

	
	/**
	 * String value for enum.
	 */
	private final String stringValue;
	
	
	/**
	 * Hidden Constructor.
	 *
	 * @param stringValue The string value for enum.
	 */
	private HistoryStrategy(final String stringValue) {
		this.stringValue = stringValue;
	}
	
	
	@Override
	public String toString() {
	    return this.stringValue;
	}

}
