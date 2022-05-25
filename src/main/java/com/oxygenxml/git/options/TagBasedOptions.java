package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * Class used for the new way of saving options. Each option is saved independently using WSOptionStorage API
 * 
 * @author alex_jitianu
 * @author gabriel_nedianu
 *
 */
public class TagBasedOptions implements Options {
  /**
   * The number of user and password credential fields for one object.
   * 3 for: user, password,  host.
   */
  private static final int NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT = 3;
  
  /**
   * The number of token fields for one object.
   * 2 for: token value, host.
   */
  private static final int NO_OF_TOKEN_FIELDS_PER_OBJECT = 2;

  /**
   * Default boolean value "True"
   */
  private static final String TRUE = "true";
  
  /**
   * Default boolean value "False"
   */
  private static final String FALSE = "false";
  
  /**
   * WSOptionsStorage supports for saving and retrieving custom options in the Oxygen common preferences.
   */
  private WSOptionsStorage wsOptionsStorage;

  /**
   * Constructor 
   * 
   * @param wsOptionsStorage
   */
  public TagBasedOptions(WSOptionsStorage wsOptionsStorage) {
    this.wsOptionsStorage = wsOptionsStorage;
  }

  @Override
  public boolean isAutoPushWhenCommitting() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.AUTO_PUSH_WHEN_COMMITTING, FALSE));
  }

  @Override
  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting) {
    wsOptionsStorage.setOption(OptionTags.AUTO_PUSH_WHEN_COMMITTING, String.valueOf(isAutoPushWhenCommitting));
  }

  @Override
  public PullType getDefaultPullType() {
    String pullType = wsOptionsStorage.getOption(OptionTags.DEFAULT_PULL_TYPE, String.valueOf(PullType.MERGE_FF));
    return PullType.valueOf(pullType);
  }

  @Override
  public void setDefaultPullType(PullType defaultPullType) {
    wsOptionsStorage.setOption(OptionTags.DEFAULT_PULL_TYPE, String.valueOf(defaultPullType));

  }

  @Override
  public ResourcesViewMode getUnstagedResViewMode() {
    String unstagedResViewMode = wsOptionsStorage.getOption(OptionTags.UNSTAGED_RES_VIEW_MODE, String.valueOf(ResourcesViewMode.FLAT_VIEW));
    return ResourcesViewMode.valueOf(unstagedResViewMode);
  }

  @Override
  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) {
    wsOptionsStorage.setOption(OptionTags.UNSTAGED_RES_VIEW_MODE, String.valueOf(unstagedResViewMode));

  }

  @Override
  public ResourcesViewMode getStagedResViewMode() {
    String stagedResViewMode = wsOptionsStorage.getOption(OptionTags.STAGED_RES_VIEW_MODE, String.valueOf(ResourcesViewMode.FLAT_VIEW));
    return ResourcesViewMode.valueOf(stagedResViewMode);
  }

  @Override
  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    wsOptionsStorage.setOption(OptionTags.STAGED_RES_VIEW_MODE, String.valueOf(stagedResViewMode));

  }

  @Override
  public DestinationPaths getDestinationPaths() {
    String[] stringDestinationPaths = wsOptionsStorage.getStringArrayOption(OptionTags.DESTINATION_PATHS, new String[0]);
    DestinationPaths destinationPaths = new DestinationPaths();
    destinationPaths.setPaths(new LinkedList<>(Arrays.asList(stringDestinationPaths)));
    
    return destinationPaths;
  }

  @Override
  public void setDestinationPaths(DestinationPaths destinationPaths) {
    List<String> destPaths = destinationPaths.getPaths();
    
    wsOptionsStorage.setStringArrayOption(
        OptionTags.DESTINATION_PATHS,
        destPaths != null ? destPaths.toArray(new String[0]) : new String[0]);

  }

  @Override
  public ProjectsTestedForGit getProjectsTestsForGit() {
    String[] stringProjectsTestedForGit = wsOptionsStorage.getStringArrayOption(OptionTags.PROJECTS_TESTED_FOR_GIT, new String[0]);
    ProjectsTestedForGit projectsTestedForGit = new ProjectsTestedForGit();
    projectsTestedForGit.setPaths(Arrays.asList(stringProjectsTestedForGit));
    
    return projectsTestedForGit;
  }

  @Override
  public void setProjectsTestsForGit(ProjectsTestedForGit projectsTestsForGit) {
    List<String> projTests = projectsTestsForGit.getPaths();
    
    wsOptionsStorage.setStringArrayOption(
        OptionTags.PROJECTS_TESTED_FOR_GIT,
        projTests != null? projTests.toArray(new String[0]) : new String[0]) ;

  }

  @Override
  public RepositoryLocations getRepositoryLocations() {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.REPOSITORY_LOCATIONS, new String[0]);
    
    RepositoryLocations repositoryLocations = new RepositoryLocations();
    repositoryLocations.setLocations(Arrays.asList(stringArrayOption));
    return repositoryLocations;
  }

  @Override
  public void setRepositoryLocations(RepositoryLocations repositoryLocations) {
    List<String> locations = repositoryLocations.getLocations();
    
    wsOptionsStorage.setStringArrayOption(
        OptionTags.REPOSITORY_LOCATIONS, 
        locations != null ? locations.toArray(new String[0]) : new String[0]);
  }

  @Override
  public void setNotifyAboutNewRemoteCommits(boolean notifyAboutNewRemoteCommits) {
    wsOptionsStorage.setOption(OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS, String.valueOf(notifyAboutNewRemoteCommits));
    
  }
  
  @Override
  public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) {
    wsOptionsStorage.setOption(OptionTags.CHECKOUT_NEWLY_CREATED_LOCAL_BRANCH, String.valueOf(isCheckoutNewlyCreatedLocalBranch));
    
  }

  @Override
  public boolean isNotifyAboutNewRemoteCommits() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS, FALSE));
  }

  @Override
  public boolean isCheckoutNewlyCreatedLocalBranch() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.CHECKOUT_NEWLY_CREATED_LOCAL_BRANCH, FALSE));
  }

  @Override
  public Map<String, String> getWarnOnChangeCommitId() {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.WARN_ON_CHANGE_COMMIT_ID, new String[0]);

    return arrayToMap(stringArrayOption);
  }

  @Override
  public String getWarnOnChangeCommitId(String repositoryId) {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.WARN_ON_CHANGE_COMMIT_ID, new String[0]);
    Map<String, String> warnOnChangeCommitId = arrayToMap(stringArrayOption);
    
    return warnOnChangeCommitId.getOrDefault(repositoryId, "");
  }

  @Override
  public void setWarnOnChangeCommitId(String repositoryId, String commitId) {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.WARN_ON_CHANGE_COMMIT_ID, new String[0]);
    Map<String, String> oldOpt = arrayToMap(stringArrayOption);
    oldOpt.put(repositoryId, commitId);
    
    String[] newOpt = mapToArray(oldOpt);
    wsOptionsStorage.setStringArrayOption(OptionTags.WARN_ON_CHANGE_COMMIT_ID, newOpt);
  }
  

  @Override
  public String getSelectedRepository() {
    return wsOptionsStorage.getOption(OptionTags.SELECTED_REPOSITORY, "");
    
  }

  @Override
  public void setSelectedRepository(String selectedRepository) {
    wsOptionsStorage.setOption(OptionTags.SELECTED_REPOSITORY, selectedRepository);

  }

  @Override
  public UserCredentialsList getUserCredentialsList() {
    String[] credentialsList = wsOptionsStorage.getStringArrayOption(OptionTags.USER_CREDENTIALS_LIST, new String[0]);
    
    return arrayToCredentialsList(credentialsList);
  }

  @Override
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
    String[] credentialsArray = credentialsListToArray(userCredentialsList);
    
    wsOptionsStorage.setStringArrayOption(OptionTags.USER_CREDENTIALS_LIST, credentialsArray);
  }

  @Override
  public CommitMessages getCommitMessages() {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.COMMIT_MESSAGES, new String[0]);
    
    CommitMessages commitMessages = new CommitMessages();
    commitMessages.setMessages(Arrays.asList(stringArrayOption));
    return commitMessages;
  }

  @Override
  public void setCommitMessages(CommitMessages commitMessages) {
    List<String> comMessages = commitMessages.getMessages();
    
    wsOptionsStorage.setStringArrayOption(
        OptionTags.COMMIT_MESSAGES,
        comMessages != null ? comMessages.toArray(new String[0]) : new String[0] );
    
  }

  @Override
  public String getPassphrase() {
    return wsOptionsStorage.getOption(OptionTags.PASSPHRASE, "");
    
  }

  @Override
  public void setPassphrase(String passphrase) {
   wsOptionsStorage.setOption(OptionTags.PASSPHRASE, passphrase);

  }

  @Override
  public void setSshQuestions(Map<String, Boolean> sshPromptAnswers) {
    String[] newOpt = mapToArray(sshPromptAnswers);
    wsOptionsStorage.setStringArrayOption(OptionTags.SSH_PROMPT_ANSWERS, newOpt);

  }

  @Override
  public Map<String, Boolean> getSshPromptAnswers() {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.SSH_PROMPT_ANSWERS, new String[0]);
    LinkedHashMap<String, String> sshPromptAnswers = arrayToMap(stringArrayOption);
    
    Map<String, Boolean> sshPromptAnswersWithBool = new LinkedHashMap<>();
    
    for (Map.Entry<String, String> entry: sshPromptAnswers.entrySet()) {
      sshPromptAnswersWithBool.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
    }
    
    return sshPromptAnswersWithBool;
  }

  @Override
  public void setWhenRepoDetectedInProject(WhenRepoDetectedInProject whatToDo) {
    wsOptionsStorage.setOption(OptionTags.WHEN_REPO_DETECTED_IN_PROJECT, String.valueOf(whatToDo));

  }

  @Override
  public WhenRepoDetectedInProject getWhenRepoDetectedInProject() {
    String whenRepoDetected = wsOptionsStorage.getOption(OptionTags.WHEN_REPO_DETECTED_IN_PROJECT, String.valueOf(WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC));
    return WhenRepoDetectedInProject.valueOf(whenRepoDetected);
  }

  @Override
  public boolean getUpdateSubmodulesOnPull() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.UPDATE_SUBMODULES_ON_PULL, TRUE));
    
  }

  @Override
  public void setUpdateSubmodulesOnPull(boolean updateSubmodules) {
   wsOptionsStorage.setOption(OptionTags.UPDATE_SUBMODULES_ON_PULL, String.valueOf(updateSubmodules));

  }

  @Override
  public PersonalAccessTokenInfoList getPersonalAccessTokensList() {
    String[] tokensArray = wsOptionsStorage.getStringArrayOption(OptionTags.PERSONAL_ACCES_TOKENS_LIST, new String[0]);

    return arrayToTokenList(tokensArray);
  }

  @Override
  public void setPersonalAccessTokensList(PersonalAccessTokenInfoList paTokensList) {
    String[] tokensArray = tokenListToArray(paTokensList);
    wsOptionsStorage.setStringArrayOption(OptionTags.PERSONAL_ACCES_TOKENS_LIST, tokensArray);
  }
  
  /**
   * Used to convert an array to a UserCredentialsList
   * 
   * @param credentials the array that we want to convert to a UserCredentialsList
   * 
   * @return a UserCredentialsList
   */
  public static UserCredentialsList arrayToCredentialsList(String[] credentials) {
    UserCredentialsList userCredentialsList = new UserCredentialsList();
    
    if (credentials == null
        || credentials.length == 0 
        || credentials.length % NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT != 0 ) {
      return userCredentialsList;
    }

    List<UserAndPasswordCredentials> uapcList = new ArrayList<>();
    
    for (int i = 0;
        i <= credentials.length - NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT;
        i += NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT) {
      UserAndPasswordCredentials uapc = new UserAndPasswordCredentials();
      uapc.setUsername(credentials[i]);
      uapc.setPassword(credentials[i + 1]);
      uapc.setHost(credentials[i + 2]);
      uapcList.add(uapc);
    }
    
    userCredentialsList.setCredentials(uapcList);
    
    return userCredentialsList;
  }
  
  /**
   * Used to convert a UserCredentialsList to a credentials array.
   * 
   * @param  userCredentialsList The UserCredentialsList object that we want to convert to a array.
   * 
   * @return the array of credentials.
   */
  public static String[] credentialsListToArray(UserCredentialsList userCredentialsList) {
    List<UserAndPasswordCredentials> credentialItems = userCredentialsList.getCredentials();
    int size = NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT * credentialItems.size();
    String[] array = new String[size];
    
    int i = 0;
    for (UserAndPasswordCredentials uapc: credentialItems) {
      array[i] = uapc.getUsername();
      array[i + 1] = uapc.getPassword();
      array[i + 2] = uapc.getHost();
      i += NO_OF_USER_AND_PASS_CREDENTIAL_FIELDS_PER_OBJECT;
    }
    
    return array;
  }
  
  /**
   * Used to convert an array to a PersonalAccessTokenInfoList
   * 
   * @param array the array that we want to convert to a PersonalAccessTokenInfoList
   * 
   * @return a PersonalAccessTokenInfoList
   */
  public static PersonalAccessTokenInfoList arrayToTokenList(String[] array) {
    PersonalAccessTokenInfoList personalAccessTokenInfoList = new PersonalAccessTokenInfoList();
    
    if (array == null
        || array.length == 0
        || array.length % NO_OF_TOKEN_FIELDS_PER_OBJECT != 0 ) {
      return personalAccessTokenInfoList;
    }
    
    List<PersonalAccessTokenInfo> tokensList = new ArrayList<>();
    
    for (int i = 0; 
        i <= array.length - NO_OF_TOKEN_FIELDS_PER_OBJECT;
        i = i + NO_OF_TOKEN_FIELDS_PER_OBJECT) {
      PersonalAccessTokenInfo tokenInfo = new PersonalAccessTokenInfo();
      tokenInfo.setHost(array[i]);
      tokenInfo.setTokenValue(array[i + 1]);
      tokensList.add(tokenInfo);
    }
    
    personalAccessTokenInfoList.setPersonalAccessTokens(tokensList);
    
    return personalAccessTokenInfoList;
  }
  
  /**
   * Used to convert a PersonalAccessTokenInfoList to an array
   * 
   * @param personalAccessTokenInfoList The PersonalAccessTokenInfoList that we want to convert to an array
   * 
   * @return an array
   */
  public static String[] tokenListToArray(PersonalAccessTokenInfoList personalAccessTokenInfoList) {
    List<PersonalAccessTokenInfo> tokens = personalAccessTokenInfoList.getPersonalAccessTokens();
    int size = NO_OF_TOKEN_FIELDS_PER_OBJECT * tokens.size();
    String[] array = new String[size];
    
    int i = 0;
    for (PersonalAccessTokenInfo token : tokens) {
      array[i] = token.getHost();
      array[i + 1] = token.getTokenValue();
      i += NO_OF_TOKEN_FIELDS_PER_OBJECT;
    }
    
    return array;
  }
  
  /**
   * Used to convert an array to a Map.
   * 
   * @param array the array that we want to convert to a Map.
   * 
   * @return A map that keep the order of the keys from the array.
   */
  @SuppressWarnings("java:S1319")
  public static LinkedHashMap<String, String> arrayToMap(String[] array) {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    
    if (array.length == 0 || array.length % 2 == 1) {
      return map;
    }
    
    for (int i = 0; i <= array.length - 2; i = i + 2) {
      map.put(array[i], array[i + 1]);
    }
    
    return map;
  }
  
  /**
   * Used to convert a Map to an array
   * 
   * @param map The map that we want to convert to an array
   * 
   * @return an array
   */
  public static String[] mapToArray(Map<String, ?> map) {
    String[] array = new String[2 * map.size()];
    
    int i = 0;
    for (Map.Entry<String, ?> entry: map.entrySet()) {
      array[i] = entry.getKey();
      i++;
      array[i] = String.valueOf(entry.getValue());
      i++;
    }
    
    return array;
  }

  @Override
  public boolean getValidateFilesBeforeCommit() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(
        OptionTags.VALIDATE_FILES_BEFORE_COMMIT, FALSE));
  }

  @Override
  public void setValidateFilesBeforeCommit(boolean validateFilesBeforeCommit) {
    wsOptionsStorage.setOption(OptionTags.VALIDATE_FILES_BEFORE_COMMIT, String.valueOf(validateFilesBeforeCommit));
  }

  @Override
  public boolean getRejectCommitOnValidationProblems() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(
        OptionTags.REJECT_COMMIT_ON_VALIDATION_PROBLEMS, FALSE));
  }

  @Override
  public void setRejectCommitOnValidationProblems(boolean rejectCommitOnValidationProblems) {
    wsOptionsStorage.setOption(OptionTags.REJECT_COMMIT_ON_VALIDATION_PROBLEMS, String.valueOf(rejectCommitOnValidationProblems));
  }

  @Override
  public boolean getValidateMainFilesBeforePush() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(
        OptionTags.VALIDATE_MAIN_FILES_BEFORE_PUSH, FALSE));
  }

  @Override
  public void setValidateMainFilesBeforePush(boolean validateMainFilesBeforePush) {
    wsOptionsStorage.setOption(OptionTags.VALIDATE_MAIN_FILES_BEFORE_PUSH, String.valueOf(validateMainFilesBeforePush));    
  }

  @Override
  public boolean getRejectPushOnValidationProblems() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(
        OptionTags.REJECT_PUSH_ON_VALIDATION_PROBLEMS, FALSE));
  }

  @Override
  public void setRejectPushOnValidationProblems(boolean rejectPushOnValidationProblems) {
    wsOptionsStorage.setOption(OptionTags.REJECT_PUSH_ON_VALIDATION_PROBLEMS, String.valueOf(rejectPushOnValidationProblems));  
  }
  
}
