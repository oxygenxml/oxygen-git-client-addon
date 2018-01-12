package com.oxygenxml.git.sax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Collects all refered paths insdie an Oxygen project file.
 * 
 * @author alex_jitianu
 */
public class XPRHandler extends DefaultHandler {

	/**
	 * List to hold the file paths from the xpr file
	 */
	private List<String> filePaths = new ArrayList<>();


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("folder")) {
			// create a new file path
			String filePath = attributes.getValue("path");
			if (filePath != null && (!"".equals(filePath))) {
				filePaths.add(filePath);
			}
		}
	}
	
	public List<String> getPaths() {
		return filePaths;
	}
}
