package com.oxygenxml.git.utils.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;

/**
 * A script for creating/changing a Git repository. 
 */
@XmlRootElement(name = "script")
public class RepoGenerationScript {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RepoGenerationScript.class);
  /**
   * A set of changes to be applied on the repository.
   */
  @XmlElement(name = "changeSet")
  ArrayList<ChangeSet> changeSet;

  /**
   * Executes the change in the context of the given working tree directory.
   * 
   * @param wcTreeDir Working tree directory.
   * @param c Change to execute.
   * 
   * @throws IOException If it fails.
   */
  private static void applyChange(File wcTreeDir, Change c) throws IOException {
    File f = new File(wcTreeDir, c.path);
    f.getParentFile().mkdirs();
    
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(c.path + " " + c.type);
    }
    if (c.type.equals("delete")) {
      f.delete();
    } else {
      FileUtils.writeStringToFile(f, c.content, "UTF-8");
    }
  }

  /**
   * Generate a repository using the given script.
   * 
   * @param script Operations to be performed on the repository.
   * @param wcTree Working tree location.
   * @throws GitAPIException 
   * @throws IllegalStateException 
   */
  public static void generateRepository(RepoGenerationScript script, File wcTree) throws IllegalStateException, GitAPIException {
    initGit(wcTree);
    
    script.changeSet.forEach(ch -> applyChanges(wcTree, ch));
  }

  /**
   * Generate a repository using the given script.
   * 
   * @param script Operations to be performed on the repository.
   * @param wcTree Working tree location.
   */
  public static synchronized void generateRepository(URL script, File wcTree) throws Exception {
    try (Reader r = new InputStreamReader(script.openStream(), "UTF-8")) {
    generateRepository(loadScript(IOUtils.toString(r)), wcTree);
    }
  }

  /**
   * Create a new repository.
   * 
   * @param wcTree Working tree directory.
   * 
   * @throws GitAPIException 
   * @throws IllegalStateException 
   */
  private static void initGit(File wcTree) throws IllegalStateException, GitAPIException {
    wcTree.mkdirs();
    GitAccess.getInstance().createNewRepository(wcTree.getAbsolutePath());
    
    setUserCredentials();
  }

  public static void setUserCredentials() {
    try {
      StoredConfig config = GitAccess.getInstance().
          getRepository().
          getConfig();
      
      config.setString("user", null, "name", "AlexJitianu");
      config.setString("user", null, "email", "alex_jitianu@sync.ro");
      
      config.save();
    } catch (NoRepositorySelected | IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Loads the given script.
   * 
   * @param script Serizlied script.
   * 
   * @return The script object.
   * 
   * @throws JAXBException Fails to load the script.
   */
  static RepoGenerationScript loadScript(String script) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(RepoGenerationScript.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
     
    //We had written this file in marshalling example
    RepoGenerationScript emps = (RepoGenerationScript) jaxbUnmarshaller.unmarshal(new StringReader(script));
    return emps;
  }

  /**
   * Applies the set of changes on the repository.
   * 
   * @param wcTree Working tree directory.
   * @param ch Change set.
   */
  private static void applyChanges(File wcTree, ChangeSet ch) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("change set " + ch.message);
    }
    
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(ch.branch);
      }
      setLocalBranch(ch.branch);
      
      mergeBranch(ch.mergeBranch, ch.message);
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
    
    if (ch.changes != null) {
      ch.changes.forEach(c -> {
        try {
          applyChange(wcTree, c);
          GitAccess.getInstance().add(c.toFileStatus());
        } catch (IOException e) {
          LOGGER.error(e.getMessage(), e);
        }
      });
      
      try {
        setUserCredentials();
        
        GitAccess.getInstance().getGit().commit().setAuthor("Alex", "alex_jitianu@sync.ro").setMessage(ch.message).call();
      } catch (GitAPIException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Sets up the branch related aspects: current branch, merge another branch. 
   *  
   * @param localBranchShortNameShort name of the local branch.
   * 
   * @throws GitAPIException
   * @throws IOException 
   */
  private static void setLocalBranch(String localBranchShortName) throws GitAPIException, IOException {
    if (localBranchShortName != null) {
      boolean anyMatch = GitAccess.getInstance().getLocalBranchList().stream().anyMatch(r -> localBranchShortName.equals(Repository.shortenRefName(r.getName())));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Branch detected " + anyMatch);
      }
      if (!anyMatch) {
        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).untilAsserted(() -> {
          // Not sure why we need this. Without it, the order of changes is messed up. 
          GitAccess.getInstance().createBranch(localBranchShortName);
          GitAccess.getInstance().setBranch(localBranchShortName, Optional.empty());
        });
      } else {
        GitAccess.getInstance().setBranch(localBranchShortName, Optional.empty());
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("On branch " + GitAccess.getInstance().getBranchInfo().getBranchName());
      }
    }
  }

  /**
   * Merges a branch into the current branch.
   * 
   * @param mergeBranchShortName Short name of the branch to merge.
   * @param commitMessage Commit message.
   * 
   * @throws GitAPIException
   */
  private static void mergeBranch(String mergeBranchShortName, String commitMessage) throws GitAPIException {
    if (mergeBranchShortName != null) {
      Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).untilAsserted(() -> {
        // Not sure why we need this. Without it, the order of changes is messed up. 
        List<Ref> collect = GitAccess.getInstance().getLocalBranchList().stream().filter(r -> mergeBranchShortName.equals(Repository.shortenRefName(r.getName()))).collect(Collectors.toList());
        MergeResult call = GitAccess.getInstance().getGit().merge().setMessage(commitMessage).include(collect.get(0)).call();

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Merge result: " + call);
        }
      });
    }
  }

  public void generateGitRepository() throws Exception {
    RepoGenerationScript script = loadScript(
        "<script>\n" + 
        "    <changeSet message=\"First commit.\">\n" + 
        "        <change path=\"f1/file1.txt\" type=\"add\">file 1 content</change>\n" + 
        "        <change path=\"f2/file2.txt\" type=\"add\">file 2 content</change>\n" + 
        "        <change path=\"f2/file3.txt\" type=\"add\">file 3 content</change>\n" +
        "        <change path=\"f2/file4.txt\" type=\"add\">file 3 content</change>\n" +
        "        <change path=\"newProject.xpr\" type=\"add\">content</change>\n" +
        "    </changeSet>\n" + 
        "    \n" + 
        "    <changeSet message=\"Changes.\">\n" + 
        "        <change path=\"f1/file1.txt\" type=\"change\">file content</change>\n" + 
        "        <change path=\"f1/file2.txt\" type=\"change\">new content</change>\n" + 
        "        <change path=\"f2/file1.txt\" type=\"delete\"/>\n" + 
        "    </changeSet>\n" + 
        "</script>\n" + 
        "");
  
    File wcTree = new File("target/gen/GenerateScript_generateGitRepository");
    generateRepository(script, wcTree);
  }
}
