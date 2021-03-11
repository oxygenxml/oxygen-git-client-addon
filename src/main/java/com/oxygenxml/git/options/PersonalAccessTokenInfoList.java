package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;

/**
 * Entity for JAXB to store the user personal access tokens.
 */
@XmlRootElement(name = "personalAccessTokens")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonalAccessTokenInfoList {

	/**
	 * List with the token info items.
	 */
	@XmlElement(name = "personalAccessToken")
	private List<PersonalAccessTokenInfo> personalAccessTokens = new ArrayList<>();
	
	/**
   * @return a copy of the list containing personal access token info items.
   */
  public List<PersonalAccessTokenInfo> getPersonalAccessTokens() {
    return personalAccessTokens != null ? new ArrayList<>(personalAccessTokens) : null;
  }

  /**
   * @param personalAccessTokens the personal access token info items to set
   */
  public void setPersonalAccessTokens(List<PersonalAccessTokenInfo> personalAccessTokens) {
    this.personalAccessTokens = personalAccessTokens != null ? new ArrayList<>(personalAccessTokens) : null;
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

}
