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
package org.wcs.smart.patrol.internal.ui.observation;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.patrol.internal.ui.editor.DoubleCellEditor;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Create a cell editor for a given attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTableEditingSupport extends EditingSupport {

	private Attribute attribute;
	private CellEditor editor;
	

	private Collection<IAttributeTableChangeListener> updateListeners = new ArrayList<IAttributeTableChangeListener>();

	/**
	 * Creates new editing support 
	 * @param viewer column being edited
	 * @param table table being edited
	 * @param attribute attribute being modified
	 */
	public AttributeTableEditingSupport(ColumnViewer viewer, TableViewer table,
			Attribute attribute) {
		super(viewer);
		this.attribute = attribute;
		
		//create the required cell editor
		AttributeType type = attribute.getType();
		if (type == AttributeType.BOOLEAN) {
			ComboBoxViewerCellEditor e = new ComboBoxViewerCellEditor(
					table.getTable(), SWT.READ_ONLY | SWT.DROP_DOWN);
			e.setActivationStyle(ComboBoxViewerCellEditor.DROP_DOWN_ON_TRAVERSE_ACTIVATION |  ComboBoxViewerCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
			e.setContentProvider(ArrayContentProvider.getInstance());
			e.setLabelProvider(new LabelProvider());
			if (attribute.getIsRequired()) {
				e.setInput(new String[] { Attribute.BOOLEAN_TRUE_LABEL, Attribute.BOOLEAN_FALSE_LABEL });
			} else {
				e.setInput(new String[] { "", Attribute.BOOLEAN_TRUE_LABEL, Attribute.BOOLEAN_FALSE_LABEL });
			}
			editor = e;
		} else if (type == AttributeType.LIST) {
			ComboBoxViewerCellEditor e = new ComboBoxViewerCellEditor(
					table.getTable(), SWT.READ_ONLY | SWT.DROP_DOWN);
			e.setContentProvider(ArrayContentProvider.getInstance());
			e.setActivationStyle(ComboBoxViewerCellEditor.DROP_DOWN_ON_TRAVERSE_ACTIVATION |  ComboBoxViewerCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
			//display only active items
			ArrayList<AttributeListItem> enabledItems = new ArrayList<AttributeListItem>();
			for (AttributeListItem it : attribute.getAttributeList()){
				if (it.getIsActive()){
					enabledItems.add(it);
				}
			}
			if (!attribute.getIsRequired()){
				//add blank item (for optional)
				enabledItems.add(0, new AttributeListItem());
			}
			e.setInput(enabledItems.toArray());
			e.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof AttributeListItem) {
						return ((AttributeListItem) element).getName();
					}
					return super.getText(element);
				}
			});
			editor = e;
		} else if (type == AttributeType.TEXT) {
			editor = new TextCellEditor(table.getTable());
		} else if (type == AttributeType.TREE) {
			editor = new AttributeTreeCellEditor(table.getTable());
		} else if (type == AttributeType.NUMERIC) {
			editor = new DoubleCellEditor(table.getTable(), true);
		}
	}

	/**
	 * @see
	 * org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.Object)
	 */
	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	/**
	 * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
	 */
	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	/**
	 * 
	 * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
	 */
	@Override
	protected Object getValue(Object element) {
		AttributeType type = attribute.getType();
		WaypointObservation observation = (WaypointObservation) element;
		WaypointObservationAttribute att = observation.findAttribute(attribute);
		if (att == null) {
			att = new WaypointObservationAttribute();
			att.setAttribute(attribute);
			att.setObservation(observation);
			if (observation.getAttributes() == null) {
				observation.setAttributes(new ArrayList<WaypointObservationAttribute>());
			}
			observation.getAttributes().add(att);
		}

		if (type == AttributeType.BOOLEAN) {
			if (att.getNumberValue() == null){
				return "";
			}else if (att.getNumberValue() > 0) {
				return Attribute.BOOLEAN_TRUE_LABEL;
			} else {
				return Attribute.BOOLEAN_FALSE_LABEL;
			}
			// return att.getNumberValue();
		} else if (type == AttributeType.LIST) {
			return att.getAttributeListItem();
		} else if (type == AttributeType.TEXT) {
			if (att.getStringValue() == null) {
				return "";
			}
			return att.getStringValue();
		} else if (type == AttributeType.TREE) {
			return att;
		} else if (type == AttributeType.NUMERIC) {
			return att.getNumberValue();
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	protected void setValue(Object element, Object value) {
		AttributeType type = attribute.getType();
		WaypointObservation observation = (WaypointObservation) element;
		WaypointObservationAttribute att = observation.findAttribute(attribute);
		if (att == null) {
			att = new WaypointObservationAttribute();
			att.setAttribute(attribute);
			att.setObservation(observation);
			if (observation.getAttributes() == null) {
				observation
						.setAttributes(new ArrayList<WaypointObservationAttribute>());
			}
			observation.getAttributes().add(att);
		}

		if (type == AttributeType.BOOLEAN) {
			if (value == null || ((String) value).trim().length() == 0) {
				att.setNumberValue(null);
			} else if (((String) value).equals(Attribute.BOOLEAN_TRUE_LABEL)) {
				att.setNumberValue(1.0);
			} else {
				att.setNumberValue(0.0);
			}
		} else if (type == AttributeType.LIST) {
			if (value == null || ((AttributeListItem) value).getUuid() == null) {
				att.setAttributeListItem(null);
			} else {
				att.setAttributeListItem((AttributeListItem) value);
			}
		} else if (type == AttributeType.TEXT) {
			if (value == null || ((String)value).trim().length() == 0){
				att.setStringValue(null);
			} else {
				att.setStringValue((String) value);
			}
		} else if (type == AttributeType.TREE) {
			//updated in cell editor
		} else if (type == AttributeType.NUMERIC) {
			if (value == null){
//				//remove as it has been set to null
				att.setNumberValue(null);
			}else{
				att.setNumberValue((Double) value);
			}
		}
		
		fireChangeListeners();
	}
	
	/**
	 * @param listener listener fired when data in table is changed
	 */
	public void addChangeListener(IAttributeTableChangeListener listener){
		updateListeners.add(listener);
	}
	

	/**
	 * Fires all registered listeners
	 */
	private void fireChangeListeners(){
		for (IAttributeTableChangeListener listener : updateListeners){
			listener.updated();
		}
	}
}
