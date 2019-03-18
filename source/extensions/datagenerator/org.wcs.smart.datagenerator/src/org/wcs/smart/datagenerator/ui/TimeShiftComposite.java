/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.datagenerator.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Composite for collecting time shift details
 * 
 * @author Emily
 *
 */
public class TimeShiftComposite extends Composite{

	private Label lblTimeShiftCurrentRange;
	private Label lblTimeShiftNewRange;
	private Text txtTimeShift;
	private Button btnTimeShift;
	private Label lblTimeErrorImg;
	private Label lblTimeErrorMsg;
	
	private DataGeneratorView view;
	
	@Inject private UISynchronize ui;
	
	public TimeShiftComposite(Composite parent, DataGeneratorView view) {
		super(parent, SWT.NONE);
		this.view = view;
		createContents();
	}

	private void createContents() {
		setLayout(new GridLayout(3, false));
		
		Composite infoComp = view.createHeader(this, Messages.DataGeneratorView_TimeShiftMessage);
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		FormToolkit toolkit = view.toolkit;
		toolkit.createLabel(this, Messages.DataGeneratorView_CurrentRange);
		lblTimeShiftCurrentRange = toolkit.createLabel(this, ""); //$NON-NLS-1$
		
		Hyperlink refresh = toolkit.createHyperlink(this, Messages.TimeShiftComposite_refreshlink, SWT.NONE);
		refresh.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		refresh.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshDates();
			}
		});
		
		
		toolkit.createLabel(this, Messages.DataGeneratorView_ShiftLabel);
		
		Composite timeShift = new Composite(this, SWT.NONE);
		timeShift.setLayout(new GridLayout(3, false));
		timeShift.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		((GridLayout)timeShift.getLayout()).marginWidth = 0;
		((GridLayout)timeShift.getLayout()).marginHeight = 0;
		
		txtTimeShift = toolkit.createText(timeShift, "30"); //$NON-NLS-1$
		txtTimeShift.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)txtTimeShift.getLayoutData()).widthHint = 75;
		
		lblTimeErrorImg = toolkit.createLabel(timeShift, ""); //$NON-NLS-1$
		lblTimeErrorImg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		lblTimeErrorImg.setVisible(false);
		lblTimeErrorImg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblTimeErrorMsg = toolkit.createLabel(timeShift, ""); //$NON-NLS-1$
		lblTimeErrorMsg.setVisible(false);
		lblTimeErrorMsg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(this, Messages.DataGeneratorView_NewRange);
		lblTimeShiftNewRange = toolkit.createLabel(this, ""); //$NON-NLS-1$
		lblTimeShiftNewRange.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		toolkit.createLabel(this, ""); //$NON-NLS-1$
		
		toolkit.createLabel(this, ""); //$NON-NLS-1$
		
		btnTimeShift = toolkit.createButton(this, Messages.DataGeneratorView_ShiftButton, SWT.PUSH);
		btnTimeShift.addListener(SWT.Selection,e->view.doTimeShift());
		btnTimeShift.setEnabled(false);
		toolkit.createLabel(this, ""); //$NON-NLS-1$
		
		txtTimeShift.addListener(SWT.Modify, e->updateTimeShiftDates());
	}
	
	public String getDays() {
		return this.txtTimeShift.getText();
	}
	
	public void refreshDates() {
		refreshPatrolDates.schedule();
	}
	
	private String formatDates(LocalDate min, LocalDate max) {
		return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(min) + Messages.DataGeneratorView_DateRangeTo + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(max);
	}
	
	
	private void updateTimeShiftDates() {
		btnTimeShift.setEnabled(false);
		
		LocalDate[] currentDates = (LocalDate[]) lblTimeShiftCurrentRange.getData();
		if (currentDates == null) {
			lblTimeShiftCurrentRange.setText(""); //$NON-NLS-1$
			return;
		}
		
		 
		lblTimeShiftCurrentRange.setText(formatDates(currentDates[0], currentDates[1]));
		Integer shiftDays = 0;
		try {
			lblTimeErrorMsg.setVisible(false);
			lblTimeErrorImg.setVisible(false);
			shiftDays = Integer.parseInt(txtTimeShift.getText());
		}catch (Exception ex) {
			lblTimeErrorMsg.setVisible(true);
			lblTimeErrorImg.setVisible(true);
			lblTimeErrorMsg.setText(Messages.DataGeneratorView_IntegerRequired7);
			return;
		}
		
		LocalDate newStart = currentDates[0].plusDays(shiftDays);
		LocalDate newEnd =  currentDates[1].plusDays(shiftDays);
		
		lblTimeShiftNewRange.setText(formatDates(newStart, newEnd));
		
		btnTimeShift.setEnabled(true);
		layout(true);
	}
	private Job refreshPatrolDates = new Job("refresh patrol dates") { //$NON-NLS-1$
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			LocalDate minDate = null;
			LocalDate maxDate = null;
			
			try(Session s = HibernateManager.openSession()){
				
				Object[] data = (Object[]) s.createQuery("SELECT min(startDate), max(endDate) FROM Patrol WHERE conservationArea = :ca") //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.uniqueResult();
				
				if (data[0] != null) {
					minDate = ((java.sql.Timestamp) data[0]).toLocalDateTime().toLocalDate();
				}else {
					minDate = LocalDate.now();
				}
				if (data[1] != null) {
					maxDate = ((java.sql.Timestamp) data[1]).toLocalDateTime().toLocalDate();
				}else {
					maxDate = LocalDate.now();
				}
			}
			
			
			final LocalDate[] dates = new LocalDate[] {minDate, maxDate};
			ui.asyncExec(()->{
				lblTimeShiftCurrentRange.setData(dates);
				updateTimeShiftDates();
			});
			
			return Status.OK_STATUS;
		}
	};
}
