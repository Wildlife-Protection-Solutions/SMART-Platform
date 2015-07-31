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


import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Attribute field for modifying boolean observation
 * attributes.
 * <p>
 * Display two or three radio buttons depending on the required
 * state of the attribute.
 * </p>
 * @author egouge
 *
 */
public class BooleanAttributeField implements IAttributeField<Boolean> {
	
	/* attribute info */
	private Attribute attribute;
	private boolean isModified = false;
	private Boolean originalValue = null;
	
	/* ui fields */
	private Button btnYes;
	private Button btnNo;
	private Button btnUndefined;
	private ControlDecoration cd;
	
	SelectionListener validateListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Boolean v = getValue();
			isModified = !( (v== null && originalValue == null) || (v != null && originalValue != null && v == originalValue));
			validate();
		}
	};
	
	/**
	 * Creates a new boolean attribute field
	 * @param attribute must be boolean type attribute
	 */
	public BooleanAttributeField(Attribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return <code>true</code> if yes selected, <code>false</code> if no selected, and null if
	 * undefined.
	 */
	@Override
	public Boolean getValue() {
		if (btnYes.getSelection()){
			return true;
		}else if (btnNo.getSelection()){
			return false;
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)comp.getLayoutData()).horizontalIndent = 5;
		GridLayout gl = new GridLayout(3, false);
		gl.verticalSpacing = gl.marginHeight = 0;
		comp.setLayout(gl);
		
		
		btnYes = new Button(comp, SWT.RADIO);
		btnYes.setText(LabelConstants.BOOLEAN_TRUE_LABEL);
		btnYes.addSelectionListener(validateListener);
		
		btnNo = new Button(comp, SWT.RADIO);
		btnNo.setText(LabelConstants.BOOLEAN_FALSE_LABEL);
		btnNo.addSelectionListener(validateListener);
		
		if (!attribute.getIsRequired()){
			btnUndefined = new Button(comp, SWT.RADIO);
			btnUndefined.setText(Messages.BooleanAttributeField_UnderfinedBooleanOption);
			btnUndefined.setSelection(true);
			btnUndefined.addSelectionListener(validateListener);
		}
		
		cd = new ControlDecoration(comp, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		validate();
	
		isModified = false;
		originalValue = null;
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
		btnNo.setSelection(false);
		btnYes.setSelection(false);
		if (btnUndefined != null){
			btnUndefined.setSelection(true);
		}
		validate();
		isModified = false;
		originalValue = null;
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
	 * @param x must be Double (< 0.5 represents false, >= 0.5 represents true)
	 */
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof Double)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		if (x == null){
			this.originalValue = null;
			btnYes.setSelection(false);
			btnNo.setSelection(false);
			if (btnUndefined != null){
				btnUndefined.setSelection(true);
			}
		}else{
			if ((Double)x < 0.5){
				this.originalValue = false;
			}else{
				this.originalValue = true;
			}
			btnYes.setSelection(this.originalValue);
			btnNo.setSelection(!this.originalValue);
			if (btnUndefined != null){
				btnUndefined.setSelection(false);
			}
		}
		validate();
		this.isModified = false;
		
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus(){
		if (btnYes.getSelection()){
			btnYes.setFocus();
			return;
		}
		if (btnNo.getSelection()){
			btnNo.setFocus();
			return;
		}
		if (btnUndefined != null){
			btnUndefined.setFocus();
			return;
		}
		btnYes.setFocus();
		return;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#dispose()
	 */
	public void dispose(){
	}
}
