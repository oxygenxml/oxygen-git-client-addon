package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.utils.Equaler;

import ro.sync.exml.workspace.api.options.ExternalPersistentObject;

/**
 * Entity for JAXB to store the user personal access tokens.
 */
@XmlRootElement(name = "personalAccessTokens")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonalAccessTokenInfoList implements ExternalPersistentObject {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalAccessTokenInfoList.class);
  
	/**
	 * List with the token info items.
	 */
	@XmlElement(name = "personalAccessToken")
	private List<PersonalAccessTokenInfo> personalAccessTokens = new ArrayList<>();
	
	/**
   * @return a copy of the list containing personal access token info items.
   */
  public List<PersonalAccessTokenInfo> getPersonalAccessTokens() {
    return personalAccessTokens != null ? new ArrayList<>(personalAccessTokens) : new ArrayList<>();
  }

  /**
   * @param personalAccessTokens the personal access token info items to set
   */
  public void setPersonalAccessTokens(List<PersonalAccessTokenInfo> personalAccessTokens) {
    this.personalAccessTokens = personalAccessTokens != null ? new ArrayList<>(personalAccessTokens) : new ArrayList<>();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((personalAccessTokens == null) ? 0 : personalAccessTokens.hashCode());
    return result;
  }
  
  @Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof PersonalAccessTokenInfoList) {
	    PersonalAccessTokenInfoList tokenInfoItems = (PersonalAccessTokenInfoList) obj;
	    toReturn = Equaler.verifyListEquals(personalAccessTokens, tokenInfoItems.getPersonalAccessTokens());
	  }
	  return toReturn;
	}
  
  @SuppressWarnings("java:S2975")
  @Override
  public Object clone() {
    try {
      PersonalAccessTokenInfoList clone =(PersonalAccessTokenInfoList) super.clone();
      List<PersonalAccessTokenInfo> cloneTokensAccessTokenInfos = new ArrayList<>();
      
      if (personalAccessTokens != null) {
        for (PersonalAccessTokenInfo token : personalAccessTokens) {
          cloneTokensAccessTokenInfos.add((PersonalAccessTokenInfo) token.clone());
        }
      }
      clone.setPersonalAccessTokens(cloneTokensAccessTokenInfos);
      return clone;
    } catch (CloneNotSupportedException e) {
      LOGGER.error(e.getMessage(), e);
    }

    return new PersonalAccessTokenInfoList();
  }

  @Override
  public void checkValid() {
    // We consider it to be valid.
  }

  @Override
  public String[] getNotPersistentFieldNames() {
    return new String[0];
  }

}
