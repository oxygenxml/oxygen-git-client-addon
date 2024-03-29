package com.oxygenxml.git.service.entities;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.utils.Equaler;

/**
 * A wrapper over a diff entry representing a changed suffered by a resource. 
 */
public class FileStatusOverDiffEntry extends FileStatus {
  /**
   * Diff entry representing a changed suffered by a resource.
   */
  private DiffEntry entry;
  /**
   * The new revision id used to compute the inner {@link DiffEntry}.
   */
  private String newRevId;
  /**
   * The old revision id used to compute the inner {@link DiffEntry}.
   */
  private String oldRevId;

  /**
   * Constructor.
   * 
   * @param entry The diff entry.
   * @param newRevId The new revision id used to compute the inner {@link DiffEntry}.
   * @param oldRevId The old revision id used to compute the inner {@link DiffEntry}.
   */
  public FileStatusOverDiffEntry(DiffEntry entry, String newRevId, String oldRevId) {
    super(FileStatusUtil.toGitChangeType(entry.getChangeType()), getPath(entry));
    
    this.entry = entry;
    this.newRevId = newRevId;
    this.oldRevId = oldRevId;
  }
  
  /**
   * @return The new revision id used to compute the inner {@link DiffEntry}.
   */
  public String getNewRevId() {
    return newRevId;
  }
  
  /**
   * @return The old revision id used to compute the inner {@link DiffEntry}.
   */
  public String getOldRevId() {
    return oldRevId;
  }

  /**
   * @param entry Diff entry representing a changed suffered by a resource.
   * 
   * @return The path of the resource, relative to the WC.
   */
  private static String getPath(DiffEntry entry) {
    if (ChangeType.DELETE == entry.getChangeType()) {
      return entry.getOldPath();
    }
    
    return entry.getNewPath();
  }
  
  /**
   * @return The inner comparison data.
   */
  public DiffEntry getDiffEntry() {
    return entry;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((newRevId == null) ? 0 : newRevId.hashCode());
    result = prime * result + ((oldRevId == null) ? 0 : oldRevId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    boolean equals = false;
    if (obj instanceof FileStatusOverDiffEntry) {
      FileStatusOverDiffEntry other = (FileStatusOverDiffEntry) obj;
      equals = super.equals(other)
          && Equaler.verifyEquals(other.newRevId, newRevId)
          && Equaler.verifyEquals(other.oldRevId, oldRevId);
    }
    return equals;
  }
  
  

}
