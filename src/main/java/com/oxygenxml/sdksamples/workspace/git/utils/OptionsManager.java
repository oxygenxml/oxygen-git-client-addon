package com.oxygenxml.sdksamples.workspace.git.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.RepositoryOption;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.RepositoryOptions;

/**
 * Used to save and load different user options
 * 
 * @author intern2
 *
 */
public class OptionsManager {
	private static final String REPOSITORY_FILENAME = "Repositories.xml";
	private static final String PROPERTIES_FILENAME = "Options.properties";

	/**
	 * All Repositories that were selected by the user with their options
	 */
	private RepositoryOptions repositoryOptions = null;
	
	/**
	 * Properties file to store user options
	 */
	private Properties properties = new Properties();

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
		if (repositoryOptions == null) {
			String fileName = REPOSITORY_FILENAME;
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(RepositoryOptions.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				repositoryOptions = (RepositoryOptions) jaxbUnmarshaller
						.unmarshal(new File(Constants.RESOURCES_PATH + fileName));
			} catch (JAXBException e) {
				e.printStackTrace();
			}

			if (repositoryOptions == null) {
				repositoryOptions = new RepositoryOptions();
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

			JAXBContext jaxbContext = JAXBContext.newInstance(RepositoryOptions.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(repositoryOptions, new File(Constants.RESOURCES_PATH + fileName));
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * Retrieves the repository selection list
	 * 
	 * @return a set with the repository options
	 */
	public Set<RepositoryOption> getRepositoryEntries() {
		loadRepositoryOptions();

		return repositoryOptions.getRepositoryOptions();
	}

	/**
	 * Saves the given repository options
	 * 
	 * @param repositoryOption
	 *          - options to be saved
	 */
	public void addRepository(RepositoryOption repositoryOption) {
		loadRepositoryOptions();

		repositoryOptions.getRepositoryOptions().add(repositoryOption);
		saveRepositoryOptions();
	}

	/**
	 * Saves the last selected repository from the user
	 * 
	 * @param path
	 *          - the path to the selected repository
	 */
	public void saveSelectedRepository(String path) {
		OutputStream output = null;
		try {
			output = new FileOutputStream(Constants.RESOURCES_PATH + PROPERTIES_FILENAME);
			properties.setProperty("Selected-Repository", path);
			properties.store(output, null);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads the last selected repository from the user
	 * 
	 * @return the path to the selected repository
	 */
	public String getSelectedRepository() {
		InputStream input = null;
		try {
			input = new FileInputStream(Constants.RESOURCES_PATH + PROPERTIES_FILENAME);
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return properties.getProperty("Selected-Repository");
	}

	/**
	 * Saves the user credentials for git push and pull
	 * 
	 * @param userCredentials
	 *          - the credentials to be saved
	 */
	public void saveGitCredentials(UserCredentials userCredentials) {
		OutputStream output = null;
		try {
			output = new FileOutputStream(Constants.RESOURCES_PATH + PROPERTIES_FILENAME);
			properties.setProperty("Username", userCredentials.getUsername());
			properties.setProperty("Password", userCredentials.getPassword());
			properties.store(output, null);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Loads the user credentials for git push and pull
	 * 
	 * @return the credentials
	 */
	public UserCredentials getGitCredentials() {
		InputStream input = null;
		try {
			input = new FileInputStream(Constants.RESOURCES_PATH + PROPERTIES_FILENAME);
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String username = properties.getProperty("Username");
		String password = properties.getProperty("Password");
		UserCredentials userCredentials = new UserCredentials(username, password);
		return userCredentials;
	}

}
