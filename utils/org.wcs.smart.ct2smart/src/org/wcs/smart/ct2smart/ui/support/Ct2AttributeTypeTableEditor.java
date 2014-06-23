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
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class Ct2AttributeTypeTableEditor extends EditingSupport {

	private ComboBoxCellEditor editor;
	
	/**
	 * @param viewer
	 */
	public Ct2AttributeTypeTableEditor(TableViewer viewer) {
		super(viewer);
		Ct2AttributeType[] types = Ct2AttributeType.values();
		String[] items = new String[types.length];
		Ct2AttributeTypeLabelProvider labelProvider = new Ct2AttributeTypeLabelProvider();
		for (int i = 0; i < types.length; i++) {
			items[i] = labelProvider.getText(types[i]);
		}
		editor = new ComboBoxCellEditor(viewer.getTable(), items, SWT.DROP_DOWN);
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		return editor;
	}

	@Override
	protected Object getValue(Object arg0) {
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			return a.getType().ordinal();
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void setValue(Object arg0, Object arg1) {
		if ((arg0 instanceof Ct2Attribute) && (arg1 instanceof Integer)) {
			Integer i = (Integer) arg1;
			Ct2Attribute a = (Ct2Attribute) arg0;
			a.setType(Ct2AttributeType.values()[i]);
			if (!Ct2AttributeTypeUtil.canMap(a.getType())) {
				a.setMapTo(null);
				a.setCategoryKey(null);
			}
			getViewer().refresh();
		}
	}

}
