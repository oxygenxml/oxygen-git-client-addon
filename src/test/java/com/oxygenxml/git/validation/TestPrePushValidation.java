package com.oxygenxml.git.validation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;

/**
 * Contains tests for pre-push validation.
 * 
 * @author alex_smarandache
 *
 */
public class TestPrePushValidation extends GitTestBase {

  /**
   * The first local repository path.
   */
  private final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local";
  
  /**
   * A remote repository.
   */
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/remote";
  
  /**
   * The second test repository path.
   */
  private final static String SECOND_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local2";
  
  /**
   * The first local repository.
   */
  private Repository firstLocalRepo;
  
  /**
   * The remote repository
   */
  private Repository remoteRepo;
  
  /**
   * The second local repository.
   */
  private Repository secondLocalRepo;
  
  /**
   * The git access.
   */
  private GitAccess gitAccess;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITPRY);
    firstLocalRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITPRY);
    secondLocalRepo = gitAccess.getRepository();

    File file = new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.xpr");
    try {
      file.createNewFile();
      file = new File(SECOND_LOCAL_TEST_REPOSITPRY + "/test2.xpr");
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    final StoredConfig config = gitAccess.getRepository().getConfig();
    final RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    final URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    remoteConfig.update(config);
    config.save();

  }

  
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    gitAccess.closeRepo();
    flushAWT();
    firstLocalRepo.close();
    flushAWT();
    remoteRepo.close();
    flushAWT();
    secondLocalRepo.close();
    flushAWT();
    try {
      File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
      dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
      dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      flushAWT();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
