package com.oxygenxml.git.options;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
  private Map<String, Boolean> sshPromptAnswers = new HashMap<String, Boolean>();

	/**
	 * Wrapper for a list with the repository locations
	 */
	@XmlElement(name = "repositoryLocations")
	private RepositoryLocations repositoryLocations = new RepositoryLocations();

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
	 * Wrapper for a list of project.xpr that were tested if they want to be a git
	 * repository
	 */
	@XmlElement(name = "projectsTested")
	private ProjectsTestedForGit prjectsTestsForGit = new ProjectsTestedForGit();

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

	public DestinationPaths getDestinationPaths() {
		return destinationPaths;
	}

	public void setDestinationPaths(DestinationPaths destinationPaths) {
		this.destinationPaths = destinationPaths;
	}

	public ProjectsTestedForGit getPrjectsTestsForGit() {
		return prjectsTestsForGit;
	}

	public void setPrjectsTestsForGit(ProjectsTestedForGit prjectsTestsForGit) {
		this.prjectsTestsForGit = prjectsTestsForGit;
	}

	public RepositoryLocations getRepositoryLocations() {
		return repositoryLocations;
	}

	public void setRepositoryLocations(RepositoryLocations repositoryLocations) {
		this.repositoryLocations = repositoryLocations;
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
	public void setSshQuestions(Map<String, Boolean> sshPromptAnswers) {
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
		result = prime * result + ((prjectsTestsForGit == null) ? 0 : prjectsTestsForGit.hashCode());
		result = prime * result + ((repositoryLocations == null) ? 0 : repositoryLocations.hashCode());
		result = prime * result + ((selectedRepository == null) ? 0 : selectedRepository.hashCode());
		result = prime * result + ((userCredentialsList == null) ? 0 : userCredentialsList.hashCode());
		result = prime * result + ((sshPromptAnswers == null) ? 0 : sshPromptAnswers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Options other = (Options) obj;
		if (commitMessages == null) {
			if (other.commitMessages != null)
				return false;
		} else if (!commitMessages.equals(other.commitMessages))
			return false;
		if (destinationPaths == null) {
			if (other.destinationPaths != null)
				return false;
		} else if (!destinationPaths.equals(other.destinationPaths))
			return false;
		if (passphrase == null) {
			if (other.passphrase != null)
				return false;
		} else if (!passphrase.equals(other.passphrase))
			return false;
		if (prjectsTestsForGit == null) {
			if (other.prjectsTestsForGit != null)
				return false;
		} else if (!prjectsTestsForGit.equals(other.prjectsTestsForGit))
			return false;
		if (repositoryLocations == null) {
			if (other.repositoryLocations != null)
				return false;
		} else if (!repositoryLocations.equals(other.repositoryLocations))
			return false;
		if (selectedRepository == null) {
			if (other.selectedRepository != null)
				return false;
		} else if (!selectedRepository.equals(other.selectedRepository))
			return false;
		
		// Check user credentials.
		if (userCredentialsList == null) {
			if (other.userCredentialsList != null)
				return false;
		} else if (!userCredentialsList.equals(other.userCredentialsList))
			return false;
		
		// Check the ssq questions.
		if (sshPromptAnswers != null && other.sshPromptAnswers != null) {
		  return sshPromptAnswers.equals(other.sshPromptAnswers);
		} else if (sshPromptAnswers == null || other.sshPromptAnswers == null) {
		  return false;
		}
		
		return true;
	}

	@Override
	public String toString() {
		return "Options [repositoryLocations=" + repositoryLocations + ", selectedRepository=" + selectedRepository
				+ ", userCredentialsList=" + userCredentialsList + ", commitMessages=" + commitMessages
				+ ", prjectsTestsForGit=" + prjectsTestsForGit + ", destinationPaths=" + destinationPaths + ", passphrase="
				+ passphrase + "]";
	}

}
