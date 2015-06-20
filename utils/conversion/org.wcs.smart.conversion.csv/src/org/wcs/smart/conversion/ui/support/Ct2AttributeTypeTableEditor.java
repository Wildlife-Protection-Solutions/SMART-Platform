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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;

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
		MappedAttributeType[] types = MappedAttributeType.values();
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
		if (arg0 instanceof MappedAttribute) {
			MappedAttribute a = (MappedAttribute) arg0;
			return a.getType() != null ? a.getType().ordinal() : 0;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void setValue(Object arg0, Object arg1) {
		if ((arg0 instanceof MappedAttribute) && (arg1 instanceof Integer)) {
			Integer i = (Integer) arg1;
			MappedAttribute a = (MappedAttribute) arg0;
			MappedAttributeType newType = MappedAttributeType.values()[i];
//			if (MappedAttributeType.REF.equals(newType)) {
//				if (!MappedAttributeType.REF.equals(a.getType()) && a.getMappedAttributeValue().isEmpty()) {
//					//switched from non-REF to REF
//					//need to load possible values
//					a.getMappedAttributeValue();
//				}
//			} else if (MappedAttributeType.REF.equals(a.getType())) {
//				//switched from REF to non-REF
//				
//			}
			a.setType(newType);
			if (!Ct2AttributeTypeUtil.canMap(a.getType())) {
				a.setMapTo(null);
				a.setCategoryKey(null);
			}
			getViewer().refresh();
		}
	}

}
