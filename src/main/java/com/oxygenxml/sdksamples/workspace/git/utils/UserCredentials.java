package com.oxygenxml.sdksamples.workspace.git.utils;

/**
 * Git user credentials POJO
 * 
 * @author intern2
 *
 */
public class UserCredentials {
	/**
	 * Git username
	 */
	private String username;
	
	/**
	 * Git Password
	 */
	private String password;

	public UserCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
