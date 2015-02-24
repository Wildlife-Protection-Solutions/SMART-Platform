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
package org.wcs.smart.ui.internal.startup;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.ui.internal.backup.ImportCaHandler;
import org.wcs.smart.ui.internal.backup.RestoreHandler;

/**
 * Abstract dialog for start-up smart dialogs.
 * <p>
 * Displays a list of actions people can perform without
 * logging into a conservation area such as creating a new CA.
 * </p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class InitializeDialog  extends Dialog {

	public static int RESTART = -2;
	
	private Button opCreateNew;
	private Button opRestore;
	private Button opImport;
	
	protected InitializeDialog(Shell parent) {
		super(parent);
	}

	@Override
	protected void setShellStyle(int arg0){
	    //Use the following not to show the default close X button in the title bar
	    super.setShellStyle(SWT.APPLICATION_MODAL | getDefaultOrientation() | SWT.TITLE);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, Messages.InitializeDialog_Continue_Button, true);
	}
	
	@Override
	protected Point getInitialSize() {
		return getParentShell().getSize();
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (opCreateNew.getSelection()) {
				createConservationArea();
			} else if (opRestore.getSelection()) {
				restoreBackup();
			}else if (opImport.getSelection()){
				importCa();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			super.cancelPressed();
			onCancel();
		}
	}
	
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		super.getShell().setText(getDialogText());
		
		parent = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = gl.horizontalSpacing = gl.marginWidth = gl.marginHeight = 0;
		parent.setLayout(gl);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		DialogHeader dh = new DialogHeader(parent, SWT.NONE);
		dh.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dh.setHeader(getHeaderText());

		Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,true, true));
		gl = new GridLayout(1, false);
		gl.verticalSpacing = 10;
		contents.setLayout(gl);
		
		Label lblNewLabel = new Label(contents, SWT.WRAP);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,true, true));
		lblNewLabel.setText(getMessageText());

		Composite opComp = new Composite(contents, SWT.NONE);
		gl = new GridLayout(1, false);
		gl.marginLeft = 20;
		opComp.setLayout(gl);
		
		opCreateNew = new Button(opComp, SWT.RADIO);
		opCreateNew.setText(Messages.InitializeDialog_CreateCa_Label);
		opCreateNew.setSelection(true);
		

		opRestore = new Button(opComp, SWT.RADIO);
		opRestore.setText(Messages.InitializeDialog_Restore_Label);

		opImport = new Button(opComp, SWT.RADIO);
		opImport.setText(Messages.InitializeDialog_Import_Label);
		
		Label lbl = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,false));
		
		Composite buttonComp = new Composite(parent, SWT.NONE);
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout(2, true);
		gl.horizontalSpacing = 20;
		gl.marginHeight = 10;
		buttonComp.setLayout(gl);
				
		return parent;
	}
	
	/**
	 * Opens the new conservation area wizard.
	 */
	private void createConservationArea() {
		if (SmartStartUp.openCreateNewCaWizard(getParentShell())){
			super.setReturnCode(OK);
			close();
		}
	}
	
	
	/**
	 * Starts the process for restoring a database
	 */
	private void restoreBackup(){
		boolean restart = false;
		RestoreHandler handler = new RestoreHandler();
		try{
			restart = handler.execute(getShell());
			if (restart){
				super.setReturnCode(RESTART);
			}else{
				super.setReturnCode(OK);
			}
			close();
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.InitializeDialog_Error_SystemRestore + ex.getLocalizedMessage(), ex);
		}
	}
	
	/**
	 * Starts the process for restoring a database
	 */
	private void importCa(){
		ImportCaHandler handler = new ImportCaHandler();
		try{
			handler.execute(getShell());
			super.setReturnCode(OK);
			close();
		}catch(Exception ex){
			SmartPlugIn.displayLog(Messages.InitializeDialog_Error_ImportCa + ex.getLocalizedMessage(), ex);
		}
	}
	
	/**
	 * Code executed when cancelled pressed
	 */
	public abstract void onCancel();
	/**
	 * 
	 * @return the text that goes in the dialog header area
	 */
	public abstract String getHeaderText();
	/**
	 * 
	 * @return the message text
	 */
	public abstract String getMessageText();
	/**
	 * 
	 * @return the dialog title text
	 */
	public abstract String getDialogText();
}
