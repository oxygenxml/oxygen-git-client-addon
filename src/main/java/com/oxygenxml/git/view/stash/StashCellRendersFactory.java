package com.oxygenxml.git.view.stash;

import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.view.util.UIUtil;

/**
 * Factory for stashes table cell render.
 * 
 * @author alex_smarandache
 *
 */
public class StashCellRendersFactory {

	/**
	 * Create a new render for stash date.
	 * 
	 * @return Created table cell render.
	 */
	public static DefaultTableCellRenderer getDateCellRender() {
		return new StashDateRender();
	}


	/**
	 * Create a new render for stash index.
	 * 
	 * @return Created table cell render.
	 */
	public static DefaultTableCellRenderer getIndexCellRender() {
		return new StashIndexRender();
	}

	
	/**
	 * Create a new render for stash message.
	 * 
	 * @return Created table cell render.
	 */
	public static DefaultTableCellRenderer getMessageCellRender() {
		return new StashMessageRender();
	}

	/**
	 * A custom render for Stash index.
	 *
	 * @author Alex_Smarandache
	 */
	@SuppressWarnings("serial")
	private static class StashIndexRender extends DefaultTableCellRenderer {

		/**
		 * The border for padding.
		 */
		private static final Border PADDING = BorderFactory.createEmptyBorder(
				0, 
				UIConstants.COMPONENT_LEFT_PADDING, 
				0, 
				UIConstants.COMPONENT_RIGHT_PADDING
				);

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {  

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setBorder(BorderFactory.createCompoundBorder(getBorder(), PADDING));

			return this;
		}

	}


	/**
	 * A custom render for Stash date.
	 *
	 * @author Alex_Smarandache
	 */
	@SuppressWarnings("serial")
	private static class StashDateRender extends DefaultTableCellRenderer {

		/**
		 * The border for padding.
		 */
		private static final Border PADDING = BorderFactory.createEmptyBorder(
				0, 
				UIConstants.COMPONENT_LEFT_PADDING, 
				0, 
				UIConstants.COMPONENT_RIGHT_PADDING
				);

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {  

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setBorder(BorderFactory.createCompoundBorder(getBorder(), PADDING));

			if(value instanceof Date) {
				Date date = (Date)value;
				DateFormat formatter = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN_WITHOUT_HOUR);
				setText(formatter.format(date));
			}

			return this;
		}
	}


	/**
	 * A custom render for Stash message.
	 *
	 * @author Alex_Smarandache
	 */
	@SuppressWarnings("serial")
	private static class StashMessageRender extends DefaultTableCellRenderer {

		/**
		 * The border for padding.
		 */
		private final Border padding = BorderFactory.createEmptyBorder(
				0, 
				UIConstants.COMPONENT_LEFT_PADDING, 
				0, 
				UIConstants.COMPONENT_RIGHT_PADDING
				);

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {  

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setText((String) value);
			setToolTipText((String) value);

			setBorder(BorderFactory.createCompoundBorder(getBorder(), padding));

			return this;
		}

		@Override
		public JToolTip createToolTip() {
			return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
		}

	}

}
