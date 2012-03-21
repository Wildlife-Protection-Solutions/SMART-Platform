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
package org.wcs.smart.patrol.internal.ui.observation;

import java.util.ArrayList;

import org.eclipse.core.databinding.observable.list.ObservableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Table for displaying attribute associated with a given category and
 * allowing users to input attribute information
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTable {

	/**
	 * Creates a new attribute table.
	 * 
	 * @param canEdit true if table can be edited
	 * @param parent parent composite
	 * @param currentCategory category containing attributes to display
	 * @param listener listener fired when item in the table is changed, can be null if canEdit is false
	 * @return created attribute table
	 */
	public static TableViewer createAttributeTable(boolean canEdit, 
			Composite parent, 
			Category currentCategory, 
			IAttributeTableChangeListener listener){
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		currentCategory.getAllAttribute(attributes);
		
		final TableViewer attributeTable = new TableViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
	
		attributeTable.setContentProvider(new ObservableListContentProvider());
	
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTable.getTable().setLinesVisible(true);
		attributeTable.getTable().setHeaderVisible(true);
		
		if (canEdit){
			ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(attributeTable) {
				protected boolean isEditorActivationEvent(
						ColumnViewerEditorActivationEvent event) {
					return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
							|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
							|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
							|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
				}
			};
	
			TableViewerEditor.create(attributeTable, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR );
		}
		TableViewerColumn column  = null;
		
		if (canEdit){
			column = new TableViewerColumn(attributeTable,SWT.NONE);
			column.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element) {
					return (((ObservableList)attributeTable.getInput()).indexOf(element) + 1) + ".";
				}
			});
			column.getColumn().setText("");
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(30);
		}
	
		for (int i = 0; i < attributes.size(); i ++){
			column = new TableViewerColumn(attributeTable,SWT.NONE);
			column.setLabelProvider(new AttributeTableLabelProvider(attributes.get(i)));
			column.getColumn().setText(attributes.get(i).getName());
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(100);
			if (canEdit){
				column.setEditingSupport(createEditor(column.getViewer(), attributeTable, attributes.get(i), listener));
			}
		}
		return attributeTable;
	}
	
	/*
	 * Creates editor for given column
	 */
	private static AttributeTableEditingSupport createEditor(ColumnViewer column, TableViewer table, Attribute att, IAttributeTableChangeListener listener){
		

		AttributeTableEditingSupport support = new AttributeTableEditingSupport(column, table, att);
		if (listener != null){
			support.addChangeListener(listener);
		}
		return support;
	}
}
