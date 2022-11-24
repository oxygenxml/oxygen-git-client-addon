package com.oxygenxml.git.options;

import java.util.Map;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Options storage.
 * 
 * @author alex_jitianu
 */
public interface Options {

  /**
   * @return <code>true</code> if push should be done automatically for each commit.
   */
  public boolean isAutoPushWhenCommitting();

  /**
   * Sets if commits should be automatically pushed.
   * 
   * @param isAutoPushWhenCommitting <code>true</code> to automatically push each commit.
   */
  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting);

  /**
   * @return The type of pull (merge, rebase etc.).
   */
  public PullType getDefaultPullType() ;

  /**
   * Sets the type of conflict resolution done on pull.
   * 
   * @param defaultPullType The type of conflict resolution done on pull.
   */
  public void setDefaultPullType(PullType defaultPullType) ;

  /**
   * @return Unstaged resources view mode.
   */
  public ResourcesViewMode getUnstagedResViewMode() ;

  /**
   * Sets the unstaged resources view mode.
   * 
   * @param unstagedResViewMode The unstaged resources view mode.
   */
  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) ;

  /**
   * @return The index/staged resources view mode.
   */
  public ResourcesViewMode getStagedResViewMode() ;

  /**
   * Sets the staged/index resources view mode.
   * 
   * @param stagedResViewMode The staged/index resources view mode.
   */
  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) ;

  /**
   * @return The paths previously chosen as working copy locations.
   */
  public DestinationPaths getDestinationPaths() ;

  /**
   * Sets the paths previously chosen as working copy locations.
   * 
   * @param destinationPaths The paths previously chosen as working copy locations.
   */
  public void setDestinationPaths(DestinationPaths destinationPaths) ;

  /**
   * @return Oxygen project locations tested for inner git repositories.
   */
  public ProjectsTestedForGit getProjectsTestsForGit() ;

  /**
   * Sets the Oxygen project locations tested for inner git repositories.
   * 
   * @param prjectsTestsForGit Oxygen project locations tested for inner git repositories.
   */
  public void setProjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) ;

  /**
   * @return Locations already loaded in the staging view.
   */
  public RepositoryLocations getRepositoryLocations() ;

  /**
   * Sets the locations already loaded in the staging view.
   * 
   * @param repositoryLocations The locations already loaded in the staging view.
   */
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

  /**
   * @return The last repository loaded in the staging view.
   */
  public String getSelectedRepository() ;

  /**
   * Sets the last repository loaded in the staging view.
   * 
   * @param selectedRepository  The last repository loaded in the staging view.
   */
  public void setSelectedRepository(String selectedRepository) ;

  /**
   * The list with user credentials. The actual list, not a copy.
   * 
   * @return The user credentials.
   */
  public UserCredentialsList getUserCredentialsList() ;

  /**
   * The list with user credentials.
   * 
   * @param userCredentialsList User credentials.
   */
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) ;

  /**
   * @return The commit messages previously used in the commit panel.
   */
  public CommitMessages getCommitMessages() ;

  /**
   * Sets the commit messages history.
   * 
   * @param commitMessages The commit messages previously used in the commit panel.
   */
  public void setCommitMessages(CommitMessages commitMessages) ;

  /**
   * @return The last SSH passhrase.
   */
  public String getPassphrase() ;

  /**
   * Sets the SSH passphrase.
   * 
   * @param passphrase The SSH passphrase.
   */
  public void setPassphrase(String passphrase) ;

  /**
   * @param sshPromptAnswers A cache for asking the user for connection message.
   */
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
   * @return <code>true</code> to detect and open xpr files from opened working copies
   */
  public boolean getDetectAndOpenXprFiles();
    
  /**
   * Sets the detecting and opening xpr files in the project view
   * 
   * @param detectAndOpenXprFiles <code>true</code> to detect and open xpr files from opened working copies
   */
  public void setDetectAndOpenXprFiles(boolean detectAndOpenXprFiles);

  /**
   * @return the list of personal access token info items.
   */
  public PersonalAccessTokenInfoList getPersonalAccessTokensList() ;

  /**
   * @param paTokensList the list of personal access token info items to set.
   */
  public void setPersonalAccessTokensList(PersonalAccessTokenInfoList paTokensList) ;
  
  /**
   * @return <code>true</code> to validate files before commit.
   */
  public boolean getValidateFilesBeforeCommit() ;

  /**
   * @param validateFilesBeforeCommit <code>true</code> to validate files before commit.
   */
  public void setValidateFilesBeforeCommit(final boolean validateFilesBeforeCommit) ;
  
  /**
   * @return <code>true</code> to reject commit on validation problems.
   */
  public boolean getRejectCommitOnValidationProblems() ;

  /**
   * @param rejectCommitOnValidationProblems <code>true</code> to reject commit on validation problems.
   */
  public void setRejectCommitOnValidationProblems(final boolean rejectCommitOnValidationProblems) ;

  /**
   * @return <code>true</code> to validate main files before push.
   */
  public boolean getValidateMainFilesBeforePush() ;

  /**
   * @param validateMainFilesBeforePush <code>true</code> to validate main files before push.
   */
  public void setValidateMainFilesBeforePush(final boolean validateMainFilesBeforePush) ;
  
  /**
   * @return <code>true</code> to reject push on validation problems.
   */
  public boolean getRejectPushOnValidationProblems() ;

  /**
   * @param rejectPushOnValidationProblems <code>true</code> to reject push on validation problems.
   */
  public void setRejectPushOnValidationProblems(final boolean rejectPushOnValidationProblems) ;
  
  /**
   * @param useSshAgent <code>true</code> to use SSH agent.
   */
  public void setUseSshAgent(final boolean useSshAgent);
  
  /**
   * @return <code>true</code> if the SSH agent should be used.
   */
  public boolean getUseSshAgent();
  
  /**
   * @param defaultSshAgent Set the default SSH agent.
   */
  public void setDefaultSshAgent(final String defaultSshAgent);
  
  /**
   * @return Get the default SSH agent.
   */
  public String getDefaultSshAgent();
  
  /**
   * @param includeUntrackedFiles <code>true</code> if the stash should include the untracked files.
   */
  public void setStashIncludeUntracked(final boolean stashIncludeUntracked);
  
  /**
   * @return <code>true</code> if the stash should include the untracked files.
   */
  public boolean getStashIncludeUntracked();
  
  /**
   * @param historyStrategy The new default history presentation strategy.
   */
  public void setHistoryStrategy(final HistoryStrategy historyStrategy);
  
  /**
   * @return The default history presentation strategy.
   */
  public HistoryStrategy getHistoryStrategy();
  
}
