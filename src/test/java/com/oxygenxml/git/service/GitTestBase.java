package com.oxygenxml.git.service;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.TestHelper;
import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.PlatformDetectionUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.branches.BranchCheckoutMediator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.refresh.PanelsRefreshSupport;

import junit.extensions.jfcunit.JFCTestCase;
import junit.extensions.jfcunit.finder.ComponentFinder;
import ro.sync.basic.io.FileSystemUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.results.ResultsManager;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.ColorTheme;
import ro.sync.exml.workspace.api.util.UtilAccess;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

/**
 * A collection of handy methods. 
 * 
 * @author alex_jitianu
 */
public abstract class GitTestBase extends JFCTestCase { // NOSONAR
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GitTestBase.class);
  /**
   * i18n
   */
  protected static final Translator translator = Translator.getInstance();
  /**
   * The loaded repositories.
   */
  private Set<Repository> loadedRepos = new HashSet<> ();
  
  /**
   * The loaded repositories.
   */
  private Set<Repository> remoteRepos = new HashSet<>();

  /**
   * Installs the GIT protocol that we use to identify certain file versions.
   */
  protected void installGitProtocol() {
    // Install protocol.
    try {
      URL.setURLStreamHandlerFactory(
          protocol -> {
            return protocol.equals(GitRevisionURLHandler.GIT_PROTOCOL) ? new GitRevisionURLHandler() : null;
          });
    } catch (Throwable t) {
      if (!t.getMessage().contains("factory already defined")) {
        LOGGER.info(t.getMessage(), t);
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
    bindLocalToRemote(localRepository, remoteRepo, "origin", "main");
  }
  
  
  /**
   * Binds the local repository to the remote one.
   * 
   * @param localRepository The local repository.
   * @param remoteRepo      The remote repository.
   * @param remoteName      The remote name.
   * @param branchName      The branch name.
   * 
   * @throws NoRepositorySelected
   * @throws URISyntaxException
   * @throws MalformedURLException
   * @throws IOException
   */
  protected void bindLocalToRemote(Repository localRepository, Repository remoteRepo, 
      String remoteName, String branchName)
      throws NoRepositorySelected, URISyntaxException, MalformedURLException, IOException {
    
    StoredConfig config = localRepository.getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
    remoteConfig.addURI(new URIish(remoteRepo.getDirectory().toURI().toURL()));
    remoteConfig.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/" + remoteName + "/*"));
   

    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);

    remoteConfig.update(config);
    
    config.setString("core", null, "autocrlf", "false");
    config.save();
    
    refreshSupport.call();
    waitForScheduler();
    remoteRepos.add(remoteRepo);
    waitForScheduler();   
  }

  
  
  /**
   * Add a remote in local repository config file.
   *  
   * @param localRepository  The local repository.
   * @param remoteRepo       The remote repository.
   * @param remoteName       The remote name.
   * 
   * @throws URISyntaxException 
   * @throws IOException 
   */
  protected void addRemote(@NonNull final Repository localRepository, 
      @NonNull  final Repository remoteRepo, 
      @NonNull  final String remoteName) throws URISyntaxException, IOException {
    
    StoredConfig config = localRepository.getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
    remoteConfig.addURI(new URIish(remoteRepo.getDirectory().toURI().toURL()));
    remoteConfig.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/" + remoteName + "/*"));

    remoteConfig.update(config);
   
    config.save();
    
    refreshSupport.call();
    waitForScheduler();
    remoteRepos.add(remoteRepo);
    waitForScheduler();
    
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
    
    waitForScheduler();
  }


  /**
   * Creates a Git reposiotry at the given location.
   * 
   * @param repositoryPath Location where to create the repository.
   * 
   * @return
   * 
   * @throws NoRepositorySelected
   * @throws GitAPIException 
   * @throws IllegalStateException 
   * @throws IOException 
   */
  protected Repository createRepository(String repositoryPath) throws NoRepositorySelected, IllegalStateException, GitAPIException, IOException {
    File dirToDelete = new File(repositoryPath, ".git");
    FileUtil.deleteRecursivelly(dirToDelete);
    
    GitAccess gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(repositoryPath);
    Repository repo = gitAccess.getRepository();
    loadedRepos.add(repo);
    
    return repo;
  }
  
  /**
   * Searches for a checkbox with the specified text in the container.
   * 
   * @param parent The parent container.
   * @return The checkbox, or null if there is no checkbox having that text.
   */
  protected JCheckBox findCheckBox(Container parent, String text ){
    JCheckBox result = null;
    
    // Gets all the checkboxes.
    ComponentFinder cf = new ComponentFinder(JCheckBox.class);
    List<Component> allButtons = cf.findAll(parent);
    
    // Selects the one with the given text.
    for (Iterator<Component> iterator = allButtons.iterator(); iterator.hasNext();) {
      JCheckBox checkBox = (JCheckBox) iterator.next();
      if(checkBox.getText().equalsIgnoreCase(text)){
        result = checkBox;
        break;
      }        
    }
    
    return result;      
  }
  
  /**
   * Searches for the nearest component from the specified class, relative to the label having that text.
   * 
   * @param parent The parent container.
   * @param text The text in the label.
   * @param clazz The class of the component.
   * @return The component if found, or null.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected <T extends Component> T findComponentNearJLabel(Container parent, String text, Class<T> clazz) {
    T ret = null;
    
    ComponentFinder cf = new ComponentFinder(JLabel.class);
    List<Component> allLabels = cf.findAll(parent);
    for (Iterator iterator = allLabels.iterator(); iterator.hasNext();) {
      JLabel label= (JLabel) iterator.next();
      if(text.equals(label.getText())){
        // Found the label.
        int xl = label.getLocationOnScreen().x;
        int yl = label.getLocationOnScreen().y;
        
        LOGGER.debug("Found: " + label.getText() + " ( " +xl + "," + yl + " ) ");
        
        LOGGER.debug("Searching for " + clazz);
        List<T> allComponents = new ComponentFinder(clazz).findAll(parent);
        
        int min = Integer.MAX_VALUE;
        T closest = allComponents.get(0);
        
        for (Iterator comIter = allComponents.iterator(); comIter.hasNext();) {
          T c = (T) comIter.next(); 
          int xc = c.getLocationOnScreen().x;
          int yc = c.getLocationOnScreen().y;
          LOGGER.debug("Checking:  ( " +xl + "," + yl + " ) " + c);
          // Favour components from the left and right.
          int distance = (int) Math.sqrt((xc - xl)*(xc - xl) + (yc - yl)*(yc - yl) * 5);
          if(distance < min){
            closest = c;
            min = distance;
          }
        }
        
        ret = closest;
        LOGGER.debug("The closest is: " + ret);          
      }
    }
    return ret;
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
  private int revisionIDCounter = 1;
  /**
   * Date format.
   */
  protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");
  
  /**
   * Files that were requested for comparison.
   */
  protected final List<URL> urls2compare = new LinkedList<>();
  
  private File tempTargetHome;
  
  /**
   * Refresh support.
   */
  protected PanelsRefreshSupport refreshSupport;
  
  /**
   * Plugin Workspace.
   */
  protected StandalonePluginWorkspace pluginWSMock;
  
  /**
   * Intercepted open URL events called on the API.
   */
  protected List<URL> toOpen = new ArrayList<>();
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
   
    // Create the unstaged resources panel
    refreshSupport = new PanelsRefreshSupport(null) {
      @Override
      protected int getScheduleDelay() {
        // Execute refresh events immediately from tests.
        return 1;
      }
    };
    
    gitInit();
    OptionsManager.getInstance().setValidateMainFilesBeforePush(false);
    OptionsManager.getInstance().setValidateFilesBeforeCommit(false);
    ResultsManager resultManager = Mockito.mock(ResultsManager.class);
    ColorTheme colorTheme = Mockito.mock(ColorTheme.class);
    Mockito.when(colorTheme.isDarkTheme()).thenReturn(false);
    Mockito.when(pluginWSMock.getColorTheme()).thenReturn(colorTheme);
    Mockito.when(pluginWSMock.getResultsManager()).thenReturn(resultManager);
    
    Mockito.doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        if (arguments.length == 2) {
          urls2compare.add((URL) arguments[0]);
          urls2compare.add((URL) arguments[1]);
        }
        return null;
      }
    }).when(pluginWSMock).openDiffFilesApplication(Mockito.any(), Mockito.any());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        if (arguments != null && arguments.length > 0) {
          toOpen.add((URL) invocation.getArguments()[0]);
        }
        return null;
      }
    }).when(pluginWSMock).open(Mockito.any());
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        if (invocation.getArguments().length > 0 ) {
          WSEditorChangeListener listener = (WSEditorChangeListener) invocation.getArguments()[0];
          editorChangeListeners.add(listener);
        }
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
        WSEditor wsEditorMock = createWSEditorMock((URL) invocation.getArguments()[0]);
        
        return wsEditorMock;
      }

    });
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object object = invocation.getArguments()[0];
        return object != null ? (String) object : "";
      }
    });
    Mockito.when(xmlUtilAccess.unescapeAttributeValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object object = invocation.getArguments()[0];
        return object != null ? (String) object : "";
      }
    });
    Mockito.doReturn(xmlUtilAccess).when(pluginWSMock).getXMLUtilAccess();
    Mockito.doReturn(new JFrame()).when(pluginWSMock).getParentFrame();
    
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
    
    GitAccess gitAccess = GitAccess.getInstance();
    GitController ctrl = new GitController(gitAccess);
    ctrl.addGitListener(new GitEventAdapter() {
      private Repository oldRepository;

      @Override
      public void operationAboutToStart(GitEventInfo info) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          try {
            oldRepository = gitAccess.getRepository();
          } catch (NoRepositorySelected e) {
            // Ignore
          }
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          if (oldRepository != null) {
            loadedRepos.remove(oldRepository);
          }
          oldRepository = null;
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          oldRepository = null;
        }
      }
    });
    
    OptionsManager.getInstance().loadOptions(wsOptions);
    
    gitAccess.getStatusCache().installEditorsHook(pluginWSMock);
    gitAccess.setOperationProgressSupport(Mockito.mock(OperationProgressFactory.class));
    ctrl.setBranchesCheckoutMediator(new BranchCheckoutMediator(ctrl));
  }
  
  /**
   * 
   */
  private void gitInit() throws Exception {
    tempTargetHome = new File("target/home");
    
    MockSystemReader mockSystemReader = new MockSystemReader() {
      @Override
      public long getCurrentTime() {
        // TODO Temporary fix to make the existing tests pass. Existing tests rely on the current date. It would be best
        // to update them to use a fix date.
        return System.currentTimeMillis();
      }
    };
    SystemReader.setInstance(mockSystemReader);
    
    mockSystemReader.setProperty(Constants.GIT_COMMITTER_NAME_KEY, "AlexJitianu");
    mockSystemReader.setProperty(Constants.GIT_COMMITTER_EMAIL_KEY, "alex_jitianu@sync.ro");

    // Measure timer resolution before the test to avoid time critical tests
    // are affected by time needed for measurement.
    // The MockSystemReader must be configured first since we need to use
    // the same one here

    FileBasedConfig jgitConfig = new FileBasedConfig(
        new File(tempTargetHome, "jgitconfig"), FS.DETECTED);
    FileBasedConfig systemConfig = new FileBasedConfig(jgitConfig,
        new File(tempTargetHome, "systemgitconfig"), FS.DETECTED);
    FileBasedConfig userConfig = new FileBasedConfig(systemConfig,
        new File(tempTargetHome, "usergitconfig"), FS.DETECTED);
    
    // We have to set autoDetach to false for tests, because tests expect to be able
    // to clean up by recursively removing the repository, and background GC might be
    // in the middle of writing or deleting files, which would disrupt this.
    userConfig.setBoolean(ConfigConstants.CONFIG_GC_SECTION,
        null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
    userConfig.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME, "AlexJitianu");
    
    
    userConfig.save();
    mockSystemReader.setJGitConfig(jgitConfig);
    mockSystemReader.setSystemGitConfig(systemConfig);
    mockSystemReader.setUserGitConfig(userConfig);
    
    final WindowCacheConfig c = new WindowCacheConfig();
    c.setPackedGitLimit(128 * WindowCacheConfig.KB);
    c.setPackedGitWindowSize(8 * WindowCacheConfig.KB);
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    c.setPackedGitMMAP(false);
    c.setDeltaBaseCacheLimit(8 * WindowCacheConfig.KB);
    c.install();
  }

  /**
   * Mock editor.
   * 
   * @param editorLocation editor location.
   * 
   * @return editor mock.
   */
  private WSEditor createWSEditorMock(URL editorLocation) {
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
    
    Mockito.doReturn(editorLocation).when(wsEditorMock).getEditorLocation();

    
    return wsEditorMock;
  }
  
  /**
   * Tear down.
   */
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
 
    MessagePresenterProvider.setBuilder(null);
    
    // If there is a running task, wait for it.
    waitForScheduler();
    RepositoryCache.clear();
    
    // wait for cache to clear
    waitForScheduler();
    
    try {
      Repository currentRepo = GitAccess.getInstance().getRepository();
      GitAccess.getInstance().cleanUp();
      waitForScheduler();
      
      for (Repository repository : loadedRepos) {
        // Remove the file system resources.
        try {
          // We already closed the repository currently opened in GitAccess. Closing it
          // again writes an error in the console and can slow down the tests.
          if (currentRepo != repository) {
            repository.close();
          }
          waitForScheduler();
          flushAWT();
          deleteRepository(repository);
          waitForScheduler();
          flushAWT();
        } catch (IOException e) {
          System.err.println("Unable to delete: " + repository.getWorkTree().getAbsolutePath());
          e.printStackTrace();
        }
      }
      loadedRepos.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    System.gc();
    flushAWT();
    flushAWT();
    
    SystemReader.setInstance(null);
    
    FileSystemUtil.deleteRecursivelly(tempTargetHome);
    
    FileSystemUtil.deleteRecursivelly(new File("target/test-resources"));
    
    // Sometimes a NPE is throw here.
    Optional.ofNullable(GitAccess.getInstance()).ifPresent(gitAcc -> {
      Optional.ofNullable(gitAcc.getStatusCache()).ifPresent(StatusCache::resetCache);
    });
    
    // wait more
    waitForScheduler();
    
    PluginWorkspaceProvider.setPluginWorkspace(null);
  }

  /**
   * Remove the entire working directory of this repository.
   * 
   * @param repository Git Repository.
   * 
   * @throws IOException If it fails.
   */
  private void deleteRepository(Repository repository) throws IOException {
    if (repository != null) {
      String absolutePath = repository.getWorkTree().getAbsolutePath();
      File dirToDelete = new File(absolutePath);
      FileUtil.deleteRecursivelly(dirToDelete);
    }
  }
  
  /**
   * Loads the repository and pushes one file to the remote.
   * 
   * @throws Exception If it fails.
   */
  protected final void pushOneFileToRemote(String repository, String fileName, String fileContent) throws Exception {
    commitOneFile(repository, fileName, fileContent);
    GitAccess.getInstance().push(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()));
  }
  
  /**
   * Push changes.
   * 
   * @param username User name.
   * @param password Password.
   * 
   * @return push response.
   * 
   * @throws GitAPIException 
   */
  protected final PushResponse push(String username, String password) throws GitAPIException {
    return GitAccess.getInstance().push(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()));
  }
  
  /**
   * Pull.
   * 
   * @param username          Username.
   * @param password          Password.
   * @param pullType          Pull type.
   * @param updateSubmodules  <code>true</code> to update submodules.
   * 
   * @return Pull response.
   * 
   * @throws GitAPIException
   */
  protected PullResponse pull(String username, String password, PullType pullType, boolean updateSubmodules) throws GitAPIException {
    return GitAccess.getInstance().pull(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()),
        pullType,
        null,
        updateSubmodules);
  }
  
  /**
   * Dumps files changes in a string representation.
   * 
   * @param changes Files changes.
   * 
   * @return An assertable string representation of the files.
   */
  protected String dumpFileStatuses(List<FileStatus> changes) {
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
  
    commitsCharacteristics.stream().forEach(t -> b.append(serializeCommit(t, replaceDateWithMarker)).append("\n"));
  
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
    return TestHelper.findDialog(title);
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
    
    String putIfAbsent = idMapper.putIfAbsent(id, String.valueOf(revisionIDCounter));
    if (putIfAbsent == null) {
      revisionIDCounter ++;
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
  public String serializeCommit(CommitCharacteristics c) {
    return serializeCommit(c, false);
  }
  
  /**
   * Serialize the given commit.
   * 
   * @param c Commit data.
   * @param replaceDateWithMarker <code>true</code> is not interested in date. Put just a marker {date}.
   * 
   * @return A string representation that can be asserted.
   */
  public String serializeCommit(CommitCharacteristics c, boolean replaceDateWithMarker) {
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
    return TestHelper.findFirstButton(parent, text);
  }
  
  
  /**
   * Searches for the first text area.
   * 
   * @param parent  The parent container.
   * @param index   The index of the button in the list of all buttons having that text.
   * @return        The text area, or null if there is no button having that text.
   */
  protected JTextArea findFirstTextArea(Container parent) {
    JTextArea result = null;

    // Gets all the buttons.
    ComponentFinder cf = new ComponentFinder(JTextArea.class);
    List<Component> allTextAreas = cf.findAll(parent);

    // Selects the one with the given text.
    for (Iterator<Component> iterator = allTextAreas.iterator(); iterator.hasNext();) {
      JTextArea textarea = (JTextArea) iterator.next();
      boolean equals = textarea.getText() != null ;
      if (equals) {
        result = textarea;
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
    GitAccess.getInstance().closeRepo();
    
    // Clean up.
    FileSystemUtil.deleteRecursivelly(wcTree);
    
    RepoGenerationScript.generateRepository(script, wcTree);
    
    GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
    
    Repository repository = GitAccess.getInstance().getRepository();
    if (repository != null) {
      loadedRepos.add(repository);
    }
  }

  /**
   * Wait for refresh task and any other tasks that the added on the scheduler.
   */
  protected void waitForScheduler() {
    flushAWT();
    ScheduledFuture<?> task = refreshSupport.getScheduledTaskForTests();
    if (task != null && !task.isDone()) {
      try {
        task.get(4000, TimeUnit.MILLISECONDS);
      } catch (ExecutionException | TimeoutException | InterruptedException e) {
        LOGGER.error("The current refresh task didn't finish.");
      }
    }
    
    try {
    Semaphore s = new Semaphore(0);
    GitOperationScheduler.getInstance().schedule(() -> {s.release();}, 50);
      s.tryAcquire(1, 4000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      
      LOGGER.error(e.getMessage(), e);
    }
  }
  
  /**
   * Sleep
   * 
   * @param time
   */
  protected void sleep(int time) {
    TestHelper.sleep(time);
  }
  
  /**
   * Get last commit.
   * 
   * @return the last commit.
   * 
   * @throws Exception
   */
  protected RevCommit getLastCommit() throws Exception {
    RevCommit youngestCommit = null;
    
    List<Ref> branches = GitAccess.getInstance().getLocalBranchList();
    RevWalk walk = new RevWalk(GitAccess.getInstance().getRepository());
    for(Ref branch : branches) {
      RevCommit commit = walk.parseCommit(branch.getObjectId());
      if(youngestCommit == null || commit.getAuthorIdent().getWhen().compareTo(
          youngestCommit.getAuthorIdent().getWhen()) > 0)
        youngestCommit = commit;
    }
    walk.close();
    
    return youngestCommit;
  }
}
