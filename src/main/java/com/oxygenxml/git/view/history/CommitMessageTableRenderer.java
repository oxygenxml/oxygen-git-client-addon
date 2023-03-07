package com.oxygenxml.git.view.history;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.common.annotations.VisibleForTesting;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.RoundedLineBorder;
import com.oxygenxml.git.view.components.ApplicationLabel;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Renderer for HistoryTable including tag and branch labels.
 * 
 * @Alexandra_Dinisor
 *
 */
public class CommitMessageTableRenderer extends JPanel implements TableCellRenderer {
  /**
   * The size of the border shown for a tag or branch label.
   */
  private static final int LABEL_BORDER_CORNER_SIZE = 6;

  /**
   * Font size for the arrow characters that show the incoming and outgoing changes.
   */
  private static final int ARROWS_FONT_SIZE = 12;

  /**
   * Default horizontal insets between components.
   */
  private static final int HORIZONTAL_INSET = 3;

  /**
   * The maximum length to which a branch/tag name can be shortened.
   */
  private static final int MAX_BRANCH_OR_TAG_NAME_LENGTH_HIGH = 50;
  
  /**
   * The minimum length to which a branch/tag name can be shortened.
   */
  private static final int MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW = 5;

  /**
   * Git repository.
   */
  private final Repository repository;

  /**
   * Commits ahead (to push) and behind (to pull).
   */
  private final CommitsAheadAndBehind commitsAheadAndBehind;
  /**
   * The current branch name in the git repository.
   */
  private final String currentBranchName;
  /**
   * Commit ID to a list of tags.
   */
  private final Map<String, List<String>> tagMap;
  /**
   * Commit ID to a list of branch labels.
   */
  private final Map<String, List<String>> localBranchMap;
  /**
   * Commit ID to a list of branch labels.
   */
  private final Map<String, List<String>> remoteBranchMap;

  /**
   * Table for this render.
   */
  private JTable table;

  /**
   * The current row.
   */
  private int row;

  /**
   * The current column.
   */
  private int column;
  
  /**
   * The list of labels for current commit.
   */
  private List<JLabel> commitLabels = new ArrayList<>();
  
  /**
   * The delta for current message.
   */
  private static final int MESSAGE_DELTA = 7;


  /**
   * Construct the Table Renderer with accurate alignment.
   * 
   * @param repository            The current repository
   * @param commitsAheadAndBehind Commits ahead (to push) and behind (to pull).
   * @param branchName            Branch name.
   * @param tagMap                Tags map.
   * @param localBranchMap        Local branches map.
   * @param remoteBranchMap       Remote branches map.
   */
  public CommitMessageTableRenderer(
      Repository repository, 
      CommitsAheadAndBehind commitsAheadAndBehind,
      String branchName,
      Map<String, List<String>> tagMap,
      Map<String, List<String>> localBranchMap,
      Map<String, List<String>> remoteBranchMap) {
    this.repository = repository;
    this.commitsAheadAndBehind = commitsAheadAndBehind;
    this.currentBranchName = branchName;
    this.tagMap = tagMap;
    this.localBranchMap = localBranchMap;
    this.remoteBranchMap = remoteBranchMap;

    setLayout(new GridBagLayout());
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    this.table = table;
    this.row = row;
    this.column = column;
    
    final TableColumnModel tableColumnModel = table.getColumnModel();
    
    if(tableColumnModel.getColumnCount() > column) {
      setup(table, isSelected, row);
      
      // adding constraints for commit message label when wrapping
      final GridBagConstraints constr = new GridBagConstraints();
      constr.fill = GridBagConstraints.NONE;
      constr.anchor = GridBagConstraints.WEST;
      constr.gridy = 0;
      constr.gridx = 0;
      constr.insets = new Insets(0, HORIZONTAL_INSET, 0, HORIZONTAL_INSET);

      if (value instanceof CommitCharacteristics) {
        addAllRenderingInfoForCurrentCommit(value, constr, table);
      } else {
        final String toRender = value != null ? value.toString() : "";
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.weightx = 1;
        final JLabel comp = new JLabel(toRender);
        comp.setForeground(getForeground());
        if(!toRender.isEmpty()) {
          comp.setToolTipText(toRender);
        }
        add(comp, constr);
      }
      
    }
   
    return this;
  }

