package com.oxygenxml.sdksamples.workspace.git.jaxb.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Options")
@XmlAccessorType(XmlAccessType.FIELD)
public class Options {

	@XmlElement(name = "repositoryLocations")
	private RepositoryLocations repositoryLocations = new RepositoryLocations();

	@XmlElement(name = "selectedRepository")
	private String selectedRepository = "";

	@XmlElement(name = "username")
	private String username = "";

	@XmlElement(name = "password")
	private String password = "";

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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
