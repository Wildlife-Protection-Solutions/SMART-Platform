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
package org.wcs.smart.patrol.internal.ui.observation.field;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.patrol.model.AttributeValidator;


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
		return Double.parseDouble(txt.getText());
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(attribute.getName() + ":");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		txt = new Text(parent, SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txt.getLayoutData()).horizontalIndent = 5;
		txt.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				 final String oldS = txt.getText();
				 
		            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
		            if (newS.length() > 0){
		            	try
		            	{
		            		Float.parseFloat(newS);
		            	}
		            	catch(NumberFormatException ex)
		            	{
		            		e.doit =false;
		            	}
		            }
			}
		});
		txt.addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				Double v = getValue();
				isModified = !( (v== null && originalValue == null) || (v != null && originalValue != null && v == originalValue));
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
		String error = AttributeValidator.validateAttribute(attribute, getValue());
		cd.hide();
		if (error != null){
			cd.setDescriptionText(error);
			cd.show();
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
		txt.setText("");
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
			throw new IllegalStateException("Invalid value");
		}
		this.originalValue = (Double)x;
		if (originalValue == null){
			txt.setText("");
		}else{
			txt.setText(this.originalValue + "");
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
