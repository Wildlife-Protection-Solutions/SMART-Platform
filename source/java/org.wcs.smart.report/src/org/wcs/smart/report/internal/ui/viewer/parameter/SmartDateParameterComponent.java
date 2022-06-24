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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Special Parameter component for SMART report start and end dates.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartDateParameterComponent implements IBirtParameterComponent {

	
	private static final String DATE_OP_KEY = "org.wcs.smart.report.parameter.dateOp"; //$NON-NLS-1$
	
	private DateTime startPicker = null;
	private DateTime endPicker = null;
	
	private Label lblStart;
	private Label lblEnd;
	private ComboViewer cmbDatesOps;
	
	private ControlDecoration cdEnd;
	
	private IParameterDefn startDef;
	private IParameterDefn endDef;
	
	private IDialogSettings settings;
	/**
	 * 
	 * @param name parameter name
	 * @param displayText parameter display text
	 */
	public SmartDateParameterComponent(IParameterGroupDefn def){
		for (Object x : ((IParameterGroupDefn)def).getContents()) {
			if (x instanceof IParameterDefn) {
				if ( ((IParameterDefn) x).getName().equals(SmartReportParameters.PARAM_START_DATE_KEY)) {
					startDef = (IParameterDefn) x;
				}
				if ( ((IParameterDefn) x).getName().equals(SmartReportParameters.PARAM_END_DATE_KEY)) {
					endDef = (IParameterDefn) x;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#createComponent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent, IDialogSettings settings, Listener onParameterModified) {
		this.settings = settings;
		
		Composite param = new Composite(parent, SWT.NONE);
		param.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		GridLayout gl = new GridLayout(2, false);
		param.setLayout(gl);
		
		Label lbl = new Label(param, SWT.NONE);
		lbl.setText(Messages.SmartDateParameterComponent_DateRangeLabel);
		
		cmbDatesOps = new ComboViewer(param, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbDatesOps.setContentProvider(ArrayContentProvider.getInstance());
		cmbDatesOps.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IDateFilter){
					return ((IDateFilter)element).getGuiName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		cmbDatesOps.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		Composite custom = new Composite(param, SWT.NONE);
		custom.setLayout(new GridLayout(4, false));
		custom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridLayout)custom.getLayout()).marginWidth = 0;
		((GridLayout)custom.getLayout()).marginHeight = 0;
		
		lblStart = new Label(custom, SWT.NONE);
		lblStart.setText(Messages.SmartDateParameterComponent_StartDateLabel);
		lblStart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lblStart.getLayoutData()).widthHint = lbl.computeSize(SWT.DEFAULT,  SWT.DEFAULT).x;
		
		startPicker = new DateTime(custom, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE);
		startPicker.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		initDateTime(SmartReportParameters.PARAM_START_DATE_KEY, startPicker, settings);
		startPicker.addListener(SWT.Selection, onParameterModified);
		
		lblEnd = new Label(custom, SWT.NONE);
		lblEnd.setText(Messages.SmartDateParameterComponent_EndDateLabel);
		lblEnd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lblEnd.getLayoutData()).horizontalIndent = 15;

		
		endPicker = new DateTime(custom, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE);
		endPicker.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		initDateTime(SmartReportParameters.PARAM_END_DATE_KEY, endPicker, settings);
		endPicker.addListener(SWT.Selection, onParameterModified);
		
		cdEnd = new ControlDecoration(endPicker, SWT.LEFT | SWT.TOP);
		cdEnd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdEnd.setShowHover(true);
		cdEnd.hide();
		
		cmbDatesOps.setInput(IDateFilter.DATE_FILTERS);
		cmbDatesOps.getCombo().addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				IDateFilter filterOp = (IDateFilter) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement(); 
				boolean enabled = filterOp != null && (filterOp.getQueryKey().equals(CustomDateFilter.KEY));
				lblStart.setEnabled(enabled);
				lblEnd.setEnabled(enabled);
				startPicker.setEnabled(enabled);
				endPicker.setEnabled(enabled);
								
				if (!enabled && filterOp != null){
					LocalDate[] d = filterOp.getDates();
					if (d != null){
						if (d.length >=1){
							SmartUtils.initDateTimeWidget(startPicker, d[0]);
						}
						if (d.length == 2){
							SmartUtils.initDateTimeWidget(endPicker, d[1]);
						}else{
							SmartUtils.initDateTimeWidget(endPicker, LocalDate.now());
						}
					}
				}
				onParameterModified.handleEvent(event);
		}});
				
		String x = settings.get(DATE_OP_KEY);
		IDateFilter defaultSelection = IDateFilter.DATE_FILTERS[0];
		for (int i = 0; i < IDateFilter.DATE_FILTERS.length; i++){
			if (IDateFilter.DATE_FILTERS[i].getQueryKey().equals(x)){
				defaultSelection = IDateFilter.DATE_FILTERS[i];
				break;
			}
		}		
		cmbDatesOps.setSelection(new StructuredSelection(defaultSelection));
	}
	
	private void initDateTime(String parameter, DateTime dtime, IDialogSettings settings) {
		DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ReportParameterDialog.SIMPLE_DATE_FORMAT);
		String x = settings.get(parameter);
		if (x != null){
			try{
				LocalDateTime d = LocalDateTime.parse(x,sdf);
				SmartUtils.initDateTimeWidget(dtime, d.toLocalDate(), d.toLocalTime());
			}catch (Exception ex){
				//eat me
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent#getParameters()
	 */
	@Override
	public HashMap<IParameterDefn, Object> getParameters() {

		HashMap<IParameterDefn, Object> params = new HashMap<>();
		
		IDateFilter op = (IDateFilter) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement();

		if (op.getQueryKey().equals(CustomDateFilter.KEY)){
			//update custom date filter with values from combo boxes
			((CustomDateFilter) op).setDates(SmartUtils.toDate(startPicker), SmartUtils.toDate(endPicker));
		}	
		
		LocalDate start = null;
		LocalDate end = null;
		
		LocalDate dates[] = op.getDates();
		if (dates == null) {
			if (op.getQueryKey().equals(AllDatesFilter.INSTANCE.getQueryKey())) {
				
				//I tried LocalDate.MIN here, but that doesn't produce a date
				//that is valid with sql queries.
				start = LocalDate.of(1900, 01, 01);
				end = ChronoUnit.DAYS.addTo(LocalDate.now(), 1); // today plus 1 day
				try(Session session = HibernateManager.openSession()){
					try {
						session.beginTransaction();
	
						String hql = "SELECT min(dateTime), max(dateTime) FROM Waypoint WHERE conservationArea = :ca"; //$NON-NLS-1$
						Query<?> q = session.createQuery(hql);
						q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
						List<?> data = q.list();
						
						if (data != null && data.size() >= 1) {
							LocalDateTime startdate = (LocalDateTime) ((Object[])data.get(0))[0];
							if (startdate != null) {
								start = ChronoUnit.DAYS.addTo(startdate.toLocalDate(), -1); //subtract one day to ensure we get everything
							}
							
							LocalDateTime enddate = (LocalDateTime) ((Object[])data.get(0))[1];
							if (enddate != null) {
								if (enddate.toLocalDate().isAfter( end )) {
									end = ChronoUnit.DAYS.addTo(enddate.toLocalDate(),1);  // last date plus 1 day to ensure we get everything
								}
							}
						}
	
					} catch (Exception ex) {
						ReportPlugIn
								.log(Messages.SmartDateParameterComponent_EarliestDateError,
										ex);
					} finally {
						if (session.getTransaction().isActive()) {
							session.getTransaction().commit();
						}
					}
				}
			} else {
				throw new UnsupportedOperationException(
						MessageFormat
								.format(Messages.SmartDateParameterComponent_DateFilterNotSupported,
										new Object[] { op.getGuiName(Locale.getDefault()) }));
			}
		}else if (dates.length == 1){
			start = dates[0];
			end =  ChronoUnit.DAYS.addTo(LocalDate.now(), 1);  //add one day just to make sure we get everything
		}else if (dates.length == 2){
			start = dates[0];
			end = dates[1];
		}
		
		
		//need to convert these to java.util.date for BIRT
		
		params.put(startDef, java.sql.Date.valueOf(start));
		params.put(endDef, java.sql.Date.valueOf(end));
		settings.put(DATE_OP_KEY, op.getQueryKey());
		return params;
	}

	
	@Override
	public String validate() {
		
		String error = null;
//		IDateFilter op = (IDateFilter) 
//				((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement(); 
//		if (op.getQueryKey().equals(CustomDateFilter.KEY)){
		
		LocalDate start = SmartUtils.toDate(startPicker);
		LocalDate end = SmartUtils.toDate(endPicker);
			
		if (end.isBefore(start)){
			//warning
			error = Messages.SmartDateParameterComponent_DateWarning;
			cdEnd.setDescriptionText(error);
			cdEnd.show();
			
		}else{
			cdEnd.hide();
		}			

		
		return error;
	}

}
