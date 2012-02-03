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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.startup.SmartStartUp;

/**
 * Displays the advanced dialog associated
 * with the login page. 
 * 
 * @author Emily Gouge
 *
 */
public class StartUpAdvancedDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public StartUpAdvancedDialog(Shell parent) {
		super(parent, SWT.APPLICATION_MODAL);
		setText("Welcome To Smart");

	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
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
		shell.setLayout(new FormLayout());

		Composite composite_1 = new Composite(shell, SWT.NONE);
		FormData fd_composite_1 = new FormData();
		fd_composite_1.left = new FormAttachment(0);
		fd_composite_1.right = new FormAttachment(0, 442);
		fd_composite_1.top = new FormAttachment(0);
		composite_1.setLayoutData(fd_composite_1);
		composite_1.setLayout(new RowLayout(SWT.HORIZONTAL));

		DialogHeader dh = new DialogHeader(composite_1, SWT.BORDER);
		dh.setLayoutData(new RowData(438, SWT.DEFAULT));
		dh.setHeader("SMART Advanced Options");
		
		Composite composite = new Composite(shell, SWT.NONE);
		FormData fd_composite = new FormData();
		fd_composite.top = new FormAttachment(composite_1, 17);
		fd_composite.right = new FormAttachment(100, -67);
		fd_composite.left = new FormAttachment(0, 77);
		composite.setLayoutData(fd_composite);

		Label lblNewLabel = new Label(composite, SWT.WRAP);
		lblNewLabel.setLocation(0, 0);
		lblNewLabel.setSize(276, 34);
		lblNewLabel.setText("Would you like to:");

		final Button opCreateNew = new Button(composite, SWT.RADIO);
		opCreateNew.setLocation(32, 40);
		opCreateNew.setSize(256, 16);
		opCreateNew.setText("Create a New Conservation Area");

		
		final Button opRestore = new Button(composite, SWT.RADIO);
		opRestore.setLocation(32, 62);
		opRestore.setSize(256, 16);
		opRestore.setText("Restore a Backup");


		Button btnContinue = new Button(shell, SWT.NONE);
		fd_composite.bottom = new FormAttachment(100, -92);
		FormData fd_btnContinue = new FormData();
		fd_btnContinue.bottom = new FormAttachment(100, -10);
		fd_btnContinue.right = new FormAttachment(100, -145);
		btnContinue.setLayoutData(fd_btnContinue);
		btnContinue.setText("Continue");

		
		btnContinue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (opCreateNew.getSelection()){
					createConservationArea();					
				}else if (opRestore.getSelection()){
					MessageDialog.openInformation(StartUpAdvancedDialog.this.getParent(), "Not Yet Completed", "Not yet implemented");
				}else{
					MessageDialog.openInformation(StartUpAdvancedDialog.this.getParent(), "Error", "Invalid selection.  Please select one of the above options.");
				}
			}
		});
		
		Button btnCancel = new Button(shell, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.top = new FormAttachment(btnContinue, 0, SWT.TOP);
		fd_btnCancel.right = new FormAttachment(btnContinue, -36);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent c){
				shell.dispose();
			}
		});

		Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fd_label = new FormData();
		fd_label.top = new FormAttachment(btnContinue, -11, SWT.TOP);
		fd_label.right = new FormAttachment(composite_1, 0, SWT.RIGHT);
		fd_label.bottom = new FormAttachment(btnContinue, -9);
		fd_label.left = new FormAttachment(composite_1, 0, SWT.LEFT);
		label.setLayoutData(fd_label);
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
}