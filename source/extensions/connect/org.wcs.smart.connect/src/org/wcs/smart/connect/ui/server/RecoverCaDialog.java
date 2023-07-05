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

package org.wcs.smart.connect.ui.server;

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
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog to display Conservation Area / SMART Connect recover options
 * 
 * @author Emily
 *
 */
public class RecoverCaDialog extends SmartStyledTitleDialog {

	public enum Option{
		REPLACE, RECOVER;
	}
	private Button opReplace, opRecover;
	private Button btnApplyNew;
	
	
	private boolean applyNew = false;
	private Option op = null;
	
	protected RecoverCaDialog(Shell parent) {
		super(parent);
	}
	
	public Option getOption() {
		return this.op;
	}
	public boolean getApplyNew() {
		return this.applyNew;
	}
	
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		p.y = 650;
		return p;
	}
	
	@Override
	protected void okPressed() {
		
		if (btnApplyNew.getSelection()) applyNew = true;
		if (opReplace.getSelection()) {
			op = Option.REPLACE;
		}else if (opRecover.getSelection()) {
			op = Option.RECOVER;
		}
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default		
		createButton(parent, IDialogConstants.OK_ID, "Perform Recovery", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	

	@Override
	public Control createDialogArea(Composite parent){
		int widthHint = 200;
		
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite outer = new Composite(composite, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		SmartUiUtils.createHeaderLabel(outer, "Connect Download Option");

		Composite main = new Composite(outer, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		opRecover = new Button(main, SWT.RADIO | SWT.WRAP);
		opRecover.setText("Download modifications only");
		opRecover.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)opRecover.getLayoutData()).widthHint = widthHint;
		
		Label lblTemp = new Label(main, SWT.WRAP);
		lblTemp.setText("This option will download the entire database and modified (or added) files from SMART Connect and merge this data with the local Conservation Area data. This will be faster and transfer less data.");
		lblTemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblTemp.getLayoutData()).horizontalIndent = 15;
		((GridData)lblTemp.getLayoutData()).widthHint = widthHint;

		
		new Label(main, SWT.NONE);
		
		opReplace = new Button (main, SWT.RADIO | SWT.WRAP);
		opReplace.setText("Download complete Conservation Area");
		opReplace.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)opReplace.getLayoutData()).widthHint = widthHint;
		
		lblTemp = new Label(main, SWT.WRAP);
		lblTemp.setText("This option will download the entire database and filestore from SMART Connect and replace the local Conservation Area with this downloaded data. It is the slowest option and will transfer the all files, but will guarantee your Conservation Area is identical to Connect. Use this option if you have tried downloading only modifications and you still having Connect sync errors.");
		lblTemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblTemp.getLayoutData()).horizontalIndent = 15;
		((GridData)lblTemp.getLayoutData()).widthHint = widthHint;

		new Label(main, SWT.NONE);

		Composite buttonComp = new Composite(composite, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		
		SmartUiUtils.createHeaderLabel(outer, "Data Recovery Options");
		
		Composite recoverOps = new Composite(outer, SWT.NONE);
		recoverOps.setLayout(new GridLayout());
		recoverOps.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnApplyNew = new Button(recoverOps, SWT.CHECK | SWT.WRAP);
		btnApplyNew.setText("Retain new data and re-apply to database after local Conservation Area updated from Connect" );
		btnApplyNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)btnApplyNew.getLayoutData()).widthHint = widthHint;
		
		Label lblRecoverDetails = new Label(recoverOps, SWT.WRAP);
		lblRecoverDetails.setText("This option will set aside any data that was been added to your Conservation Area but not yet synchronized to Connect, then once the local copy has been reset to match Connect, the new data will be re-added to your local database. The feature does not retain modifications to data that had already been synchronzied to Connect. Examples of new data include new patrols, new employees, new queries, and new observations added to an existing waypoint. Examples of modifications (which are not retained) include updating a patrol's type, changing employee details, editing waypoint locations, and updating observation attributes.");
		lblRecoverDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblRecoverDetails.getLayoutData()).horizontalIndent = 15;
		((GridData)lblRecoverDetails.getLayoutData()).widthHint = widthHint;

		
		getShell().setText("Conservation Area Recovery Options");
		setTitle("Conservation Area Recover Options");
		setMessage("Options for recovering data when Connect data conflict or other data issues arise.");
		
		return composite; 
	}

}
