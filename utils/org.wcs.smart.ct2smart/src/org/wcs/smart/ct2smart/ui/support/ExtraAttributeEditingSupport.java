package org.wcs.smart.ct2smart.ui.support;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.ct2smart.matcher.model.ExtraAttribute;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

public class ExtraAttributeEditingSupport extends EditingSupport {

	private ComboBoxCellEditor editor;
	List<AttributeType> attributes;
	private SmartAttributeLabelProvider labelProvider;

	public ExtraAttributeEditingSupport(ColumnViewer viewer, List<AttributeType> attributes, SmartAttributeLabelProvider labelProvider) {
		super(viewer);
		this.attributes = attributes;
		this.labelProvider = labelProvider;
		editor = new ComboBoxCellEditor(((TableViewer)viewer).getTable(), new String[0], SWT.DROP_DOWN);
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
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
