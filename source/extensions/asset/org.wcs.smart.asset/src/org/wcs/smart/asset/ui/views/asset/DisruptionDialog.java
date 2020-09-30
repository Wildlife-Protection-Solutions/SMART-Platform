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
package org.wcs.smart.asset.ui.views.asset;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for creating/editing deployment disruption events
 * 
 * @author Emily
 *
 */
public class DisruptionDialog extends SmartStyledTitleDialog {

	private AssetDeploymentDisruption disruption;
	private AssetDeployment deployment;
	
	private DateTime dtStartDate;
	private DateTime dtEndDate;
	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text txtComment;
	
	public DisruptionDialog(Shell parent, AssetDeploymentDisruption disruption) {
		super(parent);
		this.disruption = disruption;
		this.deployment = disruption.getAssetDeployment();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public void okPressed() {
		if (!validate()) return;
		
		//update disruption
		LocalDateTime start = SmartUtils.toDateTime(dtStartDate, dtStartTime);
		LocalDateTime end = SmartUtils.toDateTime(dtEndDate, dtEndTime);

		disruption.setComment(txtComment.getText());
		disruption.setStartDate(start);
		disruption.setEndDate(end);
		
		super.okPressed();
	}
	
	
	private boolean validate() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		if (btnOk == null) return false;
		btnOk.setEnabled(false);
		setErrorMessage(null);
		
		boolean overlaps = false;
		
		
		LocalDateTime start = SmartUtils.toDateTime(dtStartDate, dtStartTime);
		LocalDateTime end = SmartUtils.toDateTime(dtEndDate, dtEndTime);
		
		if (start.isAfter(end) || start.isEqual(end)) {
			setErrorMessage(Messages.DisruptionDialog_startEndError);
			return false;
		}
		LocalDateTime dstart = deployment.getStartDate();
		LocalDateTime dend = LocalDateTime.now();
		if (deployment.getEndDate() != null) {
			dend = deployment.getEndDate();
		}
		
		if (start.isAfter(dend) || start.isBefore(dstart) || end.isAfter(dend) || end.isBefore(dstart)) {
			setErrorMessage(Messages.DisruptionDialog_DateOutsideDeployment);
			return false;
		}
		
		for (AssetDeploymentDisruption other: deployment.getDisruptions()) {
			if (other.equals(disruption)) continue;
				
			LocalDateTime startTest = other.getStartDate();
			LocalDateTime endTest =other.getEndDate();
			
			if (!(endTest.isBefore(start) || startTest.isAfter(end))) { 
				overlaps = true;
			}
			if (overlaps) break;
		
		}
		if (overlaps) {
			setErrorMessage(Messages.DisruptionDialog_ExistingDiruptionOverlap);
			return false;
		}
		
		btnOk.setEnabled(true);
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite form = new Composite(parent, SWT.NONE);
		form.setLayout(new GridLayout(2, false));
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(form, SWT.NONE);
		l.setText("Start :"); //$NON-NLS-1$
		
		Composite compstart = new Composite(form, SWT.NONE);
		compstart.setLayout(new GridLayout(2, false));
		((GridLayout)compstart.getLayout()).marginWidth = 0;
		((GridLayout)compstart.getLayout()).marginHeight = 0;
		
		dtStartDate = new DateTime(compstart,  SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM);
		dtStartTime = new DateTime(compstart, SWT.TIME | SWT.MEDIUM);
		dtStartDate.addListener(SWT.Selection, e->validate());
		dtStartTime.addListener(SWT.Selection, e->validate());
		
		if (disruption.getStartDate() != null) {
			SmartUtils.initDateTimeWidget(dtStartDate, disruption.getStartDate().toLocalDate());
			SmartUtils.initDateTimeWidget(dtStartTime, disruption.getStartDate().toLocalTime());
		}else {
			SmartUtils.initDateTimeWidget(dtStartTime, LocalTime.MIN);

		}
		
		l = new Label(form, SWT.NONE);
		l.setText(AssetDeploymentTableColumn.FixedColumn.END_DATE.guiName + ":"); //$NON-NLS-1$
		
		Composite compEndDate = new Composite(form, SWT.NONE);
		compEndDate.setLayout(new GridLayout(2, false));
		((GridLayout)compEndDate.getLayout()).marginWidth = 0;
		((GridLayout)compEndDate.getLayout()).marginHeight = 0;
			
		dtEndDate = new DateTime(compEndDate,  SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM);
		dtEndTime = new DateTime(compEndDate, SWT.TIME | SWT.MEDIUM);
		dtEndDate.addListener(SWT.Selection, e->validate());
		dtEndTime.addListener(SWT.Selection, e->validate());
		
		if (disruption.getEndDate() != null) {
			SmartUtils.initDateTimeWidget(dtEndDate, disruption.getEndDate().toLocalDate());
			SmartUtils.initDateTimeWidget(dtEndTime, disruption.getEndDate().toLocalTime());
		}else {
			SmartUtils.initDateTimeWidget(dtEndTime, LocalTime.MIN);
		}
		
		l = new Label(form, SWT.NONE);
		l.setText(Messages.DisruptionDialog_CommentLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtComment = new Text(form, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (disruption.getComment() != null) {
			txtComment.setText(disruption.getComment());
		}
		txtComment.addListener(SWT.Modify, e->validate());
		
		setTitle(Messages.DisruptionDialog_Title);
		setMessage(Messages.DisruptionDialog_DialogMessage);
		getShell().setText(Messages.DisruptionDialog_Title);
		
		return parent;
	}

}
