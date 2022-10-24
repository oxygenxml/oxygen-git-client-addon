package com.oxygenxml.git.validation.internal;

import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.util.validation.ValidationUtilAccess;

/**
 * Used to validate files.
 * 
 * @author alex_smarandache
 *
 */
public class FilesValidator implements IValidator {

  /**
   * Collector for all problems that occurs.
   */
  private ICollector collector;
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FilesValidator.class);
  
  /**
   * Used to filter problems to avoid exposing issues that are not related to validated files.
   */
  private IProblemFilter filter;
  
  /**
   * Constructor.
   */
  public FilesValidator() {
    this(new ProblemsCollector());
  }
  
  /**
   * Constructor.
   * 
   * @param collector The collector to collect all problems.
   */
  public FilesValidator(final ICollector collector) {
    this.collector = collector;
  }

  /**
   * Validate the given files and collect problems in internal collector.
   * <br>
   * You can access that collector through internal <code>getCollector()</code> method.
   * <br>
   * The collector is reseted on every call of this method.
   * <br>
   * If <code>isAvailable()</code> internal method returns <code>false</code>, this method will do nothing.
   * <br>
   * By default, this collector could have a filter to eliminate unnecessary problems.  
   *
   * @see ICollector
   */
  @Override
  public void validate(final List<URL> files) {
    if(isAvailable()) {
      collector.reset();
      try {
        final PluginWorkspace pluginWorkspaceAccess = PluginWorkspaceProvider.getPluginWorkspace();
        final ValidationUtilAccess validationUtilAccess = pluginWorkspaceAccess.getValidationUtilAccess();
        validationUtilAccess.validateResources(files.iterator(), false, collector::add);
      } catch (IllegalArgumentException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  } 
  
  @Override
  public ICollector getCollector() {
    return this.collector;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

}
