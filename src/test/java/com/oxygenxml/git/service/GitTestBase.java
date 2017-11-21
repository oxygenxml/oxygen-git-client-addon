package com.oxygenxml.git.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;

import junit.framework.TestCase;

/**
 * A collection of handy methods. 
 * 
 * @author alex_jitianu
 */
public class GitTestBase extends TestCase {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitTestBase.class);
  /**
   * The loaded reposiltories.
   */
  private List<Repository> loadedRepos = new ArrayList<Repository>();

  /**
   * Installs the GIT protocol that we use to identify certain file versions.
   */
  protected void installGitProtocol() {
    // Install protocol.
    try {
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
      public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals(GitRevisionURLHandler.GIT_PROTOCOL)) {
          URLStreamHandler handler = new GitRevisionURLHandler();
          return handler;
        }
        
        return null;
      }
    });
    } catch (Throwable t) {
      logger.info(t, t);
    } 
  }

  /**
   * Binds the local repository to the remote one.
   * 
   * @param localRepository The local repository.
   * @param remoteRepo The remote repository.
   * 
   * @throws NoRepositorySelected
   * @throws URISyntaxException
   * @throws MalformedURLException
   * @throws IOException
   */
  protected void bindLocalToRemote(Repository localRepository, Repository remoteRepo)
      throws NoRepositorySelected, URISyntaxException, MalformedURLException, IOException {
    StoredConfig config = localRepository.getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    remoteConfig.update(config);
    config.save();
  }

  /**
   * Writes the content into the file.
   * 
   * @param file File to write to.
   * @param content Content to write.
   * 
   * @throws Exception If it fails.
   */
  protected void setFileContent(File file, String content) throws Exception {
    OutputStream os = new FileOutputStream(file);
    try {
      os.write(content.getBytes("UTF-8"));
    } finally {
      os.close();
    }
  }

  /**
   * Reads all the content of the given URL.
   * 
   * @param url The URL to read.
   * 
   * @return The content.
   * 
   * @throws IOException If it fails.
   */
  protected String read(URL url) throws IOException {
    StringBuilder b = new StringBuilder();
    InputStreamReader r = new InputStreamReader(url.openStream(), "UTF-8");
  
    char[] buf = new char[1024];
    int length = -1;
    while ((length = r.read(buf)) != -1) {
      b.append(buf, 0, length);
    }
    
    return b.toString();
    
  }
  

  /**
   * Creates a Git reposiotry at the given location.
   * 
   * @param repositoryPath Location where to create the repository.
   * @return
   * @throws NoRepositorySelected
   */
  protected Repository createRepository(String repositoryPath) throws NoRepositorySelected {
    GitAccess gitAccess = GitAccess.getInstance();
    // Create the remote repository.
    gitAccess.createNewRepository(repositoryPath);
    Repository remoteRepo = gitAccess.getRepository();
    
    loadedRepos.add(remoteRepo);
    
    return remoteRepo;
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    installGitProtocol();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    
    for (Repository repository : loadedRepos) {
      String absolutePath = repository.getWorkTree().getAbsolutePath();
      
      // Close the repository.
      repository.close();
      
      // Remove the file system resources.
      try {
        File dirToDelete = new File(absolutePath);
        FileUtils.deleteDirectory(dirToDelete);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
