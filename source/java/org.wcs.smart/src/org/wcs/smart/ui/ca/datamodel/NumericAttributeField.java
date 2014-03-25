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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;


/**
 * Attribute field for numeric attributes.
 * <p>
 * Represented as a text box where users can enter 
 * valid numbers only.
 * </p>
 * @author egouge
 *
 */
public class NumericAttributeField implements IAttributeField<Double> {

	
	private Attribute attribute;
	private boolean isModified;
	private Double originalValue;
	
	private Text txt;
	private ControlDecoration cd;
	
	
	/**
	 * Creates a new numeric attribute field
	 */
	public NumericAttributeField(Attribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return the double value entered by the user or null if no value entered
	 */
	@Override
	public Double getValue() {
		if (txt.getText().trim().isEmpty()){
			return null;
		}
		try{
			return Double.parseDouble(txt.getText());
		}catch (Exception ex){
			return null;
		}
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		txt = new Text(parent, SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txt.getLayoutData()).horizontalIndent = 5;

		txt.addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				Double v = getValue();
				isModified = !( (v== null && originalValue == null) || (v != null && originalValue != null && v.doubleValue() == originalValue.doubleValue()));
				validate();
			}});
		
		cd = new ControlDecoration(txt, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());

		validate();
		this.isModified = false;
		this.originalValue = null;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#validate()
	 */
	@Override
	public String validate() {
		String error = null;
		if (!txt.getText().trim().isEmpty()){
			try{
				Double.parseDouble(txt.getText());
			}catch (Exception ex){
				error = Messages.NumericAttributeField_InvalidNumericAttribute;
			}
		}
		if (error == null){
			error = AttributeValidator.validateAttribute(attribute, getValue());
		}
		
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
		this.originalValue = null;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#isModified()
	 */
	@Override
	public boolean isModified() {
		return this.isModified;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setValue(java.lang.Object)
	 * @param x the observation attribute Double value
	 */
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof Double)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		this.originalValue = (Double)x;
		if (originalValue == null){
			txt.setText(""); //$NON-NLS-1$
		}else{
			txt.setText(String.valueOf(this.originalValue));
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
