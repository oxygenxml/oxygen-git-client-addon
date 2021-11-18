package com.oxygenxml.git.view.branches;

import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;

/**
 * Extends @link{org.eclipse.jgit.lib.BranchConfig} to add the possibility to set certain values in branch configurations.
 * 
 * @author alex_smarandache
 *
 */
public class BranchConfigurations extends BranchConfig {

	/**
	 * The repository configurations.
	 */
	protected final Config config;
	
	/**
	 * The branch name to extract the configurations.
	 */
	protected final String branchName;
	
	
	/**
	 * Constructor.
	 * 
	 * @param config     The repository configurations.
	 * @param branchName The branch name to extract the configurations.
	 */
	public BranchConfigurations(Config config, String branchName) {
		super(config, branchName);
		this.config = config;
		this.branchName = branchName;
	}
	
	
	/**
	 * Set a new remote for current branch in configurations.
	 * 
	 * @param newRemote
	 */
	public void setRemote(String newRemote) {
		this.config.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REMOTE, newRemote);
	}
	
	
	/**
	 * Set a new merge for current branch in configurations.
	 * 
	 * @param newMerge The new merge.
	 */
	public void setMerge(String newMerge) {
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_MERGE, newMerge);
	}

}
