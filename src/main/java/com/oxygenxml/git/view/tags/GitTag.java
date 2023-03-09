package com.oxygenxml.git.view.tags;

import java.util.Date;

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
  private final String name;
  
  /**
   * Tag message
   */
  private final String message;
  
  /**
   * True if tag is Pushed on remote
   */
  private boolean isPushed;
  
  /**
   * The name of the tagger
   */
  private final String taggerName;
  
  /**
   * The email of the tagger
   */
  private final String taggerEmail;
  
  /**
   * The time when the tag was done
   */
  private final Date taggingDate;
  
  /**
   * The id of the commit this tag was made for
   */
  private final String commitID;

  /**
   * Constructor.
   * 
   * @param tagName        Name for tag.
   * @param tagMessage     Message for tag.
   * @param isTagPushed    <code>true</code> if the tag is pushed.
   * @param taggerName     Tagger name for tag.
   * @param taggerEmail    Tagger email for tag.
   * @param taggingDate    Date for tag.
   * @param commitID       Commit ID for tag.
   * @param tagID          ID for tag.
   */
  public GitTag(String tagName, String tagMessage, boolean isTagPushed, String taggerName, 
		  String taggerEmail, Date taggingDate, String commitID) {
    this.name = tagName;
    this.message = tagMessage;
    this.isPushed = isTagPushed;
    this.taggerName = taggerName;
    this.taggerEmail = taggerEmail;
    this.taggingDate = taggingDate;
    this.commitID = commitID;
  }

  /**
   * Get the name of the tag
   * 
   * @return tag Name
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
  
  /**
   * Get the tagger Full name
   * 
   * @return a String, tagger name
   */
  public String getTaggerName() {
    return taggerName;
  }
  
  /**
   * Get the tagger email address
   * 
   * @return the email address of the tagger
   */
  public String getTaggerEmail() {
    return taggerEmail;
  }

  /**
   * Get the tagging date
   * 
   * @return date of the tagging
   */
  public Date getTaggingDate() {
    return taggingDate;
  }

  /**
   * Get the commit id    
   *  
   * @return String form of the SHA-1, in lower case hexadecimal
   */
  public String getCommitID() {
    return commitID;
  }

}
