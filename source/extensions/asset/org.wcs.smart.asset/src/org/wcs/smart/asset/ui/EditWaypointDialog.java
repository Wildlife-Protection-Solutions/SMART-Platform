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
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class EditWaypointDialog extends TitleAreaDialog{

	private Waypoint toUpdate;
	
	private Text txtId;
	private Text txtComment;
	private DateTime dDate;
	private DateTime dTime;
		
	private boolean showId;
	private boolean showComment;
	
	public EditWaypointDialog(Shell parentShell, Waypoint toUpdate) {
		this(parentShell, toUpdate, true, true);
	}
	
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
			MessageDialog.openWarning(getShell(), "Error", "Cannot save changes until all attributes are valid.");
			return;
		}
		
		Date newDateTime =  SmartUtils.combineDateTime(SmartUtils.getDate(dDate), SmartUtils.getTime(dTime));
		if (Math.abs(toUpdate.getDateTime().getTime() - newDateTime.getTime()) > 1000 * 60 * 60 * 24) {
			if (!MessageDialog.openQuestion(getShell(), "Edit", "The new date/time is more than 1 day away from the existing date/time.  Are you sure you want to continue?")) return;
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
					setErrorMessage("Waypoint Id must be numeric greater than 0");
					return false;
				}
			}catch (Exception ex) {
				setErrorMessage("Waypoint Id must be numeric greater than 0");
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
			l.setText("ID:");
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
			txtId = new Text(form, SWT.BORDER);
			
			txtId.setText(String.valueOf(toUpdate.getId()));
			txtId.addListener(SWT.Modify, e->validate());
			txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		
		Label l = new Label(form, SWT.NONE);
		l.setText("Date/Time:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dDate = new DateTime(form, SWT.DROP_DOWN | SWT.MEDIUM | SWT.DATE);
		SmartUtils.initDateDateTimeWidget(dDate, toUpdate.getDateTime());
		dDate.addListener(SWT.Selection, e->validate());
		
		dTime = new DateTime(form, SWT.DROP_DOWN | SWT.TIME);
		SmartUtils.initTimeDateTimeWidget(dTime, toUpdate.getDateTime());
		dTime.addListener(SWT.Selection, e->validate());
		
		if (showComment) {
			l = new Label(form, SWT.NONE);
			l.setText("Comment:");
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			
			txtComment = new Text(form, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
			txtComment.setText(toUpdate.getComment() == null ? "" : toUpdate.getComment());
			txtComment.addListener(SWT.Modify,  e->validate());
			txtComment.setTextLimit(Waypoint.COMMENT_MAX_LENGTH);
			txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			((GridData)txtComment.getLayoutData()).widthHint = 120;
			((GridData)txtComment.getLayoutData()).heightHint = 300;
			
		}
		setTitle("Waypoint Attributes");
		setMessage("Select the waypoint date/time");
		getShell().setText("Waypoints Attributes");
		
		return parent;
	}
	
	
	
	@Override
	public boolean isResizable() {
		return true;
	}
}

