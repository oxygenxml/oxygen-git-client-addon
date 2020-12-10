package com.oxygenxml.git.service;

import java.util.HashMap;
import java.util.Map;

public class PullData {

  public int pullBehind;
  
  public Map<String, Integer> submodules = new HashMap<String, Integer>();
  /**
   * 
   * @param pullBehind
   */
  public PullData(int pullBehind) {
    this.pullBehind = pullBehind;
  }
  
  public void setSubmoduleData(String submodule, int pullBehind) {
    submodules.put(submodule, pullBehind);
  }
  
  public int getPullBehind() {
    return pullBehind;
  }
  
  public Map<String, Integer> getSubmodules() {
    return submodules;
  }
  
  public boolean hasSubmoduleUpdates() {
    return submodules.values().stream().count() > 0;
  }
  
  public long getSubmoduleCount() {
    return submodules.values().stream().count();
  }
  
}
