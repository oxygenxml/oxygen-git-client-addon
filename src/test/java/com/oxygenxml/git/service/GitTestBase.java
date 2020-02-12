package com.oxygenxml.git.service;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.PlatformDetectionUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

import junit.extensions.jfcunit.JFCTestCase;
import junit.extensions.jfcunit.WindowMonitor;
import junit.extensions.jfcunit.finder.ComponentFinder;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.UtilAccess;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

/**
 * A collection of handy methods. 
 * 
 * @author alex_jitianu
 */
@Ignore
public class GitTestBase extends JFCTestCase { // NOSONAR
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitTestBase.class);
  /**
   * i18n
   */
  protected static final Translator translator = Translator.getInstance();
  /**
   * The loaded reposiltories.
   */
  private List<Repository> loadedRepos = new ArrayList<Repository> ();
  
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
    config.setString("core", null, "autocrlf", "false");
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
   * @throws GitAPIException 
   * @throws IllegalStateException 
   */
  protected Repository createRepository(String repositoryPath) throws NoRepositorySelected, IllegalStateException, GitAPIException {
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
  /**
   * Maps Git revision IDs into predictable values that can be asserted in a test.
   */
  private Map<String, String> idMapper = new HashMap<>();
  /**
   * Id generation counter.
   */
  private int counter = 1;
  protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");
  
  /**
   * Files that were requested for comparison.
   */
  protected final List<URL> urls2compare = new LinkedList<URL>();
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        urls2compare.add((URL) invocation.getArguments()[0]);
        urls2compare.add((URL) invocation.getArguments()[1]);
        return null;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).openDiffFilesApplication(Mockito.any(), Mockito.any());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorChangeListener listener = (WSEditorChangeListener) invocation.getArguments()[0];
        editorChangeListeners.add(listener);
        return null;
      }
    }).when(pluginWSMock).addEditorChangeListener(
        (WSEditorChangeListener) Mockito.any(), 
        Mockito.anyInt());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        WSEditorChangeListener listener = (WSEditorChangeListener) invocation.getArguments()[0];
        editorChangeListeners.remove(listener);
        return null;
      }
    }).when(pluginWSMock).removeEditorChangeListener(
        (WSEditorChangeListener) Mockito.any(), 
        Mockito.anyInt());
    
    Mockito.when(pluginWSMock.getEditorAccess((URL) Mockito.any(), Mockito.anyInt())).then(new Answer<WSEditor>() {
      @Override
      public WSEditor answer(InvocationOnMock invocation) throws Throwable {
        WSEditor wsEditorMock = createWSEditorMock();
        
        return wsEditorMock;
      }

    });
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(pluginWSMock.getXMLUtilAccess()).thenReturn(xmlUtilAccess);

    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    });
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(utilAccessMock.locateFile((URL) Mockito.any())).then(new Answer<File>() {
      @Override
      public File answer(InvocationOnMock invocation) throws Throwable {
        URL url = (URL) invocation.getArguments()[0];
        
        String path = url.getPath();
        if (PlatformDetectionUtil.isWin() && path.startsWith("/")) {
          path = path.substring(1, path.length());
        }
        
        return new File(url.getPath());
      }
    });
    
