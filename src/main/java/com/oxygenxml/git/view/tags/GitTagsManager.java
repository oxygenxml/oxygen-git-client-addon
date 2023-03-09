package com.oxygenxml.git.view.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;

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
    
    
    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
    Collection <Ref> refs = GitAccess.getInstance()
        .getGit()
        .lsRemote()
        .setRemote(GitAccess.getInstance().getRemoteFromCurrentBranch())
        .setCredentialsProvider(credentialsProvider)
        .setTags(true)
        .call();
    
    return refs.stream().map(t -> Repository.shortenRefName(t.getName())).collect(Collectors.toList());
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
                  tag.getObject().getName()
              ));
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
  public static List<GitTag> getLocalTags() throws GitAPIException, NoRepositorySelected, IOException {
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
          String lightTagTitle = Repository.shortenRefName(ref.getName());
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
  
  /**
   * Creates a comparator for descending order of tags
   * 
   * @return A Comparator<GitTag> for descending order
   */
  private static Comparator<GitTag> getDescendingComparator() {
    return (o1, o2) -> o2.getTaggingDate().compareTo(o1.getTaggingDate());
  }
  
  /**
   * The number of Git Tags
   * 
   * @return an integer that represents the number of local Git Tags
   * 
   * @throws GitAPIException
   */
  public static int getNoOfTags() throws GitAPIException {
	  GitAccess gitAccess = GitAccess.getInstance();
    
	  List<Ref> refs = null;
    if(gitAccess.isRepoInitialized()) {
    	refs = gitAccess.getGit().tagList().call();
    }

    return Optional.ofNullable(refs).map(List<Ref>::size).orElse(0);
  }
  
}
