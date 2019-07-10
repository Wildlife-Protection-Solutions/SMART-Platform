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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.Attribute;

/**
 * Field for editing observations attribute.
 * 
 * @author egouge
 *
 * @param <T> the attribute value representation
 */
public interface IAttributeField<T> {

	/**
	 * @return the value selected by the user
	 */
	public T getValue();
	
	/**
	 * Creates the ui for editing the given attribute field
	 * @param parent
	 */
	public void createComposite(Composite parent);
	
	/**
	 * Validates the value selected by the user for the given attribute.
	 * <p>This validates a value is entered if required and 
	 * validates it meets the qa parameters provided
	 * by the data model (ex. for numeric attributes validates
	 * that the value is between the min and max entered by the user.)
	 * 
	 * @return error string or null if no error
	 */
	public String validate();
	
	/**
	 * Fired when the valid is modified
	 * @param listener
	 */
	public void addModifyListener(Listener listener);
	
	/**
	 * @return the attribute represented by the field
	 */
	public Attribute getAttribute();
	
	/**
	 * Clears the ui fields
	 */
	public void clear();
	
	/**
	 * @return <code>true</code> if the value has been modified; <code>false</code>
	 * if not.
	 */
	public boolean isModified();
	
	/**
	 * Updates the ui field to display
	 * the given value
	 * @param x
	 */
	public void setValue(Object x);
	
	/**
	 * Sets the focus
	 */
	public void setFocus();
	
	/**
	 * Disposes the control
	 */
	public void dispose();
}
