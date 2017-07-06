package com.oxygenxml.sdksamples.workspace;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Workspace access plugin. 
 */
public class WorkspaceAccessPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static WorkspaceAccessPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor
   */
  public WorkspaceAccessPlugin(PluginDescriptor descriptor) {
    super(descriptor);

    if (instance != null) {
      throw new IllegalStateException("Already instantiated!");
    }
    instance = this;
  }
  
  /**
   * Get the plugin instance.
   * 
   * @return the shared plugin instance.
   */
  public static WorkspaceAccessPlugin getInstance() {
    return instance;
  }
}