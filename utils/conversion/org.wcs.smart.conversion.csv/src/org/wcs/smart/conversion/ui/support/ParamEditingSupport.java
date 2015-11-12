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
package org.wcs.smart.conversion.ui.support;

import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Table;
import org.wcs.smart.conversion.model.Param;

/**
 * Editing support for {@link Param} values.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ParamEditingSupport extends EditingSupport {

	private TextCellEditor textEditor;
	
	private List<Param> paramList;

	/**
	 * @param viewer
	 */
	public ParamEditingSupport(TableViewer viewer, List<Param> paramList) {
		super(viewer);
		Table table = viewer.getTable();
		textEditor = new TextCellEditor(table);
		this.paramList = paramList;
	}
	
	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object obj) {
		return textEditor;
	}

	@Override
	protected Object getValue(Object obj) {
		if (obj instanceof Param) {
			Param p = (Param) obj;
			return p.getVal() != null ? p.getVal() : ""; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	protected void setValue(Object obj, Object value) {
		if (obj instanceof Param) {
			Param p = (Param) obj;
			String v = value != null ? value.toString() : ""; //$NON-NLS-1$
			p.setVal(v);
			if (v.trim().isEmpty()) {
				paramList.remove(p);
			} else if (!paramList.contains(p)) {
				paramList.add(p);
			}
		}
		getViewer().refresh();
	}

}
