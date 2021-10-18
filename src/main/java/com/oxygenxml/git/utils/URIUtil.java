package com.oxygenxml.git.utils;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.eclipse.jgit.transport.URIish;

/**
 * A utility class for URIs.
 * 
 * @author alex_jitianu
 */
public class URIUtil {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(URIUtil.class);
  /**
   * Utility class.
   */
  private URIUtil() {}
  
  /**
   * Extract the host for given URL or an empty string
   * 
   * @param url The URL where the host is extracted from.
   * 
   * @return The host.
   */
  public static String extractHostName(String url) {
    String hostName = "";
    try {
      hostName = new URIish(url).getHost();
    } catch (URISyntaxException e) {
      logger.debug(e, e);
    }
    
    return hostName;
  }
}
