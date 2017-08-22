package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.StageController;

public class TableRendererEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

	private JTable table;
	private StageController stageController;
	private JButton button;
	private JButton editedButton;
	private int[] hovered = null;
	private int[] previousHovered = null;
	

	private TableRendererEditor(JTable table, StageController stageController) {
		this.stageController = stageController;
		this.table = table;
		this.button = new JButton();
		this.editedButton = new JButton();

		addMouseMotionListener();
	}

	public static void install(JTable table, StageController stageController) {
		TableRendererEditor tableRendereEditor = new TableRendererEditor(table, stageController);
		TableColumn column = table.getColumnModel().getColumn(Constants.STAGE_BUTTON_COLUMN);
		column.setCellRenderer(tableRendereEditor);
		column.setCellEditor(tableRendereEditor);
	}

	private void addMouseMotionListener() {
		table.addMouseMotionListener(new MouseMotionListener() {

			public void mouseMoved(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				int row = table.convertRowIndexToModel(table.rowAtPoint(point));
				int column = table.columnAtPoint(point);

				TableModel model = table.getModel();
				Class<?> columnClass = model.getColumnClass(column);

				if (hovered != null) {
					previousHovered = new int[] { hovered[0], hovered[1] };
				}

				if (String.class.equals(columnClass) && column == 2 && point.getX() < table.getWidth() - 10 && point.getY() > 2
						&& row != -1) {
					hovered = new int[] { row, column };
				} else {
					hovered = null;
				}

				if (hovered != null) {
					table.repaint(table.getCellRect(hovered[0], hovered[1], true));
					if (previousHovered != null) {
						table.repaint(table.getCellRect(previousHovered[0], previousHovered[1], true));
					}
				} else if (previousHovered != null) {
					table.repaint(table.getCellRect(previousHovered[0], previousHovered[1], true));
					previousHovered = null;
				}
			}

			public void mouseDragged(MouseEvent e) {
			}
		});

	}

	public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row,
			int column) {

		if (hovered != null && hovered[0] == row && hovered[1] == column) {

			if (editedButton.getActionListeners().length != 0) {
				editedButton.removeActionListener(editedButton.getActionListeners()[0]);
			}
			final StagingResourcesTableModel model = (StagingResourcesTableModel) table.getModel();
			int convertedRow = table.convertRowIndexToModel(row);
			final FileStatus file = model.getUnstageFile(convertedRow);
			if (file.getChangeType() == GitChangeType.CONFLICT) {
				editedButton.setText("Resolve");
				editedButton.addActionListener(new ActionListener() {
					
					public void actionPerformed(ActionEvent e) {
						/*DiffPresenter diff = new DiffPresenter(file, stageController, translator);
						diff.showDiff();
						fireEditingStopped();*/
					}
				});
			} else {
				editedButton.setText((String) value);
				editedButton.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						StagingResourcesTableModel unstagedTableModel = (StagingResourcesTableModel) table.getModel();
						int convertedRow = table.convertRowIndexToModel(row);
						unstagedTableModel.switchFileStageState(convertedRow);

						fireEditingStopped();

					}
				});
			}
			return editedButton;
		}

		return button;
	}

	public Object getCellEditorValue() {

		return null;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		boolean hov = hovered != null && hovered[0] == row && hovered[1] == column;
		button.getModel().setRollover(hov);

		StagingResourcesTableModel model = (StagingResourcesTableModel) table.getModel();
		int convertedRow = table.convertRowIndexToModel(row);
		FileStatus file = model.getUnstageFile(convertedRow);
		if (file.getChangeType() == GitChangeType.CONFLICT) {
			button.setText("Resolve");
			return button;
		} else {

			if (value == null) {
				button.setText("");
				button.setIcon(null);
			} else {
				button.setText(value.toString());
				button.setIcon(null);
			}

			return button;
		}

	}

}
