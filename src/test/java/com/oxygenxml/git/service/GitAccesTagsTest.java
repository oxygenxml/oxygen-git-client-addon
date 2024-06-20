package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.mockito.Mockito;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.ColorTheme;


/**
 * <p><b>Description:</b> Tests the methods for Git Tags action.</p>
 * <p><b>Bug ID:</b> EXM-46109</p>
 *
 * @author gabriel_nedianu
 *
 */
public class GitAccesTagsTest extends TestCase {

  /**
   * The local repository.
   */
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccesTagsTest";
  
  /**
   * The local repository.
   */
  private final static String REPOSITORY_TEST_CLONE = "target/test-resources/GitAccesTagsTest-clone";
  
  /**
   * The GitAccess instance.
   */
  private GitAccess gitAccess = GitAccess.getInstance();

  
  /**
   * Initialize the git, repository and generate the commits with the script.
   * 
   * @throws Exception 
   */
  @Override
  protected void setUp() throws Exception {
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
    
    ColorTheme colorTheme = Mockito.mock(ColorTheme.class);
    Mockito.when(colorTheme.isDarkTheme()).thenReturn(false);
    Mockito.when(pluginWSMock.getColorTheme()).thenReturn(colorTheme);
    
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    URL script = getClass().getClassLoader().getResource("scripts/git_tags_script.txt");
    File wcTree = new File(LOCAL_TEST_REPOSITORY);
    RepoGenerationScript.generateRepository(script, wcTree);
  }
  
  @Override
  protected void tearDown() throws Exception {
    gitAccess.cleanUp();
    File dirToDelete = new File(LOCAL_TEST_REPOSITORY);
    File dirToDelete2 = new File(REPOSITORY_TEST_CLONE);
    FileUtil.deleteRecursivelly(dirToDelete);
    FileUtil.deleteRecursivelly(dirToDelete2);
    PluginWorkspaceProvider.setPluginWorkspace(null);
  }
  
  /**
   * <p><b>Description:</b> Tests case when a commit has multiple tags associated with it </p>
   * <br><br>
   * <p><b>Bug ID:</b> EXM-49330</p>
   *
   * @author Alex_Smarandache 
   * 
   * @throws Exception
   */
  public void testMultipleTagsOnACommit() throws Exception {
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    
    //Make 3tags on the same commit
    String commitID = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag1", "lala", commitID);
    gitAccess.tagCommit("Tag2", "", commitID);
    gitAccess.tagCommit("Tag3", "bum", commitID);
    String tagsShortcommitID = commitsCharacteristics.get(0).getCommitAbbreviatedId();
    
    Map<String, List<String>> tagsMap = gitAccess.getTagMap(gitAccess.getRepository());
    List<String> tagsList = tagsMap.get(tagsShortcommitID);
    
    assertEquals(3, tagsList.size());
    assertTrue(tagsMap.get(tagsShortcommitID).contains("Tag1") 
        && tagsMap.get(tagsShortcommitID).contains("Tag2")
        && tagsMap.get(tagsShortcommitID).contains("Tag3")
    );
  }
  
  
  /**
   * <p><b>Description:</b> Test create a tag method and existsTag method </p>
   * <p><b>Bug ID:</b> EXM-46109</p>
   *
   * @author gabriel_nedianu 
   * @throws Exception
   */
  public void testCreateAndExistsMethods() throws Exception {
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    
    //Make 2 tags on 2 commits
    gitAccess.tagCommit("Tag1", "lala", commitsCharacteristics.get(0).getCommitId());
    gitAccess.tagCommit("Tag2", "", commitsCharacteristics.get(2).getCommitId());
    String tag1ShortcommitID = commitsCharacteristics.get(0).getCommitAbbreviatedId();
    String tag2ShortcommitID = commitsCharacteristics.get(2).getCommitAbbreviatedId();
    
    //Get the map with all the tags and verify if the tags exists
    Map<String, List<String>> tagsMap = gitAccess.getTagMap(gitAccess.getRepository());
    assertNotNull(tagsMap.get(tag1ShortcommitID));
    assertNotNull(tagsMap.get(tag2ShortcommitID));
    
    assertTrue(tagsMap.get(tag1ShortcommitID).contains("Tag1"));
    assertTrue(tagsMap.get(tag2ShortcommitID).contains("Tag2"));
    assertTrue(gitAccess.existsTag("Tag1"));
    assertTrue(gitAccess.existsTag("Tag2"));
  }


