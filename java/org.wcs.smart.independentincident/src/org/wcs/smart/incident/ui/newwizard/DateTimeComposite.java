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
import org.hibernate.Session;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;
/**
 * Incident date time composite.
 * @author Emily
 *
 */
public class DateTimeComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.datetime"; //$NON-NLS-1$
	
	private DateTime date;
	private DateTime time;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Composite item = new Composite(main, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		item.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.DateTimeComposite_DateLabel);
		
		date = new DateTime(item, SWT.DATE | SWT.DROP_DOWN | SWT.LONG | SWT.BORDER );
		date.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		
		date.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fireChange(new Event());
        	}
		});
        
		
		l = new Label(item, SWT.NONE);
		l.setText(Messages.DateTimeComposite_TimeLabel);
		time = new DateTime(item, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		time.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
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
		return Messages.DateTimeComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.DateTimeComposite_Description;
	}
	
}
