package com.oxygenxml.git.options;

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

	public UserCredentialsList getUserCredentialsList() {
		return userCredentialsList;
	}

	public void setUserCredentialsList(UserCredentialsList userCredentialsList) {
		this.userCredentialsList = userCredentialsList;
	}

	public CommitMessages getCommitMessages() {
		return commitMessages;
	}

	public void setCommitMessages(CommitMessages commitMessages) {
		this.commitMessages = commitMessages;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commitMessages == null) ? 0 : commitMessages.hashCode());
		result = prime * result + ((prjectsTestsForGit == null) ? 0 : prjectsTestsForGit.hashCode());
		result = prime * result + ((repositoryLocations == null) ? 0 : repositoryLocations.hashCode());
		result = prime * result + ((selectedRepository == null) ? 0 : selectedRepository.hashCode());
		result = prime * result + ((userCredentialsList == null) ? 0 : userCredentialsList.hashCode());
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
		if (userCredentialsList == null) {
			if (other.userCredentialsList != null)
				return false;
		} else if (!userCredentialsList.equals(other.userCredentialsList))
			return false;
		return true;
	}

}
