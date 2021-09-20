package com.oxygenxml.git.options;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.options.WSOptionsStorage;

public class OptionsWithTags implements OptionsInterface {
  
  private static final String TRUE = "true";
  private static final String FALSE = "false";
  private WSOptionsStorage wsOptionsStorage;

  public OptionsWithTags(WSOptionsStorage wsOptionsStorage) {
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
    destinationPaths.setPaths(Arrays.asList(stringDestinationPaths));
    
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

  /**
   * Set when to verify for remote changes in the repository.
   * 
   * @param notifyAboutNewRemoteCommits Option chosen about if to verify or not.
   */
  @Override
  public void setNotifyAboutNewRemoteCommits(boolean notifyAboutNewRemoteCommits) {
    wsOptionsStorage.setOption(OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS, String.valueOf(notifyAboutNewRemoteCommits));
    
  }
  
  /**
   * @param isCheckoutNewlyCreatedLocalBranch <code>true</code> to automatically
   * checkout a newly created local branch.
   */
  @Override
  public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) {
    wsOptionsStorage.setOption(OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS, String.valueOf(isCheckoutNewlyCreatedLocalBranch));
    
  }

  /**
   * Get the option about when to verify about remote changes in the repository.
   * 
   * @return Option stored about to verify or not.
   */
  @Override
  public boolean isNotifyAboutNewRemoteCommits() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.NOTIFY_ABOUT_NEW_REMOTE_COMMITS, FALSE));
  }

  /**
   * @return <code>true</code> to automatically checkout a newly created local branch.
   */
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
    return null;
    //TODO: ...
  }

  @Override
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
   //TODO: ...
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
    wsOptionsStorage.setStringArrayOption(OptionTags.WARN_ON_CHANGE_COMMIT_ID, newOpt);
  }

  @Override
  public Map<String, Boolean> getSshPromptAnswers() {
    String[] stringArrayOption = wsOptionsStorage.getStringArrayOption(OptionTags.SSH_PROMPT_ANSWERS, new String[0]);
    Map<String, String> sshPromptAnswers = arrayToMap(stringArrayOption);
    
    Map<String, Boolean> sshPromptAnswersWithBool = new HashMap<>();
    
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPersonalAccessTokensList(PersonalAccessTokenInfoList paTokensList) {
    // TODO Auto-generated method stub

  }
  
  /**
   * Used to convert an array to a Map
   * 
   * @param array the array that we want to convert to a Map
   * 
   * @return a Map
   */
  private static Map<String, String> arrayToMap(String[] array) {
    Map<String, String> map = new HashMap<>();
    
    if (array.length%2 == 1) {
      return map;
    }
    
    for (int i = 0; i <= array.length/2; i = i+2) {
      map.put(array[i], array[i+1]);
    }
    return map;
  }
  
  /**
   * Used to convert a Map to an array
   * 
   * @param Map<String, String> the map that we want to convert to an array
   * 
   * @return an array
   */
  private static String[] mapToArray(Map<String, ?> map) {
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
  
}
