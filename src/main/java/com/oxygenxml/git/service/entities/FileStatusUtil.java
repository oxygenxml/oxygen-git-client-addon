package com.oxygenxml.git.service.entities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.utils.FileUtil;

/**
 * Class with usefully methods for files statues.
 * 
 * @author Alex_Smarandache
 *
 */
public class FileStatusUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileStatusUtil.class);


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
      final TreeWalk walk, final RevCommit commit, @Nullable final RevCommit oldCommit,
      final TreeFilter... markTreeFilters) throws MissingObjectException,
  IncorrectObjectTypeException, CorruptObjectException, IOException {
    return compute(repository, walk, commit, oldCommit, commit.getParents(),
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
  @NonNull
  public static List<FileStatus> compute(final Repository repository,
      final TreeWalk walk, final RevCommit commit, final RevCommit oldCommit,
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

    final String commitName     = commit.getName();
    final String oldCommitName  = oldCommit !=null ? oldCommit.getName() : null;
    if (walk.getTreeCount() <= 2) {
      List<DiffEntry> entries = DiffEntry.scan(walk, false, markTreeFilters);
      List<DiffEntry> xentries = new LinkedList<>(entries);
      RenameDetector detector = new RenameDetector(repository);
      detector.addAll(entries);
      List<DiffEntry> renames = detector.compute(walk.getObjectReader(),NullProgressMonitor.INSTANCE);
      
      for (DiffEntry fileDiff : renames) { 
        final FileStatus currentFileStatus = new FileStatusOverDiffEntry
            (fileDiff, commitName, oldCommitName);
        filesToReturn.add(currentFileStatus);
        cleanDiffEntries(fileDiff, xentries);
      }
      
      addFiles(filesToReturn, xentries, commitName, oldCommitName);
      
    } else { 
    	// This case is for merge commits, this file extraction method is a bit slower than before. 
    	// It should be seen in the future if a faster way can be found to generate affected files in merge commits.
        try {
          filesToReturn.addAll(getChanges(repository, commit, oldCommit));
        } catch (IOException | GitAPIException e) {
         
        }
      }    

    return filesToReturn;
  }
  
  
  /**
   * Clean diff entries from xentries list raported to file diff. If the list has an element with the same old and new path, this element will be removed from list.
   * 
   * @param fileDiff The diff entry to delete from xentries.
   * @param xentries The list with diff entry.
   */
  private static void cleanDiffEntries(DiffEntry fileDiff, List<DiffEntry> xentries) {
	  for (Iterator<DiffEntry> xentriesIterator = xentries.iterator(); xentriesIterator.hasNext();) {
		  DiffEntry n = xentriesIterator.next();
		  if (fileDiff.getOldPath().equals(n.getOldPath()) || 
				  fileDiff.getNewPath().equals(n.getNewPath())) {
			  xentriesIterator.remove();
		  }
	  }
  }
  
  
  /**
   * Add the file status for each diff entry from xentries.
   * 
   * @param files      list to append the new files statuses
   * @param xentries   list with diff entry to append
   */
  private static void addFiles(final List<FileStatus> files, 
      final List<DiffEntry> xentries, final String commit, final String oldCommit) {
	  for (DiffEntry fileDiff : xentries) {  
	    final FileStatus currentFileStatus = new FileStatusOverDiffEntry
          (fileDiff, commit, oldCommit);
	    files.add(currentFileStatus);
	  }
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
   * Map between the {@link ChangeType} types and our {@link GitChangeType}
   * 
   * @param diffChange Comparison data.
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
   * Computes a list of files statues URLs.
   * 
   * @param files          The files.
   * @param computeGitURLs <code>true</code> if the computed URL should be a git URL, <code>false</code> if the URL should be from the WC.
   * 
   * @return The computed URLs.
   */
  public static List<URL> getFilesStatuesURL(@NonNull final List<FileStatus> files, 
      final boolean computeGitURLs) {
    final List<URL> filesURL = new ArrayList<>();
    files.forEach(file -> {
      try {
        filesURL.add(computeGitURLs ? computeFileStatusURL(file) :
            FileUtil.getFileURL(file.getFileLocation()));
      } catch (NoRepositorySelected | MalformedURLException e) {
        LOGGER.debug(e.getMessage(), e);
      }
    });
      
    return filesURL;
  }
  
  /**
   * Computes URL for the given file.
   * 
   * @param file File to compute URL.
   * 
   * @return Computed URL.
   * 
   * @throws MalformedURLException
   * @throws NoRepositorySelected
   */
  public static URL computeFileStatusURL(final FileStatus file) throws MalformedURLException, NoRepositorySelected {
    URL fileURL = null;
    final String fileLocation = file.getFileLocation();
    if (file.getChangeType() == GitChangeType.ADD || file.getChangeType() == GitChangeType.CHANGED) {
      // A file from the INDEX. We need a special URL to access it.
      fileURL = GitRevisionURLHandler.encodeURL(
          VersionIdentifier.INDEX_OR_LAST_COMMIT,
          fileLocation);
    } else {
      // We must open a local copy.
      fileURL = FileUtil.getFileURL(fileLocation);
    }
    
    return fileURL;
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

  
  /**
   * Gets all the files changed between two revisions.
   * 
   * @param repository Repository.
   * @param newCommit The new commit.
   * @param oldCommit The previous commit. Maybe <code>null<code>.
   * 
   * @return A list with changed files. Never <code>null</code>.
   * @throws IOException
   * @throws GitAPIException
   */
  private static List<FileStatus> getChanges(Repository repository, RevCommit newCommit, RevCommit oldCommit) throws IOException, GitAPIException {
    List<DiffEntry> diffs = diff(repository, newCommit, oldCommit);

    return diffs
        .stream()
        .map(t -> new FileStatusOverDiffEntry(t, newCommit.getId().name(), oldCommit != null ? oldCommit.getId().name() : null))
        .collect(Collectors.toList());
  }


  /**
   * Gets all the files changed between two revisions.
   * 
   * @param repository Repository.
   * @param newCommit The new commit.
   * @param oldCommit The previous commit. Maybe <code>null<code>.
   * 
   * @return A list with changed files. Never <code>null</code>.
   * @throws IOException
   * @throws GitAPIException
   */
  private static List<DiffEntry> diff(
      Repository repository, 
      RevCommit newCommit, 
      RevCommit oldCommit) throws IOException, GitAPIException {
    List<DiffEntry> collect = Collections.emptyList();
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
      newTreeIter.reset(reader, newCommit.getTree().getId());

      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      if (oldCommit != null) {
        oldTreeIter.reset(reader, oldCommit.getTree().getId());
      }


      // finally get the list of changed files
      try (Git git = new Git(repository)) {
        List<DiffEntry> diffs= git.diff()
            .setNewTree(newTreeIter)
            .setOldTree(oldTreeIter)
            .call();

        // Identify potential renames.
        RenameDetector rd = new RenameDetector(git.getRepository());
        rd.addAll(diffs);
        collect = rd.compute();
      }
    }

    return collect;
  }

  
  /**
   * Remove all files statues with the given extension.
   * 
   * @param files     The files.
   * @param extension The extension.
   * 
   * @return The files without files with the given extension.
   */
  @NonNull 
  public static List<FileStatus> removeFilesByExtension(@NonNull final List<FileStatus> files,
      final String extension) {
    if(extension != null && !extension.isEmpty()) {
      files.removeIf(file -> file.getFileLocation() != null && file.getFileLocation().endsWith(extension));
    }
    return files;
  }
  
}
