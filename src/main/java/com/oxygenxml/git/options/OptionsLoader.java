package com.oxygenxml.git.options;

import java.io.File;
import java.io.StringReader;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.oxygenxml.git.OxygenGitPlugin;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

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
  private static final String OLD_GIT_PLUGIN_OPTIONS = "MY_PLUGIN_OPTIONS";
  /**
   * A proper name for the options.
   */
  private static final String GIT_PLUGIN_OPTIONS = "GIT_PLUGIN_OPTIONS";

  /**
   * Uses JAXB to load all the selected repositories from the users in the
   * repositoryOptions variable.
   * 
   * @return An options instance.
   */
  public static OptionsInterface loadOptions() {
    OptionsInterface options = loadOldJaxbOptions();
    logger.info("Old options " + options);
    if (options != null) {
      // Reset old keys.
      resetOldJaxbOptions();
      // Backwards compatibility. Copy old options into the new one.
      options = copyOldOptionsIntoNewTagsOptions(options);
    } else {
      options = new OptionsWithTags(PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
    }
    
    logger.info("Goinf with the new : " + options);
    
    return options;
  }

  /**
   * Copy this old options into a new options object based on tags.
   * 
   * @param oldOptions Old options.
   * 
   * @return A new instance of options that uses tags.
   */
  private static OptionsInterface copyOldOptionsIntoNewTagsOptions(OptionsInterface oldOptions) {
    logger.info("Use " + PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
    OptionsInterface newOptions = new OptionsWithTags(PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
    
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
   * Resets the old Jaxb-related options.
   */
  private static void resetOldJaxbOptions() {
    if (PluginWorkspaceProvider.getPluginWorkspace() != null && PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage() != null) {
      PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(OLD_GIT_PLUGIN_OPTIONS, null);
      PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(GIT_PLUGIN_OPTIONS, null);
    }
  }

  /**
   * @return The old Jaxb-based options stored in Oxygen options under a single key.
   */
  private static OptionsInterface loadOldJaxbOptions() {
    OptionsInterface options;
    options = new Options();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Options.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      if (OxygenGitPlugin.getInstance() == null) {
        // Running outside Oxygen, for example from tests.
        File optionsFileForTests = getOptionsFileForTests();
        if (optionsFileForTests.exists()) {
          options = (Options) jaxbUnmarshaller.unmarshal(optionsFileForTests);
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
          options = (Options) jaxbUnmarshaller.unmarshal(new StringReader(
              PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
          // 2. Reset
          PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(OLD_GIT_PLUGIN_OPTIONS, null);
        } else {
          option = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().getOption(GIT_PLUGIN_OPTIONS,
              null);
          // Load the new key if exists.
          if (option != null) {
            options = (Options) jaxbUnmarshaller.unmarshal(new StringReader(
                PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess().unescapeAttributeValue(option)));
          }
        }
 
      }
    } catch (JAXBException e) {
      logger.warn("Options not loaded: " + e, e);
    }
    
    return options;
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
