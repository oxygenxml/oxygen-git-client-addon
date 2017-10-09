package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
	private String username = "";

	/**
	 * Git Password
	 */
	@XmlElement(name = "password")
	private String password = "";

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

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return "UserCredentials [host=" + host + ", username=" + username + ", password=" + password + "]";
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserCredentials other = (UserCredentials) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

}
