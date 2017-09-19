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

/**
 * Custom authenticator used when accessing git related hosts, also to get over
 * the Oxygen's own authenticator. In other words, when accessing git related
 * hosts this overrides the Oxigen's authenticator
 * 
 * @author Beniamin savu
 *
 */
public class CustomAuthenticator {

	/**
	 * 
	 * The custom Authenticator. Replaces the Oxygen's Authenticator only for git
	 * related hosts otherwise it delegates back to the Oxygen's Authenticator
	 * 
	 * @author Beniamin Savu
	 *
	 */
	private static final class MyAuth extends Authenticator {

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

		private Set<String> binded = new HashSet<String>();

		private MyAuth(Authenticator oldAuth) throws NoSuchFieldException, SecurityException {
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
				// get the old Authenticator values that will be used to delegate back
				final String oldRequestingHost = (String) requestingHost.get(this);
				final InetAddress oldRequestingSite = (InetAddress) requestingSite.get(this);
				final int oldRequestingPort = (Integer) requestingPort.get(this);
				final String oldRequestingProtocol = (String) requestingProtocol.get(this);
				final String oldRequestingPrompt = (String) requestingPrompt.get(this);
				final String oldRequestingScheme = (String) requestingScheme.get(this);
				final URL oldRequestingURL = (URL) requestingURL.get(this);
				final RequestorType oldRequestingAuthType = (RequestorType) requestingAuthType.get(this);

				if (isBinded(getRequestingHost())) {
					// we need to return null to let our own authentication dialog
					// (LoginDialog)
					// appear for git related hosts. Thus preventing the Oxygen's
					// dialog appear
					return null;
				} else {
					// Resets the Authenticator, thus making it ready to be restored
					Method reset = Authenticator.class.getDeclaredMethod("reset");
					reset.setAccessible(true);
					reset.invoke(oldAuth);

					// Set the Authenticator current values with the values from the old
					// Authenticator
					requestingHost.set(oldAuth, oldRequestingHost);
					requestingSite.set(oldAuth, oldRequestingSite);
					requestingPort.set(oldAuth, oldRequestingPort);
					requestingProtocol.set(oldAuth, oldRequestingProtocol);
					requestingPrompt.set(oldAuth, oldRequestingPrompt);
					requestingScheme.set(oldAuth, oldRequestingScheme);
					requestingURL.set(oldAuth, oldRequestingURL);
					requestingAuthType.set(oldAuth, oldRequestingAuthType);

					// Delegate back to the old Authenticator
					Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
					getPasswordAuthentication.setAccessible(true);
					return (PasswordAuthentication) getPasswordAuthentication.invoke(oldAuth);
				}
			} catch (Exception e) {
				logger.error(e, e);
			}
			return null;
		}

		/**
		 * Check if the given host is git related host
		 * 
		 * @param requestingHost
		 *          - the host to check
		 * @return true if the host is a git host and false otherwise
		 */
		private boolean isBinded(String requestingHost) {

			return GitAccess.getInstance().getHostName().equals(requestingHost) || binded.contains(requestingHost);
		}

		/**
		 * Adds a temporary host on which my Authenticator will be active
		 * 
		 * @param hostName
		 *          - the host to add
		 */
		public void bind(String hostName) {
			binded.add(hostName);
		}

		/**
		 * 
		 * @param hostName
		 */
		public void unbind(String hostName) {
			binded.remove(hostName);
		}
	}

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CustomAuthenticator.class);

	/**
	 * This is used to check if the authenticator has been set by Oxygen, and if
	 * this is true, then on the install() method my authenticator will be used
	 * instead of Oxygen's authenticator
	 */
	private static Authenticator previousAuthenticator;

	/**
	 * Installs my custom authenticator. Gets the current authenticator by
	 * reflection and compares it with the previous one. If they are not the same
	 * instance, then my authenticator will be installed. If not, then it means
	 * that my authenticator is already installed
	 */
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
