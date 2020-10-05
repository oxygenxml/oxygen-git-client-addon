package com.oxygenxml.git;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Workspace access plugin. 
 */
public class OxygenGitPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static OxygenGitPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor 
   */
  public OxygenGitPlugin(PluginDescriptor descriptor) {
    super(descriptor);

    if (instance != null) {
      throw new IllegalStateException("Already instantiated!");
    }
    instance = this; // NOSONAR
  }
  
  /**
   * Get the plugin instance.
   * 
   * @return the shared plugin instance.
   */
  public static OxygenGitPlugin getInstance() {
    return instance;
  }
}