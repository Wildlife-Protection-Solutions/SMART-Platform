/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui;

import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for collecting the id, comment and date/time associated with
 * a waypoint.  This dialog does not update the database, only the object
 * provided. 
 * @author Emily
 *
 */
public class EditWaypointDialog extends TitleAreaDialog{

	private Waypoint toUpdate;
	
	private Text txtId;
	private Text txtComment;
	private DateTime dDate;
	private DateTime dTime;
		
	private boolean showId;
	private boolean showComment;
	
	/**
	 * Create a new dialog that allows users to edit id, comment, date and time
	 * @param parentShell
	 * @param toUpdate the waypoint to update
	 */
	public EditWaypointDialog(Shell parentShell, Waypoint toUpdate) {
		this(parentShell, toUpdate, true, true);
	}
	
	/**
	 * Create a new dialog.
	 * 
	 * @param parentShell
	 * @param toUpdate waypoint to update
	 * @param showId true if users should be able to edit the waypoint id
	 * @param showComment true if users should be able to edit the waypoint comment
	 */
	public EditWaypointDialog(Shell parentShell, Waypoint toUpdate, boolean showId, boolean showComment) {
		super(parentShell);
		this.toUpdate = toUpdate;
		this.showId = showId;
		this.showComment = showComment;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	public void okPressed() {
		if (!validate()) {
			MessageDialog.openWarning(getShell(), Messages.EditWaypointDialog_ErrorTitle, Messages.EditWaypointDialog_ErrorMessage);
			return;
		}
		
		Date newDateTime =  SmartUtils.combineDateTime(SmartUtils.getDate(dDate), SmartUtils.getTime(dTime));
		if (Math.abs(toUpdate.getDateTime().getTime() - newDateTime.getTime()) > 1000 * 60 * 60 * 24) {
			if (!MessageDialog.openQuestion(getShell(), Messages.EditWaypointDialog_EditTitle, Messages.EditWaypointDialog_EditMessage)) return;
		}
		toUpdate.setDateTime(newDateTime);
		
		if (showId) {
			Integer id = Integer.parseInt(txtId.getText().trim());
			toUpdate.setId(id);
		}
		
		if (showComment) {
			if (txtComment.getText().trim().isEmpty()) {
				toUpdate.setComment(null);
			}else {
				toUpdate.setComment(txtComment.getText().trim());
			}
		}
		
		super.okPressed();
	}
	
	private boolean validate() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		btnOk.setEnabled(false);
		setErrorMessage(null);
		
		if (showId) {
			try {
				int wpid = Integer.parseInt(txtId.getText().trim());
				if (wpid < 0) {
					setErrorMessage(Messages.EditWaypointDialog_InvalidWpId);
					return false;
				}
			}catch (Exception ex) {
				setErrorMessage(Messages.EditWaypointDialog_InvalidWpId);
				return false;
			}
		}
		btnOk.setEnabled(true);
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite form = new Composite(parent, SWT.NONE);
		form.setLayout(new GridLayout(3, false));
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (showId) {
			Label l = new Label(form, SWT.NONE);
			l.setText(Messages.EditWaypointDialog_IdLabel);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
			txtId = new Text(form, SWT.BORDER);
			
			txtId.setText(String.valueOf(toUpdate.getId()));
			txtId.addListener(SWT.Modify, e->validate());
			txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		
		Label l = new Label(form, SWT.NONE);
		l.setText(Messages.EditWaypointDialog_DateTimeLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dDate = new DateTime(form, SWT.DROP_DOWN | SWT.MEDIUM | SWT.DATE);
		SmartUtils.initDateDateTimeWidget(dDate, toUpdate.getDateTime());
		dDate.addListener(SWT.Selection, e->validate());
		
		dTime = new DateTime(form, SWT.DROP_DOWN | SWT.TIME);
		SmartUtils.initTimeDateTimeWidget(dTime, toUpdate.getDateTime());
		dTime.addListener(SWT.Selection, e->validate());
		
		if (showComment) {
			l = new Label(form, SWT.NONE);
			l.setText(Messages.EditWaypointDialog_CommentLabel);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			
			txtComment = new Text(form, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
			txtComment.setText(toUpdate.getComment() == null ? "" : toUpdate.getComment()); //$NON-NLS-1$
			txtComment.addListener(SWT.Modify,  e->validate());
			txtComment.setTextLimit(Waypoint.COMMENT_MAX_LENGTH);
			txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			((GridData)txtComment.getLayoutData()).widthHint = 120;
			((GridData)txtComment.getLayoutData()).heightHint = 300;
			
		}
		setTitle(Messages.EditWaypointDialog_Title);
		setMessage(Messages.EditWaypointDialog_Message);
		getShell().setText(Messages.EditWaypointDialog_Title);
		
		return parent;
	}
	
	
	
	@Override
	public boolean isResizable() {
		return true;
	}
}

