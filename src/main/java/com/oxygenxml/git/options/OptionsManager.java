package com.oxygenxml.git.options;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitPlugin;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Used to save and load different plugin options
 * 
 * @author Beniamin Savu
 *
 */
public class OptionsManager {
  /**
   * The initial key used to saved options.
   */
	private static final String OLD_GIT_PLUGIN_OPTIONS = "MY_PLUGIN_OPTIONS";
	/**
	 * A proper name for the options.
	 */
	private static final String GIT_PLUGIN_OPTIONS = "GIT_PLUGIN_OPTIONS";

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
	 * Constant for how many project paths that have been tested for git to store
	 */
	private static final int MAXIMUM_PROJECTS_TESTED = 10;

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
	public static synchronized OptionsManager getInstance() {
	  if (instance == null) {
	    instance = new OptionsManager();
	  }
	  return instance;
	}

	/**
	 * Uses JAXB to load all the selected repositories from the users in the
	 * repositoryOptions variable
	 */
	private void loadOptions() {
		if (options == null) {
			options = new Options();
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				if (OxygenGitPlugin.getInstance() == null) {
				  // Running outside Oxygen, for example from tests.
					File optionsFile = getOptionsFile();
					if (optionsFile.exists()) {
						options = (Options) jaxbUnmarshaller.unmarshal(optionsFile);
					} else {
						logger.warn("Options file doesn't exist:" + optionsFile.getAbsolutePath());
					}
				} else {
				  // Running in Oxygen's context. Save inside Oxygen's options. 
					String option = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage()
							.getOption(OLD_GIT_PLUGIN_OPTIONS, null);

					if (option != null) {
						// Backwards.
						// 1. Load
						options = (Options) jaxbUnmarshaller.unmarshal(new StringReader(
								PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
						// 2. Reset
						PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(OLD_GIT_PLUGIN_OPTIONS, null);
						// 3. Save with the new option
						saveOptions();
					} else {
						option = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().getOption(GIT_PLUGIN_OPTIONS,
								null);
						// Load the new key if exists.
						if (option != null) {
							options = (Options) jaxbUnmarshaller.unmarshal(new StringReader(
									PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
						}
					}

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
		if (OxygenGitPlugin.getInstance() != null) {
			baseDir = OxygenGitPlugin.getInstance().getDescriptor().getBaseDir();
		} else {
			baseDir = new File("src/main/resources");
		}
		return new File(baseDir, PLUGIN_OPTIONS_FILENAME);
	}

	/**
	 * Uses JAXB to save all the selected repositories from the users in the
	 * repositoryOptions variable
	 */
	public void saveOptions() {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			if (OxygenGitPlugin.getInstance() == null) {
				jaxbMarshaller.marshal(options, getOptionsFile());
			} else {
				StringWriter stringWriter = new StringWriter();
				jaxbMarshaller.marshal(options, stringWriter);

				PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(GIT_PLUGIN_OPTIONS,
						PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().escapeTextValue(stringWriter.toString()));
			}
		} catch (JAXBException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}

	}

	/**
	 * Retrieves the repository selection list
	 * 
	 * @return a set with the repository options
	 */
	public List<String> getRepositoryEntries() {
		loadOptions();

		return options.getRepositoryLocations().getLocations();
	}

	/**
	 * Saves the given repository options
	 * 
	 * @param repositoryOption
	 *          - options to be saved
	 */
	public void addRepository(String repositoryOption) {
		loadOptions();

		LinkedList<String> locations = (LinkedList<String>) options.getRepositoryLocations().getLocations();
		locations.remove(repositoryOption);
		locations.addFirst(repositoryOption);
		if(locations.size() > 20) {
		  locations.removeLast();
		}
		
		saveOptions();
	}

	/**
	 * Saves the last selected repository from the user
	 * 
	 * @param path
	 *          - the path to the selected repository
	 */
	public void saveSelectedRepository(String path) {
		loadOptions();
		options.setSelectedRepository(path);

		saveOptions();
	}

	/**
	 * Loads the last selected repository from the user
	 * 
	 * @return the path to the selected repository
	 */
	public String getSelectedRepository() {
		loadOptions();

		return options.getSelectedRepository();
	}

	public void removeRepositoryLocation(String path) {
		loadOptions();
		options.getRepositoryLocations().getLocations().remove(path);

		saveOptions();
	}

	/**
	 * Saves the user credentials for git push and pull
	 * 
	 * @param userCredentials
	 *          - the credentials to be saved
	 */
	public void saveGitCredentials(UserCredentials userCredentials) {
		loadOptions();

		UserCredentials uc = new UserCredentials();
		String encryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.getUtilAccess().encrypt(userCredentials.getPassword());
		uc.setPassword(encryptedPassword);
		uc.setUsername(userCredentials.getUsername());
		uc.setHost(userCredentials.getHost());

		List<UserCredentials> credentials = options.getUserCredentialsList().getCredentials();
		for (Iterator<UserCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
			UserCredentials alreadyHere = iterator.next();
			if (alreadyHere.getHost().equals(uc.getHost())) {
				// Replace.
				iterator.remove();
				break;
			}
		}

		credentials.add(uc);
		saveOptions();
	}

	/**
	 * Loads the user credentials for git push and pull
	 * 
	 * @return the credentials
	 */
	public UserCredentials getGitCredentials(String host) {
		loadOptions();

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
		String decryptedPassword = null;
		if (OxygenGitPlugin.getInstance() != null) {
			decryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getUtilAccess()
					.decrypt(password);
		}
		if (decryptedPassword == null) {
			decryptedPassword = "";
		}

		return new UserCredentials(username, decryptedPassword, host);
	}

	/**
	 * Loads the last PREVIOUSLY_COMMITED_MESSAGES massages
	 * 
	 * @return a list with the previously committed messages
	 */
	public List<String> getPreviouslyCommitedMessages() {
		loadOptions();

		return options.getCommitMessages().getMessages();

	}

	/**
	 * Saves the last commit message and promotes it in front of the list
	 * 
	 * @param commitMessage
	 *          - the last commitMessage
	 */
	public void saveCommitMessage(String commitMessage) {
		loadOptions();

		List<String> messages = options.getCommitMessages().getMessages();
		if (messages.contains(commitMessage)) {
			messages.remove(commitMessage);
		}
		messages.add(0, commitMessage);
		if (messages.size() > PREVIOUSLY_COMMITED_MESSAGES) {
			messages.remove(messages.size() - 1);
		}
		options.getCommitMessages().setMessages(messages);

		saveOptions();
	}

	/**
	 * Gets the last MAXIMUM_PROJECTS_TESTED from the project view tested to be
	 * git repositories
	 * 
	 * @return a list with the last MAXIMUM_PROJECTS_TESTED paths
	 */
	public List<String> getProjectsTestedForGit() {
		loadOptions();

		return options.getProjectsTestsForGit().getPaths();
	}

	/**
	 * Saves the given project path from the project view
	 * 
	 * @param projectPath
	 *          - the project path to be saved
	 */
	public void saveProjectTestedForGit(String projectPath) {
		loadOptions();

		List<String> projectsPath = options.getProjectsTestsForGit().getPaths();
		projectsPath.add(projectPath);
		if (projectsPath.size() > MAXIMUM_PROJECTS_TESTED) {
			projectsPath.remove(0);
		}
		options.getProjectsTestsForGit().setPaths(projectsPath);

		saveOptions();
	}

	/**
	 * Save the last destination path entered by the user when he successfully
	 * clones a repository
	 * 
	 * @param destinationPath
	 *          - the destination path entered by the user
	 */
	public void saveDestinationPath(String destinationPath) {
		loadOptions();

		LinkedList<String> destinationPaths = (LinkedList<String>) options.getDestinationPaths().getPaths();
		destinationPaths.remove(destinationPath);
		destinationPaths.add(0, destinationPath);
		if (destinationPaths.size() > 20) {
		  destinationPaths.removeLast();
		}

		saveOptions();
	}

	/**
	 * Loads and returns all the destination paths entered by the user
	 * 
	 * @return a list containing the destinations paths
	 */
	public List<String> getDestinationPaths() {
		loadOptions();

		return options.getDestinationPaths().getPaths();
	}
	
	 /**
	  * Returns the stored answer for the given prompt.
   * @return The stored answer for the given prompt or <code>null</code> if this question was never asked.
   */
  public Boolean getSshPromptAnswer(String prompt) {
    loadOptions();
    
    return options.getSshPromptAnswers().get(prompt);
  }
  
  /**
   * @return A cache for asking the user for connection message.
   */
  public void saveSshPrompt(String prompt, boolean answer) {
    loadOptions();

    options.getSshPromptAnswers().put(prompt, answer);
    
    saveOptions();
  }

	/**
	 * Saves and encrypts the SSH pass phrase entered by the user
	 * 
	 * @param passphrase
	 *          - the SSH pass phrase
	 */
	public void saveSshPassphare(String passphrase) {
		loadOptions();

		String encryptPassphrase = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.getUtilAccess().encrypt(passphrase);
		options.setPassphrase(encryptPassphrase);
	}

	/**
	 * Loads the SSH pass phrase that was entered by the user
	 * 
	 * @return the SSH pass phrase
	 */
	public String getSshPassphrase() {
		loadOptions();

		String decryptPassphrase = null;
		if (OxygenGitPlugin.getInstance() != null) {
			decryptPassphrase = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
					.getUtilAccess().decrypt(options.getPassphrase());
		}
		if (decryptPassphrase == null) {
			decryptPassphrase = "";
		}

		return decryptPassphrase;
	}
}
