package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CredentialsBase {
  
  public enum CredentialsType {
    USER_AND_PASSWORD,
    PERSONAL_ACCESS_TOKEN
  }

  /**
   * The host for which the username and password are validF
   */
  @XmlElement(name = "host")
  protected String host = "";
  
  /**
   * Constructor.
   */
  public CredentialsBase() {
    //
  }
  
  /**
   * Constructor.
   * 
   * @param host The host.
   */
  public CredentialsBase(String host) {
    super();
    this.host = host;
  }

  /**
   * @return The host. Can be <code>null</code>.
   */
  public String getHost() {
    return host;
  }

  /**
   * @param host The host to set.
   */
  public void setHost(String host) {
    this.host = host;
  }
  
  /**
   * @return The credentials type.
   */
  public abstract CredentialsType getType();
  
}
