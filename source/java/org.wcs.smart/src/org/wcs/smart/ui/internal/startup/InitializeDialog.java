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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.ui.internal.backup.RestoreHandler;

/**
 * Abstract dialog for start-up smart dialogs.
 * <p>
 * Displays a list of items people can perform without
 * logging into a conservation area.
 * </p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class InitializeDialog  extends Dialog {

	public final static int CANCEL = 0;
	public final static int OK = 1;
	
	protected int result;
	protected Shell shell;
	

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public InitializeDialog(Shell parent) {
		super(parent, SWT.APPLICATION_MODAL);
		setText(getDialogText());

	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public int open() {
		createContents();
		
		Rectangle bds = shell.getDisplay().getMonitors()[0].getBounds();
		Point p = shell.getSize();

		int nLeft = (bds.width - p.x) / 2;
		int nTop = (bds.height - p.y) / 2;

		shell.setBounds(nLeft, nTop, p.x, p.y);
		
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.BORDER | SWT.TITLE);
		shell.setSize(448, 324);
		shell.setText(getText());
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = gl.horizontalSpacing = gl.marginWidth = gl.marginHeight = 0;
		shell.setLayout(gl);

		
		DialogHeader dh = new DialogHeader(shell, SWT.NONE);
		dh.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dh.setHeader(getHeaderText());

		Composite contents = new Composite(shell, SWT.NONE);
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
		
		final Button opCreateNew = new Button(opComp, SWT.RADIO);
		opCreateNew.setText("Create a New Conservation Area");
		opCreateNew.setSelection(true);
		

		final Button opRestore = new Button(opComp, SWT.RADIO);
		opRestore.setText("Restore a Backup");

		Label lbl = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,false));
		
		Composite buttonComp = new Composite(shell, SWT.NONE);
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gl = new GridLayout(2, true);
		gl.horizontalSpacing = 20;
		gl.marginHeight = 10;
		buttonComp.setLayout(gl);
		
		
		
		Button btnCancel = new Button(buttonComp, SWT.NONE);
		btnCancel.setText("Cancel");
		btnCancel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent c) {
				result = CANCEL;
				shell.dispose();
				onCancel();
			}
		});

		Button btnContinue = new Button(buttonComp, SWT.NONE);
		btnContinue.setText("Continue");
		btnContinue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		btnContinue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result = OK;
				if (opCreateNew.getSelection()) {
					createConservationArea();
				} else if (opRestore.getSelection()) {
					restoreBackup();
				} else {
					MessageDialog
							.openError(shell, "Error",
									"Invalid option selected.  Please select one of the above options.");
				}
			}

		});
		
		int b1 = btnContinue.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int b2 = btnCancel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		b1 = (int) ( Math.max(b1, b2) * 1.2 );
		((GridData)btnContinue.getLayoutData()).widthHint = b1;
		((GridData)btnCancel.getLayoutData()).widthHint = b1;
	}
	
	/**
	 * Opens the new conservation area wizard.
	 */
	private void createConservationArea() {
		if (SmartStartUp.openCreateNewCaWizard(shell)){
			//hide this page
			shell.dispose();
		}
	}
	
	
	/**
	 * Starts the process for restoring a database
	 */
	private void restoreBackup(){
		RestoreHandler handler = new RestoreHandler();
		try{
			handler.execute(shell);
			shell.dispose();
		}catch (Exception ex){
			MessageDialog.openError(shell, "Restore Error", "Error occurred during system restore.  You may have to restore the system manually. \n\n" + ex.getMessage());
			SmartPlugIn.log("Error during restore", ex);
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