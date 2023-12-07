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
package org.wcs.smart.query.common.ui.edit;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.celleditor.TreeViewerCellEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.ui.CheckBoxDropDownCellEditor;

/**
 * Cell editor for editing attribute query column results.
 * 
 * @author Emily
 *
 */
public class AttributeColumnEditor extends AbstractQueryColumnEditor {

	private ComboBoxViewerCellEditor listCellEditor;
	private TreeViewerCellEditor treeCellEditor;
	private GeometryAttributeDialogCellEditor geometryCellEditor;
	
	private CheckBoxDropDownCellEditor multiListCellEditor;
	
	public AttributeColumnEditor (ColumnViewer viewer, AttributeQueryColumn queryColumn, IQueryEditor editor ){
		super(viewer, queryColumn, editor);
	}

	@Override
	protected Object getValue(Object element) {
		Object value = super.getValue(element);
		if (((AttributeQueryColumn)queryColumn).getAttributeType() == Attribute.AttributeType.TEXT 
				&& value == null) return ""; //$NON-NLS-1$
		
		if (((AttributeQueryColumn)queryColumn).getAttributeType() == Attribute.AttributeType.MLIST) {
			//we need to map these to attribute list items associated with the attribute
			//to work properly with 
			
			if (element instanceof ObservationQueryResultItem) {
				ObservationQueryResultItem i = (ObservationQueryResultItem)element;
				if (i.getObservationUuid() != null) {
					try(Session session = HibernateManager.openSession()){
						WaypointObservation wo = session.get(WaypointObservation.class, i.getObservationUuid());
						for (WaypointObservationAttribute a : wo.getAttributes()) {
							if (a.getAttribute().getKeyId().equalsIgnoreCase(   ((AttributeQueryColumn)queryColumn).getAttributeId() )) {
								Collection<AttributeListItem> items =a.getAttributeListItems().stream().map(e->e.getAttributeListItem()).collect(Collectors.toList());
								items.forEach(e->e.getName());
								value = items;
								break;
							}
						}
					}
				}
			}
			
		}
		return value;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		switch (((AttributeQueryColumn)queryColumn).getAttributeType()) {
		case BOOLEAN:
			return getBooleanCellEditor();
		case DATE:
			return getDateCellEditor();
		case LIST:
			return getListCellEditor();
		case MLIST:
			return getMultiListCellEditor();
		case NUMERIC:
			return getDoubleCellEditor(true);
		case TEXT:
			return getTextCellEditor();
		case TREE:
			return getTreeCellEditor();
		case LINE:
		case POLYGON:
			return getGeometryCellEditor();
				
		}
		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		if (super.canEdit(element)){
			//only edit not null values
			//this is done so that you cannot edit attributes
			//that are not relevant for you current observation category
			//if you want to add an attribute to a category that is missing; then
			//edit the category
			return getValue(element) != null;
		}
		return false;
	}
	
	private CellEditor getListCellEditor(){
		if (listCellEditor == null){
			listCellEditor = CellEditorFactory.newAttributeListCellEditor((Composite) getViewer().getControl(), ((AttributeQueryColumn)queryColumn).getAttributeId());
		}
		return listCellEditor;
	}

	private CellEditor getMultiListCellEditor(){
		if (multiListCellEditor == null){
			multiListCellEditor = CellEditorFactory.newAttributeMultiListCellEditor((Composite) getViewer().getControl(), ((AttributeQueryColumn)queryColumn).getAttributeId());
		}
		return multiListCellEditor;
	}
	
	private CellEditor getTreeCellEditor(){
		if (treeCellEditor == null){
			treeCellEditor = CellEditorFactory.newAttributeTreeCellEditor((Composite) getViewer().getControl(), ((AttributeQueryColumn)queryColumn).getAttributeId());
		}
		return treeCellEditor;
	}
	
	private CellEditor getGeometryCellEditor() {
		if (geometryCellEditor == null) {
			geometryCellEditor = CellEditorFactory.newGeometryCellEditor((Composite) getViewer().getControl(), ((AttributeQueryColumn)queryColumn).getAttributeType());
		}
		return geometryCellEditor;
	}
}
