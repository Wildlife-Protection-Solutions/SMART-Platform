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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.internal.Messages;

/**
 * Dialog for selecting import option for sampling unit
 * attributes.
 * 
 * @author Emily
 *
 */
public class ImportOptionDialog extends TitleAreaDialog{

	private Button opNew;
	private Button opFields;
	
	private boolean isNew;
	public ImportOptionDialog(Shell parentShell) {
		super(parentShell);
		
	}
	
	public boolean importNew(){
		return this.isNew;
	}
	
	@Override
	public void okPressed(){
		isNew = opNew.getSelection();
		super.okPressed();
	}
	
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(IDialogConstants.NEXT_LABEL);
	}
	@Override
	public Control createDialogArea(Composite parent){
		Composite comp = (Composite) super.createDialogArea(parent);
		
		Composite c = new Composite(comp, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.ImportOptionDialog_ActionOpLabel);
		
		Composite c2 = new Composite(c, SWT.NONE);
		c2.setLayout(new GridLayout());
		((GridLayout)c2.getLayout()).marginWidth = 20;
		
		opNew = new Button(c2, SWT.RADIO);
		opNew.setSelection(true);
		opNew.setText(Messages.ImportOptionDialog_NewLabel);
		
		opFields = new Button(c2, SWT.RADIO);
		opFields.setSelection(false);
		opFields.setText(Messages.ImportOptionDialog_AttributeLabel);
		
		setTitle(Messages.ImportOptionDialog_Title);
		getShell().setText(Messages.ImportOptionDialog_Title);
		setMessage(Messages.ImportOptionDialog_Message);
		
		return comp;
	}

}
