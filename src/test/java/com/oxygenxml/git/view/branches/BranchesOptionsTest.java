package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import com.oxygenxml.git.OxygenGitPluginExtension;
import com.oxygenxml.git.options.OptionTags;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.ExternalPersistentObject;
import ro.sync.exml.workspace.api.options.WSOptionListener;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test if the options are updated properly for branch changing.
 * 
 * @author alex_smarandache
 *
 */
public class BranchesOptionsTest extends GitTestBase {

  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/BranchesOptionsTest/localRepository1";
  private final static String LOCAL_TEST_REPOSITORY2 = "target/test-resources/BranchesOptionsTest/localRepository2";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private final static String LOCAL_BRANCH_NAME2 = "LocalBranch2";
  
  private GitAccess gitAccess;
  
  @Override
  public void setUp() throws Exception {
    
    super.setUp();
    gitAccess = GitAccess.getInstance();
    
    
    //Creates the remote repository.
    createRepository(LOCAL_TEST_REPOSITORY2);
   
    //Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
  }
  
  /**
   * <p><b>Description:</b> Test if the options for current branch and repositories are properly updated.</p>
   * 
   * <p><b>Bug ID:</b> EXM-52783</p>
   *
   * @author Alex_Smarandache
   * 
   * @throws Exception 
   */ 
  public void testCurrentBranchOption() throws Exception {
    try {
      final List<String> optionValues = new ArrayList<>();
     
      final WSOptionsStorage wsOptionsStorage = new WSOptionsStorage() {
        
        private String lastRepository = null;
        
        private String lastBranch = null;
        
        private List<String> repositories = new ArrayList<>();
        
        @Override
        public void setStringArrayOption(String key, String[] values) {
          if(OptionTags.REPOSITORY_LOCATIONS.equals(key)) {
            repositories.clear();
            repositories.addAll(Arrays.asList(values));
          }
        }
        
        @Override
        public void setPersistentObjectOption(String key, ExternalPersistentObject persistentObject) {}
        
        @Override
        public void setOptionsDoctypePrefix(String optionsDoctypePrefix) {}
        
        @Override
        public void setOption(String key, String value) {
          if(OptionTags.CURRENT_BRANCH.equals(key)) {
            lastBranch = value;
            optionValues.add(value);
          } else if(OptionTags.SELECTED_REPOSITORY.equals(key)) {
            lastRepository = value;
            optionValues.add(value);
          }
        }
        
        @Override
        public void removeOptionListener(WSOptionListener listener) { }
        
        @Override
        public String[] getStringArrayOption(String key, String[] defaultValues) {
          return OptionTags.REPOSITORY_LOCATIONS.equals(key) ? repositories.toArray(new String[0]) : new String[0];
        }
        
        @Override
        public ExternalPersistentObject getPersistentObjectOption(String key, ExternalPersistentObject defaultValue) {
          return null;
        }
        
        @Override
        public String getOption(String key, String defaultValue) {
          String toReturn = null;
          if(OptionTags.CURRENT_BRANCH.equals(key)) {
            toReturn = lastBranch != null? lastBranch : defaultValue;
          } else if(OptionTags.SELECTED_REPOSITORY.equals(key)) {
            toReturn = lastRepository != null? lastRepository : defaultValue;
          }
          return toReturn;
        }
        
        @Override
        public void addOptionListener(WSOptionListener listener) { }
      };
      Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptionsStorage);
      new OxygenGitPluginExtension().applicationStarted((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace());
      File file = new File(LOCAL_TEST_REPOSITORY2 + "local.txt");
      file.createNewFile();
      setFileContent(file, "local content");
      //Make the first commit for the local repository and create a branch for it.
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First local commit.");
      gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY2);
      sleep(50);
      refreshSupport.call();
      file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
      file.createNewFile();
      setFileContent(file, "local content");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
      sleep(50);
      refreshSupport.call();
      gitAccess.createBranch(LOCAL_BRANCH_NAME1);
      gitAccess.setBranch(LOCAL_BRANCH_NAME1);
      sleep(50);
      refreshSupport.call();
      gitAccess.createBranch(LOCAL_BRANCH_NAME2);
      gitAccess.setBranch(LOCAL_BRANCH_NAME2);
      sleep(50);
      refreshSupport.call();
      refreshSupport.call();
      refreshSupport.call();
      gitAccess.setBranch(LOCAL_BRANCH_NAME1);
      sleep(50);
      refreshSupport.call();
      gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY2);
      sleep(50);
      refreshSupport.call();

      assertEquals(9, optionValues.size());
      assertEquals(new File(LOCAL_TEST_REPOSITORY2).getAbsolutePath(), optionValues.get(0)); // first load of local repo 2
      assertEquals("main", optionValues.get(1)); // branch from first load of local repo 2
      assertEquals(new File(LOCAL_TEST_REPOSITORY).getAbsolutePath(), optionValues.get(2)); // first load of local repo 1
      assertEquals("main", optionValues.get(3)); // branch from first load of local repo 1
      assertEquals(LOCAL_BRANCH_NAME1, optionValues.get(4)); // first set local branch name 1 of local repo 1
      assertEquals(LOCAL_BRANCH_NAME2, optionValues.get(5)); // first set local branch name 2 of local repo 1
      assertEquals(LOCAL_BRANCH_NAME1, optionValues.get(6)); // second set local branch name 1 of local repo 1
      assertEquals(new File(LOCAL_TEST_REPOSITORY2).getAbsolutePath(), optionValues.get(7)); // second load of local repo 2
      assertEquals("main", optionValues.get(8));  // branch from second load of local repo 2
    } finally {
      FileUtil.deleteRecursivelly(new File("target/test-resources/BranchesOptionsTest"));
    }
  }
  
  
}
