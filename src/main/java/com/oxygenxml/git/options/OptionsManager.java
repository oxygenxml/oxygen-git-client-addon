package com.oxygenxml.git.options;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.OxygenGitPlugin;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Used to save and load different plugin options
 * 
 * @author Beniamin Savu
 *
 */
public class OptionsManager {
  /**
   * Maximum number of locations stored in history.
   */
  private static final int HISTORY_MAX_COUNT = 20;
  /**
   * Maximum number of destinations stored in history.
   */
  private static final int DESTINATIONS_MAX_COUNT = 20;
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
  private static final Logger LOGGER = Logger.getLogger(OptionsManager.class);

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
            LOGGER.warn("Options file doesn't exist:" + optionsFileForTests.getAbsolutePath());
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
        LOGGER.warn("Options not loaded: " + e, e);
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
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e, e);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
    
    if (save) {
      // pluginWorkspace and optionsStorage can be null from tests.
      PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
      if (pluginWorkspace != null) {
        WSOptionsStorage optionsStorage = pluginWorkspace.getOptionsStorage();
        if (optionsStorage != null) {
          optionsStorage.setOption(
              GIT_PLUGIN_OPTIONS,
              pluginWorkspace.getXMLUtilAccess().escapeTextValue(optionsWriter.toString()));
        }
      }
    }

  }
  
  /**
   * Save the new view mode for the staged resources.
   * 
   * @param stagedResViewMode The view mode.
   */
  public void saveStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    getOptions().setStagedResViewMode(stagedResViewMode);
  }
  
  /**
   * Init and get the options.
   * @return The initialized options.
   */
  private OptionsInterface getOptions() {
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
   * 
   * @param unstagedResViewMode The view mode.
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
    if(locations.size() > HISTORY_MAX_COUNT) {
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
    // TODO Bad technique. make the removval and set the new paths back into options.
    getOptions().getRepositoryLocations().getLocations().remove(path);

    saveOptions();
  }
  
  /**
   * Remove repository locations.
   * 
   * @param paths The locations/paths of the repositories to remove.
   */
  public void removeRepositoryLocations(Collection<String> paths) {
    // TODO Bad technique. make the removval and set the new paths back into options.
    getOptions().getRepositoryLocations().getLocations().removeAll(paths);

    saveOptions();
  }
  
  /**
   * Gets the user personal access token info item for a given host.
   * 
   * @param host The host.
   * 
   * @return the token info items. Never <code>null</code>.
   */
  public PersonalAccessTokenInfo getPersonalAccessTokenInfo(String host) {
    String decryptedTokenValue = null;
    if (host != null) {
      String tokenVal = null;
      List<PersonalAccessTokenInfo> tokens = getOptions().getPersonalAccessTokensList().getPersonalAccessTokens();
      if (tokens != null) { 
        for (PersonalAccessTokenInfo token : tokens) {
          if (host.equals(token.getHost())) {
            tokenVal = token.getTokenValue();
            break;
          }
        }
      }
      if (OxygenGitPlugin.getInstance() != null) {
        decryptedTokenValue = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
            .getUtilAccess()
            .decrypt(tokenVal);
      }
    }
    return new PersonalAccessTokenInfo(host, decryptedTokenValue);
  }

  /**
   * Saves the user credentials.
   * 
   * @param credentials The credentials to be saved.
   */
  public void saveGitCredentials(CredentialsBase credentials) {
    if (credentials != null) {
      // Keep only one type of credentials for a host
      CredentialsType type = credentials.getType();
      if (type == CredentialsType.USER_AND_PASSWORD) {
        saveUserAndPasswordCredentials((UserAndPasswordCredentials) credentials);
        savePersonalAccessToken(null);
      } else if (type == CredentialsType.PERSONAL_ACCESS_TOKEN) {
        savePersonalAccessToken((PersonalAccessTokenInfo) credentials);
        saveUserAndPasswordCredentials(null);
      }
    } else {
      saveUserAndPasswordCredentials(null);
      savePersonalAccessToken(null);
    }
  }

  /**
   * Save user and password credentials.
   * 
   * @param userAndPasswordCredentials User and password credentials.
   */
  private void saveUserAndPasswordCredentials(UserAndPasswordCredentials userAndPasswordCredentials) {
    if (userAndPasswordCredentials == null) {
      // Reset
      getOptions().getUserCredentialsList().setCredentials(null);
    } else {
      String encryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .getUtilAccess().encrypt(userAndPasswordCredentials.getPassword());
      
      UserAndPasswordCredentials uc = new UserAndPasswordCredentials();
      uc.setPassword(encryptedPassword);
      uc.setUsername(userAndPasswordCredentials.getUsername());
      uc.setHost(userAndPasswordCredentials.getHost());

      List<UserAndPasswordCredentials> credentials = getOptions().getUserCredentialsList().getCredentials();
      if (credentials != null) {
        for (Iterator<UserAndPasswordCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
          UserAndPasswordCredentials alreadyHere = iterator.next();
          if (alreadyHere.getHost().equals(uc.getHost())) {
            // Replace.
            iterator.remove();
            break;
          }
        }
        credentials.add(uc);
        getOptions().getUserCredentialsList().setCredentials(credentials);
      } else {
        getOptions().getUserCredentialsList().setCredentials(Arrays.asList(uc));
      }
    }
    saveOptions();
  }
  
  /**
   * Save personal access token.
   * 
   * @param tokenInfo Personal access token info.
   */
  private void savePersonalAccessToken(PersonalAccessTokenInfo tokenInfo) {
    if (tokenInfo == null) {
      // Reset
      getOptions().getPersonalAccessTokensList().setPersonalAccessTokens(null);
    } else {
      StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      String encryptedToken = pluginWS.getUtilAccess().encrypt(tokenInfo.getTokenValue());
      PersonalAccessTokenInfo paTokenInfo = new PersonalAccessTokenInfo(tokenInfo.getHost(), encryptedToken);
      
      List<PersonalAccessTokenInfo> personalAccessTokens = 
          getOptions().getPersonalAccessTokensList().getPersonalAccessTokens();
      if (personalAccessTokens != null) {
        for (Iterator<PersonalAccessTokenInfo> iterator = personalAccessTokens.iterator(); iterator.hasNext();) {
          PersonalAccessTokenInfo alreadyHere = iterator.next();
          if (alreadyHere.getHost().equals(paTokenInfo.getHost())) {
            // Replace.
            iterator.remove();
            break;
          }
        }
        personalAccessTokens.add(paTokenInfo);
        getOptions().getPersonalAccessTokensList().setPersonalAccessTokens(personalAccessTokens);
      } else {
        getOptions().getPersonalAccessTokensList().setPersonalAccessTokens(Arrays.asList(paTokenInfo));
      }
    }
    saveOptions();
  }

  /**
   * Loads the user credentials for git push and pull
   * 
   * @param host Host.
   * 
   * @return the credentials. Never <code>null</code>.
   */
  public CredentialsBase getGitCredentials(String host) {
    String username = null;
    String decryptedPassword = null;
    String decryptedToken = null;
    CredentialsType detectedCredentialsType = null;
    if (host != null) {
      Optional<CredentialsBase> credential = getAllCredentials().stream()
          .filter(c -> c.getHost().equals(host))
          .findFirst();
      if (credential.isPresent()) {
        CredentialsBase credentialsBase = credential.get();
        StandalonePluginWorkspace saPluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
        UtilAccess utilAccess = saPluginWS.getUtilAccess();
        detectedCredentialsType = credentialsBase.getType();
        if (detectedCredentialsType == CredentialsType.USER_AND_PASSWORD) {
          username = ((UserAndPasswordCredentials) credentialsBase).getUsername();
          decryptedPassword = utilAccess.decrypt(((UserAndPasswordCredentials) credentialsBase).getPassword());
        } else if (detectedCredentialsType == CredentialsType.PERSONAL_ACCESS_TOKEN) {
          decryptedToken = utilAccess.decrypt(((PersonalAccessTokenInfo) credentialsBase).getTokenValue());
        }
      }
    }

    return detectedCredentialsType != null && detectedCredentialsType == CredentialsType.PERSONAL_ACCESS_TOKEN 
        ? new PersonalAccessTokenInfo(host, decryptedToken)
            : new UserAndPasswordCredentials(username, decryptedPassword, host);
  }

  /**
   * @return All credentials: user + password ones, as well as tokens.
   */
  private List<CredentialsBase> getAllCredentials() {
    List<CredentialsBase> allCredentials = new ArrayList<>();
    List<UserAndPasswordCredentials> userAndPassCredentialsList = getOptions().getUserCredentialsList().getCredentials();
    if (userAndPassCredentialsList != null) { 
      allCredentials.addAll(userAndPassCredentialsList);
    }
    List<PersonalAccessTokenInfo> personalAccessTokens = 
        getOptions().getPersonalAccessTokensList().getPersonalAccessTokens();
    if (personalAccessTokens != null) {
      allCredentials.addAll(personalAccessTokens);
    }
    return allCredentials;
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
    if (destinationPaths.size() > DESTINATIONS_MAX_COUNT) {
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
   * 
   * @param prompt The prompt.
   * 
   * @return The stored answer for the given prompt or <code>null</code> if this question was never asked.
   */
  public Boolean getSshPromptAnswer(String prompt) {
    return getOptions().getSshPromptAnswers().get(prompt);
  }
  
  /**
   * Save SSH prompt.
   * 
   * @param prompt The prompt.
   * @param answer The answer.
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
   * @param notifyAboutNewRemoteCommits Option chosen about if to verify or not.
   */
 public void setNotifyAboutNewRemoteCommits(boolean notifyAboutNewRemoteCommits) {
   getOptions().setNotifyAboutNewRemoteCommits(notifyAboutNewRemoteCommits);
 }
 
 /**
  * Get the option about when to verify about remote changes in the repository.
  * @return Option stored about to verify or not.
  */
 public boolean isNotifyAboutNewRemoteCommits() {
   return getOptions().isNotifyAboutNewRemoteCommits();
 }
 
 /**
  * @param isCheckoutNewlyCreatedLocalBranch <code>true</code> to automatically
  * checkout a newly created local branch.
  */
 public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) {
   getOptions().setCheckoutNewlyCreatedLocalBranch(isCheckoutNewlyCreatedLocalBranch);
 }
 
 /**
  * @return <code>true</code> to automatically checkout a newly created local branch.
  */
 public boolean isCheckoutNewlyCreatedLocalBranch() {
   return getOptions().isCheckoutNewlyCreatedLocalBranch();
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

 /**
  * Set what to do when a repository is detected when opening an Oxygen project.
  *  
  * @param whatToDo What to do.
  */
 public void setWhenRepoDetectedInProject(WhenRepoDetectedInProject whatToDo) {
   getOptions().setWhenRepoDetectedInProject(whatToDo);
 }

 /**
  * @return what to do when a repo is detected inside an Oxygen project.
  */
 public WhenRepoDetectedInProject getWhenRepoDetectedInProject() {
   return getOptions().getWhenRepoDetectedInProject();
 }

  /**
   * @return <code>true</code> to update submodules after a pull.
   */
  public boolean getUpdateSubmodulesOnPull() {
    return getOptions().getUpdateSubmodulesOnPull();
  }
  
  /**
   * Sets the submodule update policy on pull.
   * 
   * @param updateSubmodules <code>true</code> to execute the equivalent of a "git submodule update --recursive".
   */
  public void setUpdateSubmodulesOnPull(boolean updateSubmodules) {
    getOptions().setUpdateSubmodulesOnPull(updateSubmodules);
  }

}
