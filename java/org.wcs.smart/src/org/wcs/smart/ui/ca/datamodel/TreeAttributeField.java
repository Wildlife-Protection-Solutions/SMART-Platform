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

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.TreeEditorField;
import org.wcs.smart.util.SmartUtils;

/**
 * A attribute field for tree attributes.
 * <p>
 * This attribute is represented as a text box and a separate
 * drop-down tree object that is displayed when users start typing
 * in the text box.
 * </p>
 * 
 * @author egouge
 *
 */
public class TreeAttributeField extends TreeEditorField implements IAttributeField<AttributeTreeNode> {
	
	private Attribute attribute;

	
	/**
	 * Creates a new attribute tree field
	 * @param attribute
	 */
	public TreeAttributeField(Attribute attribute){
		super();
		this.attribute = attribute;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		super.createComposite(parent);
		
		super.setAttribute(this.attribute);
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#validate()
	 */
	@Override
	public String validate() {
		String error = null;
		if (txtText.getText().length() > 0 && txtText.getData() == null){
			error = MessageFormat.format(Messages.TreeAttributeField_InvalidTreeValue, new Object[]{ attribute.getName()});
		}else{
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

}
