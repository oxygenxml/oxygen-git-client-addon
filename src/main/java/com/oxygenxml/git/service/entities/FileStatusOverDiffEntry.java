package com.oxygenxml.git.service.entities;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.service.RevCommitUtil;

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
    super(map(entry), getPath(entry));
    
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
   * Map between the {@link DiffEntry} types and our {@link GitChangeType}
   * 
   * @param entry Comparison data.
   * 
   * @return The type of change.
   */
  private static GitChangeType map(DiffEntry entry) {
    GitChangeType toreturn = GitChangeType.ADD;
    ChangeType diffChange = entry.getChangeType();
    if (ChangeType.DELETE == diffChange) {
      toreturn = GitChangeType.REMOVED;
    } else if (ChangeType.MODIFY == diffChange) {
      toreturn = GitChangeType.CHANGED;
    } else if (RevCommitUtil.isRename(entry)) {
      toreturn = GitChangeType.RENAME;
    }
    
    return toreturn;
  }
  
  /**
   * @return The inner comparison data.
   */
  public DiffEntry getDiffEntry() {
    return entry;
  }

}
