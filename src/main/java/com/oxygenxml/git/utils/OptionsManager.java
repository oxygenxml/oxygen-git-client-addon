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
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(OptionsManager.class);
	
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
					logger.warn("Options not loaded: " + e,  e);
				}

			}
	}

	private File getOptionsFile() {
		File baseDir = WorkspaceAccessPlugin.getInstance().getDescriptor().getBaseDir();
		return new File(baseDir, REPOSITORY_FILENAME);
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
		List<UserCredentials> credentials = options.getUserCredentialsList().getCredentials();
		for (Iterator<UserCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
			UserCredentials alreadyHere = (UserCredentials) iterator.next();
			if (alreadyHere.getHost().equals(userCredentials.getHost())) {
				//Replace.
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

		Cipher cipher = new Cipher();
		password = cipher.decrypt(password);

		UserCredentials userCredentials = new UserCredentials(username, password, host);
		return userCredentials;
	}
	
	public boolean isAlwaysSave(){
		loadRepositoryOptions();
		
		return options.isAlwaysSave();
	}
	
	public void setAlwaysSave(boolean alwaysSave){
		loadRepositoryOptions();
		options.setAlwaysSave(alwaysSave);
		
		saveRepositoryOptions();
	}

	
}
