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
package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;

/**
 * Dialog for selecting a sampling unit state.  This allows
 * users to select a new state, but does not update the database.
 * 
 * @author Emily
 *
 */
public class SamplingUnitStateDialog extends TitleAreaDialog{

	private Button btnActive;
	private Button btnInactive;
	
	private SamplingUnit.State state;
	
	public SamplingUnitStateDialog(Shell parentShell) {
		super(parentShell);
	}

	public boolean isResizable(){
		return true;
	}
	
	@Override
	public void okPressed(){
		if (btnActive.getSelection()){
			state = SamplingUnit.State.ACTIVE;
		}else{
			state = SamplingUnit.State.INACTIVE;
		}
		super.okPressed();
	}
	
	public SamplingUnit.State getState(){
		return this.state;
	}
	
	@Override
	public Composite createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		btnActive = new Button(parent, SWT.RADIO);
		btnActive.setText(Messages.StatusComposite_Active);
		btnActive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnActive.setSelection(true);

		btnInactive = new Button(parent, SWT.RADIO);
		btnInactive.setText(Messages.StatusComposite_Inactive);
		btnInactive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		setTitle(Messages.SamplingUnitStateDialog_Title);
		setMessage(Messages.SamplingUnitStateDialog_Message);
		getShell().setText(Messages.SamplingUnitStateDialog_Title);
		return parent;
	}
}
