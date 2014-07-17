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

import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.wcs.smart.ct2smart.matcher.model.ExtraAttribute;
import org.wcs.smart.ct2smart.ui.DataModelLookup;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeValueEditingSupport extends EditingSupport {

	private static final String[] BOOL_ITEMS = new String[] {"False", "True"};

	private DataModelLookup lookup;
	private SmartAttributeValueLabelProvider labelProvider;
	
	private ComboBoxCellEditor cbEditor;
	private TextCellEditor textEditor;
	private AttributeTreeDropDownViewer treeViewer;
	private TreeCellEditor treeEditor;
	
	private AttributeTreeKeyLookup treeNodeLookup;


	/**
	 * @param viewer
	 */
	public SmartAttributeValueEditingSupport(TableViewer viewer, DataModelLookup lookup, SmartAttributeValueLabelProvider labelProvider) {
		super(viewer);
		this.lookup = lookup;
		this.labelProvider = labelProvider;
		Table table = viewer.getTable();
		cbEditor = new ComboBoxCellEditor(table, new String[0], SWT.DROP_DOWN | SWT.READ_ONLY);
		textEditor = new TextCellEditor(table);

		AttributeTreeContentProvider contentProvider = new AttributeTreeContentProvider(true);
		treeViewer = new AttributeTreeDropDownViewer(table.getShell(), contentProvider, labelProvider);
		treeEditor = new TreeCellEditor(table, treeViewer);
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		if (arg0 instanceof ExtraAttribute) {
			ExtraAttribute a = (ExtraAttribute) arg0;
			return getAttributeEditor(a.getAttributeKey());
		}
		return null;
	}

	protected CellEditor getAttributeEditor(String attributeKey) {
		AttributeType a = lookup.getAttribute(attributeKey);
		if (a == null)
			return null;
		
		String type = a.getType();
		if ("LIST".equals(type)) { //$NON-NLS-1$
			List<ListNode> values = a.getValues();
			String[] items = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {
				items[i] = labelProvider.getText(values.get(i));
			}
			cbEditor.setItems(items);
			return cbEditor;
			
			
		} else if ("TREE".equals(type)) { //$NON-NLS-1$
			treeNodeLookup = new AttributeTreeKeyLookup(a);
			treeViewer.setInput(a);
			return treeEditor;
			
		} else if ("NUMERIC".equals(type)) { //$NON-NLS-1$
			return textEditor;
			
		} else if ("TEXT".equals(type)) { //$NON-NLS-1$
			return textEditor;
			
		} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
			cbEditor.setItems(BOOL_ITEMS);
			return cbEditor;
		}
		return null;
	}
	
	@Override
	protected Object getValue(Object arg0) {
		if (arg0 instanceof ExtraAttribute) {
			ExtraAttribute a = (ExtraAttribute) arg0;
			return getEditorValue(a.getAttributeKey(), a.getValueKey());
		}
		return 0;
	}

	protected Object getEditorValue(String attributeKey, String valueKey) {
		AttributeType a = lookup.getAttribute(attributeKey);
		String type = a.getType();
		if ("LIST".equals(type)) { //$NON-NLS-1$
			if (valueKey == null)
				return 0;
			List<ListNode> values = a.getValues();
			for (int i = 0; i < values.size(); i++) {
				if (valueKey.equals(values.get(i).getKey()))
					return i;
			}
			return 0;
			
		} else if ("TREE".equals(type)) { //$NON-NLS-1$
			return treeNodeLookup.getTreeNode(valueKey);
			
		} else if ("NUMERIC".equals(type)) { //$NON-NLS-1$
			return valueKey != null ? valueKey : ""; //$NON-NLS-1$
			
		} else if ("TEXT".equals(type)) { //$NON-NLS-1$
			return valueKey != null ? valueKey : ""; //$NON-NLS-1$
			
		} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
			return BOOL_ITEMS[1].equals(valueKey) ? 1 : 0;
		}
		return null;
	}

	@Override
	protected void setValue(Object arg0, Object arg1) {
		if (arg0 instanceof ExtraAttribute) {
			ExtraAttribute a = (ExtraAttribute) arg0;
			a.setValueKey(getModelValue(a.getAttributeKey(), arg1));
		}
		getViewer().refresh();
	}

	protected String getModelValue(String attributeKey, Object editorValue) {
		AttributeType a = lookup.getAttribute(attributeKey);
		String type = a.getType();
		if ("LIST".equals(type)) { //$NON-NLS-1$
			Integer i = (Integer) editorValue;
			return a.getValues().get(i).getKey();
			
		} else if ("TREE".equals(type)) { //$NON-NLS-1$
			TreeNodeType nt = (TreeNodeType) editorValue;
			if (nt != null) {
				return treeNodeLookup.getFullKey(nt);
			}
			
		} else if ("NUMERIC".equals(type)) { //$NON-NLS-1$
			return (String) editorValue;
			
		} else if ("TEXT".equals(type)) { //$NON-NLS-1$
			return (String) editorValue;
			
		} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
			Integer i = (Integer) editorValue;
			return BOOL_ITEMS[i];
		}
		return null;
		
	}
}
