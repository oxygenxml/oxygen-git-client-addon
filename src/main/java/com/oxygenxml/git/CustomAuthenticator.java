package com.oxygenxml.git;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;

public class CustomAuthenticator {

	private static final class MyAuth extends Authenticator {
    private final Field requestingHost;
    private final Field requestingScheme;
    private final Field requestingPort;
    private final Field requestingProtocol;
    private final Field requestingURL;
    private final Field requestingPrompt;
    private final Field requestingSite;
    private final Field requestingAuthType;
    private final Authenticator oldAuth;
    
    private Set<String> binded = new HashSet<String>();

    private MyAuth(Authenticator oldAuth) throws NoSuchFieldException, SecurityException {
      this.oldAuth = oldAuth;
      
      requestingHost = Authenticator.class.getDeclaredField("requestingHost");
      requestingSite = Authenticator.class.getDeclaredField("requestingSite");
      requestingPort = Authenticator.class.getDeclaredField("requestingPort");
      requestingProtocol = Authenticator.class.getDeclaredField("requestingProtocol");
      requestingPrompt = Authenticator.class.getDeclaredField("requestingPrompt");
      requestingScheme = Authenticator.class.getDeclaredField("requestingScheme");
      requestingURL = Authenticator.class.getDeclaredField("requestingURL");
      requestingAuthType = Authenticator.class.getDeclaredField("requestingAuthType");

      requestingHost.setAccessible(true);
      requestingSite.setAccessible(true);
      requestingPort.setAccessible(true);
      requestingProtocol.setAccessible(true);
      requestingPrompt.setAccessible(true);
      requestingScheme.setAccessible(true);
      requestingURL.setAccessible(true);
      requestingAuthType.setAccessible(true);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
    	try {
    		final String oldRequestingHost = (String) requestingHost.get(this);
    		final InetAddress oldRequestingSite = (InetAddress) requestingSite.get(this);
    		final int oldRequestingPort = (Integer) requestingPort.get(this);
    		final String oldRequestingProtocol = (String) requestingProtocol.get(this);
    		final String oldRequestingPrompt = (String) requestingPrompt.get(this);
    		final String oldRequestingScheme = (String) requestingScheme.get(this);
    		final URL oldRequestingURL = (URL) requestingURL.get(this);
    		final RequestorType oldRequestingAuthType = (RequestorType) requestingAuthType.get(this);

    		System.out.println("getRequestingHost()                   " + getRequestingHost());
    		
    		if (isBinded(getRequestingHost())) {
    			// we need to return null to let our own authentication dialog
    			// (LoginDialog)
    			// appear for git related hosts. Thus preventing the Oxygen's
    			// dialog appear
    			return null;
    		} else {
    		  
    		  System.out.println("Delegate to Oxygen");
    		  
    			Method reset = Authenticator.class.getDeclaredMethod("reset");
    			reset.setAccessible(true);
    			reset.invoke(oldAuth);
    			requestingHost.set(oldAuth, oldRequestingHost);
    			requestingSite.set(oldAuth, oldRequestingSite);
    			requestingPort.set(oldAuth, oldRequestingPort);
    			requestingProtocol.set(oldAuth, oldRequestingProtocol);
    			requestingPrompt.set(oldAuth, oldRequestingPrompt);
    			requestingScheme.set(oldAuth, oldRequestingScheme);
    			requestingURL.set(oldAuth, oldRequestingURL);
    			requestingAuthType.set(oldAuth, oldRequestingAuthType);

    			Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
    			getPasswordAuthentication.setAccessible(true);
    			return (PasswordAuthentication) getPasswordAuthentication.invoke(oldAuth);
    		}
    	} catch (Exception e) {
    		logger.error(e, e);
    	}
    	return null;
    }

    private boolean isBinded(String requestingHost2) {
      
      
      System.out.println("requestingHost " + requestingHost2);
      System.out.println("Loaded " + GitAccess.getInstance().getHostName());
      System.out.println("Binded " + binded);
      
      return 
          GitAccess.getInstance().getHostName().equals(requestingHost2)
          || binded.contains(requestingHost2);
    }

    public void bind(String hostName) {
      binded.add(hostName);
    }
    
    public void unbind(String hostName) {
      binded.remove(hostName);
    }
  }

  /**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CustomAuthenticator.class);

	private static Authenticator previousAuthenticator;

  public static void install() {
    final Authenticator[] oldAuth = new Authenticator[1];
    try {

      Field declaredField = Authenticator.class.getDeclaredField("theAuthenticator");
      declaredField.setAccessible(true);
      oldAuth[0] = (Authenticator) declaredField.get(null);

      if (oldAuth[0] != previousAuthenticator) {

        previousAuthenticator = new MyAuth(oldAuth[0]);

        Authenticator.setDefault(previousAuthenticator);
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  
  public static void bind(String hostName) {
    install();
    
    ((MyAuth) previousAuthenticator).bind(hostName);
  }
  
  
  public static void unbind(String hostName) {
    try {
      if (previousAuthenticator != null) {
        ((MyAuth) previousAuthenticator).unbind(hostName);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
	
	
	
}