  /**
   * This method is used for initial setup of this render.
   * 
   * @param table       The table of this render.
   * @param isSelected  <code>true</code> if the current element is selected.
   * @param row         The current row.
   */
  private void setup(JTable table, boolean isSelected, int row) {
    removeAll();

    // keep the selection for whole columns of the row when selecting.
    if (isSelected) {
      super.setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      Color background = table.getBackground();
      if (background == null || background instanceof javax.swing.plaf.UIResource) {
        Color alternateColor = UIManager.getColor("Table.alternateRowColor");
        if (alternateColor != null && row % 2 != 0) {
          background = alternateColor;
        }
      }
      super.setForeground(table.getForeground());
      super.setBackground(background);
    }

    setFont(table.getFont());
    setBorder(getNoFocusBorder());
  }

  /**
   * Get the text to render for a commit.
   * 
   * @param value  The commit characteristics.
   * @param constr Grid bag constraints.
   * @param table  The table.
   * 
   * @return the text to render.
   */
  private void addAllRenderingInfoForCurrentCommit(Object value, GridBagConstraints constr, JTable table) {
    final CommitCharacteristics commitCharacteristics = (CommitCharacteristics) value;
    int availableWidth = table.getColumnModel().getColumn(column).getWidth();
    String commitMessageToRender = commitCharacteristics.getCommitMessage().replaceAll("\\n+", " ").trim();
    
    commitLabels.clear();

    // Show outgoing and incoming commits using arrows
    String arrow = "";
    if (isAheadCommit(commitCharacteristics.getCommitId())) {
      // Up arrow
      arrow = "\u2191";
    } else if (isBehindCommit(commitCharacteristics.getCommitId())) {
      // Down arrow
      arrow = "\u2193";
    }
    
    if (!arrow.isEmpty()) {
      final JLabel arrowLabel = new JLabel(arrow);
      arrowLabel.setFont(new Font("Dialog", Font.PLAIN, ARROWS_FONT_SIZE));
      arrowLabel.setForeground(getForeground());
      constr.gridx ++;
      add(arrowLabel, constr);
      availableWidth -= arrowLabel.getPreferredSize().width;
    }
    
    JLabel comp = new ApplicationLabel(commitMessageToRender);

    // bold the text for uncommitted changes
    final String uncommittedChangesMessage = Translator.getInstance().getTranslation(Tags.UNCOMMITTED_CHANGES);
    if (commitMessageToRender.equals(uncommittedChangesMessage)) {
      commitMessageToRender = "<html><body><b>" + uncommittedChangesMessage + "</b></body></html>";
      comp = new ApplicationLabel(commitMessageToRender); 
    } else if (repository != null) {
      commitLabels = computeLabelsForCurrentCommit(table, commitCharacteristics);
      
      comp = new ApplicationLabel(commitMessageToRender); 
      if(!commitMessageToRender.trim().isEmpty()) {
        comp.setToolTipText(commitMessageToRender);
      }
      
      int labelsTotalWidth = commitLabels.stream().mapToInt(label -> label.getPreferredSize().width).sum();
      final int commitMessageWidth = comp.getPreferredSize().width;
      if((labelsTotalWidth + commitMessageWidth) > availableWidth) {
        int labelsMaxWidth = availableWidth / 2;
        if(commitMessageWidth < labelsMaxWidth) {
          labelsMaxWidth = Math.max(availableWidth - commitMessageWidth - MESSAGE_DELTA, labelsMaxWidth);
        }
        processingCommitLabelsToFitByWidth(commitLabels, labelsMaxWidth);
      }
      
      addAllCommitLabels(constr);
    }
    
    constr.fill = GridBagConstraints.HORIZONTAL;
    constr.weightx = 1;
    constr.gridx++;
    comp.setForeground(getForeground());
    if(!commitMessageToRender.isEmpty()) {
      comp.setToolTipText(commitMessageToRender);
    }
    add(comp, constr);
  }

