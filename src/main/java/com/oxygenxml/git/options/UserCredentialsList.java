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
	/**
	 * The list with user credentials. The actual list, not a copy.
	 * 
	 * @return The user credentials.
	 */
	public List<UserCredentials> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<UserCredentials> credentials) {
		this.credentials = credentials;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
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
		UserCredentialsList other = (UserCredentialsList) obj;
		if (credentials == null) {
			if (other.credentials != null)
				return false;
		} else if (!credentials.equals(other.credentials))
			return false;
		return true;
	}

}
