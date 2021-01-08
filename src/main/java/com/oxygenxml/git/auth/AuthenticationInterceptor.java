package com.oxygenxml.git.auth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Custom authenticator used when accessing Git-related hosts and also to get over
 * Oxygen's own authenticator. In other words, when accessing Git-related
 * hosts, this overrides the Oxigen's authenticator.
 * 
 * It is still thread safe since 
 * {@link Authenticator#requestPasswordAuthentication(InetAddress, int, String, String, String)}
 * uses a synchronized block when getting the data.
 * 
 * @author Beniamin savu
 *
 */
public class AuthenticationInterceptor {

	/**
   * Hidden constructor.
   */
  private AuthenticationInterceptor() {
    // Nothing.
  }

  /**
	 * 
	 * The custom Authenticator. Replaces the Oxygen's Authenticator only for git
	 * related hosts otherwise it delegates back to the Oxygen's Authenticator.
	 * 
	 * @author Beniamin Savu
	 */
	private static final class GitAuth extends Authenticator {

		// The fields for for the Authenticator. They will be retrieved using java
		// reflection
		private final Field requestingHost;
		private final Field requestingScheme;
		private final Field requestingPort;
		private final Field requestingProtocol;
		private final Field requestingURL;
		private final Field requestingPrompt;
		private final Field requestingSite;
		private final Field requestingAuthType;
		private final Authenticator oldAuth;

		/**
		 * The lists of hosts for which to intercept the authentication.
		 */
		private Set<String> boundHosts = new HashSet<>();

		/**
		 * Constructor.
		 * 
		 * @param oldAuth The wrapped authenticator.
		 * 
		 * @throws NoSuchFieldException Unable to access wrapped authenticator data.
		 */
		private GitAuth(Authenticator oldAuth) throws NoSuchFieldException {
			this.oldAuth = oldAuth;

			// Getting the fields with java reflection
			requestingHost = Authenticator.class.getDeclaredField("requestingHost");
			requestingSite = Authenticator.class.getDeclaredField("requestingSite");
			requestingPort = Authenticator.class.getDeclaredField("requestingPort");
			requestingProtocol = Authenticator.class.getDeclaredField("requestingProtocol");
			requestingPrompt = Authenticator.class.getDeclaredField("requestingPrompt");
			requestingScheme = Authenticator.class.getDeclaredField("requestingScheme");
			requestingURL = Authenticator.class.getDeclaredField("requestingURL");
			requestingAuthType = Authenticator.class.getDeclaredField("requestingAuthType");

			// Making the fields accessible
			requestingHost.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingSite.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingPort.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingProtocol.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingPrompt.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingScheme.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingURL.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
			requestingAuthType.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
		  PasswordAuthentication passwordAuth = null;
			try {
				// get the old Authenticator values that will be used to delegate back
				final String oldRequestingHost = (String) requestingHost.get(this);
				final InetAddress oldRequestingSite = (InetAddress) requestingSite.get(this);
				final int oldRequestingPort = (Integer) requestingPort.get(this);
				final String oldRequestingProtocol = (String) requestingProtocol.get(this);
				final String oldRequestingPrompt = (String) requestingPrompt.get(this);
				final String oldRequestingScheme = (String) requestingScheme.get(this);
				final URL oldRequestingURL = (URL) requestingURL.get(this);
				final RequestorType oldRequestingAuthType = (RequestorType) requestingAuthType.get(this);

				if (isBound(getRequestingHost())) {
					// we need to return null to let our own authentication dialog
					// (LoginDialog)
					// appear for git related hosts. Thus preventing the Oxygen's
					// dialog appear
				  
				  /*
				   * Ideally, we could just present the dialog here and request for credentials.
           * We chose to abort and handle the auth later on GitController,
           * because there we also get a message stating the cause: either the credentials are wrong
           * or perhaps the user doesn't have permissions on the repository. 
				   */
				} else {
					// Resets the Authenticator, thus making it ready to be restored
					Method reset = Authenticator.class.getDeclaredMethod("reset");
					reset.setAccessible(true); // NOSONAR We don't have API to intercept Oxygen's authenticator
					reset.invoke(oldAuth);

					// Set the Authenticator current values with the values from the old
					// Authenticator
					requestingHost.set(oldAuth, oldRequestingHost); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingSite.set(oldAuth, oldRequestingSite); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingPort.set(oldAuth, oldRequestingPort); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingProtocol.set(oldAuth, oldRequestingProtocol); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingPrompt.set(oldAuth, oldRequestingPrompt); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingScheme.set(oldAuth, oldRequestingScheme); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingURL.set(oldAuth, oldRequestingURL); // NOSONAR We don't have API to intercept Oxygen's authenticator
					requestingAuthType.set(oldAuth, oldRequestingAuthType); // NOSONAR We don't have API to intercept Oxygen's authenticator

					// Delegate back to the old Authenticator
					Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
					// Don't NOSONAR the following setAccessible(), because we want to be reminded that sometime
					// in the future we should replace this with some API
					getPasswordAuthentication.setAccessible(true);
					
					passwordAuth = (PasswordAuthentication) getPasswordAuthentication.invoke(oldAuth);
				}
			} catch (Exception e) {
				logger.error(e, e);
			}
			return passwordAuth;
		}
		
		/**
     * @return the boundHosts
     */
    public Set<String> getBoundHosts() {
      return boundHosts;
    }

    /**
     * @param boundHosts the bound hosts to set.
     */
    public void setBoundHosts(Set<String> boundHosts) {
      this.boundHosts = boundHosts;
    }

		/**
		 * Check if the given host is git related host
		 * 
		 * @param requestingHost
		 *          - the host to check
		 * @return true if the host is a git host and false otherwise
		 */
		private boolean isBound(String requestingHost) {
			return boundHosts.contains(requestingHost);
		}

		/**
		 * Adds a temporary host on which MyAuthenticator will be active
		 * 
		 * @param hostName
		 *          - the host to add
		 */
		public void bind(String hostName) {
			boundHosts.add(hostName);
		}

		/**
		 * Removes the temporary host on which MyAuthenticator will be active
		 * 
		 * @param hostName
		 */
		public void unbind(String hostName) {
			boundHosts.remove(hostName);
		}
	}

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(AuthenticationInterceptor.class);

	/**
	 * This is used to check if the authenticator has been set by Oxygen, and if
	 * this is true, then on the install() method my authenticator will be used
	 * instead of Oxygen's authenticator
	 */
	private static GitAuth installedAuthenticator;

	/**
	 * Installs my custom authenticator. Gets the current authenticator by
	 * reflection and compares it with the previous one. If they are not the same
	 * instance, then my authenticator will be installed. If not, then it means
	 * that my authenticator is already installed
	 */
	public static void install() {
		final Authenticator[] currentAuth = new Authenticator[1];
		try {
			Field declaredField = Authenticator.class.getDeclaredField("theAuthenticator");
			declaredField.setAccessible(true);
			currentAuth[0] = (Authenticator) declaredField.get(null);

			if (currentAuth[0] == null || currentAuth[0] != installedAuthenticator) {
			  GitAuth oldInstalledAuth = installedAuthenticator;

				installedAuthenticator = new GitAuth(currentAuth[0]);
				
				if (oldInstalledAuth != null) {
				  // Keep the previously bound hosts, if any.
				  installedAuthenticator.setBoundHosts(oldInstalledAuth.getBoundHosts());
				}
				
				setUseDefaultAuthenticator(true);
				
				Authenticator.setDefault(installedAuthenticator);
			}

		} catch (IllegalArgumentException | NoSuchFieldException | IllegalAccessException e) {
		  logger.error(e, e);
		}
	}

	/**
	 * Set to <code>true</code> to use the default Authenticator instead of the asynchronous one.
	 * 
	 * @param useDefaultAuth <code>true</code> to use the default Authenticator instead of the asynchronous one.
	 * 
	 * @throws IllegalAccessException
	 */
  private static void setUseDefaultAuthenticator(boolean useDefaultAuth) throws IllegalAccessException {
    try {
      // EXM-43925
      Class<?> webdavAsyncAuth = Class.forName("ro.sync.net.protocol.http.HttpAsyncAuthenticator");
      Method setUseDefaultAuth = webdavAsyncAuth.getMethod("setUseDefaultAuthenticator", boolean.class);
      setUseDefaultAuth.invoke(null, useDefaultAuth);
    } catch (ClassNotFoundException | NoSuchMethodException 
        | SecurityException | InvocationTargetException e) {
      // Ignore
    }
  }

	/**
	 * Binds the host to MyAuthenticator
	 * 
	 * @param hostName
	 *          - the host to bind
	 */
	public static void bind(String hostName) {
		install();

		installedAuthenticator.bind(hostName);
	}

	/**
	 * Unbinds the host from MyAuthenticator
	 * 
	 * @param hostName
	 *          - the host to unbind
	 */
	public static void unbind(String hostName) {
		try {
			if (installedAuthenticator != null) {
				installedAuthenticator.unbind(hostName);
			}
		} catch (Throwable e) { // NOSONAR
		  logger.warn(e, e);
		}
	}

}
