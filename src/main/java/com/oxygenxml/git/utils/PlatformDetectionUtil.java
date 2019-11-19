package com.oxygenxml.git.utils;

/**
 * Platform detector, 
 */
public class PlatformDetectionUtil {

  /**
   * OS name system property.
   */
  private static final String OS_NAME = "os.name";

  /**
   * Avoid instantiation.
   */
  private PlatformDetectionUtil() {
    // Nothing
  }
  
  /**
   * @return <code>true</code> if Windows.
   */
  public static final boolean isWin() {
    String osName = System.getProperty(OS_NAME);
    osName = osName.toUpperCase();
    return osName.startsWith("WIN");
  }
  
  /**
   * @return <code>true</code> if MacOS.
   */
  public static final boolean isMacOS() {
    String osName = System.getProperty(OS_NAME);
    osName = osName.toUpperCase();
    return osName.startsWith("MAC OS") || osName.equals("MAC");
  }
  
}
