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
  
  private WSOptionsStorage wsOptionsStorage;

  public OptionsWithTags(WSOptionsStorage wsOptionsStorage) {
    this.wsOptionsStorage = wsOptionsStorage;
  }

  @Override
  public boolean isAutoPushWhenCommitting() {
    return Boolean.parseBoolean(wsOptionsStorage.getOption(OptionTags.AUTO_PUSH_WHEN_COMMITTING, "false"));
  }

  @Override
  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting) {
    wsOptionsStorage.setOption(OptionTags.AUTO_PUSH_WHEN_COMMITTING, String.valueOf(isAutoPushWhenCommitting));
  }

  @Override
  public PullType getDefaultPullType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDefaultPullType(PullType defaultPullType) {
    // TODO Auto-generated method stub

  }

  @Override
  public ResourcesViewMode getUnstagedResViewMode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) {
    // TODO Auto-generated method stub

  }

  @Override
  public ResourcesViewMode getStagedResViewMode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    // TODO Auto-generated method stub

  }

  @Override
  public DestinationPaths getDestinationPaths() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDestinationPaths(DestinationPaths destinationPaths) {
    // TODO Auto-generated method stub

  }

  @Override
  public ProjectsTestedForGit getProjectsTestsForGit() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPrjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) {
    // TODO Auto-generated method stub

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
    // TODO Auto-generated method stub

  }

  @Override
  public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isNotifyAboutNewRemoteCommits() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isCheckoutNewlyCreatedLocalBranch() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Map<String, String> getWarnOnChangeCommitId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getWarnOnChangeCommitId(String repositoryId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setWarnOnChangeCommitId(String repositoryId, String commitId) {
    // TODO Auto-generated method stub
    
  }
  

  @Override
  public String getSelectedRepository() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setSelectedRepository(String selectedRepository) {
    // TODO Auto-generated method stub

  }

  @Override
  public UserCredentialsList getUserCredentialsList() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
    // TODO Auto-generated method stub

  }

  @Override
  public CommitMessages getCommitMessages() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setCommitMessages(CommitMessages commitMessages) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getPassphrase() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPassphrase(String passphrase) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setSshQuestions(HashMap<String, Boolean> sshPromptAnswers) {
    // TODO Auto-generated method stub

  }

  @Override
  public Map<String, Boolean> getSshPromptAnswers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setWhenRepoDetectedInProject(WhenRepoDetectedInProject whatToDo) {
    // TODO Auto-generated method stub

  }

  @Override
  public WhenRepoDetectedInProject getWhenRepoDetectedInProject() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getUpdateSubmodulesOnPull() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setUpdateSubmodulesOnPull(boolean updateSubmodules) {
    // TODO Auto-generated method stub

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

}
