package com.oxygenxml.sdksamples.workspace.git.utils;

import java.io.File;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.RepositoryOption;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.RepositoryOptions;

public class OptionsManager {
	private static final String REPOSITORY_FILENAME = "Repositories.xml";

	private static RepositoryOptions repositoryOptions = null;

	private static OptionsManager instance;

	public static OptionsManager getInstance() {
		if (instance == null) {
			instance = new OptionsManager();
		}
		return instance;
	}

	private void loadRepositoryOptions() {
		if (repositoryOptions == null) {
			String fileName = REPOSITORY_FILENAME;
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(RepositoryOptions.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				repositoryOptions = (RepositoryOptions) jaxbUnmarshaller
						.unmarshal(new File(Constants.RESOURCES_PATH + fileName));
			} catch (JAXBException e) {
				e.printStackTrace();
			}

			if (repositoryOptions == null) {
				repositoryOptions = new RepositoryOptions();
			}
		}
	}

	private void saveRepositoryOptions() {
		String fileName = REPOSITORY_FILENAME;
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(RepositoryOptions.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(repositoryOptions, new File(Constants.RESOURCES_PATH + fileName));
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}

	}

	public Set<RepositoryOption> getRepositoryEntries() {
		loadRepositoryOptions();

		return repositoryOptions.getRepositoryOptions();
	}

	public void addRepository(RepositoryOption repositoryOption) {
		loadRepositoryOptions();

		repositoryOptions.getRepositoryOptions().add(repositoryOption);
		saveRepositoryOptions();
	}
}
