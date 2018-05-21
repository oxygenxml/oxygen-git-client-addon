package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Ref;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.UndoSupportInstaller;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

public class CloneRepositoryDialog extends OKCancelDialog {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CloneRepositoryDialog.class);

	/**
	 * HTML start tag.
	 */
	private static final String HTML_END_TAG = "</html>";
	
	/**
	 * HTML end tag.
	 */
	private static final String HTML_START_TAG = "<html>";
	
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
		private final URL sourceUrl;
		/**
		 * Destination file.
		 */
		private final File destFile;

		/**
		 * Constructor.
		 * 
		 * @param progressDialog The progress dialog for the cloning operation.
		 * @param sourceUrl      Repository (source) URL.
		 * @param destFile       Destination file.
		 */
		private CloneWorker(ProgressDialog progressDialog, URL sourceUrl, File destFile) {
			this.progressDialog = progressDialog;
			this.sourceUrl = sourceUrl;
			this.destFile = destFile;
		}

		@Override
		protected Void doInBackground() throws Exception {
			CloneRepositoryDialog.this.setVisible(false);
			// Intercept all authentication requests.
			AuthenticationInterceptor.bind(sourceUrl.getHost());
			GitAccess.getInstance().clone(sourceUrl, destFile, progressDialog);
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
				if (logger.isDebugEnabled()) {
				  logger.debug(e, e);
				}
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				progressDialog.dispose();
				handleExecutionException(e);
				return;
			} finally {
			  AuthenticationInterceptor.unbind(sourceUrl.getHost());
			}
		}

		/**
		 * Handle execution exception.
		 * 
		 * @param ex The exception.
		 */
		private void handleExecutionException(ExecutionException ex) {
		  Throwable cause = ex.getCause();
		  while (cause != null) {
		    
		    boolean doBreak = false;
		    if (cause.getMessage().contains("Download cancelled")) {
		      cleanDestDir();
		      doBreak = true;
		    } else if (cause instanceof NoRemoteRepositoryException) {
		      CloneRepositoryDialog.this.setVisible(true);
		      CloneRepositoryDialog.this.setMinimumSize(new Dimension(400, 190));
		      informationLabel.setText(
		          HTML_START_TAG 
		          + translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY) 
		          + HTML_END_TAG);
		      doBreak = true;
		    } else if (cause instanceof org.eclipse.jgit.errors.TransportException) {
		      LoginDialog loginDialog = new LoginDialog(
		          sourceUrl.getHost(),
		          translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_LOGIN_MESSAGE));
          UserCredentials userCredentials = loginDialog.getUserCredentials();
		      if (userCredentials != null) {
		        doOK();
		      }
		      doBreak = true;
		    }
		    
		    if (doBreak) {
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
        if (logger.isDebugEnabled()) {
          logger.debug(e1, e1);
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
          String text = sourceUrlTextField.getText();
          if (text != null && !text.isEmpty()) {
            branchesComboBox.removeAllItems();
            try {
              URL sourceURL = new URL(text);
              AuthenticationInterceptor.bind(sourceURL.getHost());
              List<Ref> remoteBranches = new ArrayList<>(GitAccess.getInstance().listRemoteBranchesForURL(text));
              Collections.sort(remoteBranches, refComparator);
              // Re-populate the combo
              for (Ref ref : remoteBranches) {
                branchesComboBox.addItem(ref);
              }
            } catch (MalformedURLException e) {
              branchesComboBox.removeAllItems();
              if (logger.isDebugEnabled()) {
                logger.debug(e, e);
              }
            }
          } else {
            branchesComboBox.removeAllItems();
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
	 * Constructor.
	 *  
	 * @param parentFrame Parent frame.
	 * @param translator Translation support.
	 */
	public CloneRepositoryDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_TITLE), true);

		createGUI();

		this.setMinimumSize(new Dimension(475, 160));
		this.setResizable(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
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
		sourceUrlTextField = UIUtil.createTextField();
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
    branchesComboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
          label.setText(getBranchShortName((Ref) value));
        }
        return label;
      }
    });

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
		UndoSupportInstaller.installUndoManager(((JTextComponent) destinationPathCombo.getEditor().getEditorComponent()));
		destinationPathCombo.setEditable(true);
		List<String> destinationPaths = OptionsManager.getInstance().getDestinationPaths();
		for (String string : destinationPaths) {
			destinationPathCombo.addItem(string);
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
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					destinationPathCombo.setSelectedItem(directory.getAbsolutePath());
				}
			}
		};
		ToolbarButton browseButton = new ToolbarButton(browseButtonAction, false);
		ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
		URL resource = getClass().getResource(ImageConstants.FILE_CHOOSER_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  browseButton.setIcon(icon);
		}
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
	 * 
	 * Get a branch short name.
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
	  boolean doOK = false;
		final String selectedPath = (String) destinationPathCombo.getSelectedItem();
		try {
			final URL url = new URL(sourceUrlTextField.getText());
			final File file = new File(selectedPath);
			if (!isDestinationPathValid(file)) {
				return;
			}
			
			// Progress dialog.
	    final ProgressDialog progressDialog = new ProgressDialog(
	        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());

	    new CloneWorker(progressDialog, url, file).execute();
	    
	    // Make sure we present the dialog after this one is closed.
	    // TODO There is a progress dialog support in Java. Maybe is better to use that.
	    SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
	    
	    doOK = true;
		} catch (MalformedURLException e) {
			informationLabel.setText(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_URL));
			this.pack();
		}
		
		if (doOK) {
		  // Close the dialog.
		  super.doOK();
		}

	}

	/**
	 * Check if the destination path is valid (for example, it does not yet exist).
	 * 
	 * @param destFile The destination.
	 * 
	 * @return <code>true</code> if the destination path is valid.
	 */
	private boolean isDestinationPathValid(final File destFile) {
		if (destFile.exists()) {
			if (destFile.list().length > 0) {
				CloneRepositoryDialog.this.setVisible(true);
				this.setMinimumSize(new Dimension(400, 190));
				informationLabel.setText(
						HTML_START_TAG 
						+ translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY) 
						+ HTML_END_TAG);
				return false;
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
				CloneRepositoryDialog.this.setVisible(true);
				this.setMinimumSize(new Dimension(400, 180));
				informationLabel.setText(
						HTML_START_TAG 
						+ translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH) 
						+ HTML_END_TAG);
				return false;
			}
		}
		return true;
	}

}
