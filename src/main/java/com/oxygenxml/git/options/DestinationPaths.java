package com.oxygenxml.git.options;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.oxygenxml.git.utils.Equaler;

/**
 * Entity for the JAXB to store the list of destination paths.
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "destinationPaths")
@XmlAccessorType(XmlAccessType.FIELD)
public class DestinationPaths {

	/**
	 * The list with the previous destination paths
	 */
	@XmlElement(name = "path")
	private List<String> paths = new LinkedList<>();

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
	  if (obj instanceof DestinationPaths) {
	    DestinationPaths destPaths = (DestinationPaths) obj;
	    toReturn = Equaler.verifyListEquals(paths, destPaths.getPaths());
	  }
	  return toReturn;
	}

	@Override
	public String toString() {
		return "DestinationPaths [paths=" + paths + "]";
	}

}
