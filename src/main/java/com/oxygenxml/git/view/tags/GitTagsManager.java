package com.oxygenxml.git.view.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;

/**
 * Used for working with GitTags
 * 
 * @author gabriel_nedianu
 *
 */
public class GitTagsManager {
  
  /**
   * private constructor
   */
  private GitTagsManager() {}
  
  /**
   * Get the titles of the remote Tags
   * 
   * @return A List<String> with all the titles of the remote Tags
   * 
   * @throws GitAPIException
   */
  public static List<String> getRemoteTagsTitle() throws GitAPIException{
    
    List<String> remoteTagsTitles = new ArrayList<>(); 
    
    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
    Collection <Ref> refs = GitAccess.getInstance().getGit().lsRemote().setCredentialsProvider(credentialsProvider).setTags(true).call();
    for (Ref ref : refs) {
      remoteTagsTitles.add(Repository.shortenRefName(ref.getName()));
    }
    
    return remoteTagsTitles;
  }
  
  /**
   * Get all the remote Tags
   * 
   * @return A GitTag List wit all the remote tags
   * 
   * @throws GitAPIException
   * @throws NoRepositorySelected
   * @throws IOException
   */
  public static List<GitTag> getRemoteTags() throws GitAPIException, NoRepositorySelected, IOException{

    List<GitTag> remoteTags = new ArrayList<>(); 

    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
    Collection <Ref> refs = GitAccess.getInstance().getGit().lsRemote().setCredentialsProvider(credentialsProvider).setTags(true).call();

    Repository repository = GitAccess.getInstance().getRepository();
    try (RevWalk walk = new RevWalk(repository)) {
      for (Ref ref : refs) {
        ObjectId objectIdOfTag = ref.getObjectId();
        RevObject object = walk.parseAny(objectIdOfTag);
        if (object instanceof RevTag) {
          RevTag tag = (RevTag) object;
          remoteTags.add(
              new GitTag(tag.getTagName(),
                  tag.getFullMessage(),
                  true,
                  tag.getTaggerIdent().getName(),
                  tag.getTaggerIdent().getEmailAddress(),
                  tag.getTaggerIdent().getWhen(),
                  tag.getObject().getName()));
        } else if (object instanceof RevCommit) {
          RevCommit lightTag = (RevCommit) object;
          String lightTagTitle = Repository.shortenRefName(lightTag.getName());
          remoteTags.add(
              new GitTag(lightTagTitle,
                  "",
                  true,
                  lightTag.getAuthorIdent().getName(),
                  lightTag.getAuthorIdent().getEmailAddress(),
                  lightTag.getAuthorIdent().getWhen(),
                  lightTag.getName()));
        } 
      }
    }
    return remoteTags;
  }
  
  /**
   * Get all of the local tags ( can be pushed or not pushed )
   * 
   * @return A GitTag list with all the Tags
   * 
   * @throws GitAPIException 
   * @throws NoRepositorySelected 
   * @throws IOException 
   */
  public static List<GitTag> getLocalTags() throws GitAPIException, NoRepositorySelected, IOException{
    List<GitTag> allTags = new ArrayList<>();
    List<String> remoteTagsTitle = getRemoteTagsTitle();
    
    
    List<Ref> refs = GitAccess.getInstance().getGit().tagList().call();
    Repository repository = GitAccess.getInstance().getRepository();
    try (RevWalk walk = new RevWalk(repository)) {
      walk.sort(RevSort.COMMIT_TIME_DESC);
      for (Ref ref : refs) {
        ObjectId objectIdOfTag = ref.getObjectId();
        RevObject object = walk.parseAny(objectIdOfTag);
        
        if (object instanceof RevTag) {
          RevTag tag = (RevTag) object;
          boolean isPushed = remoteTagsTitle.contains(tag.getTagName());
          allTags.add(
              new GitTag(tag.getTagName(),
                  tag.getFullMessage(),
                  isPushed,
                  tag.getTaggerIdent().getName(),
                  tag.getTaggerIdent().getEmailAddress(),
                  tag.getTaggerIdent().getWhen(),
                  tag.getObject().getName()));
          
        } else if (object instanceof RevCommit) {
          RevCommit lightTag = (RevCommit) object;
          String lightTagTitle = Repository.shortenRefName(lightTag.getName());
          boolean isPushed = remoteTagsTitle.contains(lightTagTitle);
          allTags.add(
              new GitTag(lightTagTitle,
                  "",
                  isPushed,
                  lightTag.getAuthorIdent().getName(),
                  lightTag.getAuthorIdent().getEmailAddress(),
                  lightTag.getAuthorIdent().getWhen(),
                  lightTag.getName()));
        } 
      }
    }
    allTags.sort(getDescendingComparator());
    
    return allTags;
  }
  
  private static Comparator<GitTag> getDescendingComparator() {
    return (o1, o2) -> {
      int compareResult = o1.getTaggingDate().compareTo(o2.getTaggingDate());
      if(compareResult > 0) {
        return -1;
      } else if (compareResult < 0) {
        return 1;
      }
      return 0;
    };
  }
  
  private static Comparator<GitTag> getAscendingComparator() {
    return (o1, o2) -> {
      int compareResult = o1.getTaggingDate().compareTo(o2.getTaggingDate());
      if(compareResult < 0) {
        return -1;
      } else if (compareResult > 0) {
        return 1;
      }
      return 0;
    };
  }
  
}
