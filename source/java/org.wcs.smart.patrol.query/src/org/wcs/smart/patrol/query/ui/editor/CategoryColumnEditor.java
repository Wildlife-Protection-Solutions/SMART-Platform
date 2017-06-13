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
package org.wcs.smart.patrol.query.ui.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.common.ui.edit.AbstractQueryColumnEditor;
import org.wcs.smart.query.common.ui.edit.CellEditorFactory;
import org.wcs.smart.query.model.CategoryQueryColumn;

/**
 * Column editor for category columns.
 * 
 * @author Emily
 *
 */
public class CategoryColumnEditor extends AbstractQueryColumnEditor {

	
	private CellEditor categoryCellEditor = null;

	public CategoryColumnEditor (ColumnViewer viewer, CategoryQueryColumn queryColumn, QueryResultsEditor editor ){
		super(viewer, queryColumn, editor);
	}
	
	@Override
	protected Object getValue(Object element) {
		if (element instanceof PatrolQueryResultItem) {
			return ((PatrolQueryResultItem) element).getObservationUuid();
		}
		return null;
	}
	
	@Override
	protected CellEditor getCellEditor(Object element) {
		if (categoryCellEditor == null){
			categoryCellEditor = CellEditorFactory.newObservationEditor((Composite)getViewer().getControl());
		}
		return categoryCellEditor;
	}

}
