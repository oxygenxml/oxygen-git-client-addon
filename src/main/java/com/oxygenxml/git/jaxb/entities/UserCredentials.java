package com.oxygenxml.git.jaxb.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Git user credentials POJO
 * 
 * @author intern2
 *
 */
@XmlRootElement(name = "credential")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserCredentials {

	@XmlElement(name = "host")
	private String host="";

	/**
	 * Git username
	 */
	@XmlElement(name = "username")
	private String username="";

	/**
	 * Git Password
	 */
	@XmlElement(name = "password")
	private String password="";

	public UserCredentials() {

	}

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
	
	

}
