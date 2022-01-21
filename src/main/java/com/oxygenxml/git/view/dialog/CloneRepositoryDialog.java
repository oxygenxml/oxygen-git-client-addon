package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.CredentialsBase;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.UndoRedoSupportInstaller;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

public class CloneRepositoryDialog extends OKCancelDialog { // NOSONAR squid:MaximumInheritanceDepth

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CloneRepositoryDialog.class);
	
	/**
	 * The default branch marker.
	 */
	private static final Ref DEFAULT_BRANCH_MARKER = new ObjectIdRef(Storage.NEW, "DEFAULT_BRANCH", null, -1) {
    @Override
    public boolean isPeeled() {
      return false;
    }
    @Override
    public ObjectId getPeeledObjectId() {
      return null;
    }
  };

	/**
	 * Clone worker.
	 */
	private class CloneWorker extends SwingWorker<Void, Void> {
	  
	  /**
	   * The progress dialog for the cloning operation.
	   */
    private final ProgressDialog progressDialog;
    /**
     * Repository (source) URL.
     */
		private final URIish sourceUrl;
		/**
		 * Destination file.
		 */
		private final File destFile;
		/**
		 * The branch to checkout.
		 */
    private Ref branch;

		/**
		 * Constructor.
		 * 
		 * @param progressDialog The progress dialog for the cloning operation.
		 * @param sourceUrl      Repository (source) URL.
		 * @param destFile       Destination file.
		 * @param branch         The branch to checkout.
		 */
		private CloneWorker(ProgressDialog progressDialog, URIish sourceUrl, File destFile, Ref branch) {
			this.progressDialog = progressDialog;
			this.sourceUrl = sourceUrl;
			this.destFile = destFile;
      this.branch = branch;
		}

		@Override
		protected Void doInBackground() throws Exception {
			CloneRepositoryDialog.this.setVisible(false);
			GitAccess.getInstance().clone(
			    sourceUrl,
			    destFile,
			    progressDialog,
			    branch != null && branch != DEFAULT_BRANCH_MARKER ? branch.getName() : null);
			progressDialog.dispose();
			return null;
		}

		@Override
		protected void done() {
			try {
			  // TODO Strange...when done is called, the entire processing should be done.
				get();
				OptionsManager.getInstance().saveDestinationPath(destFile.getAbsolutePath());
			} catch (InterruptedException e) {
				if (LOGGER.isDebugEnabled()) {
				  LOGGER.debug(e.getMessage(), e);
				}
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
			  LOGGER.debug(e.getMessage(), e);
			  
				progressDialog.dispose();

	      treatExecutionExceptionCause(e);
			}
		}

		/**
		 * Treat exception's cause.
		 * 
		 * @param e The exception.
		 */
    private void treatExecutionExceptionCause(ExecutionException e) {
      Throwable cause = e.getCause();
      while (cause != null) {
        boolean shouldBreak = false;
        if (cause.getMessage() != null && cause.getMessage().contains("Download cancelled")) {
          // Download cancelled
          shouldBreak = true;
        } else if (cause instanceof InvalidRemoteException) {
          // Invalid remote
          SwingUtilities.invokeLater(() -> pluginWorkspace.showErrorMessage(
              translator.getTranslation(Tags.INVALID_REMOTE)
              + ": " 
              + sourceUrl));
          shouldBreak = true;
        } else if (cause instanceof TransportException) {
          // Invalid credentials
          String lowercaseMsg = cause.getMessage().toLowerCase();
          if (lowercaseMsg.contains(AuthUtil.NOT_AUTHORIZED)) {
            String message = translator.getTranslation(Tags.AUTHENTICATION_FAILED) + " ";
            CredentialsBase creds = OptionsManager.getInstance().getGitCredentials(sourceUrl.getHost());
            if (creds.getType() == CredentialsType.USER_AND_PASSWORD) {
              message += translator.getTranslation(Tags.CHECK_CREDENTIALS);
            } else if (creds.getType() == CredentialsType.PERSONAL_ACCESS_TOKEN) {
              message += translator.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS);
            }
            final String fMsg = message;
            SwingUtilities.invokeLater(() -> pluginWorkspace.showErrorMessage(fMsg));
            shouldBreak = true;
          }
        }
        if (shouldBreak) {
          cleanDestDir();
          break;
        }
        cause = cause.getCause();
      }
    }

		/**
		 * Clean the destination directory.
		 */
    private void cleanDestDir() {
      try {
        FileUtils.cleanDirectory(destFile);
      } catch (IOException e1) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e1.getMessage(), e1);
        }
      }
    }
	}
	
	/**
	 * Listens to insertions in / deletions from the source (repository) URL text field.
	 */
	private transient DocumentListener sourceUrlTextFieldDocListener = new DocumentListener() {
	  /**
	   * Timer task for checking the connection to the repository (source) URL.
	   */
	  private TimerTask checkConnectionTask;
	  /**
	   * Reference comparator (for branch names).
	   */
	  private Comparator<Ref> refComparator = (o1, o2) -> {
	    int toReturn = 0;
	    if (o1 != null && o2 != null) {
	      String name1 = getBranchShortName(o1);
	      String name2 = getBranchShortName(o2);
	      toReturn = name1.compareTo(name2);
	    }
	    return toReturn;
	  };

	  @Override
	  public void removeUpdate(DocumentEvent e) {
	    checkURLConnection();
	  }

	  @Override
	  public void insertUpdate(DocumentEvent e) {
	    checkURLConnection();
	  }

	  @Override
	  public void changedUpdate(DocumentEvent e) {
	    // Nothing to do here
	  }

	  /**
	   * Check the connection to the repository (source) URL.
	   */
	  private void checkURLConnection() {
	    if (checkConnectionTask != null) {
	      checkConnectionTask.cancel();
	    }
	    checkConnectionTask = new TimerTask() {
	      @Override
	      public void run() {
	        if (CloneRepositoryDialog.this.isShowing()
	            && KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() == CloneRepositoryDialog.this) {

	          try {
	            final List<Ref> remoteBranches = new ArrayList<>();
	            SwingUtilities.invokeLater(() -> {
	              branchesComboBox.removeAllItems();
	              setProgressVisible(true);
	            });

	            String sourceUrlAsText = sourceUrlTextField.getText();
	            boolean wasUrlProvided = sourceUrlAsText != null && !sourceUrlAsText.isEmpty();
	            if (wasUrlProvided) {
	              addBranches(remoteBranches, sourceUrlAsText);
	            }

	            SwingUtilities.invokeLater(() -> {
	              boolean shouldEnableBranchesCombo = !remoteBranches.isEmpty();
	              if (shouldEnableBranchesCombo) {
	                branchesComboBox.addItem(DEFAULT_BRANCH_MARKER);
	                for (Ref ref : remoteBranches) {
	                  branchesComboBox.addItem(ref);
	                }
	              }
	              branchesComboBox.setEnabled(shouldEnableBranchesCombo);
	              if (wasUrlProvided) {
	                // If we have branches, then we didn't have any problems.
	                // Hide the information label. Otherwise, show it.
	                informationLabel.setVisible(!shouldEnableBranchesCombo);
	              }
	            });
	          } catch (JGitInternalException e) {
	            Throwable cause = e.getCause();
	            if (cause instanceof NotSupportedException) {
	              showInfoMessage(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
	            } else {
	              pluginWorkspace.showErrorMessage(e.getMessage());
	              if (LOGGER.isDebugEnabled())  {
	                LOGGER.debug(e.getMessage(), e);
	              }
	            }
	          } finally {
	            SwingUtilities.invokeLater(() -> setProgressVisible(false));
	          }
	        }
	      }

	      /**
	       * Add the remote branches for the given URL to the given list.
	       * 
	       * @param remoteBranches  The list to be populated.
	       * @param sourceUrlAsText The remote URL as text.
	       */
        private void addBranches(final List<Ref> remoteBranches, String sourceUrlAsText) {
          try {
            URIish sourceURL = new URIish(sourceUrlAsText);
            Collection<Ref> branches = GitAccess.getInstance().listRemoteBranchesForURL(
                sourceURL,
                // Maybe there was a problem with getting the remote branches
                CloneRepositoryDialog.this::showInfoMessage);
            if (!branches.isEmpty()) {
              remoteBranches.addAll(branches);
              Collections.sort(remoteBranches, refComparator);
            }
          } catch (URISyntaxException e) {
            showInfoMessage(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(e.getMessage(), e);
            }
          }
        }

	      /**
	       * Show/hide the progress circle for branch retrieval.
	       * 
	       * @param shouldShow <code>true</code> to show the progress circle.
	       */
        private void setProgressVisible(boolean shouldShow) {
          if (loadingLabel != null) {
            JRootPane rootPane = getRootPane();
            JLayeredPane layeredPane = rootPane.getLayeredPane();
            layeredPane.remove(loadingLabel);
            if (shouldShow) {
              Rectangle rectToShowAt = SwingUtilities.convertRectangle(
                  branchesComboBox.getParent(), 
                  branchesComboBox.getBounds(), 
                  rootPane);
              rectToShowAt.translate(5, 0);
              loadingLabel.setBounds(rectToShowAt);
              layeredPane.add(loadingLabel, JLayeredPane.POPUP_LAYER);
              loadingLabel.setIcon(loadIcon);
            }
            branchesComboBox.repaint();
          }
        }
	    };
	    checkRepoURLConTimer.schedule(checkConnectionTask, 500);
	  }
	};

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Source URL text field.
	 */
	private JTextField sourceUrlTextField;

	/**
	 * Destination path combo box.
	 */
	private JComboBox<String> destinationPathCombo;

	/**
	 * Label for displaying information.
	 */
	private JLabel informationLabel;
	
	/**
	 * Timer for checking the connection to the repository (source) URL.
	 */
	private transient Timer checkRepoURLConTimer = new Timer("Check Repo URL Connection Daemon", false);

	/**
	 * The combo box containing the remote branches for a given repository URL.
	 */
  private JComboBox<Ref> branchesComboBox;
  
  /**
   * Plugin workspace access.
   */
  private PluginWorkspace pluginWorkspace;

  /**
   * Loading label with animated GIF.
   */
  private JLabel loadingLabel = new JLabel();
  
  /**
   * Load icon.
   */
  private Icon loadIcon;
	
	/**
	 * Constructor.
	 */
	public CloneRepositoryDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_TITLE), true);
		
		pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();

		createGUI();

		this.setMinimumSize(new Dimension(475, 160));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo((JFrame) pluginWorkspace.getParentFrame());
		this.setVisible(true);
	}

	/**
	 * Create the graphical user interface.
	 */
	private void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// "Repository URL" label
		JLabel lblURL = new JLabel(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_LABEL) + ":");
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(lblURL, gbc);

		// Repository URL input text field
		sourceUrlTextField = new TextField();
		sourceUrlTextField.setPreferredSize(new Dimension(350, sourceUrlTextField.getPreferredSize().height));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx ++;
		gbc.gridwidth = 2;
		panel.add(sourceUrlTextField, gbc);
		// Add document listener for URL validation and remote branch retrieval
		sourceUrlTextField.getDocument().addDocumentListener(sourceUrlTextFieldDocListener);

		// "Checkout branch" label
		JLabel branchesLabel = new JLabel(translator.getTranslation(Tags.CHECKOUT_BRANCH) + ":");
    gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy ++;
    gbc.gridwidth = 1;
    panel.add(branchesLabel, gbc);
    
    // Branches combo box
    branchesComboBox = new JComboBox<>();
    gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridx ++;
    gbc.gridwidth = 2;
    panel.add(branchesComboBox, gbc);
    branchesComboBox.setEnabled(false);
    branchesComboBox.setRenderer(
        new DefaultListCellRenderer() { // NOSONAR squid:MaximumInheritanceDepth
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
          if (value == DEFAULT_BRANCH_MARKER) {
            label.setText("<Default branch>");
          } else {
            label.setText(getBranchShortName((Ref) value));
          }
        }
        return label;
      }
    });
    
    // Loading icon
    loadIcon = Icons.getIcon(Icons.LOADING_ICON);
    
    // "Destination path" label
		JLabel lblPath = new JLabel(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_LABEL));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy ++;
		gbc.gridwidth = 1;
		panel.add(lblPath, gbc);

		// Destination path combo
		destinationPathCombo = new JComboBox<>();
		UndoRedoSupportInstaller.installManager(((JTextComponent) destinationPathCombo.getEditor().getEditorComponent()));
		destinationPathCombo.setEditable(true);
		List<String> destinationPaths = OptionsManager.getInstance().getDestinationPaths();
		if (!destinationPaths.isEmpty()) {
		  for (String string : destinationPaths) {
		    destinationPathCombo.addItem(string);
		  }
		  destinationPathCombo.setSelectedItem("");
		}
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx ++;
		panel.add(destinationPathCombo, gbc);

		// Browse action
		Action browseButtonAction = new AbstractAction() {
			@Override
      public void actionPerformed(ActionEvent e) {
				File directory = pluginWorkspace.chooseDirectory();
				if (directory != null) {
					destinationPathCombo.setSelectedItem(directory.getAbsolutePath());
				}
			}
		};
		ToolbarButton browseButton = new ToolbarButton(browseButtonAction, false);
		browseButton.setIcon(Icons.getIcon(Icons.FILE_CHOOSER_ICON));
		browseButton.setToolTipText(translator.getTranslation(Tags.BROWSE_BUTTON_TOOLTIP));
		browseButton.setOpaque(false);

		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx ++;
		panel.add(browseButton, gbc);
		
		// Information label shown when some problems occur
		informationLabel = new JLabel();
		informationLabel.setForeground(Color.RED);
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy ++;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 3;
		panel.add(informationLabel, gbc);

		this.add(panel, BorderLayout.NORTH);
	}
	
	/**
	 * Show info message in the dialog.
	 * 
	 * @param message The message.
	 */
	private void showInfoMessage(String message) {
	  SwingUtilities.invokeLater(() -> {
	    informationLabel.setText(message);
	    informationLabel.setVisible(true);
	    pack();
	  });
	  setMinimumSize(getSize());
	}

	/**
	 * 
	 * Get a branch short name.
	 * 
	 * @param ref The current reference from which to get the name.
	 * 
	 * @return the name or an empty string.
	 */
	private String getBranchShortName(Ref ref) {
	  String name = "";
	  if (ref != null) {
	    name = ref.getName();
	    int indexOf = name.indexOf("heads/");
	    if (indexOf != -1) {
	      name = name.substring(indexOf + "heads/".length());
	    }
	  }
	  return name;
	  
	}

	/**
	 * @see ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog.doOK()
	 */
	@Override
	protected void doOK() {
		if (areSourceAndDestValid()) {
		  super.doOK();
		}
	}
	
	/**
	 * Validate the source URL and the destination path.
	 * 
	 * @return <code>true</code> if bothe the source URL and the destination path are valid.
	 */
	private boolean areSourceAndDestValid() {
	  boolean areValid = false;
	  URIish sourceURL = getAndValidateSourceURL();
	  if (sourceURL != null) {
	    final String selectedDestPath = (String) destinationPathCombo.getSelectedItem();
	    if (selectedDestPath != null && !selectedDestPath.isEmpty()) {
	      final File destFile = new File(selectedDestPath);
	      if (isDestinationPathValid(destFile)) {
	        JFrame parentFrame = (JFrame) pluginWorkspace.getParentFrame();
          final ProgressDialog progressDialog = new ProgressDialog(parentFrame);
	        CloneWorker cloneWorker = new CloneWorker(
	            progressDialog,
	            sourceURL,
	            destFile,
	            (Ref) branchesComboBox.getSelectedItem());
          cloneWorker.execute();
	        // Make sure we present the dialog after this one is closed.
	        // TODO There is a progress dialog support in Java. Maybe is better to use that.
	        SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
	        areValid = true;
	      }
	    } else {
	      pluginWorkspace.showErrorMessage(
	          translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH));
	    }
	  }
	  return areValid;
	}
	
	/**
	 * Get the repository/source URL.
	 * 
	 * @return the URL or <code>null</code> if the provided text is invalid.
	 */
	private URIish getAndValidateSourceURL() {
	  URIish url = null;
	  final String repoURLText = sourceUrlTextField.getText();
	  if (repoURLText != null && !repoURLText.isEmpty()) {
	    try {
	      url = new URIish(repoURLText);
	    } catch (URISyntaxException e) {
        pluginWorkspace.showErrorMessage(
            translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
      }
	  } else {
	    pluginWorkspace.showErrorMessage(
	        translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
	  }
	  return url;
	}

	/**
	 * Check if the destination path is valid (for example, it does not yet exist).
	 * 
	 * @param destFile The destination.
	 * 
	 * @return <code>true</code> if the destination path is valid.
	 */
	private boolean isDestinationPathValid(final File destFile) {
	  boolean isDestPathValid = true;
		if (destFile.exists()) {
			if (destFile.list().length > 0) {
			  pluginWorkspace.showErrorMessage(
			      translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY));
			  isDestPathValid = false;
			}
		} else {
			File tempFile = destFile.getParentFile();
			while (tempFile != null) {
				if (tempFile.exists()) {
					destFile.mkdirs();
					break;
				}
				tempFile = tempFile.getParentFile();
			}
			if (tempFile == null) {
			  pluginWorkspace.showErrorMessage(
			      translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH));
			  isDestPathValid = false;
			}
		}
		return isDestPathValid;
	}

}
