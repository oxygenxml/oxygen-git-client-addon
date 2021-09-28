package com.oxygenxml.git.options;

import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitPlugin;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * Creates an options object. handles bakwards compatibility.
 * 
 * @author alex_jitianu
 */
public class OptionsLoader {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(OptionsManager.class);
  
  /**
   * The initial key used to saved options.
   */
  public static final String OLD_GIT_PLUGIN_OPTIONS = "MY_PLUGIN_OPTIONS";
  /**
   * A proper name for the options.
   */
  private static final String GIT_PLUGIN_OPTIONS = "GIT_PLUGIN_OPTIONS";

  /**
   * Uses JAXB to load all the selected repositories from the users in the
   * repositoryOptions variable.
   * 
   * @param wsOptionsStorage Oxygen options storage.
   * 
   * @return An options instance.
   */
  public static Options loadOptions(WSOptionsStorage wsOptionsStorage) {
    Options options = null;
    Optional<Options> oldOptions = loadOldJaxbOptions();
    if (oldOptions.isPresent()) {
      // Reset old keys.
      resetOldJaxbOptions(wsOptionsStorage);
      // Backwards compatibility. Copy old options into the new one.
      options = copyOldOptionsIntoNewTagsOptions(oldOptions.get(), wsOptionsStorage);
    } else {
      options = new TagBasedOptions(wsOptionsStorage);
    }
    
    return options;
  }

  /**
   * Copy this old options into a new options object based on tags.
   * 
   * @param oldOptions Old options.
   * @param os Oxygen API options storage. 
   * 
   * @return A new instance of options that uses tags.
   */
  private static Options copyOldOptionsIntoNewTagsOptions(Options oldOptions, WSOptionsStorage os) {
    Options newOptions = new TagBasedOptions(os);
    
    newOptions.setAutoPushWhenCommitting(oldOptions.isAutoPushWhenCommitting());
    newOptions.setDefaultPullType(oldOptions.getDefaultPullType()) ;
    newOptions.setUnstagedResViewMode(oldOptions.getUnstagedResViewMode()) ;
    newOptions.setStagedResViewMode(oldOptions.getStagedResViewMode()) ;
    newOptions.setDestinationPaths(oldOptions.getDestinationPaths()) ;
    newOptions.setProjectsTestsForGit(oldOptions.getProjectsTestsForGit()) ;
    newOptions.setRepositoryLocations(oldOptions.getRepositoryLocations()) ;
    newOptions.setNotifyAboutNewRemoteCommits(oldOptions.isNotifyAboutNewRemoteCommits()) ;
    newOptions.setCheckoutNewlyCreatedLocalBranch(oldOptions.isCheckoutNewlyCreatedLocalBranch()) ;


    
    newOptions.setSelectedRepository(oldOptions.getSelectedRepository()) ;
    newOptions.setUserCredentialsList(oldOptions.getUserCredentialsList()) ;
    newOptions.setCommitMessages(oldOptions.getCommitMessages()) ;
    newOptions.setPassphrase(oldOptions.getPassphrase()) ;

    newOptions.setSshQuestions(oldOptions.getSshPromptAnswers()) ;

    newOptions.setWhenRepoDetectedInProject(oldOptions.getWhenRepoDetectedInProject());
    newOptions.setUpdateSubmodulesOnPull(oldOptions.getUpdateSubmodulesOnPull());
    newOptions.setPersonalAccessTokensList(oldOptions.getPersonalAccessTokensList());
    
    Map<String, String> warnOnChangeCommitId = oldOptions.getWarnOnChangeCommitId();
    warnOnChangeCommitId.forEach(newOptions::setWarnOnChangeCommitId);
    
    return newOptions;
  }

  /**
   * Resets the old JAXB-related options.
   * 
   * @param wsOptionsStorage Oxygen API options storage.
   */
  private static void resetOldJaxbOptions(WSOptionsStorage wsOptionsStorage) {
    wsOptionsStorage.setOption(OLD_GIT_PLUGIN_OPTIONS, null);
    wsOptionsStorage.setOption(GIT_PLUGIN_OPTIONS, null);
  }

  /**
   * @return The old Jaxb-based options stored in Oxygen options under a single key.
   */
  private static Optional<Options> loadOldJaxbOptions() {
    Options options = null;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(JaxbOptions.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      if (OxygenGitPlugin.getInstance() == null) {
        // Running outside Oxygen, for example from tests.
        File optionsFileForTests = getOptionsFileForTests();
        if (optionsFileForTests.exists()) {
          options = (JaxbOptions) jaxbUnmarshaller.unmarshal(optionsFileForTests);
        } else {
          logger.warn("Options file doesn't exist:" + optionsFileForTests.getAbsolutePath());
        }
      } else {
        // Running in Oxygen's context. Save inside Oxygen's options. 
        String option = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage()
            .getOption(OLD_GIT_PLUGIN_OPTIONS, null);
 
        if (option != null) {
          // Backwards.
          // 1. Load
          options = (JaxbOptions) jaxbUnmarshaller.unmarshal(new StringReader(
              PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
        } else {
          option = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().getOption(GIT_PLUGIN_OPTIONS,
              null);
          // Load the new key if exists.
          if (option != null) {
            options = (JaxbOptions) jaxbUnmarshaller.unmarshal(new StringReader(
                PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
          }
        }
 
      }
    } catch (JAXBException e) {
      logger.warn("Options not loaded: " + e, e);
    }
    
    return Optional.ofNullable(options);
  }
  
  /**
   * !!! FOR TESTS !!!
   * 
   * Creates the the options file and returns it
   * 
   * @return the options file
   */
  private static File getOptionsFileForTests() {
    File baseDir = null;
    if (OxygenGitPlugin.getInstance() != null) {
      baseDir = OxygenGitPlugin.getInstance().getDescriptor().getBaseDir();
    } else {
      baseDir = new File("src/test/resources");
    }
    return new File(baseDir, OPTIONS_FILENAME_FOR_TESTS);
  }
  

  /**
   * The filename in which all the options are saved
   */
  private static final String OPTIONS_FILENAME_FOR_TESTS = "Options.xml";

}
