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
package org.wcs.smart.ui.internal.ca.properties;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.referencing.CRS;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for editing projection information.
 * 
 * @author egouge
 *
 */
public class EditProjectionDialog extends SmartStyledTitleDialog implements Listener{

	
	private Projection toEdit = null;
	private Text txtDef;
	private Text txtName;
	
	/**
	 * 
	 * @param parentShell
	 * @param toEdit the projection option to eidt
	 */
	public EditProjectionDialog(Shell parentShell, Projection toEdit) {
		super(parentShell);
		this.toEdit = toEdit;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		super.getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite pp = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(pp, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.EditProjectionDialog_Name_Label);
		
		txtName = new Text(main, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.setText(toEdit.getName());
		((GridData)txtName.getLayoutData()).widthHint = 350;
		txtName.addListener(SWT.Modify, this);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.EditProjectionDialog_WKT_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		txtDef = new Text(main, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true,2,1);
		gd.heightHint = 250;
		gd.widthHint = 350;
		txtDef.setLayoutData(gd);
		txtDef.setText(toEdit.getDefinition());
		txtDef.addListener(SWT.Modify, this);
		
		txtName.addListener(SWT.Modify, this);
		txtDef.addListener(SWT.Modify, this);
		
		getShell().setText(Messages.EditProjectionDialog_Dialog_Title);
		setTitle(Messages.EditProjectionDialog_Dialog_Title);
		setMessage(Messages.EditProjectionDialog_Dialog_Message);
		
		return main;
	}
	
	
	@Override
	protected void okPressed() {
		if (validate()){
			toEdit.setName(txtName.getText());
			toEdit.setDefinition(txtDef.getText());
			super.okPressed();
		}
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void handleEvent(Event event) {
		validate();
		getButton(IDialogConstants.OK_ID).setEnabled(getErrorMessage() == null);
		
	}
	
	/**
	 * validates input
	 * @return <code>true</code> if validates, <code>false</code>if error
	 */
	private boolean validate(){
		
		if (txtName.getText().length() <=0 || txtName.getText().length() > Projection.MAX_NAME_LENGTH){
			setErrorMessage(
					MessageFormat.format(Messages.EditProjectionDialog_Error_NameLength, new Object[]{1, Projection.MAX_NAME_LENGTH }));
			return false;
		}
		
		if (txtDef.getText().length() <=0 || txtDef.getText().length() > Projection.MAX_DEF_LENGTH){
			setErrorMessage(
					MessageFormat.format(Messages.EditProjectionDialog_Error_DefLength, new Object[]{1, Projection.MAX_DEF_LENGTH }));
			return false;
		}
		
		try{
			CRS.parseWKT(txtDef.getText());
		}catch (Exception ex){
			String err = Messages.EditProjectionDialog_Error_ParsingWKT;
			SmartPlugIn.log(err, ex);
			setErrorMessage(err + ex.getLocalizedMessage());
			return false;
		}
		setErrorMessage(null);
		return true;
	}

}
