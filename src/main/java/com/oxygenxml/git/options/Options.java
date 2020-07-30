package com.oxygenxml.git.options;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension;
import com.oxygenxml.git.utils.Equaler;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.event.PullType;

/**
 * Entity for the JAXB to store the plugin options
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "Options")
@XmlAccessorType(XmlAccessType.FIELD)
public class Options {
  
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
	 * Stores the notify option selected for when there are new changes in the remote
	 */
	private String warnOnUpstreamChange = OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_NEVER;

	/**
	 * The id from the last commit fetched.
	 */
	private String warnOnCommitIdChange = "";
	
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
	 * <code>true</code> to automatically push to remote when committing.
	 */
	@XmlElement(name = "isAutoPushWhenCommitting")
	private boolean isAutoPushWhenCommitting = false;
	
	public boolean isAutoPushWhenCommitting() {
    return isAutoPushWhenCommitting;
  }
	
	public void setAutoPushWhenCommitting(boolean isAutoPushWhenCommitting) {
    this.isAutoPushWhenCommitting = isAutoPushWhenCommitting;
  }
	
  public PullType getDefaultPullType() {
    return defaultPullType;
  }

  public void setDefaultPullType(PullType defaultPullType) {
    this.defaultPullType = defaultPullType;
  }
  
  public ResourcesViewMode getUnstagedResViewMode() {
    return unstagedResViewMode;
  }

  public void setUnstagedResViewMode(ResourcesViewMode unstagedResViewMode) {
    this.unstagedResViewMode = unstagedResViewMode;
  }

  public ResourcesViewMode getStagedResViewMode() {
    return stagedResViewMode;
  }

  public void setStagedResViewMode(ResourcesViewMode stagedResViewMode) {
    this.stagedResViewMode = stagedResViewMode;
  }

  public DestinationPaths getDestinationPaths() {
		return destinationPaths;
	}

	public void setDestinationPaths(DestinationPaths destinationPaths) {
		this.destinationPaths = destinationPaths;
	}

	public ProjectsTestedForGit getProjectsTestsForGit() {
		return projectsTestsForGit;
	}

	public void setPrjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) {
		this.projectsTestsForGit = prjectsTestsForGit;
	}

	public RepositoryLocations getRepositoryLocations() {
		return repositoryLocations;
	}

	public void setRepositoryLocations(RepositoryLocations repositoryLocations) {
		this.repositoryLocations = repositoryLocations;
	}
	
	 
  public void setWarnOnUpstreamChange(String warnOnUpstreamChange) {
    this.warnOnUpstreamChange = warnOnUpstreamChange;
  }
  
  public String getWarnOnUpstreamChange() {
    return warnOnUpstreamChange;
  }
  
  public String getWarnOnCommitIdChange() {
    return warnOnCommitIdChange;
  }
  
  public void setWarnOnCommitIdChange(String warnOnCommitIdChange) {
    this.warnOnCommitIdChange = warnOnCommitIdChange;
  }

	public String getSelectedRepository() {
		return selectedRepository;
	}

	public void setSelectedRepository(String selectedRepository) {
		this.selectedRepository = selectedRepository;
	}

	 /**
   * The list with user credentials. The actual list, not a copy.
   * 
   * @return The user credentials.
   */
	public UserCredentialsList getUserCredentialsList() {
		return userCredentialsList;
	}

	/**
	 * The list with user credentials.
	 * @param userCredentialsList
	 */
	public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
		this.userCredentialsList = userCredentialsList;
	}

	public CommitMessages getCommitMessages() {
		return commitMessages;
	}

	public void setCommitMessages(CommitMessages commitMessages) {
		this.commitMessages = commitMessages;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	/**
	 * @param sshPromptAnswers A cache for asking the user for connection message.
	 */
	public void setSshQuestions(HashMap<String, Boolean> sshPromptAnswers) {
    this.sshPromptAnswers = sshPromptAnswers;
  }
	
	/**
	 * @return A cache for asking the user for connection message.
	 */
	public Map<String, Boolean> getSshPromptAnswers() {
    return sshPromptAnswers;
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
		result = prime * result + ((sshPromptAnswers == null) ? 0 : sshPromptAnswers.hashCode());
		result = prime * result + ((warnOnUpstreamChange == null) ? 0 : warnOnUpstreamChange.hashCode());
    result = prime * result + ((warnOnCommitIdChange == null) ? 0 : warnOnCommitIdChange.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof Options) {
	    Options opt = (Options) obj;
	    toReturn = Equaler.verifyEquals(commitMessages, opt.getCommitMessages())
	        && Equaler.verifyEquals(destinationPaths, opt.getDestinationPaths())
	        && Equaler.verifyEquals(passphrase, opt.getPassphrase())
	        && Equaler.verifyEquals(projectsTestsForGit, opt.getProjectsTestsForGit())
	        && Equaler.verifyEquals(repositoryLocations, opt.getRepositoryLocations())
	        && Equaler.verifyEquals(warnOnUpstreamChange, opt.getWarnOnUpstreamChange())
	        && Equaler.verifyEquals(selectedRepository, opt.getSelectedRepository())
	        && Equaler.verifyEquals(sshPromptAnswers, opt.getSshPromptAnswers())
	        && Equaler.verifyEquals(userCredentialsList, opt.getUserCredentialsList())
	        && Equaler.verifyEquals(stagedResViewMode, opt.stagedResViewMode)
	        && Equaler.verifyEquals(defaultPullType, opt.defaultPullType)
	        && Equaler.verifyEquals(warnOnCommitIdChange, opt.getWarnOnCommitIdChange());
	  }
	  return toReturn;
	}

	@Override
	public String toString() {
		return "Options [repositoryLocations=" + repositoryLocations + ", selectedRepository=" + selectedRepository
				+ ", userCredentialsList=" + "CLASSIFIED" + ", commitMessages=" + commitMessages
				+ ", prjectsTestsForGit=" + projectsTestsForGit + ", destinationPaths=" + destinationPaths + ", passphrase="
				+ "CLASSIFIED" + ", resourcesViewMode=" + stagedResViewMode
				+ ", defaultPullType=" + defaultPullType + "]";
	}

}
