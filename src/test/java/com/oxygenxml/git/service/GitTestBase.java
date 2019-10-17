package com.oxygenxml.git.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * A collection of handy methods. 
 * 
 * @author alex_jitianu
 */
public class GitTestBase extends JFCTestCase {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitTestBase.class);
  /**
   * The loaded reposiltories.
   */
  private List<Repository> loadedRepos = new ArrayList<>();
  
  /**
   * The loaded reposiltories.
   */
  private List<Repository> remoteRepos = new ArrayList<>();

  /**
   * Installs the GIT protocol that we use to identify certain file versions.
   */
  protected void installGitProtocol() {
    // Install protocol.
    try {
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
      @Override
      public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals(GitRevisionURLHandler.GIT_PROTOCOL)) {
          URLStreamHandler handler = new GitRevisionURLHandler();
          return handler;
        }
        
        return null;
      }
    });
    } catch (Throwable t) {
      if (!t.getMessage().contains("factory already defined")) {
        logger.info(t, t);
      }
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
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    
    String branchName = "master";
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);

    
    remoteConfig.update(config);
    config.save();
    
    remoteRepos.add(remoteRepo);
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
    for (Iterator<WSEditorChangeListener> iterator = editorChangeListeners.iterator(); iterator.hasNext();) {
      WSEditorChangeListener wsEditorChangeListener = iterator.next();
      wsEditorChangeListener.editorOpened(file.toURI().toURL());
    }
    
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      os.write(content.getBytes("UTF-8"));
    } finally {
      if (os != null) {
        try {
          os.close();
          
          for (Iterator<WSEditorListener> iterator = editorListeners.iterator(); iterator.hasNext();) {
            WSEditorListener wsEditorChangeListener = iterator.next();
            wsEditorChangeListener.editorSaved(WSEditorListener.SAVE_OPERATION);
          }
          
        } catch (IOException ex) {}
      }
      
      for (Iterator<WSEditorChangeListener> iterator = editorChangeListeners.iterator(); iterator.hasNext();) {
        WSEditorChangeListener wsEditorChangeListener = iterator.next();
        wsEditorChangeListener.editorClosed(file.toURI().toURL());
      }
    }
  }

  /**
   * Gets the content from a given URL.
   * 
   * @param url The URL from where to read.
   * 
   * @return The content, never <code>null</code>.
   */
  @SuppressWarnings("unused")
  protected String read(URL url) throws IOException {
    String result = null;
    try (
        // Java will try to automatically close each of the declared resources
        InputStream openedStream = url.openStream();
        InputStreamReader inputStreamReader = new InputStreamReader(openedStream, "UTF-8")) {
      result = read(inputStreamReader);
    } catch (IOException e) {
      if (result == null) {
        throw e;
      } else {
        // Just some info about this error, the method will return the result.
        e.printStackTrace();
      }
    }
    return result;
  }
  
  /**
   * Reads all the content from a given reader.
   * 
   * @param isr The reader.
   *
   * @return The content.
   *
   * @throws IOException If cannot read.
   */
  private static String read(InputStreamReader isr) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    char[] buf = new char[1024];
    int length = -1;
    while ((length = isr.read(buf)) != -1) {
      stringBuilder.append(buf, 0, length);
    }
    return stringBuilder.toString();
  }
  

  /**
   * Creates a Git reposiotry at the given location.
   * 
   * @param repositoryPath Location where to create the repository.
   * @return
   * @throws NoRepositorySelected
   */
  protected Repository createRepository(String repositoryPath) throws NoRepositorySelected {
    try {
      File dirToDelete = new File(repositoryPath, ".git");
      FileUtils.deleteDirectory(dirToDelete);
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    GitAccess gitAccess = GitAccess.getInstance();
    // Create the remote repository.
    gitAccess.createNewRepository(repositoryPath);
    Repository remoteRepo = gitAccess.getRepository();
    
    loadedRepos.add(remoteRepo);
    
    return remoteRepo;
  }
  
  /**
   * Records the given repository for clean up when the test is finished.
   * 
   * @param repo Repository to clean up.
   */
  protected final void record4Cleanup(Repository repo) {
    loadedRepos.add(repo);
  }
  
  /**
   * Listeners interested in editor change events.
   */
  protected final List<WSEditorChangeListener> editorChangeListeners = new ArrayList<>();
  /**
   * Listeners interested in editor events.
   */
  protected final List<WSEditorListener> editorListeners = new ArrayList<>();
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    StandalonePluginWorkspace mock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(mock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorChangeListener listener = (WSEditorChangeListener) invocation.getArguments()[0];
        editorChangeListeners.add(listener);
        return null;
      }
    }).when(mock).addEditorChangeListener(
        (WSEditorChangeListener) Mockito.any(), 
        Mockito.anyInt());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorChangeListener listener = (WSEditorChangeListener) invocation.getArguments()[0];
        editorChangeListeners.remove(listener);
        return null;
      }
    }).when(mock).removeEditorChangeListener(
        (WSEditorChangeListener) Mockito.any(), 
        Mockito.anyInt());
    
    Mockito.when(mock.getEditorAccess((URL) Mockito.any(), Mockito.anyInt())).then(new Answer<WSEditor>() {
      @Override
      public WSEditor answer(InvocationOnMock invocation) throws Throwable {
        WSEditor wsEditorMock = createWSEditorMock();
        
        return wsEditorMock;
      }

    });
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(mock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(utilAccessMock.locateFile((URL) Mockito.any())).then(new Answer<File>() {
      @Override
      public File answer(InvocationOnMock invocation) throws Throwable {
        URL url = (URL) invocation.getArguments()[0];
        
        String path = url.getPath();
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win") && path.startsWith("/")) {
          path = path.substring(1, path.length());
        }
        
        return new File(url.getPath());
      }
    });
   
    ImageUtilities imgUtils = Mockito.mock(ImageUtilities.class);
    Mockito.when(mock.getImageUtilities()).thenReturn(imgUtils);
    Mockito.when(imgUtils.loadIcon((URL) Mockito.any())).thenAnswer(new Answer<ImageIcon>() {
      @Override
      public ImageIcon answer(InvocationOnMock invocation) throws Throwable {
        URL url = (URL) invocation.getArguments()[0];
        return new ImageIcon(url);
      }
    });
    
    installGitProtocol();
  }
  
  private WSEditor createWSEditorMock() {
    WSEditor wsEditorMock = Mockito.mock(WSEditor.class);
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorListener l = (WSEditorListener) invocation.getArguments()[0];
        editorListeners.add(l);
        return null;
      }
    }).when(wsEditorMock).addEditorListener((WSEditorListener) Mockito.any());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorListener l = (WSEditorListener) invocation.getArguments()[0];
        editorListeners.remove(l);
        return null;
      }
    }).when(wsEditorMock).removeEditorListener((WSEditorListener) Mockito.any());

    
    return wsEditorMock;
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    WindowCache.getInstance().cleanup();
    
    // Wait for JGit threads to finish up work.
    flushAWT();
    
    // Only one repository is open at a given time.
    GitAccess.getInstance().closeRepo();
    
    for (Repository repository : loadedRepos) {
      // Remove the file system resources.
      try {
        repository.close();
        String absolutePath = repository.getWorkTree().getAbsolutePath();
        File dirToDelete = new File(absolutePath);
        FileUtils.deleteDirectory(dirToDelete);
      } catch (IOException e) {
        System.err.println("Unable to delete: " + repository.getWorkTree().getAbsolutePath());
        e.printStackTrace();
      }
    }
    
    GitAccess.getInstance().cleanUp();
  }
  
  /**
   * Loads the repository and pushes one file to the remote.
   * 
   * @throws Exception If it fails.
   */
  protected final void pushOneFileToRemote(String repository, String fileName, String fileContent) throws Exception {
    commitOneFile(repository, fileName, fileContent);
    GitAccess.getInstance().push("", "");
  }
  
  /**
   * Loads the repository and pushes one file to the remote.
   * 
   * @throws Exception If it fails.
   */
  protected final void commitOneFile(String repository, String fileName, String fileContent) throws Exception {
    GitAccess gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(repository);

    PrintWriter out = new PrintWriter(repository + "/" + fileName);
    out.println(fileContent);
    out.close();
    gitAccess.add(new FileStatus(GitChangeType.ADD, fileName));
    gitAccess.commit("New file: " + fileName);
  }
}
