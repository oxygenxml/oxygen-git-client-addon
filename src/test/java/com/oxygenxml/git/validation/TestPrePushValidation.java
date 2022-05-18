package com.oxygenxml.git.validation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

import ro.sync.document.DocumentPositionedInfo;

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

  /**
   * <p><b>Description:</b> This test cover pre-push validation behavior for case when this option
   * is not enabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50426</p>
   *
   * @author Alex_Smarandache
   *
   */ 
  @Test
  public void testPrePushValidationDisabled() throws Exception {
    // Disable pre-push validation option 
    OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

    // Create a custom collector constructed to behave as if it contains validation problems
    final ICollector collector = Mockito.mock(ICollector.class);
    Mockito.when(collector.isEmpty()).then((Answer<Boolean>) 
        invocation -> {
          return false;
        });
    Mockito.when(collector.getAll()).then((Answer<DocumentPositionedInfo[]>) 
        invocation -> {
          return new DocumentPositionedInfo[0];
        }); 

    // A custom validator that is always available and return the custom collector created before
    final IValidator validator = Mockito.mock(IValidator.class);
    Mockito.when(validator.isAvailable()).then((Answer<Boolean>) 
        invocation -> {
          return true;
        });
    Mockito.when(validator.getCollector()).then((Answer<ICollector>) 
        invocation -> {
          return collector;
        });

    ValidationManager.getInstance().setPrePushFilesValidator(validator);
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");

    push("", "");

    assertEquals(firstLocalRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"),
        remoteRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"));

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
