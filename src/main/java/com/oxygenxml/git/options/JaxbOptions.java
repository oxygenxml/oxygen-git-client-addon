package com.oxygenxml.git.options;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.utils.Equaler;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Entity for the JAXB to store the plugin options
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "Options")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbOptions implements Options {
  
  /**
   * A cache for the SSH questions and the user answer.
   */
  @XmlElement(name = "sshPromptAnswers")
  private HashMap<String, Boolean> sshPromptAnswers = new HashMap<>();

	/**
	 * Wrapper for a list with the repository locations
	 */
	@XmlElement(name = "repositoryLocations")
	private RepositoryLocations repositoryLocations = new RepositoryLocations();
	
	/**
	 * Stores the option selected to notify or not if there are new changes in the remote
	 */
	private boolean notifyAboutNewRemoteCommits;
	
	/**
	 * <code>true</code> to automatically checkout a newly created local branch.
	 */
	private boolean isCheckoutNewlyCreatedLocalBranch = true;

	/**
	 * <code>true</code> to validate files before commit.
	 */
	private boolean validateFilesBeforeCommit = false;
	
	/**
	 * <code>true</code> to reject commit on validation problems.
	 */
	private boolean rejectCommitOnValidationProblems = false;
	
	/**
   * <code>true</code> to validate main files before push.
   */
  private boolean validateMainFilesBeforePush = false;
  
  /**
   * <code>true</code> to reject push on validation problems.
   */
  private boolean rejectPushOnValidationProblems = false;
	
	/**
	 * The id from the last commit fetched.
	 */
	private final HashMap<String, String> warnOnChangeCommitId = new HashMap<>();
	
	/**
	 * Last selected repository from the user
	 */
	@XmlElement(name = "selectedRepository")
	private String selectedRepository = "";

	/**
	 * A list of user credentials containing the username, password and the host.
	 * Only one credential per host can be stored
	 */
	@XmlElement(name = "userCredentials")
	private UserCredentialsList userCredentialsList = new UserCredentialsList();
	
	/**
	 * A list of personal access token + host entries. Only one personal access token per host.
	 */
	@XmlElement(name = "personalAccessTokens")
	private PersonalAccessTokenInfoList paTokensList = new PersonalAccessTokenInfoList();

	/**
	 * Wrapper for a list of commit messages
	 */
	@XmlElement(name = "commitMessages")
	private CommitMessages commitMessages = new CommitMessages();
	
	/**
	 * The default pull type: with merge or rebase.
	 */
	@XmlElement(name = "defaultPullType")
	private PullType defaultPullType = PullType.MERGE_FF;

  /**
	 * Wrapper for a list of project.xpr that were tested if they want to be a git
	 * repository
	 */
	@XmlElement(name = "projectsTested")
	private ProjectsTestedForGit projectsTestsForGit = new ProjectsTestedForGit();

	/**
	 * Wrapper for a list of previously selected destination paths
	 */
	@XmlElement(name = "destinationPaths")
	private DestinationPaths destinationPaths = new DestinationPaths();

	/**
	 * The passphrase for the SSH
	 */
	@XmlElement(name = "passphrase")
	private String passphrase = "";
	
	/**
	 * The view mode for the staged resources: tree or table.
	 */
	@XmlElement(name = "stagedResViewMode")
	private ResourcesViewMode stagedResViewMode = ResourcesViewMode.FLAT_VIEW;
	
	/**
   * The view mode for the unstaged resources: tree or table.
   */
	@XmlElement(name = "unstagedResViewMode")
  private ResourcesViewMode unstagedResViewMode = ResourcesViewMode.FLAT_VIEW;
	
	/**
	 * Option about what to do when opening a prject in Oxygen and detecting a Git repository.
	 */
	@XmlElement(name = "whenRepoDetectedInProject")
	private WhenRepoDetectedInProject whenRepoDetectedInProject = WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC;
	/**
	 * <code>true</code> to update submodules on pull.
	 */
	private boolean updateSubmodulesOnPull = true;
	/**
	 * <code>true</code> to automatically push to remote when committing.
	 */
	@XmlElement(name = "isAutoPushWhenCommitting")
	private boolean isAutoPushWhenCommitting = false;

	@Override
  public boolean isAutoPushWhenCommitting() {
    return isAutoPushWhenCommitting;
  }
	
	@Override
  public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting) {
    this.isAutoPushWhenCommitting = isAutoPushWhenCommitting;
  }
	
  @Override
  public PullType getDefaultPullType() {
    return defaultPullType;
  }

  @Override
  public void setDefaultPullType(PullType defaultPullType) {
    this.defaultPullType = defaultPullType;
  }
  
  @Override
  public ResourcesViewMode getUnstagedResViewMode() {
    return unstagedResViewMode;
  }

  @Override
  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) {
    this.unstagedResViewMode = unstagedResViewMode;
  }

  @Override
  public ResourcesViewMode getStagedResViewMode() {
    return stagedResViewMode;
  }

  @Override
  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    this.stagedResViewMode = stagedResViewMode;
  }

  @Override
  public DestinationPaths getDestinationPaths() {
		return destinationPaths;
	}

	@Override
  public void setDestinationPaths(DestinationPaths destinationPaths) {
		this.destinationPaths = destinationPaths;
	}

	@Override
  public ProjectsTestedForGit getProjectsTestsForGit() {
		return projectsTestsForGit;
	}

	@Override
  public void setProjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) {
		this.projectsTestsForGit = prjectsTestsForGit;
	}

	@Override
  public RepositoryLocations getRepositoryLocations() {
		return repositoryLocations;
	}

	@Override
  public void setRepositoryLocations(RepositoryLocations repositoryLocations) {
		this.repositoryLocations = repositoryLocations;
	}
	
  /**
   * Set when to verify for remote changes in the repository.
   * 
   * @param notifyAboutNewRemoteCommits Option chosen about if to verify or not.
   */
  @Override
  public void setNotifyAboutNewRemoteCommits(boolean notifyAboutNewRemoteCommits) {
    this.notifyAboutNewRemoteCommits = notifyAboutNewRemoteCommits;
  }
  
  /**
   * @param isCheckoutNewlyCreatedLocalBranch <code>true</code> to automatically
   * checkout a newly created local branch.
   */
  @Override
  public void setCheckoutNewlyCreatedLocalBranch(boolean isCheckoutNewlyCreatedLocalBranch) {
    this.isCheckoutNewlyCreatedLocalBranch = isCheckoutNewlyCreatedLocalBranch;
  }
  
  /**
   * Get the option about when to verify about remote changes in the repository.
   * 
   * @return Option stored about to verify or not.
   */
  @Override
  public boolean isNotifyAboutNewRemoteCommits() {
    return notifyAboutNewRemoteCommits;
  }
  
  /**
   * @return <code>true</code> to automatically checkout a newly created local branch.
   */
  @Override
  public boolean isCheckoutNewlyCreatedLocalBranch() {
    return isCheckoutNewlyCreatedLocalBranch;
  }
  
  @Override
  public Map<String, String> getWarnOnChangeCommitId() {
    return warnOnChangeCommitId;
  }
  
  /**
   * Get the ID of the latest commit fetched from a given repository.
   * 
   * @param repositoryId The repository from which to get the commit ID, obtained from
   *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
   *                     
   * @return The commit ID that comes from  {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
   */
  
  @Override
  public String getWarnOnChangeCommitId(String repositoryId) {
    return warnOnChangeCommitId.getOrDefault(repositoryId,"");
  }
  
  /**
   * Set the commit ID to the newest commit fetched from a given repository.
   * 
   * @param repositoryId The repository in which to put the commit ID, obtained from
   *                     {@link org.eclipse.jgit.lib.Repository.getIdentifier()}.
   *                     
   * @param commitId     The newest commit ID, obtained from 
   *                     {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}.
   */
  @Override
  public void setWarnOnChangeCommitId(String repositoryId, String commitId) {
    warnOnChangeCommitId.put(repositoryId, commitId);
  }

	@Override
  public String getSelectedRepository() {
		return selectedRepository;
	}

	@Override
  public void setSelectedRepository(String selectedRepository) {
		this.selectedRepository = selectedRepository;
	}

	 /**
   * The list with user credentials. The actual list, not a copy.
   * 
   * @return The user credentials.
   */
	@Override
  public UserCredentialsList getUserCredentialsList() {
		return userCredentialsList;
	}

	/**
	 * The list with user credentials.
	 * @param userCredentialsList
	 */
	@Override
  public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
		this.userCredentialsList = userCredentialsList;
	}

	@Override
  public CommitMessages getCommitMessages() {
		return commitMessages;
	}

	@Override
  public void setCommitMessages(CommitMessages commitMessages) {
		this.commitMessages = commitMessages;
	}

	@Override
  public String getPassphrase() {
		return passphrase;
	}

	@Override
  public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	/**
	 * @param sshPromptAnswers A cache for asking the user for connection message.
	 */
	@Override
  @SuppressWarnings("java:S1319")
	public void setSshQuestions(Map<String, Boolean> sshPromptAnswers) {
	  this.sshPromptAnswers.clear();
    this.sshPromptAnswers.putAll(sshPromptAnswers);
  }
	
	/**
	 * @return A cache for asking the user for connection message.
	 */
	@Override
  public Map<String, Boolean> getSshPromptAnswers() {
    return sshPromptAnswers;
  }

	/**
	 * Set what to do when a repository is detected when opening an Oxygen project.
	 *  
	 * @param whatToDo What to do.
	 */
	@Override
  public void setWhenRepoDetectedInProject(WhenRepoDetectedInProject whatToDo) {
	  this.whenRepoDetectedInProject = whatToDo;
	}

	/**
	 * @return what to do when a repo is detected inside an Oxygen project.
	 */
	@Override
  public WhenRepoDetectedInProject getWhenRepoDetectedInProject() {
	  return whenRepoDetectedInProject;
	}
	
	 /**
   * @return <code>true</code> to update submodules after a pull.
   */
  @Override
  public boolean getUpdateSubmodulesOnPull() {
    return updateSubmodulesOnPull;
  }

	/**
   * Sets the submodule update policy on pull.
   * 
   * @param updateSubmodules <code>true</code> to execute the equivalent of a "git submodule update --recursive".
   */
  @Override
  public void setUpdateSubmodulesOnPull(boolean updateSubmodules) {
    this.updateSubmodulesOnPull = updateSubmodules;
  }

	/**
   * @return the list of personal access token info items.
   */
  @Override
  public PersonalAccessTokenInfoList getPersonalAccessTokensList() {
    return paTokensList;
  }

  /**
   * @param paTokensList the list of personal access token info items to set.
   */
  @Override
  public void setPersonalAccessTokensList(PersonalAccessTokenInfoList paTokensList) {
    this.paTokensList = paTokensList;
  }

  @Override
  public boolean getValidateFilesBeforeCommit() {
    return validateFilesBeforeCommit;
  }

  @Override
  public void setValidateFilesBeforeCommit(boolean validateFilesBeforeCommit) {
    this.validateFilesBeforeCommit = validateFilesBeforeCommit;
  }

  @Override
  public boolean getRejectCommitOnValidationProblems() {
    return rejectCommitOnValidationProblems;
  }

  @Override
  public void setRejectCommitOnValidationProblems(boolean rejectCommitOnValidationProblems) {
    this.rejectCommitOnValidationProblems = rejectCommitOnValidationProblems;
  }
  
  @Override
  public boolean getValidateMainFilesBeforePush() {
    return validateMainFilesBeforePush;
  }

  @Override
  public void setValidateMainFilesBeforePush(boolean validateMainFilesBeforePush) {
    this.validateMainFilesBeforePush = validateMainFilesBeforePush;
    
  }

  @Override
  public boolean getRejectPushOnValidationProblems() {
    return rejectPushOnValidationProblems;
  }

  @Override
  public void setRejectPushOnValidationProblems(boolean rejectPushOnValidationProblems) {
    this.rejectPushOnValidationProblems = rejectPushOnValidationProblems;
  }
  
  @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commitMessages == null) ? 0 : commitMessages.hashCode());
		result = prime * result + ((destinationPaths == null) ? 0 : destinationPaths.hashCode());
		result = prime * result + ((passphrase == null) ? 0 : passphrase.hashCode());
		result = prime * result + ((projectsTestsForGit == null) ? 0 : projectsTestsForGit.hashCode());
		result = prime * result + ((repositoryLocations == null) ? 0 : repositoryLocations.hashCode());
		result = prime * result + ((selectedRepository == null) ? 0 : selectedRepository.hashCode());
		result = prime * result + ((userCredentialsList == null) ? 0 : userCredentialsList.hashCode());
		result = prime * result + ((paTokensList == null) ? 0 : paTokensList.hashCode());
		result = prime * result + ((sshPromptAnswers == null) ? 0 : sshPromptAnswers.hashCode());
		result = prime * result + (notifyAboutNewRemoteCommits ? 1 : 0);
		result = prime * result + (isCheckoutNewlyCreatedLocalBranch ? 1 : 0);
	  result = prime * result + (validateFilesBeforeCommit ? 1 : 0);
    result = prime * result + (rejectCommitOnValidationProblems ? 1 : 0);
    result = prime * result + (validateMainFilesBeforePush ? 1 : 0);
    result = prime * result + (rejectPushOnValidationProblems ? 1 : 0);
    result = prime * result + warnOnChangeCommitId.hashCode();
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof JaxbOptions) {
	    JaxbOptions opt = (JaxbOptions) obj;
	    toReturn = Equaler.verifyEquals(commitMessages, opt.getCommitMessages())
	        && Equaler.verifyEquals(destinationPaths, opt.getDestinationPaths())
	        && Equaler.verifyEquals(passphrase, opt.getPassphrase())
	        && Equaler.verifyEquals(projectsTestsForGit, opt.getProjectsTestsForGit())
	        && Equaler.verifyEquals(repositoryLocations, opt.getRepositoryLocations())
	        && Equaler.verifyEquals(notifyAboutNewRemoteCommits, opt.isNotifyAboutNewRemoteCommits())
	        && Equaler.verifyEquals(validateFilesBeforeCommit, opt.getValidateFilesBeforeCommit())
	        && Equaler.verifyEquals(rejectCommitOnValidationProblems, opt.getRejectCommitOnValidationProblems())
	        && Equaler.verifyEquals(validateMainFilesBeforePush, opt.getValidateMainFilesBeforePush())
          && Equaler.verifyEquals(rejectPushOnValidationProblems, opt.getRejectPushOnValidationProblems())
	        && Equaler.verifyEquals(isCheckoutNewlyCreatedLocalBranch, opt.isCheckoutNewlyCreatedLocalBranch)
	        && Equaler.verifyEquals(selectedRepository, opt.getSelectedRepository())
	        && Equaler.verifyEquals(sshPromptAnswers, opt.getSshPromptAnswers())
	        && Equaler.verifyEquals(userCredentialsList, opt.getUserCredentialsList())
	        && Equaler.verifyEquals(paTokensList, opt.getPersonalAccessTokensList())
	        && Equaler.verifyEquals(stagedResViewMode, opt.stagedResViewMode)
	        && Equaler.verifyEquals(defaultPullType, opt.defaultPullType)
	        && Equaler.verifyEquals(warnOnChangeCommitId, opt.getWarnOnChangeCommitId());
	  }
	  return toReturn;
	}

  @Override
  public String toString() {
    return "Options [sshPromptAnswers=" + sshPromptAnswers + ", repositoryLocations=" + repositoryLocations
        + ", notifyAboutNewRemoteCommits=" + notifyAboutNewRemoteCommits + ", isCheckoutNewlyCreatedLocalBranch="
        + isCheckoutNewlyCreatedLocalBranch + ", warnOnChangeCommitId=" + warnOnChangeCommitId + ", selectedRepository="
        + selectedRepository + ", userCredentialsList=" + userCredentialsList + ", paTokensList=" + paTokensList
        + ", commitMessages=" + commitMessages + ", defaultPullType=" + defaultPullType + ", projectsTestsForGit="
        + projectsTestsForGit + ", destinationPaths=" + destinationPaths + ", passphrase=" + passphrase
        + ", stagedResViewMode=" + stagedResViewMode + ", unstagedResViewMode=" + unstagedResViewMode
        + ", whenRepoDetectedInProject=" + whenRepoDetectedInProject + ", updateSubmodulesOnPull="
        + updateSubmodulesOnPull + ", isAutoPushWhenCommitting=" + isAutoPushWhenCommitting + 
        ", validateFilesBeforeCommit=" + validateFilesBeforeCommit + ", rejectCommitOnValidationProblems=" 
        + rejectCommitOnValidationProblems + ", validateMainFilesBeforePush=" + validateMainFilesBeforePush + 
        ", rejectPushOnValidationProblems=" + rejectPushOnValidationProblems + "]";
  }

}
