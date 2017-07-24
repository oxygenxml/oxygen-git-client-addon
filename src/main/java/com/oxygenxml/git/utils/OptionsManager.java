package com.oxygenxml.git.utils;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.oxygenxml.git.WorkspaceAccessPlugin;
import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.jaxb.entities.Options;
import com.oxygenxml.git.jaxb.entities.UserCredentials;

/**
 * Used to save and load different user options
 * 
 * @author intern2
 *
 */
public class OptionsManager {
	/**
	 * TODO Set it from the outside.
	 * 
	 * Why not store everything into one file?
	 */
	private static final String REPOSITORY_FILENAME = "Options.xml";
			/*new File(
					WorkspaceAccessPlugin.getInstance().getDescriptor().getBaseDir(), 
					"Options.xml").getAbsolutePath();*/

	/**
	 * All Repositories that were selected by the user with their options
	 */
	private Options options = null;

	/**
	 * Singletone instance.
	 */
	private static OptionsManager instance;

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
		}
		if (options != null) {
			String fileName = REPOSITORY_FILENAME;
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(Options.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				options = (Options) jaxbUnmarshaller.unmarshal(new File(getClass().getClassLoader().getResource("Options.xml").getPath()));
			} catch (JAXBException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Uses JAXB to save all the selected repositories from the users in the
	 * repositoryOptions variable
	 */
	private void saveRepositoryOptions() {
		String fileName = REPOSITORY_FILENAME;
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(options, new File(getClass().getClassLoader().getResource("Options.xml").getPath()));
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

	/**
	 * Saves the user credentials for git push and pull
	 * 
	 * @param userCredentials
	 *          - the credentials to be saved
	 */
	public void saveGitCredentials(UserCredentials userCredentials) {
		loadRepositoryOptions();

		Cipher cipher = new Cipher();
		String password = cipher.encrypt(userCredentials.getPassword());
		userCredentials.setPassword(password);
		options.getUserCredentialsList().getCredentials().add(userCredentials);

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

		Cipher cipher = new Cipher();
		password = cipher.decrypt(password);

		UserCredentials userCredentials = new UserCredentials(username, password, host);
		return userCredentials;
	}


	
}
