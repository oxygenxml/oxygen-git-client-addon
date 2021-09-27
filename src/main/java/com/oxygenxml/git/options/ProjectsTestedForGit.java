package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;


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
	private List<String> projectPaths = new ArrayList<>();

	public List<String> getPaths() {
		return projectPaths;
	}

	public void setPaths(List<String> paths) {
		this.projectPaths = paths != null ? new ArrayList<>(paths) : new ArrayList<>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((projectPaths == null) ? 0 : projectPaths.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof ProjectsTestedForGit) {
	    ProjectsTestedForGit projects = (ProjectsTestedForGit) obj;
	    toReturn = Equaler.verifyListEquals(projectPaths, projects.getPaths());
	  }
	  return toReturn;
	}

}
