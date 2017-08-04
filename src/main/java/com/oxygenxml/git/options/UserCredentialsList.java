package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Enitity for the JAXB to store the user credentials
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "userCredentials")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserCredentialsList {

	/**
	 * List with the credentials
	 */
	@XmlElement(name = "credential")
	private List<UserCredentials> credentials = new ArrayList<UserCredentials>();

	public List<UserCredentials> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<UserCredentials> credentials) {
		this.credentials = credentials;
	}

}
