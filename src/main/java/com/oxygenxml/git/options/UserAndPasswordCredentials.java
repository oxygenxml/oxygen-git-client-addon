package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;

import ro.sync.exml.workspace.api.options.ExternalPersistentObject;

/**
 * Git user credentials POJO for the JAXB
 */
@XmlRootElement(name = "credential")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserAndPasswordCredentials extends CredentialsBase implements ExternalPersistentObject {

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
	public UserAndPasswordCredentials() {
	  super();
	}

	/**
	 * Constructor.
	 * 
	 * @param username User name.
	 * @param password password.
	 * @param host Host name.
	 */
	public UserAndPasswordCredentials(String username, String password, String host) {
		super(host);
		this.username = username;
		this.password = password;
	}

	/**
	 * @return The user name or <code>null</code> if not available.
	 */
	public String getUsername() {
		return username != null && username.length() > 0 ? username : null;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	 /**
   * @return The password or an empty string if one is not available.
   */
	public String getPassword() {
	  // Later on, org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider will throw a NPE 
	  // if we pass a NULL.
		return password != null ? password : "";
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public CredentialsType getType() {
	  return CredentialsType.USER_AND_PASSWORD;
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
	  if (obj instanceof UserAndPasswordCredentials) {
	    UserAndPasswordCredentials creds = (UserAndPasswordCredentials) obj;
	    toReturn = Equaler.verifyEquals(host, creds.getHost())
	        && Equaler.verifyEquals(password, creds.getPassword())
	        && Equaler.verifyEquals(username, creds.getUsername());
	  }
	  return toReturn;
	}

  @Override
  public void checkValid() {
    //Consider it to be valid.
  }

  @Override
  public String[] getNotPersistentFieldNames() {
    return new String[0];
  }
	
	
  @SuppressWarnings("java:S2975")
	@Override
  public Object clone() {
    return super.clone();
	}
}
