package org.wcs.smart.incident.ui.newwizard;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

public class DateTimeComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.datetime";
	
	private DateTime date;
	private DateTime time;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText("Date:");
		
		date = new DateTime(item, SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER );
		date.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		date.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fireChange(new Event());
        	}
		});
        
		
		l = new Label(item, SWT.NONE);
		l.setText("Time:");
		time = new DateTime(item, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		time.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		time.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fireChange(new Event());
        	}
		});
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		Date dt = SmartUtils.combineDateTime(SmartUtils.getDate(date), SmartUtils.getTime(time));
		incident.setDateTime(dt);
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getDateTime() != null){
			SmartUtils.initDateDateTimeWidget(date, incident.getDateTime());
			SmartUtils.initTimeDateTimeWidget(time, incident.getDateTime());
		}else{
			SmartUtils.initDateDateTimeWidget(date, new Date());
			SmartUtils.initTimeDateTimeWidget(time, new Date());
		}
	}


	@Override
	public String getName() {
		return "Date and Time";
	}

	@Override
	public String getDescription() {
		return "The incident date and time.  Only a single value can be entered.";
	}
	
}
