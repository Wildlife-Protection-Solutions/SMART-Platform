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
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeEditingSupport extends EditingSupport {

	private ComboBoxCellEditor editor;
	private Map<Ct2AttributeType, String[]> itemsMap;
	private SmartAttributeLabelProvider labelProvider;
	
	/**
	 * @param viewer
	 */
	public SmartAttributeEditingSupport(ColumnViewer viewer, List<AttributeType> attributes, SmartAttributeLabelProvider labelProvider) {
		super(viewer);
		editor = new ComboBoxCellEditor(((TableViewer)viewer).getTable(), new String[0], SWT.DROP_DOWN | SWT.READ_ONLY);
		this.labelProvider = labelProvider;
		//building values map
		Map<Ct2AttributeType, List<String>> tempMap = new HashMap<Ct2AttributeType, List<String>>();
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
			List<String> items = tempMap.get(ctType);
			if (items == null) {
				items = new ArrayList<String>();
				tempMap.put(ctType, items);
			}
			items.add(a.getKey());
		}
		itemsMap = new HashMap<Ct2AttributeType, String[]>();
		for (Ct2AttributeType key : tempMap.keySet()) {
			List<String> list = tempMap.get(key);
			itemsMap.put(key, list.toArray(new String[list.size()]));
		}
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
		if (arg0 instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) arg0;
			String[] items = itemsMap.get(a.getType());
			if (items != null) {
				String[] names = new String[items.length];
				for (int i = 0; i < items.length; i++) {
					names[i] = labelProvider.getText(items[i]);
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
			String[] items = itemsMap.get(a.getType());
			if (v == null || items == null)
				return 0;
			for (int i = 0; i < items.length; i++) {
				if (v.equals(items[i]))
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
			String[] items = itemsMap.get(a.getType());
			a.setMapTo(items[i]);
			getViewer().refresh();
		}
	}

}
