package com.oxygenxml.git.utils;

/**
 * System properties.
 */
public class GitAddonSystemProperties {
  
  /**
   * Hidden constructor.
   */
  private GitAddonSystemProperties() {
    // Nada
  }

  /**
   * By default the add-on uses Apache MINA for SSH operations. Setting this property to <code>true</code>
   * makes the add-on use JSCH for SSH operations.
   */
  public static final String USE_JSCH_FOR_SSH_OPERATIONS = "useJschForSSHOperations";
  
}
