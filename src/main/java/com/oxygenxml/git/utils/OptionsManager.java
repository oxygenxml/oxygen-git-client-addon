package com.oxygenxml.git.utils;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.WorkspaceAccessPlugin;
import com.oxygenxml.git.options.Options;
import com.oxygenxml.git.options.UserCredentials;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Used to save and load different user options
 * 
 * @author Beniamin Savu
 *
 */
public class OptionsManager {
	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(OptionsManager.class);

	/**
	 * The filename in which all the options are saved
	 */
	private static final String PLUGIN_OPTIONS_FILENAME = "Options.xml";

	/**
	 * Constant for how many commits messages to be saved
	 */
	private static final int PREVIOUSLY_COMMITED_MESSAGES = 7;

	/**
	 * All Repositories that were selected by the user with their options
	 */
	private Options options = null;

	/**
	 * Singleton instance.
	 */
	private static OptionsManager instance;

	/**
	 * Gets the singleton instance
	 * 
	 * @return singleton instance
	 */
	public static OptionsManager getInstance() {
		if (instance == null) {
			instance = new OptionsManager();
		}
		return instance;
	}

	/**
	 * Uses JAXB to load all the selected repositories from the users in the
	 * repositoryOptions variable
	 */
	private void loadRepositoryOptions() {
		if (options == null) {
			options = new Options();
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				File optionsFile = getOptionsFile();
				if (optionsFile.exists()) {
					options = (Options) jaxbUnmarshaller.unmarshal(optionsFile);
				} else {
					logger.warn("Options file doesn't exist:" + optionsFile.getAbsolutePath());
				}
			} catch (JAXBException e) {
				logger.warn("Options not loaded: " + e, e);
			}

		}
	}

	/**
	 * Creates the the options file and returns it
	 * 
	 * @return the options file
	 */
	private File getOptionsFile() {
		File baseDir = null;
		if (WorkspaceAccessPlugin.getInstance() != null) {
			baseDir = WorkspaceAccessPlugin.getInstance().getDescriptor().getBaseDir();
		} else {
			baseDir = new File("src/main/resources");
		}
		return new File(baseDir, PLUGIN_OPTIONS_FILENAME);
	}

	/**
	 * Uses JAXB to save all the selected repositories from the users in the
	 * repositoryOptions variable
	 */
	private void saveRepositoryOptions() {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(options, getOptionsFile());
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * Retrieves the repository selection list
	 * 
	 * @return a set with the repository options
	 */
	public Set<String> getRepositoryEntries() {
		loadRepositoryOptions();

		return options.getRepositoryLocations().getLocations();
	}

	/**
	 * Saves the given repository options
	 * 
	 * @param repositoryOption
	 *          - options to be saved
	 */
	public void addRepository(String repositoryOption) {
		loadRepositoryOptions();

		options.getRepositoryLocations().getLocations().add(repositoryOption);
		saveRepositoryOptions();
	}

	/**
	 * Saves the last selected repository from the user
	 * 
	 * @param path
	 *          - the path to the selected repository
	 */
	public void saveSelectedRepository(String path) {
		loadRepositoryOptions();
		options.setSelectedRepository(path);

		saveRepositoryOptions();
	}

	/**
	 * Loads the last selected repository from the user
	 * 
	 * @return the path to the selected repository
	 */
	public String getSelectedRepository() {
		loadRepositoryOptions();

		return options.getSelectedRepository();
	}

	public void removeSelectedRepository(String path) {
		loadRepositoryOptions();
		options.getRepositoryLocations().getLocations().remove(path);

		saveRepositoryOptions();
	}

	/**
	 * Saves the user credentials for git push and pull
	 * 
	 * @param userCredentials
	 *          - the credentials to be saved
	 */
	public void saveGitCredentials(UserCredentials userCredentials) {
		loadRepositoryOptions();

		String encryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.getUtilAccess().encrypt(userCredentials.getPassword());
		userCredentials.setPassword(encryptedPassword);
		List<UserCredentials> credentials = options.getUserCredentialsList().getCredentials();
		for (Iterator<UserCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
			UserCredentials alreadyHere = (UserCredentials) iterator.next();
			if (alreadyHere.getHost().equals(userCredentials.getHost())) {
				// Replace.
				iterator.remove();
				break;
			}
		}

		credentials.add(userCredentials);

		saveRepositoryOptions();

	}

	/**
	 * Loads the user credentials for git push and pull
	 * 
	 * @return the credentials
	 */
	public UserCredentials getGitCredentials(String host) {
		loadRepositoryOptions();

		List<UserCredentials> userCredentialsList = options.getUserCredentialsList().getCredentials();
		String username = "";
		String password = "";
		for (UserCredentials credential : userCredentialsList) {
			if (host.equals(credential.getHost())) {
				username = credential.getUsername();
				password = credential.getPassword();
				break;
			}
		}

		String decryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.getUtilAccess().decrypt(password);
		if (decryptedPassword == null) {
			decryptedPassword = "";
		}

		UserCredentials userCredentials = new UserCredentials(username, decryptedPassword, host);
		return userCredentials;
	}

	/**
	 * Loads the last PREVIOUSLY_COMMITED_MESSAGES massages
	 * 
	 * @return a list with the previously committed messages
	 */
	public List<String> getPreviouslyCommitedMessages() {
		loadRepositoryOptions();

		return options.getCommitMessages().getMessages();

	}

	/**
	 * Saves the last commit message and promotes it in front of the list
	 * 
	 * @param commitMessage
	 *          - the last commitMessage
	 */
	public void saveCommitMessage(String commitMessage) {
		loadRepositoryOptions();

		List<String> messages = options.getCommitMessages().getMessages();
		if (messages.contains(commitMessage)) {
			messages.remove(commitMessage);
		}
		messages.add(0, commitMessage);
		if (messages.size() > PREVIOUSLY_COMMITED_MESSAGES) {
			messages.remove(messages.size() - 1);
		}
		options.getCommitMessages().setMessages(messages);

		saveRepositoryOptions();
	}

}
