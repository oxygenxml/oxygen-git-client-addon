package com.oxygenxml.git.options;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitPlugin;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.event.PullType;

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
  private static final String OPTIONS_FILENAME_FOR_TESTS = "Options.xml";

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
          File optionsFileForTests = getOptionsFileForTests();
          if (optionsFileForTests.exists()) {
            options = (Options) jaxbUnmarshaller.unmarshal(optionsFileForTests);
          } else {
            logger.warn("Options file doesn't exist:" + optionsFileForTests.getAbsolutePath());
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
   * !!! FOR TESTS !!!
   * 
   * Creates the the options file and returns it
   * 
   * @return the options file
   */
  private File getOptionsFileForTests() {
    File baseDir = null;
    if (OxygenGitPlugin.getInstance() != null) {
      baseDir = OxygenGitPlugin.getInstance().getDescriptor().getBaseDir();
    } else {
      baseDir = new File("src/test/resources");
    }
    return new File(baseDir, OPTIONS_FILENAME_FOR_TESTS);
  }

  /**
   * Save options.
   */
  public void saveOptions() {
    boolean save = true;
    StringWriter optionsWriter = new StringWriter();
    
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(JAXBContext.class.getClassLoader());

      JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      if (OxygenGitPlugin.getInstance() == null) {
        jaxbMarshaller.marshal(getOptions(), getOptionsFileForTests());
      } else {
        jaxbMarshaller.marshal(getOptions(), optionsWriter);
      }
    } catch (JAXBException e) {
      save = false;
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
    
    if (save) {
      PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(GIT_PLUGIN_OPTIONS,
          PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().escapeTextValue(optionsWriter.toString()));
    }

  }
  
  /**
   * Save the new view mode for the staged resources.
   */
  public void saveStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    getOptions().setStagedResViewMode(stagedResViewMode);
  }
  
  /**
   * Init and get the options.
   * @return The initialized options.
   */
  private Options getOptions() {
	  loadOptions();
	  return options;
  }
  
  /**
   * Save the default pull type: with merge or with rebase.
   * 
   * @param pullType The pull type.
   */
  public void saveDefaultPullType(PullType pullType) {
    getOptions().setDefaultPullType(pullType);
  }
  
  /**
   * Save the new view mode for the unstaged resources.
   */
  public void saveUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) {
    getOptions().setUnstagedResViewMode(unstagedResViewMode);
  }

  /**
   * Retrieves the repository selection list
   * 
   * @return a set with the repository options
   */
  public List<String> getRepositoryEntries() {
    return getOptions().getRepositoryLocations().getLocations();
  }

  /**
   * Saves the given repository options
   * 
   * @param repositoryOption
   *          - options to be saved
   */
  public void addRepository(String repositoryOption) {
    LinkedList<String> locations = (LinkedList<String>) getOptions().getRepositoryLocations().getLocations();
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
    getOptions().setSelectedRepository(path);

    saveOptions();
  }

  /**
   * Loads the last selected repository from the user
   * 
   * @return the path to the selected repository
   */
  public String getSelectedRepository() {
    return getOptions().getSelectedRepository();
  }

  /**
   * Remove repository location.
   * 
   * @param path The location/path of the repository.
   */
  public void removeRepositoryLocation(String path) {
    getOptions().getRepositoryLocations().getLocations().remove(path);

    saveOptions();
  }
  
  /**
   * Remove repository locations.
   * 
   * @param paths The locations/paths of the repositories to remove.
   */
  public void removeRepositoryLocations(Collection<String> paths) {
    getOptions().getRepositoryLocations().getLocations().removeAll(paths);

    saveOptions();
  }

  /**
   * Saves the user credentials for git push and pull
   * 
   * @param userCredentials
   *          - the credentials to be saved
   */
  public void saveGitCredentials(UserCredentials userCredentials) {
    if (userCredentials == null) {
      // Reset
      getOptions().getUserCredentialsList().setCredentials(null);
    } else {
      String encryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .getUtilAccess().encrypt(userCredentials.getPassword());
      
      UserCredentials uc = new UserCredentials();
      uc.setPassword(encryptedPassword);
      uc.setUsername(userCredentials.getUsername());
      uc.setHost(userCredentials.getHost());

      List<UserCredentials> credentials = getOptions().getUserCredentialsList().getCredentials();
      if (credentials != null) {
        for (Iterator<UserCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
          UserCredentials alreadyHere = iterator.next();
          if (alreadyHere.getHost().equals(uc.getHost())) {
            // Replace.
            iterator.remove();
            break;
          }
        }
        credentials.add(uc);
      } else {
        getOptions().getUserCredentialsList().setCredentials(Arrays.asList(uc));
      }
    }
    saveOptions();
  }

  /**
   * Loads the user credentials for git push and pull
   * 
   * @return the credentials. Never <code>null</code>.
   */
  public UserCredentials getGitCredentials(String host) {
    String username = null;
    String decryptedPassword = null;
    if (host != null) {
      String password = null;
      List<UserCredentials> userCredentialsList = getOptions().getUserCredentialsList().getCredentials();
      if (userCredentialsList != null) { 
        for (UserCredentials credential : userCredentialsList) {
          if (host.equals(credential.getHost())) {
            username = credential.getUsername();
            password = credential.getPassword();
            break;
          }
        }
      }
      if (OxygenGitPlugin.getInstance() != null) {
        decryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getUtilAccess()
            .decrypt(password);
      }
    }

    return new UserCredentials(username, decryptedPassword, host);
  }

  /**
   * Loads the last PREVIOUSLY_COMMITED_MESSAGES massages
   * 
   * @return a list with the previously committed messages
   */
  public List<String> getPreviouslyCommitedMessages() {
    return getOptions().getCommitMessages().getMessages();
  }

  /**
   * Saves the last commit message and promotes it in front of the list
   * 
   * @param commitMessage
   *          - the last commitMessage
   */
  public void saveCommitMessage(String commitMessage) {
    List<String> messages = getOptions().getCommitMessages().getMessages();
    if (messages.contains(commitMessage)) {
      messages.remove(commitMessage);
    }
    messages.add(0, commitMessage);
    if (messages.size() > PREVIOUSLY_COMMITED_MESSAGES) {
      messages.remove(messages.size() - 1);
    }
    getOptions().getCommitMessages().setMessages(messages);

    saveOptions();
  }

  /**
   * Gets the last MAXIMUM_PROJECTS_TESTED from the project view tested to be
   * git repositories
   * 
   * @return a list with the last MAXIMUM_PROJECTS_TESTED paths
   */
  public List<String> getProjectsTestedForGit() {
    return getOptions().getProjectsTestsForGit().getPaths();
  }

  /**
   * Saves the given project path from the project view
   * 
   * @param projectPath
   *          - the project path to be saved
   */
  public void saveProjectTestedForGit(String projectPath) {
    List<String> projectsPath = getOptions().getProjectsTestsForGit().getPaths();
    projectsPath.add(projectPath);
    if (projectsPath.size() > MAXIMUM_PROJECTS_TESTED) {
      projectsPath.remove(0);
    }
    getOptions().getProjectsTestsForGit().setPaths(projectsPath);

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
    LinkedList<String> destinationPaths = (LinkedList<String>) getOptions().getDestinationPaths().getPaths();
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
    return getOptions().getDestinationPaths().getPaths();
  }
  
  /**
   * @return the staged resources view mode: tree or table.
   */
  public ResourcesViewMode getStagedResViewMode() {
    return getOptions().getStagedResViewMode();
  }
  
  /**
   * @return The default pull type.
   */
  public PullType getDefaultPullType() {
    return getOptions().getDefaultPullType();
  }
  
  /**
   * @return the unstaged resources view mode: tree or table.
   */
  public ResourcesViewMode getUntagedResViewMode() {
    return getOptions().getUnstagedResViewMode();
  }
  
   /**
    * Returns the stored answer for the given prompt.
   * @return The stored answer for the given prompt or <code>null</code> if this question was never asked.
   */
  public Boolean getSshPromptAnswer(String prompt) {
    return getOptions().getSshPromptAnswers().get(prompt);
  }
  
  /**
   * @return A cache for asking the user for connection message.
   */
  public void saveSshPrompt(String prompt, boolean answer) {
    getOptions().getSshPromptAnswers().put(prompt, answer);
    
    saveOptions();
  }

  /**
   * Saves and encrypts the SSH pass phrase entered by the user
   * 
   * @param passphrase
   *          - the SSH pass phrase
   */
  public void saveSshPassphare(String passphrase) {
    String encryptPassphrase = passphrase == null ? null
        : ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
              .getUtilAccess().encrypt(passphrase);
    getOptions().setPassphrase(encryptPassphrase);
  }

  /**
   * Loads the SSH pass phrase that was entered by the user
   * 
   * @return the SSH pass phrase
   */
  public String getSshPassphrase() {
    String decryptPassphrase = null;
    if (OxygenGitPlugin.getInstance() != null) {
      decryptPassphrase = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .getUtilAccess().decrypt(getOptions().getPassphrase());
    }
    if (decryptPassphrase == null) {
      decryptPassphrase = "";
    }

    return decryptPassphrase;
  }
  
  public boolean isAutoPushWhenCommitting() {
    return getOptions().isAutoPushWhenCommitting();
  }
  
  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting) {
    getOptions().setAutoPushWhenCommitting(isAutoPushWhenCommitting);
  }
  
  /**
   * Set when to verify for remote changes in the repository.
   * @param warnOnUpstreamChange Option chosen about when to verify.
   */
 public void setWarnOnUpstreamChange(String warnOnUpstreamChange) {
   getOptions().setWarnOnUpstreamChange(warnOnUpstreamChange);
 }
 /**
  * Get the option about when to verify about remote changes in the repository.
  * @return Option stored about when to verify.
  */
 public String getWarnOnUpstreamChange() {
   return getOptions().getWarnOnUpstreamChange();
 }
 /**
  * Get the ID of the latest commit fetched from a given repository.
  * @param repositoryId The repository from which to get the commit ID, obtained from
  *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
  * @return The commit ID that comes from  {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
  */
 public String getWarnOnChangeCommitId(String repositoryId) {
   return getOptions().getWarnOnChangeCommitId(repositoryId);
 }
 /**
  * Set the commit ID to the newest commit fetched from a given repository.
  * @param repositoryId The repository in which to put the commit ID, obtained from
  *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
  * @param commitId     The newest commit ID, obtained from 
  *                     {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
  */
 public void setWarnOnChangeCommitId(String repositoryId, String commitId) {
   getOptions().setWarnOnChangeCommitId(repositoryId, commitId);
 }
}