  /**
   * Add to UI all commit labels.
   * 
   * @param constr The initial constraints.
   */
  private void addAllCommitLabels(final GridBagConstraints constr) {
    final Insets oldInsets = constr.insets;
    constr.insets = new Insets(0, 0, 0, 0);
    
    commitLabels.forEach(label -> {
      constr.gridx++;
      CommitMessageTableRenderer.this.add(label, constr); 
    });
    
    constr.insets = oldInsets;
  }

  /**
   * Short the labels to respect the available width.
   * <br>
   * Found the maximum value between [{MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW}, {MAX_BRANCH_OR_TAG_NAME_LENGTH_HIGH}]
   * for which the labels can be shortened so that they do not exceed the available size.
   * If no value is okay, the labels will be shortened to a {MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW} maximum length.
   *  
   * @param commitLabels   The labels for current commit.
   * @param availableWidth The available width for commit labels.
   */
  @VisibleForTesting
  protected static void processingCommitLabelsToFitByWidth(final List<JLabel> commitLabels, final int availableWidth) { 
    int left = MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW;
    int right = MAX_BRANCH_OR_TAG_NAME_LENGTH_HIGH;
    int current = 0;
    int currentLabelsWidth = 0;
    
    // use binary search algorithm to find the best value 
    while(left < right) {
      current = (left + right) / 2;
      currentLabelsWidth = shortenLabelsText(commitLabels, current);
      if(currentLabelsWidth > availableWidth) {
        right = current - 1;
      } else if(currentLabelsWidth < availableWidth) {
        left = current + 1;
      } else {
        break;
      }
    }
   
    // checks and adjust current labels width not to exceed the available size
    if(current > MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW && currentLabelsWidth > availableWidth) {
      do {
        current--;
      } while(current > MAX_BRANCH_OR_TAG_NAME_LENGTH_LOW && shortenLabelsText(commitLabels, current) > availableWidth);
    }

  }

  /**
   * Put on list of labels for current commit all the branches or tags labels.
   * 
   * @param table                  The current table.
   * @param commitCharacteristics  The current commit.
   * 
   * @return The computed list with all commit labels or an empty list.
   */
  @NonNull
  private List<JLabel> computeLabelsForCurrentCommit(final JTable table,
      final CommitCharacteristics commitCharacteristics) {
    final List<JLabel> commitLabelsList = new ArrayList<>();
    String abbreviatedId = commitCharacteristics.getCommitAbbreviatedId();
    boolean isDarkTheme = PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme();

    List<String> tagList = tagMap.get(abbreviatedId);
    Color tagBackgroundColor = isDarkTheme ? UIUtil.TAG_GRAPHITE_BACKGROUND 
        : UIUtil.TAG_LIGHT_BACKGROUND;
    createAndPutBranchOrTagLabelOnList(commitLabelsList, tagList, tagBackgroundColor, table.getForeground());

    List<String> localBranchList = localBranchMap.get(abbreviatedId);
    createAndPutBranchOrTagLabelOnList(commitLabelsList, localBranchList, table.getBackground(), table.getForeground());

    List<String> remoteBranchList = remoteBranchMap.get(abbreviatedId);
    Color remoteBackgroundColor = isDarkTheme ? UIUtil.REMOTE_BRANCH_GRAPHITE_BACKGROUND 
        : UIUtil.REMOTE_BRANCH_LIGHT_BACKGROUND;

    createAndPutBranchOrTagLabelOnList(commitLabelsList, remoteBranchList, remoteBackgroundColor, table.getForeground());
    
    return commitLabelsList;
  }

  /**
   * Short all label texts which are biggest than given maximum length.
   * <br><br>
   * !!! IMPORTANT !!! The complete name should be set on the tooltip text which is used as original text.
   * 
   * @param commitLabels   The labels for current commit.
   * @param maxLabelLength The maximum length for a label.
   * 
   * @return The sum of all new labels.
   */
  private static int shortenLabelsText(final List<JLabel> commitLabels, final int maxLabelLength) {
    return commitLabels.stream().mapToInt(commitLabel -> {
      final String name = commitLabel.getToolTipText(); 
      final String shortenName = name.length() > maxLabelLength ? 
          (name.substring(0, maxLabelLength - "...".length()) + "...") 
          : name;
      commitLabel.setText(shortenName);
      return commitLabel.getPreferredSize().width;
    }).sum();
  }

