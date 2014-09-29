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
package org.wcs.smart.er.ui.mision.editor;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to select a day to move waypoints to.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MoveWaypointDialog extends TitleAreaDialog {

	private Mission mission;

	private List<Date> dates;
	private Date moveTo;
	
	public MoveWaypointDialog(Shell parentShell, Mission m, Date date) {
		super(parentShell);
		
		this.mission = m;
		moveTo = date;
	}

	/**
	 * @return the day picked by the user
	 */
	public Date getMoveToDate(){
		return this.moveTo;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	public List<Date> createDates() {
		Calendar calStart = SmartUtils.convertDate(mission.getStartDate());
		calStart.set(Calendar.HOUR, 0);
		calStart.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
		
		Calendar calEnd = SmartUtils.convertDate(mission.getEndDate());
		List<Date> daysList = new ArrayList<Date>();
		
		while (calStart.before(calEnd) || calStart.equals(calEnd)) {
			daysList.add(SmartUtils.getDatePart(calStart.getTime(), false));
			calStart.add(Calendar.DAY_OF_MONTH, 1);
		}
		return daysList;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
		
		final Composite legtype = new Composite(parent, SWT.NONE);
		legtype.setLayout(new GridLayout(2, false));
		legtype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(legtype, SWT.NONE);
		lbl.setText("Move To:");
		
		final Combo dtCombo = new Combo(legtype, SWT.DROP_DOWN | SWT.READ_ONLY);

		dates = createDates();
		for (Date dt : dates) {
			dtCombo.add(DateFormat.getDateInstance(DateFormat.MEDIUM).format(dt));
		}
		dtCombo.select(dates.indexOf(moveTo));
		
		dtCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveTo = dates.get(dtCombo.getSelectionIndex());
			}
		});
		
		setMessage("Select day to move selected waypoints to.");
		
		getShell().setText("Move Waypoints");
		return parent;
	}
	
}