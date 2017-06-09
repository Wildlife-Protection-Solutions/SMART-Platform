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
package org.wcs.smart.observation.query.ui;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.AttributeQueryColumn;

/**
 * Cell editor for editing attribute query column results.
 * 
 * @author Emily
 *
 */
public class AttributeColumnEditor extends AbstractQueryColumnEditor {

	private ComboBoxViewerCellEditor listCellEditor;
	
	public AttributeColumnEditor (ColumnViewer viewer, AttributeQueryColumn queryColumn, QueryResultsEditor editor ){
		super(viewer, queryColumn, editor);
	}

	@Override
	protected Object getValue(Object element) {
		Object value = super.getValue(element);
		if (((AttributeQueryColumn)queryColumn).getAttributeType() == Attribute.AttributeType.TEXT && value == null) return ""; //$NON-NLS-1$
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
		case NUMERIC:
			return getDoubleCellEditor();
		case TEXT:
			return getTextCellEditor();
		case TREE:
			//TODO: a tree attribute editor
			break;
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

}
