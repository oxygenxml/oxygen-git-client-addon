package com.oxygenxml.git.view.history.graph;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.revplot.PlotCommit;

import com.oxygenxml.git.view.history.graph.VisualCommitsList.VisualLane;

/**
 * 
 * A cell render for history commits graph.
 * 
 * @author alex_smarandache
 *
 */
@SuppressWarnings("serial")
public class CommitsGraphCellRender extends JPanel implements TableCellRenderer {

	/**
	 * The graph render. 
	 */
	private final transient GraphRender cellRender;
	
	/**
	 * The table.
	 */
	private JTable table;
	
	/**
	 * The cell value.
	 */
	private transient Object value;

	/**
	 * <code>true</code> if the cell should be painted.
	 */
	private boolean shouldBePainted = true;
	
	 /**
	  * The string id for the last commit of current branch.
	  */
	 private String lastCommitIdForCurrentBranch;
	 

	/**
	 * Constructor.
	 */
	public CommitsGraphCellRender() {
		cellRender = new GraphRender();
	}
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column) {
		this.table = table;
		this.value = value;
		
		setBackground(table.getBackground());
        cellRender.setBackground(getBackground());
		
		return this;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(shouldBePainted && value != null) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setBackground(getBackground());
			final PlotCommit<VisualLane> currentCommit = (PlotCommit<VisualCommitsList.VisualLane>)value;
			cellRender.paint(currentCommit, 
					table.getRowHeight(), 
					g2d,
				  lastCommitIdForCurrentBranch != null && lastCommitIdForCurrentBranch.equals(currentCommit.getId().getName())
		    );
		}
	}
	
	
    /**
     * @param shouldBePainted <code>true</code> if the cell should be painted.
     */
	public void setShouldBePainted(boolean shouldBePainted) {
		this.shouldBePainted = shouldBePainted;
	}

	/**
	  * Setter. This commit will be painted special.
	  * 
	  * @param lastCommitIdForCurrentBranch The current commit of current local branch.
	  */
	 public void setLastCommitIdForCurrentBranch(@Nullable final String lastCommitIdForCurrentBranch) {
	  this.lastCommitIdForCurrentBranch = lastCommitIdForCurrentBranch;
	} 
	 
	 
}
