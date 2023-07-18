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
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeValidator;
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
public class ListAttributeField implements IAttributeField<AttributeListItem> {

	/* attribute fields */
	private Attribute attribute;
	private boolean isModified = false;
	private AttributeListItem originalValue = null;
	
	/* ui fields */
	private TableComboViewer cmbViewer;
	private ControlDecoration cd;
	private Label lbl;
	
	private Collection<Listener> listeners;
	
	/**
	 * Creates a new list field
	 * @param attribute
	 */
	public ListAttributeField(Attribute attribute){
		this.attribute = attribute;
		listeners = new ArrayList<>();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return the list item selected by the user or null if none selected
	 */
	@Override
	public AttributeListItem getValue() {
		if (cmbViewer.getSelection().isEmpty()){
			return null;
		}else{
			IStructuredSelection sel = (IStructuredSelection)cmbViewer.getSelection();
			if (sel.getFirstElement() instanceof AttributeListItem){
				return (AttributeListItem) sel.getFirstElement();
			}else{
				return null;
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (cmbViewer != null) cmbViewer.getControl().setEnabled(enabled);
		if (lbl != null) lbl.setEnabled(enabled);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName())); 
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbViewer = new TableComboViewer(parent, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbViewer.getControl().getLayoutData()).horizontalIndent = 5;
		((GridData)cmbViewer.getControl().getLayoutData()).widthHint = 50;
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());	
		cmbViewer.setLabelProvider(new NamedIconItemLabelProvider(16));
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				AttributeListItem v = getValue();
				isModified = !( (v== null && originalValue == null) || (v != null && originalValue != null && v.equals(originalValue)));
				validate();
				fireModified();
			}
		});
		
		List<Object> items = new ArrayList<Object>();
		items.addAll(this.attribute.getActiveListItems());
		if (!attribute.getIsRequired()){
			items.add(0, ""); //$NON-NLS-1$
		}
		cmbViewer.setInput(items);
		
		cd = new ControlDecoration(cmbViewer.getControl(), SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		
		validate();
		originalValue = null;
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
		cmbViewer.setSelection(null);
		//seems to be required for nebula widget otherwise text field doesn't get cleared
		cmbViewer.getTableCombo().setText(""); //$NON-NLS-1$
		validate();
		originalValue = null;
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
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof AttributeListItem)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		this.originalValue = (AttributeListItem)x;
		if (originalValue == null){
			cmbViewer.setSelection(null);
		}else{			
			if (!((List<Object>)cmbViewer.getInput()).contains(this.originalValue)){
				//then the original value is not in the list; probably not longer active
				//for the purpose of this particular field we wnat to add it
				 ((List<Object>)cmbViewer.getInput()).add(this.originalValue);
				 cmbViewer.refresh();
			}
			cmbViewer.setSelection(new StructuredSelection(this.originalValue));
		}
		validate();
		this.isModified = false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus(){
		cmbViewer.getControl().setFocus();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#dispose()
	 */
	@Override
	public void dispose(){
	}
}
