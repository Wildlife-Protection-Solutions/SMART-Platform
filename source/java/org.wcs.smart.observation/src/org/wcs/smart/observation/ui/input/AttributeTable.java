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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
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

	/**
	 * Maximum width of table column
	 */
	private static final int MAX_COLUMN_WIDTH = 200;

	private static final String ATTACHMENT_COLUMN_NAME=Messages.AttributeTable_AttachmentsColumnName;

	private static Color darkRed = null;
	/**
	 * Creates a new attribute table.
	 * 
	 * @param parent parent composite
	 * @param currentCategory category containing attributes to display
	 * @return created attribute table
	 */
	public static TableViewer createAttributeTable( 
			Composite parent, 
			Category currentCategory){
		
		darkRed = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		currentCategory.getAllAttribute(attributes, true);
		
		final TableViewer attributeTable = new TableViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTable.getTable().setLinesVisible(true);
		attributeTable.getTable().setHeaderVisible(true);
		attributeTable.setLabelProvider(new AttributeTableLabelProvider(attributes));
		//TableViewerColumn column  = null;
		GC gc = new GC(parent.getShell());
		try{
			gc.setFont(attributeTable.getTable().getFont());
			for (int i = 0; i < attributes.size(); i ++){
				TableColumn column = new TableColumn(attributeTable.getTable(),SWT.NONE);
				column.setText(SmartUtils.formatStringForLabel(attributes.get(i).getName()));
				column.setResizable(true);
				column.setMoveable(false);
				String name = attributes.get(i).getName();
				int width = gc.textExtent(name == null? "" : name ).x + 20;
				if (width < 60){
					width = 60;
				}
				column.setWidth( width );
			}
			TableColumn column = new TableColumn(attributeTable.getTable(),SWT.NONE);
			column.setText(ATTACHMENT_COLUMN_NAME);
			column.setResizable(true);
			column.setMoveable(false);
			
			int width = gc.textExtent(ATTACHMENT_COLUMN_NAME ).x + 20;
			if (width < 60){
				width = 60;
			}
			column.setWidth( width );
		}finally{
			gc.dispose();
		}
		
		attributeTable.getTable().addListener(SWT.PaintItem, new Listener(){

			@Override
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				Color c = ((ITableColorProvider)attributeTable.getLabelProvider()).getForeground(item.getData(), event.index);
				if (c != null){
					event.gc.setForeground(((ITableColorProvider)attributeTable.getLabelProvider()).getForeground(item.getData(), event.index));
					Rectangle rect = item.getTextBounds(event.index);
					event.gc.drawString(item.getText(event.index), rect.x-1, rect.y); // Out-by-one x pixel!
				}
				
			}
			
		});
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
		if (table.getTable().getShell().isDisposed()){
			return;
		}
		
		GC gc = new GC(table.getTable().getShell());
		try {
			gc.setFont(table.getTable().getFont());

			/* compute width based on text size */
			HashMap<Integer, Integer> columnWidth = new HashMap<Integer, Integer>();
			for (int i = 0; i < table.getTable().getColumnCount(); i++) {
				columnWidth.put(i, 0);
			}
			for (int j = 0; j < table.getTable().getItemCount(); j++) {
				for (int i = 0; i < table.getTable().getColumnCount(); i++) {
					String string = ((ITableLabelProvider) table
							.getLabelProvider()).getColumnText(table.getTable()
							.getItem(j).getData(), i);
					int width = gc.textExtent(string).x + 20;
					if (columnWidth.get(i) < width) {
						columnWidth.put(i, width);
					}
				}
			}
			//update column widths
			//not smaller than current width; not greater than 200
			for (int i = 0; i < table.getTable().getColumnCount(); i++) {
				int width = table.getTable().getColumn(i).getWidth();
				int newWidth = columnWidth.get(i);

				if (newWidth > width) {
					if (newWidth > MAX_COLUMN_WIDTH) {
						newWidth = MAX_COLUMN_WIDTH;
					}
					table.getTable().getColumn(i).setWidth(newWidth);
				}
			}
		} finally {
			gc.dispose();
		}
	}
	
	
	static class AttributeTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider{

		private WaypointObservation editingObservation = null;
		public List<Attribute> columns;
		
		public AttributeTableLabelProvider(List<Attribute> columns){
			this.columns = columns;
		}

		public void setEditingObservation(WaypointObservation ob) {
			this.editingObservation = ob;
		}
		


		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (this.editingObservation != null && this.editingObservation == element){
				return darkRed;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {

			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof WaypointObservation) {
				WaypointObservation observation = (WaypointObservation) element;
				if (columnIndex >= columns.size() ){
					//assume this is the attachments column
					if (observation.getAttachments() != null && observation.getAttachments().size() > 0){
						return MessageFormat.format(Messages.AttributeTable_AttachmentsFileCountLabel, new Object[]{String.valueOf(observation.getAttachments().size())});
					}
					return ""; //$NON-NLS-1$
				}else{
					Attribute attribute = columns.get(columnIndex);
					WaypointObservationAttribute att = observation.findAttribute(attribute);
				
					if (att != null) {
						return att.getAttributeValueAsString(Locale.getDefault());
					}
				}
				
			}
			return ""; //$NON-NLS-1$
		}
		
	}

}
