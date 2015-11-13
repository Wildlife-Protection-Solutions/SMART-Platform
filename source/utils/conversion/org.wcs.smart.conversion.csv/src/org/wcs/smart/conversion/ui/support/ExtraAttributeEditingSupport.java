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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.conversion.model.ExtraAttribute;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

public class ExtraAttributeEditingSupport extends EditingSupport {

	private ComboBoxCellEditor editor;
	List<AttributeType> attributes;
	private SmartAttributeLabelProvider labelProvider;

	public ExtraAttributeEditingSupport(ColumnViewer viewer, List<AttributeType> attributes, SmartAttributeLabelProvider labelProvider) {
		super(viewer);
		this.labelProvider = labelProvider;
		this.attributes = new ArrayList<AttributeType>(attributes.size());
		this.attributes.addAll(attributes);
		editor = new ComboBoxCellEditor(((TableViewer)viewer).getTable(), new String[0], SWT.DROP_DOWN | SWT.READ_ONLY);
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		Collections.sort(this.attributes, new Comparator<AttributeType>() {
			@Override
			public int compare(AttributeType a1, AttributeType a2) {
				return labelProvider.getText(a1).toLowerCase().compareTo(labelProvider.getText(a2).toLowerCase());
			}
		});
		List<String> items = new ArrayList<String>();
		for (AttributeType a : attributes) {
			items.add(labelProvider.getText(a));
		}
		editor.setItems(items.toArray(new String[items.size()]));
		return editor;
	}

	@Override
	protected Object getValue(Object arg0) {
		if (arg0 instanceof ExtraAttribute) {
			ExtraAttribute a = (ExtraAttribute) arg0;
			String key = a.getAttributeKey();
			if (key == null || attributes == null)
				return 0;
			for (int i = 0; i < attributes.size(); i++) {
				if (key.equals(attributes.get(i).getKey()))
					return i;
			}
		}
		return 0;
	}

	@Override
	protected void setValue(Object arg0, Object arg1) {
		if ((arg0 instanceof ExtraAttribute) && (arg1 instanceof Integer)) {
			Integer i = (Integer) arg1;
			ExtraAttribute a = (ExtraAttribute) arg0;
			a.setAttributeKey(attributes.get(i).getKey());
			getViewer().refresh();
		}
	}
	
}
