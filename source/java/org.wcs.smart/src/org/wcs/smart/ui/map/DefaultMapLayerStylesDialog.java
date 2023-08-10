/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ui.map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuring default styles for various map layers.
 *  
 * @author Emily
 * @since 8.0
 *
 */
public class DefaultMapLayerStylesDialog extends SmartStyledTitleDialog {

	private DefaultMapLayerStylesComposite layerComp;
	
	/**
	 * 
	 * @param parentShell
	 * @param current current projection or null if does not exist
	 */
	public DefaultMapLayerStylesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x, 500);
	}
	 
	@Override
	protected void okPressed(){
		try {
			layerComp.save();
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite)super.createDialogArea(parent);
		 
		 layerComp = new DefaultMapLayerStylesComposite(composite, SWT.NONE);
		 layerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		 layerComp.addListener(SWT.Modify, e->{
			 getButton(IDialogConstants.OK_ID).setEnabled(true);
		 });
		 getShell().setText("Default Map Layer Styles");
		 setTitle("Default Map Layer Styles");
		 setMessage("Configure the default styles for specific map layers");
		 return composite;
	 }
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
