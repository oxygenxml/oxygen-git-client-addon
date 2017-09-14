package com.oxygenxml.git;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;

public class CustomAuthenticator {

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

				final Field requestingHost = Authenticator.class.getDeclaredField("requestingHost");
				final Field requestingSite = Authenticator.class.getDeclaredField("requestingSite");
				final Field requestingPort = Authenticator.class.getDeclaredField("requestingPort");
				final Field requestingProtocol = Authenticator.class.getDeclaredField("requestingProtocol");
				final Field requestingPrompt = Authenticator.class.getDeclaredField("requestingPrompt");
				final Field requestingScheme = Authenticator.class.getDeclaredField("requestingScheme");
				final Field requestingURL = Authenticator.class.getDeclaredField("requestingURL");
				final Field requestingAuthType = Authenticator.class.getDeclaredField("requestingAuthType");

				requestingHost.setAccessible(true);
				requestingSite.setAccessible(true);
				requestingPort.setAccessible(true);
				requestingProtocol.setAccessible(true);
				requestingPrompt.setAccessible(true);
				requestingScheme.setAccessible(true);
				requestingURL.setAccessible(true);
				requestingAuthType.setAccessible(true);

				previousAuthenticator = new Authenticator() {

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

							if (GitAccess.getInstance().getHostName().equals(getRequestingHost())) {
								// we need to return null to let our own authentication dialog
								// (LoginDialog)
								// appear for git related hosts. Thus preventing the Oxygen's
								// dialog appear
								return null;
							} else {
								Method reset = Authenticator.class.getDeclaredMethod("reset");
								reset.setAccessible(true);
								reset.invoke(oldAuth[0]);
								requestingHost.set(oldAuth[0], oldRequestingHost);
								requestingSite.set(oldAuth[0], oldRequestingSite);
								requestingPort.set(oldAuth[0], oldRequestingPort);
								requestingProtocol.set(oldAuth[0], oldRequestingProtocol);
								requestingPrompt.set(oldAuth[0], oldRequestingPrompt);
								requestingScheme.set(oldAuth[0], oldRequestingScheme);
								requestingURL.set(oldAuth[0], oldRequestingURL);
								requestingAuthType.set(oldAuth[0], oldRequestingAuthType);

								Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
								getPasswordAuthentication.setAccessible(true);
								return (PasswordAuthentication) getPasswordAuthentication.invoke(oldAuth[0]);
							}
						} catch (Exception e) {
							logger.error(e, e);
						}
						return null;
					}
				};

				Authenticator.setDefault(previousAuthenticator);
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
