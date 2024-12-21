/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.observation.ui.input;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

/**
 * A table for displaying observations for a specific 
 * category.  This is a read-only table.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTable {

	public static final String EDITING_OBS_KEY = "EDITING_OBS_KEY"; //$NON-NLS-1$
	/**
	 * Maximum width of table column
	 */
	private static final int MAX_COLUMN_WIDTH = 200;

	private static final String ATTACHMENT_COLUMN_NAME=Messages.AttributeTable_AttachmentsColumnName;
	
	/**
	 * Creates a new attribute table.
	 * 
	 * @param parent parent composite
	 * @param currentCategory category containing attributes to display
	 * @return created attribute table
	 */
	public static TableViewer createAttributeTable( 
			Composite parent, 
			List<Attribute> attributes){
		
		final TableViewer attributeTable = new TableViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
//		attributeTable.getTable().setLinesVisible(true);
		attributeTable.getTable().setHeaderVisible(true);
		
		for (int i = 0; i < attributes.size(); i ++) {
			TableViewerColumn column = new TableViewerColumn(attributeTable, SWT.NONE);
			column.getColumn().setText(SmartUtils.formatStringForLabel(attributes.get(i).getName()));
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.setLabelProvider(new AttributeTableLabelProvider(attributes.get(i), attributeTable));
			
		}
		TableViewerColumn attachments = new TableViewerColumn(attributeTable, SWT.NONE);
		attachments.getColumn().setText(ATTACHMENT_COLUMN_NAME);
		attachments.getColumn().setResizable(true);
		attachments.getColumn().setMoveable(false);
		attachments.setLabelProvider(new AttributeTableLabelProvider(null, attributeTable));
		return attributeTable;
	}
	
	/**
	 * Set the column size based on the data in the table. 
	 * Thie tableViewer must be created from the createAttributeTable
	 * function.
	 * 
	 * @param table
	 */
	public static void resizeColumns(TableViewer table){
		for (TableColumn c : table.getTable().getColumns()) {
			c.pack();
			if (c.getWidth() > MAX_COLUMN_WIDTH) c.setWidth(MAX_COLUMN_WIDTH);
		}
	}
	
	
	static class AttributeTableLabelProvider extends ColumnLabelProvider{

		private Attribute attribute;
		private TableViewer viewer;
		
		private Color yellow;
		
		private IconCache iconCache;
		
		public AttributeTableLabelProvider(Attribute attribute, TableViewer viewer){
			this.iconCache = new IconCache(viewer.getControl(), IconManager.Size.ICON);
			this.attribute = attribute;
			this.viewer = viewer;
			yellow = SmartPlugIn.createYellow();
			viewer.getControl().addDisposeListener(e->{
				yellow.dispose();
			});
		}

		public WaypointObservation getEditingObservation() {
			return (WaypointObservation) viewer.getControl().getData(EDITING_OBS_KEY);
		}
		
		@Override
		public Color getForeground(Object element) {
			if (element.equals(getEditingObservation())) return viewer.getTable().getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			if (element.equals(getEditingObservation())) return yellow;			
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof WaypointObservation) {
				WaypointObservation observation = (WaypointObservation) element;
				if (attribute != null) {
					WaypointObservationAttribute att = observation.findAttribute(attribute);
					if (att != null) return att.getAttributeValueAsString(Locale.getDefault());
					return ""; //$NON-NLS-1$
				}else {
					if (observation.getAttachments() != null && observation.getAttachments().size() > 0){
						return MessageFormat.format(Messages.AttributeTable_AttachmentsFileCountLabel, new Object[]{String.valueOf(observation.getAttachments().size())});
					}
					return ""; //$NON-NLS-1$
				}
			}
			return ""; //$NON-NLS-1$
		}
		
		@Override
		public Image getImage(Object element) {
			if (attribute == null) return null;
			
			if (element instanceof WaypointObservation) {
				WaypointObservation observation = (WaypointObservation) element;
				WaypointObservationAttribute att = observation.findAttribute(attribute);
				if (att == null) return null;
				if (att.getAttributeListItem() != null ) {
					return iconCache.getImage(att.getAttributeListItem());
				}else if (att.getAttributeTreeNode() != null) {
					return iconCache.getImage(att.getAttributeTreeNode());					
				}
			}
			return null;
		}
		
	}

}
