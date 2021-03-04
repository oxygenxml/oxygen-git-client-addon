package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;

/**
 * Personal access token POJO for JAXB.
 */
@XmlRootElement(name = "personalAccessToken")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonalAccessTokenInfo {

  /**
   * The host for which the username and password are valid.
   */
  @XmlElement(name = "host")
  private String host = "";
  
  /**
   * The personal access token value.
   */
  @XmlElement(name = "tokenValue")
  private String tokenValue = "";
  
  /**
   * Default constructor.
   */
  public PersonalAccessTokenInfo() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param host       The host.
   * @param tokenValue Token value. 
   */
  public PersonalAccessTokenInfo(String host, String tokenValue) {
    this.host = host;
    this.tokenValue = tokenValue;
  }

  /**
   * @return the host.
   */
  public String getHost() {
    return host;
  }

  /**
   * @param host the host to set.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * @return the token value.
   */
  public String getTokenValue() {
    return tokenValue;
  }

  /**
   * @param tokenValue the token value to set.
   */
  public void setTokenValue(String tokenValue) {
    this.tokenValue = tokenValue;
  }

  @Override
  public String toString() {
    return "PersonalAccessTokenInfo [host=" + host + ", personalAccessToken=" + tokenValue + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + ((tokenValue == null) ? 0 : tokenValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    boolean toReturn = false;
    if (obj instanceof PersonalAccessTokenInfo) {
      PersonalAccessTokenInfo personalAccessToken = (PersonalAccessTokenInfo) obj;
      toReturn = Equaler.verifyEquals(host, personalAccessToken.getHost())
          && Equaler.verifyEquals(tokenValue, personalAccessToken.getTokenValue());
    }
    return toReturn;
  }
  
}
