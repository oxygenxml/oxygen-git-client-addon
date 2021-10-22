package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.OxygenGitPlugin;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Used to save and load different plugin options
 * 
 * @author Beniamin Savu
 *
 */
public class OptionsManager {

  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(OptionsManager.class);
  /**
   * Maximum number of locations stored in history.
   */
  private static final int HISTORY_MAX_COUNT = 20;
  /**
   * Maximum number of destinations stored in history.
   */
  private static final int DESTINATIONS_MAX_COUNT = 20;

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
  private Options getOptions() {
    if (options == null) {
      String home = System.getProperty("com.oxygenxml.editor.home.url");
      if (home == null) {
        // Probably test environment.
        logger.warn("Options not initialized.");
        if (PluginWorkspaceProvider.getPluginWorkspace() != null
            && PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage() != null) {
          options = OptionsLoader.loadOptions(PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
        } else {
          options = new JaxbOptions();
        }
      } else {
        // Oxygen environment. Should not happen.
        logger.warn("Options not initialized! Custom ro.sync.exml.workspace.api.options.ExternalPersistentObject will not be loaded/saved.");
        options = OptionsLoader.loadOptions(PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
      }
    }
	  return options;
  }
  
  /**
   * Loads options from from the Oxygen options storage.
   * 
   * @param ws Oxygen API.
   */
  public void loadOptions(WSOptionsStorage wsOptionsStorage) {
    if (options == null) {
      options = OptionsLoader.loadOptions(wsOptionsStorage);
    }
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
    List<String> locations = new ArrayList<>(getOptions().getRepositoryLocations().getLocations());
    locations.remove(repositoryOption);
    locations.add(0, repositoryOption);
    if(locations.size() > HISTORY_MAX_COUNT) {
      locations.remove(locations.size() - 1);
    }
    
    RepositoryLocations newRepositoryLocations = new RepositoryLocations();
    newRepositoryLocations.setLocations(locations);
    getOptions().setRepositoryLocations(newRepositoryLocations);
  }

  /**
   * Saves the last selected repository from the user
   * 
   * @param path
   *          - the path to the selected repository
   */
  public void saveSelectedRepository(String path) {
    getOptions().setSelectedRepository(path);
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
    List<String> locations = new ArrayList<>( getOptions().getRepositoryLocations().getLocations() );
    locations.remove(path);
    
    RepositoryLocations newRepositoryLocations = new RepositoryLocations();
    newRepositoryLocations.setLocations(locations);
    getOptions().setRepositoryLocations(newRepositoryLocations);
  }
  
  /**
   * Remove repository locations.
   * 
   * @param paths The locations/paths of the repositories to remove.
   */
  public void removeRepositoryLocations(Collection<String> paths) {
    List<String> locations = new ArrayList<>( getOptions().getRepositoryLocations().getLocations() ); 
    locations.removeAll(paths);
    
    RepositoryLocations newRepositoryLocations = new RepositoryLocations();
    newRepositoryLocations.setLocations(locations);
    getOptions().setRepositoryLocations(newRepositoryLocations);
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
      List<PersonalAccessTokenInfo> tokens = new ArrayList<>( getOptions().getPersonalAccessTokensList().getPersonalAccessTokens() );
      if (getOptions().getPersonalAccessTokensList().getPersonalAccessTokens() != null) { 
        for (PersonalAccessTokenInfo token : tokens) {
          if (host.equals(token.getHost())) {
            tokenVal = token.getTokenValue();
            break;
          }
        }
      }
      if (OxygenGitPlugin.getInstance() != null) {
        decryptedTokenValue = PluginWorkspaceProvider.getPluginWorkspace()
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
      getOptions().setUserCredentialsList(new UserCredentialsList());
      
    } else {
      String encryptedPassword = PluginWorkspaceProvider.getPluginWorkspace()
          .getUtilAccess().encrypt(userAndPasswordCredentials.getPassword());
      
      UserAndPasswordCredentials uc = new UserAndPasswordCredentials();
      uc.setPassword(encryptedPassword);
      uc.setUsername(userAndPasswordCredentials.getUsername());
      uc.setHost(userAndPasswordCredentials.getHost());

      List<UserAndPasswordCredentials> credentials = new ArrayList<>( getOptions().getUserCredentialsList().getCredentials());
      if (getOptions().getUserCredentialsList().getCredentials() == null) {
        for (Iterator<UserAndPasswordCredentials> iterator = credentials.iterator(); iterator.hasNext();) {
          UserAndPasswordCredentials alreadyHere = iterator.next();
          if (alreadyHere.getHost().equals(uc.getHost())) {
            // Replace.
            iterator.remove();
            break;
          }
        }
        credentials.add(uc);
        
        UserCredentialsList newUserCredentialsList = getOptions().getUserCredentialsList(); 
        newUserCredentialsList.setCredentials(credentials);
        getOptions().setUserCredentialsList(newUserCredentialsList);
        
      } else {
        UserCredentialsList newUsrCredentialsList = getOptions().getUserCredentialsList(); 
        newUsrCredentialsList.setCredentials(Arrays.asList(uc));
        getOptions().setUserCredentialsList(newUsrCredentialsList);
      }
    }
  }
  
  /**
   * Save personal access token.
   * 
   * @param tokenInfo Personal access token info.
   */
  private void savePersonalAccessToken(PersonalAccessTokenInfo tokenInfo) {
    if (tokenInfo == null) {
      // Reset
      getOptions().setPersonalAccessTokensList(new PersonalAccessTokenInfoList());
      
    } else {
      PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
      String encryptedToken = pluginWS.getUtilAccess().encrypt(tokenInfo.getTokenValue());
      PersonalAccessTokenInfo paTokenInfo = new PersonalAccessTokenInfo(tokenInfo.getHost(), encryptedToken);
      
      List<PersonalAccessTokenInfo> personalAccessTokens = 
          new ArrayList<>(  getOptions().getPersonalAccessTokensList().getPersonalAccessTokens() );
      if (getOptions().getPersonalAccessTokensList().getPersonalAccessTokens() != null) {
        for (Iterator<PersonalAccessTokenInfo> iterator = personalAccessTokens.iterator(); iterator.hasNext();) {
          PersonalAccessTokenInfo alreadyHere = iterator.next();
          if (alreadyHere.getHost().equals(paTokenInfo.getHost())) {
            // Replace.
            iterator.remove();
            break;
          }
        }
        personalAccessTokens.add(paTokenInfo);
        
        PersonalAccessTokenInfoList newPersonalAccessTokensList = getOptions().getPersonalAccessTokensList(); 
        newPersonalAccessTokensList.setPersonalAccessTokens(personalAccessTokens);
        getOptions().setPersonalAccessTokensList(newPersonalAccessTokensList);
        
      } else {
        
        PersonalAccessTokenInfoList newPrsonalAccessTokensList = getOptions().getPersonalAccessTokensList(); 
        newPrsonalAccessTokensList.setPersonalAccessTokens(Arrays.asList(paTokenInfo));
        getOptions().setPersonalAccessTokensList(newPrsonalAccessTokensList);
      }
    }
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
        PluginWorkspace saPluginWS = PluginWorkspaceProvider.getPluginWorkspace();
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
    List<UserAndPasswordCredentials> userAndPassCredentialsList =  getOptions().getUserCredentialsList().getCredentials();
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
    List<String> messages = new ArrayList<>( getOptions().getCommitMessages().getMessages());
    if (messages.contains(commitMessage)) {
      messages.remove(commitMessage);
    }
    messages.add(0, commitMessage);
    if (messages.size() > PREVIOUSLY_COMMITED_MESSAGES) {
      messages.remove(messages.size() - 1);
    }
    
    CommitMessages newCommitMessages = new CommitMessages();
    newCommitMessages.setMessages(messages);
    getOptions().setCommitMessages(newCommitMessages);
    
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
    List<String> projectsPath =  new ArrayList<>( getOptions().getProjectsTestsForGit().getPaths() );
    projectsPath.add(projectPath);
    if (projectsPath.size() > MAXIMUM_PROJECTS_TESTED) {
      projectsPath.remove(0);
    }
    
    ProjectsTestedForGit newProjectsTestedForGit = new ProjectsTestedForGit();
    newProjectsTestedForGit.setPaths(projectsPath);
    getOptions().setProjectsTestsForGit(newProjectsTestedForGit);
  }

  /**
   * Save the last destination path entered by the user when he successfully
   * clones a repository
   * 
   * @param destinationPath
   *          - the destination path entered by the user
   */
  public void saveDestinationPath(String destinationPath) {
    List<String> destinationPaths = getOptions().getDestinationPaths().getPaths();
    destinationPaths.remove(destinationPath);
    destinationPaths.add(0, destinationPath);
    if (destinationPaths.size() > DESTINATIONS_MAX_COUNT) {
      destinationPaths.remove(destinationPaths.size() - 1);
    }
    
    DestinationPaths newDestinationPaths = new DestinationPaths();
    newDestinationPaths.setPaths(destinationPaths);
    getOptions().setDestinationPaths(newDestinationPaths);
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
    Map<String, Boolean> sshPromptAnswers = getOptions().getSshPromptAnswers(); 
    sshPromptAnswers.put(prompt, answer);
    getOptions().setSshQuestions(sshPromptAnswers);
  }

  /**
   * Saves and encrypts the SSH pass phrase entered by the user
   * 
   * @param passphrase
   *          - the SSH pass phrase
   */
  public void saveSshPassphare(String passphrase) {
    String encryptPassphrase = passphrase == null ? null
        : PluginWorkspaceProvider.getPluginWorkspace()
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
      decryptPassphrase = PluginWorkspaceProvider.getPluginWorkspace()
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
