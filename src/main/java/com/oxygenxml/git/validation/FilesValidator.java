package com.oxygenxml.git.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.document.DocumentPositionedInfo;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

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
   * Method to validate.
   */
  private Method validateMethod;

  /**
   * The problems collector instance.
   * 
   * @see ro.sync.exml.workspace.api.util.validation.ValidatorProblemCollector.
   */
  private Object problemsCollector;

  /**
   * The validation util access.
   */
  private Object validationUtilAccess;
  
  /**
   * <code>true</code> if the validation service is available.
   */
  private boolean isValidatorServiceAvailable = true;
  
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
    extractResources();
  }

  /**
   * Extract methods and classes needed, using reflexion.
   */
  private void extractResources() {
    try {
      final PluginWorkspace pluginWorkspaceAccess = PluginWorkspaceProvider.getPluginWorkspace();
      Method method = pluginWorkspaceAccess.getClass().getMethod("getValidationUtilAccess", new Class[0]);
      validationUtilAccess = method.invoke(pluginWorkspaceAccess, new Object[0]);
      Class valClass = Class.forName("ro.sync.exml.workspace.api.util.validation.ValidatorProblemCollector");
      problemsCollector = Proxy.newProxyInstance(pluginWorkspaceAccess.getClass().getClassLoader(), new Class[] {
          Class.forName("ro.sync.exml.workspace.api.util.validation.ValidatorProblemCollector")
      }, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          if("problemsOccured".equals(method.getName())) {
            DocumentPositionedInfo[] dpis = (DocumentPositionedInfo[]) args[0];
            collector.add(dpis);
          }
          return null;
        }
      });
      validateMethod = validationUtilAccess.getClass().getMethod("validateResources", Iterator.class, boolean.class, valClass);
      final String NOT_SUPPORTED_FILE = (String) Class.forName("ro.sync.exml.workspace.api.util.validation.ValidationProblemsCodes")
          .getField("NOT_SUPPORTED_FILE_WARNING").get(null);
      collector.setFilter(dpi -> NOT_SUPPORTED_FILE != null ? !NOT_SUPPORTED_FILE.equals(dpi.getErrorKey()) : true);
    } catch (Exception e) {
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }
      isValidatorServiceAvailable = false;
    }
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
        validateMethod.invoke(validationUtilAccess, files.iterator(), false, problemsCollector);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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
    return this.isValidatorServiceAvailable;
  }

}
