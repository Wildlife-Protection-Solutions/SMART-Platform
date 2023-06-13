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
package org.wcs.smart.ui.ca.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.util.SmartUtils;


/**
 * Attribute field for list attributes.
 * <p>
 * Displays a combobox to users for 
 * selecting the attribute value.  For optional attributes
 * a blank option is added to the combobox.
 * <p>
 * @author egouge
 *
 */
public class MultiListAttributeField implements IAttributeField<Collection<AttributeListItem>> {

	/* attribute fields */
	private Attribute attribute;
	private boolean isModified = false;
	
	/* ui fields */
	private CheckBoxDropDown cmbViewer;
	private ControlDecoration cd;
	private Label lbl;

	private Collection<Listener> listeners;
	
	/**
	 * Creates a new list field
	 * @param attribute
	 */
	public MultiListAttributeField(Attribute attribute){
		this.attribute = attribute;
		listeners = new ArrayList<>();
	}
	
	/**
	 * @return a collection of attribute list items selected by the user
	 */
	@Override
	public Collection<AttributeListItem> getValue() {
		Collection<?> selection = cmbViewer.getCheckObjects();
		
		List<AttributeListItem> items = new ArrayList<>();
		for (Object x  : selection) {
			if (x instanceof AttributeListItem) items.add((AttributeListItem)x);
		}
		if(items.isEmpty()) return null;
		return items;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (cmbViewer != null) cmbViewer.setEnabled(enabled);
		if (lbl != null) lbl.setEnabled(enabled);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbViewer = new CheckBoxDropDown(parent);
		cmbViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbViewer.getLayoutData()).horizontalIndent = 5;
		((GridData)cmbViewer.getLayoutData()).widthHint = 50;
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());	
		cmbViewer.setLabelProvider(new NamedIconItemLabelProvider(16));
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				isModified = true;
				validate();
				fireModified();
			}
		});
		
		List<AttributeListItem> items = new ArrayList<>(this.attribute.getActiveListItems());
		cmbViewer.setInput(items);
		
		cd = new ControlDecoration(cmbViewer, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		
		validate();
		isModified = false;
		
	}
	
	/**
	 * Fired when the valid is modified
	 * @param listener
	 */
	public void addModifyListener(Listener listener) {
		this.listeners.add(listener);
	}
	
	protected void fireModified() {
		Event evt = new Event();
		for (Listener l : listeners)l.handleEvent(evt);
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#validate()
	 */
	@Override
	public String validate() {
		String error = AttributeValidator.validateAttribute(attribute, getValue());
		if (error != null){
			cd.setDescriptionText(error);
			cd.show();
		}else{
			cd.hide();
		}
		return error;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getAttribute()
	 */
	@Override
	public Attribute getAttribute() {
		return this.attribute;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#clear()
	 */
	@Override
	public void clear() {
		cmbViewer.setValue(Collections.emptyList());
		validate();
		isModified = false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#isModified()
	 */
	@Override
	public boolean isModified(){
		return this.isModified;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setValue(java.lang.Object)
	 * @param x the observation AttributeListItem value
	 */
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof Collection)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		
		List<AttributeListItem> originalValues = new ArrayList<>();
		for (Object item : ((Collection<?>)x)) {
			if (item instanceof AttributeListItem) {
				originalValues.add((AttributeListItem)item);
			}else {
				throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
			}
		}

		cmbViewer.setValue(originalValues);
		validate();
		this.isModified = false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus(){
		cmbViewer.setFocus();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#dispose()
	 */
	@Override
	public void dispose(){
	}
}
