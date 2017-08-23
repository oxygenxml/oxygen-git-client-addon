package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

}
