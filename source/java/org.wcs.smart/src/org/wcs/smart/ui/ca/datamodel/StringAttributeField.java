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

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.common.control.MultiLineText;
import org.wcs.smart.util.SmartUtils;

/**
 * String attribute field for Stringg type attributes.
 * <p>Represented as a text field where users can enter
 * free-form text.
 * </p>
 * @author egouge
 *
 */
public class StringAttributeField implements IAttributeField<String>{

	private Attribute attribute;
	private boolean isModified = false;
	private String originalValue = null;
	
	private Label lbl;
	private MultiLineText txt;
	private ControlDecoration cd;
	
	private Collection<Listener> listeners;
	
	/**
	 * creates a new string attribute field.
	 * @param attribute
	 */
	public StringAttributeField(Attribute attribute){
		this.attribute = attribute;
		listeners = new ArrayList<>();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return the string value entered or null if value entered
	 */
	@Override
	public String getValue() {
		if (txt.getText().trim().isEmpty()){
			return null;
		}
		return txt.getText().trim();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (txt != null) txt.setEnabled(enabled);
		if (lbl != null) lbl.setEnabled(enabled);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		((GridData)lbl.getLayoutData()).verticalIndent = 2;
		
		txt = new MultiLineText(parent);
		txt.setTextLimit(Attribute.STRING_ATTRIBUTE_MAX_LENGTH);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txt.getLayoutData()).horizontalIndent = 5;
		txt.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				isModified = (!txt.getText().equals(originalValue));
				validate();
				fireModified();
			}});
		
		cd = new ControlDecoration(txt, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());

		validate();
		this.isModified = false;
		this.originalValue = ""; //$NON-NLS-1$
	}

	@Override
	public void addResizeListener(Listener l) {
		txt.addListener(SWT.Resize, l);
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
		txt.setText(""); //$NON-NLS-1$
		validate();
		this.isModified = false;
		this.originalValue = ""; //$NON-NLS-1$
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
	 * @param x the initial string value
	 */
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof String)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		this.originalValue = (String)x;
		if (originalValue == null){
			txt.setText(""); //$NON-NLS-1$
		}else{
			txt.setText(originalValue);
		}
		validate();
		this.isModified = false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus(){
		txt.setFocus();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#dispose()
	 */
	@Override
	public void dispose(){
	}
}
