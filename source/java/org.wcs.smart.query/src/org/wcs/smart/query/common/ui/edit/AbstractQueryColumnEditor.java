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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.celleditor.DateCellEditor;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.ui.editor.IQueryEditor;

/**
 * Abstract class to support editing in query results table.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryColumnEditor extends EditingSupport {

	private IntegerCellEditor integerCellEditor = null;
	private DoubleCellEditor doubleCellEditor = null;
	private TextCellEditor textCellEdit = null;
	private TimeCellEditor timeCellEditor = null;
	private DateCellEditor dateCellEditor = null;;
	private ComboBoxViewerCellEditor booleanCellEditor = null;;

	protected QueryColumn queryColumn;
	protected IQueryEditor editor;
	
	public AbstractQueryColumnEditor (ColumnViewer viewer, QueryColumn queryColumn, IQueryEditor editor ){
		super(viewer);
		this.queryColumn = queryColumn;
		this.editor = editor;
	}
	
	@Override
	protected void setValue(Object element, Object value) {
		if (!(element instanceof IResultItem)) return;
		Object resultSet = editor.getQueryProxy().getQuery().getCachedResults();
		if (! (resultSet instanceof IUpdateableResultSet)) return;
		IUpdateableResultSet results = (IUpdateableResultSet) resultSet;
		if (!results.canUpdate(((IResultItem)element).getClass())) return;
		
		boolean wasUpdated = false;
		try{
			wasUpdated = results.update(queryColumn, (IResultItem)element, value);
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.AbstractQueryColumnEditor_UpdateError + ex.getMessage(), ex);
			return;
		}
		
		if (wasUpdated){
			editor.refreshResults();
		}
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof IResultItem) {
			Object value = queryColumn.getValue((IResultItem)element);
			if (value == null && queryColumn.getType() == ColumnType.STRING){
				return ""; //$NON-NLS-1$
			}
			return value;
		}
		return null;
	}

	@Override
	protected abstract CellEditor getCellEditor(Object element);

	@Override
	protected boolean canEdit(Object element) {
		if (editor.getQueryProxy().getQuery().getCachedResults() instanceof IUpdateableResultSet && 
				element instanceof IResultItem &&
				((IUpdateableResultSet)editor.getQueryProxy().getQuery().getCachedResults()).canUpdate(((IResultItem)element).getClass())){
			return true;
		}
		return false;
	}

	protected CellEditor getIntegerCellEditor() {
		if (integerCellEditor == null) {
			integerCellEditor = CellEditorFactory.newIntegerCellEditor((Composite) getViewer().getControl());
		}
		return integerCellEditor;
	}

	protected CellEditor getDoubleCellEditor() {
		if (doubleCellEditor == null) {
			doubleCellEditor = CellEditorFactory.newDoubleCellEditor((Composite) getViewer().getControl());
		}
		return doubleCellEditor;
	}

	protected CellEditor getTextCellEditor() {
		if (textCellEdit == null) {
			textCellEdit = CellEditorFactory.newTextCellEditor((Composite) getViewer().getControl());
		}
		return textCellEdit;
	}

	protected CellEditor getTimeCellEditor() {
		if (timeCellEditor == null) {
			timeCellEditor = CellEditorFactory.newTimeCellEditor((Composite) getViewer().getControl());
		}
		return timeCellEditor;
	}
	
	protected CellEditor getDateCellEditor() {
		if (dateCellEditor == null) {
			dateCellEditor = CellEditorFactory.newDateCellEditor((Composite) getViewer().getControl());
		}
		return dateCellEditor;
	}
	
	protected CellEditor getBooleanCellEditor() {
		if (booleanCellEditor == null) {
			booleanCellEditor = CellEditorFactory.newBooleanCellEditor((Composite) getViewer().getControl());
		}
		return booleanCellEditor;
	}

	protected CellEditor getBooleanCellEditor(LabelProvider lblProvider) {
		if (booleanCellEditor == null) {
			booleanCellEditor = CellEditorFactory.newBooleanCellEditor((Composite) getViewer().getControl(), lblProvider);
		}
		return booleanCellEditor;
	}
}
