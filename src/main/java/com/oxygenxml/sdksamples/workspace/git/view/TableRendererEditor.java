package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;

public class TableRendererEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

	private JTable table;
	private JButton button;
	private JButton editedButton;
	private int[] hovered = null;
	private int[] previousHovered = null;

	private TableRendererEditor(JTable table) {

		this.table = table;
		this.button = new JButton();
		this.editedButton = new JButton();

		addMouseMotionListener();
	}

	public static void install(JTable table) {
		TableRendererEditor tableRendereEditor = new TableRendererEditor(table);
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

			public void mouseDragged(MouseEvent e) {}
		});

	}

	public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, int column) {

		if (hovered != null && hovered[0] == row && hovered[1] == column) {

			editedButton.setText("Stage");
			editedButton.setForeground(table.getForeground());
			editedButton.setBackground(UIManager.getColor("Button.select"));
			if (editedButton.getActionListeners().length != 0) {
				editedButton.removeActionListener(editedButton.getActionListeners()[0]);
			}
			editedButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					FileTableModel unstagedTableModel = (FileTableModel) table.getModel();
					int convertedRow = table.convertRowIndexToModel(row);
					unstagedTableModel.removeUnstageFile(convertedRow);

					fireEditingStopped();

				}
			});
			return editedButton;
		}

		return button;
	}

	public Object getCellEditorValue() {

		return null;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (isSelected) {
			button.setForeground(table.getSelectionForeground());
			button.setBackground(table.getSelectionBackground());

		} else {
			button.setForeground(table.getForeground());
			button.setBackground(UIManager.getColor("Button.background"));

		}

		if (value == null) {
			button.setText("");
			button.setIcon(null);
		} else {
			button.setText(value.toString());
			button.setIcon(null);
		}

		boolean hov = hovered != null && hovered[0] == row && hovered[1] == column;
		button.getModel().setRollover(hov);

		return button;

	}

}
