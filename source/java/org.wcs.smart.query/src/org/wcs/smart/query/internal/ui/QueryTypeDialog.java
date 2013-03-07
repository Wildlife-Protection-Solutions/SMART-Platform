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
package org.wcs.smart.query.internal.ui;

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
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query.QueryType;

/**
 * Dialog for users to select the type of
 * query they want to create.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryTypeDialog extends TitleAreaDialog {

	private QueryType selectedQueryType = null;

	private Button[] btns;
	
	/**
	 * @param parentShell
	 */
	public QueryTypeDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @return the backup file selected by the user
	 */
	public QueryType getSelectedQueryType(){
		return this.selectedQueryType;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.QueryTypeDialog_QueryTypeLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Composite option = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginLeft = 15;
		option.setLayout(gl);
		
		QueryType[] queryTypes = QueryHibernateManager.getInstance().getSupportedQueryTypes();
		btns = new Button[queryTypes.length];
		for (int i = 0; i < queryTypes.length; i ++){
			btns[i] = new Button(option, SWT.RADIO);
			btns[i].setData(queryTypes[i]);
			btns[i].setText(queryTypes[i].getUiName());
			if (i == 0){
				btns[i].setSelection(true);
				selectedQueryType = queryTypes[i];
			}
		}
		
		setTitle(Messages.QueryTypeDialog_Title);
		setMessage(Messages.QueryTypeDialog_Message);
		getShell().setText(Messages.QueryTypeDialog_DialogText);
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.QueryTypeDialog_CreateButton, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			for (int i = 0; i < btns.length; i ++){
				if (btns[i].getSelection()){
					selectedQueryType = (QueryType) btns[i].getData();
					break;
				}
			}
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}


}
