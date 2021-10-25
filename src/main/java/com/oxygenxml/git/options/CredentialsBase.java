package com.oxygenxml.git.options;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import ro.sync.exml.workspace.api.options.ExternalPersistentObject;
import ro.sync.options.j;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CredentialsBase implements ExternalPersistentObject {
  
  /**
   * Credentials type.
   */
  public enum CredentialsType {
    /**
     * Username + password.
     */
    USER_AND_PASSWORD,
    /**
     * Personal access token.
     */
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
  
  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      // Should not happen.
    }
    return new CredentialsBase(host != null ? host : "") {
      @Override
      public String[] getNotPersistentFieldNames() {
        return new String[0];
      }
      @Override
      public void checkValid() throws j {
        // Nothing
      }
      @Override
      public CredentialsType getType() {
        return CredentialsBase.this.getType();
      }
    };
  }
  
}
