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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * A table for displaying observations for a specific 
 * category.  This is a read-only table.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTable {

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
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		currentCategory.getAllAttribute(attributes, true);
		
		final TableViewer attributeTable = new TableViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTable.getTable().setLinesVisible(true);
		attributeTable.getTable().setHeaderVisible(true);
		
		TableViewerColumn column  = null;
		GC gc = new GC(Display.getDefault().getActiveShell());
		try{
			gc.setFont(attributeTable.getTable().getFont());
			for (int i = 0; i < attributes.size(); i ++){
				column = new TableViewerColumn(attributeTable,SWT.NONE);
				column.setLabelProvider(new AttributeTableLabelProvider(attributes.get(i)));
				column.getColumn().setText(attributes.get(i).getName());
				column.getColumn().setResizable(true);
				column.getColumn().setMoveable(false);
				
				int width = gc.textExtent(attributes.get(i).getName() ).x + 20;
				if (width < 60) width = 60;
				column.getColumn().setWidth( width );
			}
		}finally{
			gc.dispose();
		}
		return attributeTable;
	}
	
	 static class AttributeTableLabelProvider extends ColumnLabelProvider {

			private Attribute attribute;

			public AttributeTableLabelProvider(Attribute attribute) {
				this.attribute = attribute;
			}

			public String getText(Object element) {
				if (element instanceof WaypointObservation){
					WaypointObservation observation = (WaypointObservation) element;					
					WaypointObservationAttribute att = observation.findAttribute(attribute);
					if (att != null){
						if (this.attribute.getType() == AttributeType.TEXT){
							if (att.getStringValue() == null){
								return "";
							}
							return att.getStringValue();
						}else if (this.attribute.getType() == AttributeType.BOOLEAN ){
							if (att.getNumberValue() == null){
								return "";
							}
							if (att.getNumberValue() < 0.5){
								return Attribute.BOOLEAN_FALSE_LABEL;
							}else{
								return Attribute.BOOLEAN_TRUE_LABEL;
							}
						}else if (this.attribute.getType() == AttributeType.NUMERIC){
							if (att.getNumberValue() == null){
								return "";
							}
							return String.valueOf(att.getNumberValue());
						}else if (this.attribute.getType() == AttributeType.LIST){
							if (att.getAttributeListItem() == null){
								return "";
							}
							return att.getAttributeListItem().getName();
						}else if (this.attribute.getType() == AttributeType.TREE){
							if (att.getAttributeTreeNode() == null){
								return "";
							}
							return att.getAttributeTreeNode().getName();
						}
					}
					return "";
				}
				return super.getText(element);
			}
	 }

}
