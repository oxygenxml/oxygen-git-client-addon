package com.oxygenxml.git.utils;

import java.io.File;

import org.eclipse.jgit.api.Git;

import com.oxygenxml.git.service.GitTestBase;

/**
 * Utility methods tests.
 * 
 * @author alex_jitianu
 */
public class RepoUtilTest extends GitTestBase {
  /**
   * The place where to create the test structure.
   */
  private File workDir = new File("target/RepoUtilTest");
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    workDir.mkdir();
    
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    
    org.apache.commons.io.FileUtils.deleteDirectory(workDir);
  }

  /**
   * <p><b>Description:</b> Detect Git repository in an Oxygen project structure.
   * The *.xpr and the repository are on the same level.</p>
   * <p><b>Bug ID:</b> EXM-45472</p>
   *
   * <pre>
   * rootDir
   *   .git
   *   *.xpr
   * </pre>
   * 
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testRepositoryDetection_SameLevel() throws Exception {
    File xprFile = new File(workDir, "new.xpr");
    String projectContent = 
        "<project version=\"22.1\">\n" + 
        "    <meta>\n" + 
        "        <filters directoryPatterns=\"\" filePatterns=\"\\QnewProject.xpr\\E\" positiveFilePatterns=\"\" showHiddenFiles=\"false\"/>\n" + 
        "        <options/>\n" + 
        "    </meta>\n" + 
        "    <projectTree name=\"newProject.xpr\">\n" + 
        "        <folder path=\".\"/>\n" + 
        "    </projectTree>\n" + 
        "</project>";
    
    setFileContent(xprFile, projectContent);
    
    // Create a repository.
    Git.init().setDirectory(workDir).call();
    
    File detectRepositoryInProject = RepoUtil.detectRepositoryInProject(xprFile);
    
    assertNotNull("No repository detected", detectRepositoryInProject);
    
    assertEquals(workDir.getAbsolutePath(), detectRepositoryInProject.getAbsolutePath());
  }
  
  /**
   * <p><b>Description:</b> Detect Git repository in an Oxygen project structure.
   * The repository is found by going upwards from the project file.</p>
   * <p><b>Bug ID:</b> EXM-45472</p>
   *
   * <pre>
   * rootDir
   *   child
   *     *.xpr
   *   .git
   * </pre>
   * 
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testRepositoryDetection_Upwards() throws Exception {
    File projectDir = new File(workDir, "child");
    projectDir.mkdir();
    
    File xprFile = new File(projectDir, "new.xpr");
    String projectContent = 
        "<project version=\"22.1\">\n" + 
        "    <meta>\n" + 
        "        <filters directoryPatterns=\"\" filePatterns=\"\\QnewProject.xpr\\E\" positiveFilePatterns=\"\" showHiddenFiles=\"false\"/>\n" + 
        "        <options/>\n" + 
        "    </meta>\n" + 
        "    <projectTree name=\"newProject.xpr\">\n" + 
        "        <folder path=\".\"/>\n" + 
        "    </projectTree>\n" + 
        "</project>";
    
    setFileContent(xprFile, projectContent);
    
    // Create a repository.
    Git.init().setDirectory(workDir).call();
    
    File detectRepositoryInProject = RepoUtil.detectRepositoryInProject(xprFile);
    
    assertNotNull("No repository detected", detectRepositoryInProject);
    
    assertEquals(workDir.getAbsolutePath(), detectRepositoryInProject.getAbsolutePath());
  }

  /**
   * <p><b>Description:</b> Detect Git repository in an Oxygen project structure.
   * The repository is found by going downwards from the project file.</p>
   * <p><b>Bug ID:</b> EXM-45472</p>
   *
   * <pre>
   * rootDir
   *   child
   *     .git
   *   *.xpr
   * </pre>
   * 
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testRepositoryDetection_Downwards() throws Exception {
    File gitDir = new File(workDir, "child");
    gitDir.mkdir();
    
    File xprFile = new File(workDir, "new.xpr");
    String projectContent = 
        "<project version=\"22.1\">\n" + 
        "    <meta>\n" + 
        "        <filters directoryPatterns=\"\" filePatterns=\"\\QnewProject.xpr\\E\" positiveFilePatterns=\"\" showHiddenFiles=\"false\"/>\n" + 
        "        <options/>\n" + 
        "    </meta>\n" + 
        "    <projectTree name=\"newProject.xpr\">\n" + 
        "        <folder path=\".\"/>\n" + 
        "    </projectTree>\n" + 
        "</project>";
    
    setFileContent(xprFile, projectContent);
    
    // Create a repository.
    Git.init().setDirectory(gitDir).call();
    
    File detectRepositoryInProject = RepoUtil.detectRepositoryInProject(xprFile);
    
    assertNotNull("No repository detected", detectRepositoryInProject);
    
    assertEquals(gitDir.getAbsolutePath(), detectRepositoryInProject.getAbsolutePath());
  }

  /**
   * <p><b>Description:</b> Detect Git repository in an Oxygen project structure.
   * The repository is found by going downwards from the project file.</p>
   * <p><b>Bug ID:</b> EXM-45472</p>
   *
   * <pre>
   * rootDir
   *   child
   *     .git
   *   *.xpr
   * </pre>
   * 
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testRepositoryDetection_Downwards_ExplicitPath() throws Exception {
    File gitDir = new File(workDir, "child");
    gitDir.mkdir();
    
    File xprFile = new File(workDir, "new.xpr");
    String projectContent = 
        "<project version=\"22.1\">\n" + 
        "    <meta>\n" + 
        "        <filters directoryPatterns=\"\" filePatterns=\"\\QnewProject.xpr\\E\" positiveFilePatterns=\"\" showHiddenFiles=\"false\"/>\n" + 
        "        <options/>\n" + 
        "    </meta>\n" + 
        "    <projectTree name=\"newProject.xpr\">\n" + 
        "        <folder path=\"child\"/>\n" + 
        "    </projectTree>\n" + 
        "</project>";
    
    setFileContent(xprFile, projectContent);
    
    // Create a repository.
    Git.init().setDirectory(gitDir).call();
    
    File detectRepositoryInProject = RepoUtil.detectRepositoryInProject(xprFile);
    
    assertNotNull("No repository detected", detectRepositoryInProject);
    
    assertEquals(gitDir.getAbsolutePath(), detectRepositoryInProject.getAbsolutePath());
  } 
  
  /**
   * Description: Tests if the extracted repository name is correct.
   * <br>
   * EXM-50538
   * 
   * @author alex_smarandache 
   * 
   * @throws Exception
   */
  public void testExtractRepositoryName() throws Exception {
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project/"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project.git"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project.git/"));
  }
}