  /**
   * Add Label to "Commit Message" column: tag or local/remote branch to te list of current commit labels.
   * 
   * @param commitLabels     The collection to add the commit labels.
   * @param nameForLabelList List of tags or branches corresponding the commit.
   * @param backgroundColor  The background color.
   * @param foregroundColor  The foreground color.
   */
  private void createAndPutBranchOrTagLabelOnList(
      List<JLabel> commitLabels,
      List<String> nameForLabelList,
      Color backgroundColor,
      Color foregroundColor) {
    if (nameForLabelList != null && !nameForLabelList.isEmpty()) {
      int lineSize = 1;

      for (String name : nameForLabelList) {
        final RoundedLineBorder border = new RoundedLineBorder(null, lineSize, LABEL_BORDER_CORNER_SIZE, true);
        final JLabel label = new ApplicationLabel(name) {

          @Override
          protected void paintComponent(Graphics g) {
            border.fillBorder(this, g, 0, 0, getWidth(), getHeight());
            super.paintComponent(g);
          }
        };
        if (name.equals(currentBranchName)) {
          label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        label.setBackground(backgroundColor);
        label.setForeground(foregroundColor);
        label.setBorder(border);
        label.setToolTipText(name);
        commitLabels.add(label);
      }
    }
  }

  /**
   * Check if this is a commit to push.
   * 
   * @param commitID Commit ID.
   *  
   * @return true if this is a commit to push.
   */
  private boolean isAheadCommit(String commitID) {
    boolean isIt = false;
    if (commitsAheadAndBehind != null) {
      List<RevCommit> commitsAhead = commitsAheadAndBehind.getCommitsAhead();
      isIt = commitsAhead.stream().anyMatch(commit -> commit.getId().getName().equals(commitID));
    }
    return isIt;
  }

  /**
   * Check if this is a commit to pull.
   * 
   * @param commitID Commit ID.
   *  
   * @return true if this is a commit to pull.
   */
  private boolean isBehindCommit(String commitID) {
    boolean isIt = false;
    if (commitsAheadAndBehind != null) {
      List<RevCommit> commitsBehind = commitsAheadAndBehind.getCommitsBehind();
      isIt = commitsBehind.stream().anyMatch(commit -> commit.getId().getName().equals(commitID));
    }
    return isIt;
  }

  /**
   * @see javax.swing.table.DefaultTableCellRenderer.getNoFocusBorder()
   * @return The Border with no focus
   */
  private Border getNoFocusBorder() {
    Border toReturn = noFocusBorder;

    Border border = UIManager.getBorder("Table.cellNoFocusBorder");
    if (System.getSecurityManager() != null) {
      toReturn = border != null ? border : SAFE_NO_FOCUS_BORDER;
    } else if (border != null) {
      if (noFocusBorder == null || noFocusBorder == DEFAULT_NO_FOCUS_BORDER) { // NOSONAR squid:S1066
        toReturn = border;
      }
    }

    return toReturn;
  }

  @Override
  public String getToolTipText(MouseEvent event) {

    String tooltipText = null;

    Rectangle rectangle = table.getCellRect(row, column, true);
    setBounds(rectangle);
    doLayout();

    final Component component = getComponentAt(event.getPoint());

    if (component instanceof JLabel) {
      final JLabel label = (JLabel)component;
      tooltipText = label.getToolTipText();
    }

    return tooltipText;

  }

  /**
   * An empty <code>Border</code>. This field might not be used. To change the
   * <code>Border</code> used by this renderer override the
   * <code>getTableCellRendererComponent</code> method and set the border of the
   * returned component directly.
   */
  private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
  private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
  protected static Border noFocusBorder = DEFAULT_NO_FOCUS_BORDER;

}