  /**
   * <p><b>Description:</b> Tests delete Tag method </p>
   * <p><b>Bug ID:</b> EXM-46109</p>
   *
   * @author gabriel_nedianu
   * 
   * @throws NoRepositorySelected 
   * @throws IOException 
   * @throws GitAPIException 
   */
  public void testDeleteLocalTagMethod() throws GitAPIException, IOException, NoRepositorySelected {
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    
    gitAccess.tagCommit("Tagul1", "lala", commitsCharacteristics.get(1).getCommitId());
    gitAccess.tagCommit("Tagul2", "", commitsCharacteristics.get(2).getCommitId());
    
    assertTrue(gitAccess.existsTag("Tagul1"));
    assertTrue(gitAccess.existsTag("Tagul2"));
    
    //Delete one tag
    gitAccess.deleteTags(false, "Tagul1");
    
    assertFalse(gitAccess.existsTag("Tagul1"));
  }
  
  /**
   * <p><b>Description:</b> Tests the delete remote tag method.</p>
   * <p><b>Bug ID:</b> EXM-50658</p>
   *
   * @author alex_smarandache
   * 
   * @throws NoRepositorySelected 
   * @throws IOException 
   * @throws GitAPIException 
   */
  public void testDeleteRemoteTagMethod() throws GitAPIException, IOException, NoRepositorySelected {
    
    File file = new File(REPOSITORY_TEST_CLONE);
    URL url = gitAccess.getRepository().getDirectory().toURI().toURL();
    gitAccess.clone(new URIish(url), file, null, "refs/heads/main");

    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String commitId = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag", "", commitId);
    gitAccess.pushTag("Tag");

    assertTrue(gitAccess.existsTag("Tag"));
    final List<String> remoteTags = getRemoteTags();
    assertEquals(1, remoteTags.size());
    assertEquals("Tag", remoteTags.get(0));

    //Delete one tag
    gitAccess.deleteTags(true, "Tag");
    
    assertFalse(gitAccess.existsTag("Tag"));
    assertTrue(getRemoteTags().isEmpty());
  }

  /**
   * @return The current repository remote tags.
   * 
   * @throws GitAPIException
   * @throws InvalidRemoteException
   * @throws TransportException
   */
  private List<String> getRemoteTags() throws GitAPIException, InvalidRemoteException, TransportException {
    final CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(
        GitAccess.getInstance().getHostName());
    final Collection <Ref> refs = GitAccess.getInstance()
        .getGit()
        .lsRemote()
        .setRemote(GitAccess.getInstance().getRemoteFromCurrentBranch())
        .setCredentialsProvider(credentialsProvider)
        .setTags(true)
        .call();
    
    return refs.stream().map(t -> Repository.shortenRefName(
        t.getName())).collect(Collectors.toList());
  }
  
  /**
   * <p><b>Description:</b> Tests push Tag method </p>
   * <p><b>Bug ID:</b> EXM-46109</p>
   *
   * @author gabriel_nedianu
   * 
   * @throws NoRepositorySelected 
   * @throws IOException 
   * @throws GitAPIException 
   * @throws RevisionSyntaxException 
   */
  public void testPush() throws GitAPIException, NoRepositorySelected, RevisionSyntaxException, IOException {
    File file = new File(REPOSITORY_TEST_CLONE);
    URL url = gitAccess.getRepository().getDirectory().toURI().toURL();
    gitAccess.clone(new URIish(url), file, null, "refs/heads/main");

    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String commitId = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag", "", commitId);
    gitAccess.pushTag("Tag");

    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    assertTrue(gitAccess.existsTag("Tag"));

  }
 
}
