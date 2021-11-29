package com.oxygenxml.git.service.entities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Class with usefully methods for files statues.
 * 
 * @author Alex_Smarandache
 *
 */
public class FileStatusUtil {


  /**
   * Hidden constructor.
   */
  private FileStatusUtil() {
    // nothing.
  }
  

  /**
   * Computer files statues for specified tree walk and commit.
   *
   * @param repository         The current repository.
   * @param walk               Tree walk for current repository.
   * @param commit             The current commit
   * @param markTreeFilters    optional filters for marking entries, see {@link #isMarked(int)}
   *            
   * @return non-null but possibly empty list with file statues.
   * 
   * @throws MissingObjectException
   * @throws IncorrectObjectTypeException
   * @throws CorruptObjectException
   * @throws IOException
   */
  public static List<FileStatus> compute(final Repository repository,
      final TreeWalk walk, final RevCommit commit,
      final TreeFilter... markTreeFilters) throws MissingObjectException,
  IncorrectObjectTypeException, CorruptObjectException, IOException {
    return compute(repository, walk, commit, commit.getParents(),
        markTreeFilters);
  }
  

  /**
   * Computer files statues for specified tree walk and commit.
   *
   * @param repository         The current repository.
   * @param walk               Tree walk for current repository.
   * @param commit             The current commit
   * @param parents            The current commit parents.
   * @param markTreeFilters    optional filters for marking entries, see {@link #isMarked(int)}
   *            
   * @return non-null but possibly empty list with file statues.
   * 
   * @throws MissingObjectException
   * @throws IncorrectObjectTypeException
   * @throws CorruptObjectException
   * @throws IOException
   */

  public static List<FileStatus> compute(final Repository repository,
      final TreeWalk walk, final RevCommit commit,
      final RevCommit[] parents,
      final TreeFilter... markTreeFilters) throws MissingObjectException,
  IncorrectObjectTypeException, CorruptObjectException, IOException {

    final List<FileStatus> filesToReturn = new ArrayList<>();

    if (parents.length > 0) {
      walk.reset(trees(commit, parents));
    } else {
      walk.reset();
      walk.addTree(new EmptyTreeIterator());
      walk.addTree(commit.getTree());
    }

    if (walk.getTreeCount() <= 2) {
      List<DiffEntry> entries = DiffEntry.scan(walk, false, markTreeFilters);
      List<DiffEntry> xentries = new LinkedList<>(entries);
      RenameDetector detector = new RenameDetector(repository);
      detector.addAll(entries);
      List<DiffEntry> renames = detector.compute(walk.getObjectReader(),NullProgressMonitor.INSTANCE);
      
      for (DiffEntry fileDiff : renames) {
        final FileStatus currentFileStatus = new FileStatus(toGitChangeType(fileDiff.getChangeType()), fileDiff.getNewPath());
        filesToReturn.add(currentFileStatus);

        for (Iterator<DiffEntry> xentriesIterator = xentries.iterator(); xentriesIterator.hasNext();) {
          DiffEntry n = xentriesIterator.next();
          if (fileDiff.getOldPath().equals(n.getOldPath()) || 
              fileDiff.getNewPath().equals(n.getNewPath())) {
            xentriesIterator.remove();
          }
        }
      }
      
      for (DiffEntry fileDiff : xentries) {
        final FileStatus currentFileStatus = new FileStatus(
            toGitChangeType(fileDiff.getChangeType()), 
            fileDiff.getChangeType() != ChangeType.DELETE ?
            fileDiff.getNewPath() : fileDiff.getOldPath());
        filesToReturn.add(currentFileStatus);
      }
      
    }
    
    else { // DiffEntry does not support walks with more than two trees
      final int nTree = walk.getTreeCount();
      final int myTree = nTree - 1;

      while (walk.next()) {
        if (matchAnyParent(walk, myTree)) {
          continue;
        }

        String mergedFilePath = walk.getPathString();
        
        int m0 = 0;
        for (int i = 0; i < myTree; i++) {
          m0 |= walk.getRawMode(i);
        }
        
        ChangeType mergedFileChangeType = ChangeType.MODIFY;
        
        final int m1 = walk.getRawMode(myTree);
        
        if (m0 == 0 && m1 != 0) {
          mergedFileChangeType = ChangeType.ADD;
        } else if (m0 != 0 && m1 == 0) {
          mergedFileChangeType = ChangeType.DELETE;
        }
        
        filesToReturn.add(new FileStatus(toGitChangeType(mergedFileChangeType), mergedFilePath));
      }

    }

    return filesToReturn;
  }

  
  /**
   * @param walk   The tree walk.
   * @param myTree Index to current tree
   * 
   * @return <code>true</code> if any parent match with tree argument.
   */
  private static boolean matchAnyParent(final TreeWalk walk, final int myTree) {
    final int m = walk.getRawMode(myTree);
    boolean toReturn = false;
    
    for (int i = 0; i < myTree; i++) {
      if (walk.getRawMode(i) == m && walk.idEqual(i, myTree)) {
        toReturn = true;
        break;
      }
    }
    
    return toReturn;
  }


  
  /**
   * Compute and return the objects id of current commit trees.
   *   
   * @param commit   Current commit.
   * @param parents  Parents for this commit.
   * 
   * @return The trees objects id.
   */
  private static ObjectId[] trees(final RevCommit commit, final RevCommit[] parents) {
    final ObjectId[] toReturn = new ObjectId[parents.length + 1];
   
    for (int i = 0; i < toReturn.length - 1; i++) {
      toReturn[i] = parents[i].getTree().getId();
    }
    toReturn[toReturn.length - 1] = commit.getTree().getId();
    
    return toReturn;
  }



  /**
   * Map between the {@link DiffEntry} types and our {@link GitChangeType}
   * 
   * @param entry Comparison data.
   * 
   * @return The type of change.
   */
  public static GitChangeType toGitChangeType(ChangeType diffChange) {
    GitChangeType toReturn = GitChangeType.ADD;
    if (ChangeType.DELETE == diffChange) {
      toReturn = GitChangeType.REMOVED;
    } else if (ChangeType.MODIFY == diffChange) {
      toReturn = GitChangeType.CHANGED;
    } else if (isRename(diffChange)) {
      toReturn = GitChangeType.RENAME;
    }

    return toReturn;
  }

  
  /**
   * Checks the change type to see if it represents a rename.
   * 
   * @param diffChange The ChangeType.
   * 
   * @return <code>true</code> if this change represents a rename.
   */
  public static boolean isRename(ChangeType diffChange) {
    return diffChange == ChangeType.RENAME
        || diffChange == ChangeType.COPY;
  }

}
