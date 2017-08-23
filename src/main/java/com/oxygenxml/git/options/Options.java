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
	 * Wrapper for a list of project.xpr that were tested if they want to be a git repository
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

}
