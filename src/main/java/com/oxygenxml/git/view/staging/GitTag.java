package com.oxygenxml.git.view.staging;

/**
 * A git Tag
 * 
 * @author gabriel_nedianu
 *
 */
public class GitTag {
  
  /**
   * Tag title
   */
  private String name;
  
  /**
   * Tag message
   */
  private String message;
  
  /**
   * True if tag is Pushed on remote
   */
  private boolean isPushed;


  public GitTag(String tagName, String tagMessage, boolean isTagPushed) {
    this.name = tagName;
    this.message = tagMessage;
    this.isPushed = isTagPushed;
  }
  
  /**
   * Get the title of the tag
   * 
   * @return tag Title
   */
  public String getName() {
    return name;
  }

  /**
   * Get the message of the tag
   * 
   * @return tag Message
   */
  public String getMessage() {
    return message;
  }

  
  /**
   * Verify if the tag is pushed on remote 
   * 
   * @return <code>true</code> if the tag is pushed on remote
   */
  public boolean isPushed() {
    return isPushed;
  }

  /**
   * 
   * @param isPushed <code>true</code> if the tag is pushed on remote
   */
  public void setPushed(boolean isPushed) {
    this.isPushed = isPushed;
  }
  
  
}
