package com.oxygenxml.git.view.history;

import java.awt.Component;
import java.util.function.BooleanSupplier;

import javax.swing.JLabel;
import javax.swing.JTable;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.staging.StagingResourcesTableCellRenderer;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;


/**
 * Cell render for affected files in git history.
 * 
 * @author alex_smarandache
 *
 */
public class HistoryAffectedFileCellRender extends StagingResourcesTableCellRenderer {

	/**
	 * The current presented file.
	 */
	private FileHistoryPresenter filePresenter = null;

	/**
	 * The current file path.
	 */
	private String currentFilePath = null;


	/**
	 * Constructor.
	 * 
	 * @param contextMenuShowing
	 */
	public HistoryAffectedFileCellRender(BooleanSupplier contextMenuShowing) {
		super(contextMenuShowing);
	}

    
	/** 
	 * @param filePresenter The new file presenter.
	 */
	public void setFilePresenter(FileHistoryPresenter filePresenter) {
		this.filePresenter = filePresenter;
	}


	/**
	 * @see javax.swing.table.TableCellRenderer.getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	 */
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if(value instanceof FileStatus) {
			currentFilePath = ((FileStatus)value).getFileLocation();
		}
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}


	/**
	 * Updates the text foreground.
	 *  
	 * @param currentFilePath                The current file path.
	 * @param tableCellRendererComponent     The displayed Jlabel. 
	 */
	@Override
	protected void updateForegroundText(JLabel tableCellRendererComponent) {
		if (PluginWorkspaceProvider.getPluginWorkspace().getColorTheme() != null) {
			if(filePresenter != null && filePresenter.isCurrentPathPresented(currentFilePath)) {
				tableCellRendererComponent.setForeground(
						PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme() ?
								UIUtil.NOT_SEARCHED_FILES_COLOR_GRAPHITE_THEME : 
									UIUtil.NOT_SEARCHED_FILES_COLOR_LIGHT_THEME);
			} else {
				tableCellRendererComponent.setForeground(
						PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme() ?
								UIUtil.SEARCHED_FILES_COLOR_GRAPHITE_THEME : 
									UIUtil.SEARCHED_FILES_COLOR_LIGHT_THEME);
			}
		}
	}

}
