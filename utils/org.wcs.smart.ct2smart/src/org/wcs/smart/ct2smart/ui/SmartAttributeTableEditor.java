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
package org.wcs.smart.ct2smart.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeTableEditor extends EditingSupport {

	private ComboBoxCellEditor editor;
	private Map<Ct2AttributeType, List<AttributeType>> itemsMap;
	private SmartAttributeLabelProvider labelProvider;
	
	/**
	 * @param viewer
	 */
	public SmartAttributeTableEditor(ColumnViewer viewer, List<AttributeType> attributes, SmartAttributeLabelProvider labelProvider) {
		super(viewer);
		editor = new ComboBoxCellEditor(((TableViewer)viewer).getTable(), new String[0], SWT.DROP_DOWN);
		this.labelProvider = labelProvider;
		//building values map
		itemsMap = new HashMap<Ct2AttributeType, List<AttributeType>>();
		for (AttributeType a : attributes) {
			String type = a.getType();
			Ct2AttributeType ctType = Ct2AttributeType.IGNORE;
			if ("LIST".equals(type) || "TREE".equals(type)) { //$NON-NLS-1$ //$NON-NLS-2$
				ctType = Ct2AttributeType.REF;
			} else if ("NUMERIC".equals(type)) { //$NON-NLS-1$
				ctType = Ct2AttributeType.NUMERIC;
			} else if ("TEXT".equals(type)) { //$NON-NLS-1$
				ctType = Ct2AttributeType.TEXT;
			} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
				ctType = Ct2AttributeType.BOOL;
			}
			List<AttributeType> items = itemsMap.get(ctType);
			if (items == null) {
				items = new ArrayList<AttributeType>();
				itemsMap.put(ctType, items);
			}
			items.add(a);
		}
	}

	@Override
	protected boolean canEdit(Object arg0) {
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			switch (a.getType()) {
				case TEXT:
				case NUMERIC:
				case BOOL:
				case REF:
					return true;
				default:
					return false;
			}
		}
		return false;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			List<AttributeType> items = itemsMap.get(a.getType());
			if (items != null) {
				String[] names = new String[items.size()];
				for (int i = 0; i < items.size(); i++) {
					names[i] = labelProvider.getText(items.get(i));
				}
				editor.setItems(names);
			} else {
				editor.setItems(new String[0]);
			}
		} else {
			editor.setItems(new String[0]);
		}
		return editor;
	}

	@Override
	protected Object getValue(Object arg0) {
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			String v = a.getMapTo();
			List<AttributeType> items = itemsMap.get(a.getType());
			if (v == null || items == null)
				return 0;
			for (int i = 0; i < items.size(); i++) {
				if (v.equals(items.get(i).getKey()))
					return i;
			}
		}
		return 0;
	}

	@Override
	protected void setValue(Object arg0, Object arg1) {
		if ((arg0 instanceof Ct2Attribute) && (arg1 instanceof Integer)) {
			Integer i = (Integer) arg1;
			Ct2Attribute a = (Ct2Attribute) arg0;
			List<AttributeType> items = itemsMap.get(a.getType());
			a.setMapTo(items.get(i).getKey());
			getViewer().refresh();
		}
	}

}
