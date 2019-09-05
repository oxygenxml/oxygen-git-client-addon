package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;

/**
 * Git user credentials POJO for the JAXB
 */
@XmlRootElement(name = "credential")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserCredentials {
	/**
	 * The host for which the username and password are validF
	 */
	@XmlElement(name = "host")
	private String host = "";

	/**
	 * Git username
	 */
	@XmlElement(name = "username")
	private String username = null;

	/**
	 * Git Password
	 */
	@XmlElement(name = "password")
	private String password = null;

	/**
	 * Default constructor.
	 */
	public UserCredentials() {}

	/**
	 * Constructor.
	 * 
	 * @param username User name.
	 * @param password password.
	 * @param host Host name.
	 */
	public UserCredentials(String username, String password, String host) {
		this.host = host;
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username != null && username.length() > 0 ? username : null;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
	  // Later on, org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider will throw a NPE 
	  // if we pass a NULL.
		return password != null ? password : "";
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return "UserCredentials [host=" + host + ", username=" + username + ", password=" + "CLASSIFIED" + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof UserCredentials) {
	    UserCredentials creds = (UserCredentials) obj;
	    toReturn = Equaler.verifyEquals(host, creds.getHost())
	        && Equaler.verifyEquals(password, creds.getPassword())
	        && Equaler.verifyEquals(username, creds.getUsername());
	  }
	  return toReturn;
	}

}
