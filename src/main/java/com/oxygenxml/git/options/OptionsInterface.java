package com.oxygenxml.git.options;

import java.util.Map;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

public interface OptionsInterface {

  public boolean isAutoPushWhenCommitting();

  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting);

  public PullType getDefaultPullType() ;

  public void setDefaultPullType(PullType defaultPullType) ;

  public ResourcesViewMode getUnstagedResViewMode() ;

  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) ;

  public ResourcesViewMode getStagedResViewMode() ;

  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) ;

  public DestinationPaths getDestinationPaths() ;

  public void setDestinationPaths(DestinationPaths destinationPaths) ;

  public ProjectsTestedForGit getProjectsTestsForGit() ;

  public void setProjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) ;

  public RepositoryLocations getRepositoryLocations() ;

  public void setRepositoryLocations(RepositoryLocations repositoryLocations) ;

  /**
   * Set when to verify for remote changes in the repository.
   * 
   * @param notifyAboutNewRemoteCommits Option chosen about if to verify or not.
   */
  public void setNotifyAboutNewRemoteCommits(boolean notifyAboutNewRemoteCommits) ;

  /**
   * @param isCheckoutNewlyCreatedLocalBranch <code>true</code> to automatically
   * checkout a newly created local branch.
   */
  public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) ;

  /**
   * Get the option about when to verify about remote changes in the repository.
   * 
   * @return Option stored about to verify or not.
   */
  public boolean isNotifyAboutNewRemoteCommits() ;

  /**
   * @return <code>true</code> to automatically checkout a newly created local branch.
   */
  public boolean isCheckoutNewlyCreatedLocalBranch() ;

  public Map<String, String> getWarnOnChangeCommitId() ;

  /**
   * Get the ID of the latest commit fetched from a given repository.
   * 
   * @param repositoryId The repository from which to get the commit ID, obtained from
   *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
   *                     
   * @return The commit ID that comes from  {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
   */
  
  public String getWarnOnChangeCommitId(String repositoryId) ;

  /**
   * Set the commit ID to the newest commit fetched from a given repository.
   * 
   * @param repositoryId The repository in which to put the commit ID, obtained from
   *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
   *                     
   * @param commitId     The newest commit ID, obtained from 
   *                     {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
   */
  public void setWarnOnChangeCommitId(String repositoryId, String commitId) ;

  public String getSelectedRepository() ;

  public void setSelectedRepository(String selectedRepository) ;

  /**
   * The list with user credentials. The actual list, not a copy.
   * 
   * @return The user credentials.
   */
  public UserCredentialsList getUserCredentialsList() ;

  /**
   * The list with user credentials.
   * @param userCredentialsList
   */
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) ;

  public CommitMessages getCommitMessages() ;

  public void setCommitMessages(CommitMessages commitMessages) ;

  public String getPassphrase() ;

  public void setPassphrase(String passphrase) ;

  /**
   * @param sshPromptAnswers A cache for asking the user for connection message.
   */
  @SuppressWarnings("java:S1319")
  public void setSshQuestions(Map<String, Boolean> sshPromptAnswers) ;

  /**
   * @return A cache for asking the user for connection message.
   */
  public Map<String, Boolean> getSshPromptAnswers() ;

  /**
   * Set what to do when a repository is detected when opening an Oxygen project.
   *  
   * @param whatToDo What to do.
   */
  public void setWhenRepoDetectedInProject(WhenRepoDetectedInProject whatToDo) ;

  /**
   * @return what to do when a repo is detected inside an Oxygen project.
   */
  public WhenRepoDetectedInProject getWhenRepoDetectedInProject() ;

  /**
   * @return <code>true</code> to update submodules after a pull.
   */
  public boolean getUpdateSubmodulesOnPull() ;

  /**
   * Sets the submodule update policy on pull.
   * 
   * @param updateSubmodules <code>true</code> to execute the equivalent of a "git submodule update --recursive".
   */
  public void setUpdateSubmodulesOnPull(boolean updateSubmodules) ;

  /**
   * @return the list of personal access token info items.
   */
  public PersonalAccessTokenInfoList getPersonalAccessTokensList() ;

  /**
   * @param paTokensList the list of personal access token info items to set.
   */
  public void setPersonalAccessTokensList(PersonalAccessTokenInfoList paTokensList) ;

}
