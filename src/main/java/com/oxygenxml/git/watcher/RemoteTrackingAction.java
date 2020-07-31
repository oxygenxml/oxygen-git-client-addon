package com.oxygenxml.git.watcher;

/**
 * The possible ways of tracking the remote and notify the user about its changes.
 */
public interface RemoteTrackingAction {
  
  /**
   * Value for as soon as there are new commits upstream.
   */
  public static final String WARN_UPSTREAM_ALWAYS = "always";
  
  /**
   * Value for notifying when there are new commits upstream that may cause conflicts.
   */
  public static final String WARN_UPSTREAM_ON_CHANGE = "onChange";
  
  /**
   * Value to skip any checks on the upstream state.
   */
  public static final String WARN_UPSTREAM_NEVER = "never";
}