//    PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName()
    Mockito.when(utilAccessMock.getFileName(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        String file = (String) invocation.getArguments()[0];
        file = file.replace('\\', '/');
        int index = file.lastIndexOf("/");
        return index != -1 ? file.substring(index + 1) : file;
      }
    });
    
    Mockito.when(utilAccessMock.uncorrectURL(Mockito.anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArguments()[0].toString().replace("%20", " ");
      }
    });
   
    ImageUtilities imgUtils = Mockito.mock(ImageUtilities.class);
    Mockito.when(pluginWSMock.getImageUtilities()).thenReturn(imgUtils);
    Mockito.when(imgUtils.loadIcon((URL) Mockito.any())).thenAnswer(new Answer<ImageIcon>() {
      @Override
      public ImageIcon answer(InvocationOnMock invocation) throws Throwable {
        URL url = (URL) invocation.getArguments()[0];
        return new ImageIcon(url);
      }
    });
    
    ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);
    Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(projectCtrlMock).refreshFolders(Mockito.any());
    
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
    
    GitOperationScheduler.getInstance().shutdown();
    
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
   * Dumps files changes in a string representation.
   * 
   * @param changes Files changes.
   * 
   * @return An assertable string representation of the files.
   */
  protected String dumpFS(List<FileStatus> changes) {
    StringBuilder b = new StringBuilder();
    changes.stream().forEach(t -> b.append(t.toString()).append("\n"));
    return b.toString();
  }

  /**
   * Dumps a string version of the commits.
   * 
   * @param commitsCharacteristics Commits.
   * 
   * @return A string representation.
   */
  protected String dumpHistory(List<CommitCharacteristics> commitsCharacteristics) {
    return dumpHistory(commitsCharacteristics, false);
  }
  
  /**
   * Dumps a string version of the commits.
   * 
   * @param commitsCharacteristics Commits.
   * @param replaceDateWithMarker <code>true</code> is not interested in date. Put just a marker {date}.
   * 
   * @return A string representation.
   */
  protected String dumpHistory(List<CommitCharacteristics> commitsCharacteristics, boolean replaceDateWithMarker) {
    StringBuilder b = new StringBuilder();
  
    commitsCharacteristics.stream().forEach(t -> b.append(dump(t, replaceDateWithMarker)).append("\n"));
  
    return b.toString();
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
  
  /**
   * Searches for a visible dialog with the specified text in the title.
   * 
   * @param title The title of the dialog.
   * 
   * @return The dialog, or null if there is no dialog having that title.
   */
  protected JDialog findDialog(String title){
    
    JDialog dialogToReturn = null;
    
    for (int i = 0; i < 5; i++) {

      // Wait for WindowMonitor to get the correct opened windows
      flushAWT();
      // Get the opened windows
      Window[] windows = WindowMonitor.getWindows();
      if (windows != null && windows.length > 0) {
        for (Window window : windows) { 
          if (window.isActive() && window instanceof JDialog) {
            JDialog dialog = (JDialog) window;
            String dialogTitle = dialog.getTitle();
            if (dialogTitle != null) {
              // If the dialog title is the same or starts with the given title
              // return this dialog
              if (title.equals(dialogTitle) || dialogTitle.startsWith(title)) {
                dialogToReturn = dialog;
              }
            }
          }
        }
      }                
      if(dialogToReturn != null) {
        break;
      } else {
        logger.warn("Cannot find the dialog using the search string '" + title + "' - throttling..");
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          // Does not happen.
        }
      }
    }
    return dialogToReturn;
  }
  
  /**
   * Maps Git revision IDs into predictable values that can be asserted in a test.
   * 
   * @param id Git commit id.
   * 
   * @return A value that can be asserted in a test.
   */
  protected String getAssertableID(String id) {
    if (id == null || "*".equals(id)) {
      return id;
    }
    String putIfAbsent = idMapper.putIfAbsent(id, String.valueOf(counter));
    if (putIfAbsent == null) {
      counter ++;
    }
    
    return idMapper.get(id);
  }

  /**
   * Serialize the given commit.
   * 
   * @param c Commit data.
   * 
   * @return A string representation that can be asserted.
   */
  public String toString(CommitCharacteristics c) {
    return dump(c, false);
  }
  
  /**
   * Serialize the given commit.
   * 
   * @param c Commit data.
   * @param replaceDateWithMarker <code>true</code> is not interested in date. Put just a marker {date}.
   * 
   * @return A string representation that can be asserted.
   */
  public String dump(CommitCharacteristics c, boolean replaceDateWithMarker) {
    String date = replaceDateWithMarker ? "{date}" : dumpDate(c);
    return "[ " + c.getCommitMessage() + " , " + date + " , " + c.getAuthor() + " , " + getAssertableID(c.getCommitAbbreviatedId()) + " , " 
        + c.getCommitter() + " , " + ( c.getParentCommitId() != null ? c.getParentCommitId().stream().map(id -> getAssertableID(id)).collect(Collectors.toList()) : null) + " ]";
  
  }

  /**
   * Searches for the first button with the specified text in the container.
   * 
   * @param parent  The parent container.
   * @param index   The index of the button in the list of all buttons having that text.
   * @return        The button, or null if there is no button having that text.
   */
  protected JButton findFirstButton(Container parent, String text){
    
    JButton result = null;
    
    // Gets all the buttons.
    ComponentFinder cf = new ComponentFinder(JButton.class);
    List<Component> allButtons = cf.findAll(parent);
    
    // Selects the one with the given text.
    for (Iterator<Component> iterator = allButtons.iterator(); iterator.hasNext();) {
      JButton button = (JButton) iterator.next();
      boolean equals = button.getText() != null && button.getText().equals(text);
      if(equals){
        result = button;
        break;
      }
    }
    
    return result;      
  }

  /**
   * Serializes the commit date into a "d MMM yyyy" format that can be asserted inside tests.
   * 
   * @param c Commit data.
   * 
   * @return A string representation.
   */
  private String dumpDate(CommitCharacteristics c) {
    return c.getDate() != null ? DATE_FORMAT.format(c.getDate()) : DATE_FORMAT.format(new Date());
  }
  
  /**
   * Generates a new repository with the given scripts and loads the repository into GitAccess.
   * 
   * @param script repository generation script.
   * @param wcTree Directory for the working copy.
   * 
   * @throws Exception Problems generating the repository.
   */
  protected void generateRepositoryAndLoad(URL script, File wcTree) throws Exception {
    RepoGenerationScript.generateRepository(script, wcTree);
    
    GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
    
    Repository repository = GitAccess.getInstance().getRepository();
    if (repository != null) {
      loadedRepos.add(repository);
    }
  }
  
}
