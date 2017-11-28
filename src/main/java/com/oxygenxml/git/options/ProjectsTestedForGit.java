package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import ro.sync.util.Equaler;

/**
 * Entity for the JAXB to store the list of tested project.xpr paths
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "projectsTested")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectsTestedForGit {

	/**
	 * The list with the users repositories
	 */
	@XmlElement(name = "path")
	private List<String> paths = new ArrayList<String>();

	public List<String> getPaths() {
		return paths;
	}

	public void setPaths(List<String> paths) {
		this.paths = paths;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paths == null) ? 0 : paths.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof ProjectsTestedForGit) {
	    ProjectsTestedForGit projects = (ProjectsTestedForGit) obj;
	    toReturn = Equaler.verifyListEquals(paths, projects.getPaths());
	  }
	  return toReturn;
	}

}
