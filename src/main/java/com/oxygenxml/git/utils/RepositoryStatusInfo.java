package com.oxygenxml.git.utils;

public class RepositoryStatusInfo {

  /**
   * Repository status: available or not.
   */
  public enum RepositoryStatus {
    /**
     * Available.
     */
    AVAILABLE,
    /**
     * Unavailable.
     */
    UNAVAILABLE;
  }
  
  /**
   * Status: available or not.
   */
  private RepositoryStatus repoStatus;
  
  /**
   * Extra info.
   */
  private String extraInfo;
  
  /**
   * Constructor.
   * 
   * @param repoStatus Repository status.
   */
  public RepositoryStatusInfo(RepositoryStatus repoStatus) {
    this.repoStatus = repoStatus;
  }
  
  /**
   * Constructor.
   * 
   * @param repoStatus Repository status.
   * @param extraInfo  Extra information.
   */
  public RepositoryStatusInfo(RepositoryStatus repoStatus, String extraInfo) {
    this.repoStatus = repoStatus;
    this.extraInfo = extraInfo;
  }
  
  /**
   * @return the repo status.
   */
  public RepositoryStatus getRepoStatus() {
    return repoStatus;
  }
  
  /**
   * @return the extra information or <code>null</code>.
   */
  public String getExtraInfo() {
    return extraInfo;
  }
  
}
