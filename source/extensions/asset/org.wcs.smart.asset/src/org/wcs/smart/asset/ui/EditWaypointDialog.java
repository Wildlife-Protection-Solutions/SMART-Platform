package org.wcs.smart.asset.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Query;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class EditWaypointDialog extends TitleAreaDialog{

	private Waypoint toUpdate;
	
	private Text txtId;
	private DateTime dDate;
	private DateTime dTime;
		
	private boolean showId;
	
	public EditWaypointDialog(Shell parentShell, Waypoint toUpdate) {
		this(parentShell, toUpdate, true);
	}
	
	public EditWaypointDialog(Shell parentShell, Waypoint toUpdate, boolean showId) {
		super(parentShell);
		this.toUpdate = toUpdate;
		this.showId = showId;
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
		
		if (showId) {
			Integer id = Integer.parseInt(txtId.getText().trim());
			toUpdate.setId(id);
		}
		toUpdate.setDateTime(newDateTime);
		
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

