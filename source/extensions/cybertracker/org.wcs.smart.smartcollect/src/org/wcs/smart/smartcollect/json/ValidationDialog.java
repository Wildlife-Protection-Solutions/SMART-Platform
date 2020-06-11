/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.json;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.json.SmartCollectDataProcessor.ProcessingOption;
import org.wcs.smart.ui.SmartStyledDialog;

/**
 * SMARTCollect user validation dialog.  Displayed to the user when
 * a collect user has not been validated and desktop user has to decide
 * how to continue with the data.
 * 
 * @author Emily
 *
 */
public class ValidationDialog extends SmartStyledDialog {

	private String users;
	private SmartCollectDataProcessor.ProcessingOption option;
	
	public ValidationDialog(Shell parent, String users) {
		super(parent);
		this.users = users;
		option = ProcessingOption.CANCEL;
	}
	
	public ProcessingOption getSelectedOption() {
		return this.option;
	}

	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite) super.createDialogArea(parent);
		 composite.setLayout(new GridLayout());
		 ((GridLayout)composite.getLayout()).marginTop = 20;
		 ((GridLayout)composite.getLayout()).marginLeft= 20;
		 ((GridLayout)composite.getLayout()).marginRight = 20;
		 
		 Label l = new Label(composite, SWT.WRAP);
		 l.setText(MessageFormat.format(Messages.ValidationDialog_UserValidationMessage, users));
		 l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		 ((GridData)l.getLayoutData()).widthHint = 400;
		 
		 Composite buttonComp = new Composite(composite, SWT.NONE);
		 buttonComp.setLayout(new GridLayout());
		 buttonComp.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		 
		 Button btnLoadAnyways = new Button(buttonComp, SWT.PUSH);
		 btnLoadAnyways.setText(Messages.ValidationDialog_LoadOp);
		 btnLoadAnyways.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnLoadAnyways.setToolTipText(Messages.ValidationDialog_LoadOpTooltip);
		 btnLoadAnyways.addListener(SWT.Selection, e->{
			 option = ProcessingOption.LOADDATA;
			 okPressed();
		 });
		 
		 Button btnValidateAndLoad = new Button(buttonComp, SWT.PUSH);
		 btnValidateAndLoad.setText(Messages.ValidationDialog_AcceptLoadOp);
		 btnValidateAndLoad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnValidateAndLoad.setToolTipText(Messages.ValidationDialog_AcceptLoadOpTooltip);
		 btnValidateAndLoad.addListener(SWT.Selection, e->{
			 option = ProcessingOption.ACCEPTANDLOAD;
			 okPressed();
		 });
		
		 
		 Button btnBlacklistSkip = new Button(buttonComp, SWT.PUSH);
		 btnBlacklistSkip.setText(Messages.ValidationDialog_BlacklistDiscardOp);
		 btnBlacklistSkip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnBlacklistSkip.setToolTipText(Messages.ValidationDialog_BlacklistDiscardOpTooltip);
		 btnBlacklistSkip.addListener(SWT.Selection, e->{
			 option = ProcessingOption.BLACKLISTANDDISCARD;
			 okPressed();
		 });
		 
		 Button btnSkip = new Button(buttonComp, SWT.PUSH);
		 btnSkip.setText(Messages.ValidationDialog_DiscardOp);
		 btnSkip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnSkip.setToolTipText(Messages.ValidationDialog_DiscardOpTooltip);
		 btnSkip.addListener(SWT.Selection, e->{
			 option = ProcessingOption.DISCARD;
			 okPressed();
		 });
		 
		 Button btnRequeue = new Button(buttonComp, SWT.PUSH);
		 btnRequeue.setText(Messages.ValidationDialog_ValidateOp);
		 btnRequeue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnRequeue.setToolTipText(Messages.ValidationDialog_ValidateOpTooltip);
		 btnRequeue.addListener(SWT.Selection, e->{
			 option = ProcessingOption.VERIFYREQUEUE;
			 okPressed();
		 });
		 
		 Button btnCancel = new Button(buttonComp, SWT.PUSH);
		 btnCancel.setText(Messages.ValidationDialog_CancelOp);
		 btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnCancel.setToolTipText(Messages.ValidationDialog_CancelOpTooltip);
		 btnCancel.addListener(SWT.Selection, e->{
			 option = ProcessingOption.CANCEL;
			 okPressed();
		 });
		 getShell().setText(Messages.ValidationDialog_Title);
		 return composite;
	 }
	
	protected void createButtonsForButtonBar(Composite parent) {
	}
}
