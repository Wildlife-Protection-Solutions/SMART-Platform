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
package org.wcs.smart.ct2smart.ui.support;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.ui.DataModelLookup;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartCategoryTableEditor extends EditingSupport {
	
	private CellEditor editor;

	public SmartCategoryTableEditor(TableViewer viewer, DataModelLookup lookup, SmartCategoryLabelProvider labelProvider) {
		super(viewer);
		Table table = viewer.getTable();
		DmTreeContentProvider contentProvider = new DmTreeContentProvider(true, lookup);
		DmTreeDropDownViewer treeEditor = new DmTreeDropDownViewer(table.getShell(), contentProvider, labelProvider);
		treeEditor.setInput(lookup.getDataModel());
		editor = new DmTreeCellEditor(table, treeEditor);
	}

	@Override
	protected boolean canEdit(Object arg0) {
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			return Ct2AttributeTypeUtil.canMap(a.getType());
		}
		return false;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		return editor;
	}

	@Override
	protected Object getValue(Object arg0) {
		// TODO Auto-generated method stub
		return "asd";
	}

	@Override
	protected void setValue(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

}
