/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;

/**
 * Field editor specific for Buffer attributes for stations
 * and locations.  Adds custom validation and tooltip to label
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class BufferAttributeFieldEditor extends AttributeFieldEditor {

	public BufferAttributeFieldEditor(Composite parent, AssetAttribute attribute) {
		super(parent, attribute);
	}

	/*
	 * validates and updates control decoration icon/message
	 */
	protected boolean validate(){
		boolean ok  = super.validate();
		if (!ok) return ok;
		
		String msg = null;
		if (txtValue.getText().trim().isEmpty()) {
			msg = Messages.BufferAttributeFieldEditor_BufferRequired;
		}else {
			Double x = Double.parseDouble(txtValue.getText());
			if (x <= 0) {
				msg = Messages.BufferAttributeFieldEditor_BufferMustBePositive;
			}
		}
				
		if (msg != null){
			cd.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR));
			cd.setDescriptionText(msg);
			cd.show();
			return false;
		}
		return true;
	}
	
	protected void createControl(){
		super.createControl();
		lblAttribute.setToolTipText(Messages.BufferAttributeFieldEditor_BufferTooltip);
	}
}
