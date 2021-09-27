package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import com.oxygenxml.git.utils.Equaler;

import ro.sync.exml.workspace.api.options.ExternalPersistentObject;
import ro.sync.options.SerializableList;

/**
 * Entity for the JAXB to store the user credentials
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "userCredentials")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserCredentialsList implements ExternalPersistentObject {

  private static final Logger LOGGER = Logger.getLogger(UserCredentialsList.class);
  
	/**
	 * List with the credentials
	 */
	@XmlElement(name = "credential")
	private SerializableList<UserAndPasswordCredentials> credentials = new SerializableList<>();
	/**
	 * The list with user credentials. The actual list, not a copy.
	 * 
	 * @return The user credentials.
	 */
	public List<UserAndPasswordCredentials> getCredentials() {
		return credentials != null ? new ArrayList<>(credentials) : null;
	}

	public void setCredentials(List<UserAndPasswordCredentials> credentials) {
		this.credentials = credentials != null ? new SerializableList<>(credentials) : new SerializableList<>(0);
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
	  boolean toReturn = false;
	  if (obj instanceof UserCredentialsList) {
	    UserCredentialsList ucl = (UserCredentialsList) obj;
	    toReturn = Equaler.verifyListEquals(credentials, ucl.getCredentials());
	  }
	  return toReturn;
	}

  @Override
  public void checkValid()  {
    //Consider it to be valid.
  }
	
  @Override
  public String[] getNotPersistentFieldNames() {
    return new String[0];
  }
	
  @SuppressWarnings("java:S2975")
  @Override
  public Object clone() {
    try {
      UserCredentialsList clone =(UserCredentialsList) super.clone();
      List<UserAndPasswordCredentials> cloneUserAndPass = new ArrayList<>();
      
      if (credentials != null) {
        for (UserAndPasswordCredentials uAPCred : credentials) {
          cloneUserAndPass.add((UserAndPasswordCredentials) uAPCred.clone());
        }
      }
      clone.setCredentials(cloneUserAndPass);
      return clone;
    } catch (CloneNotSupportedException e) {
      LOGGER.error(e, e);
    }

    return new PersonalAccessTokenInfoList();
  }

}
