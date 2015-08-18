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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
import org.hibernate.Query;
import org.hibernate.Session;
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
public class SmartDateParameterComponent implements IBirtParameterComponent, Listener{

	
	private DateTime startPicker = null;
	private DateTime endPicker = null;
	
	private Label lblStart;
	private Label lblEnd;
	private ComboViewer cmbDatesOps;
	
	private ControlDecoration cdEnd;
	
	/**
	 * 
	 * @param name parameter name
	 * @param displayText parameter display text
	 */
	public SmartDateParameterComponent(IParameterGroupDefn def){
		
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#createComponent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComposite(Composite parent, IDialogSettings settings) {
		Composite param = new Composite(parent, SWT.NONE);
		
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
		lblStart = new Label(param, SWT.NONE);
		lblStart.setText(Messages.SmartDateParameterComponent_StartDateLabel);
		startPicker = new DateTime(param, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE);
		
		SimpleDateFormat sdf = new SimpleDateFormat(ReportParameterDialog.SIMPLE_DATE_FORMAT);
		String x = settings.get(SmartReportParameters.PARAM_START_DATE_KEY);
		if (x != null){
			try{
				Date d = sdf.parse(x);
				SmartUtils.initDateDateTimeWidget(startPicker, d);
			}catch (Exception ex){
				//eat me
			}
		}
		startPicker.addListener(SWT.Selection, this);
		
		
		lblEnd = new Label(param, SWT.NONE);
		lblEnd.setText(Messages.SmartDateParameterComponent_EndDateLabel);
		endPicker = new DateTime(param, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE);
		x = settings.get(SmartReportParameters.PARAM_END_DATE_KEY);
		if (x != null){
			try{
				Date d = sdf.parse(x);
				SmartUtils.initDateDateTimeWidget(endPicker, d);
			}catch (Exception ex){
				//eat me
			}
		}
		endPicker.addListener(SWT.Selection, this);
		
		cdEnd = new ControlDecoration(endPicker, SWT.LEFT | SWT.TOP);
		cdEnd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		cdEnd.setShowHover(true);
		cdEnd.hide();
		
		cmbDatesOps.setInput(IDateFilter.DATE_FILTERS);
		cmbDatesOps.getCombo().addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				IDateFilter filterOp = (IDateFilter) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement(); 
				boolean enabled = (filterOp.getQueryKey().equals(CustomDateFilter.KEY));
				lblStart.setEnabled(enabled);
				lblEnd.setEnabled(enabled);
				startPicker.setEnabled(enabled);
				endPicker.setEnabled(enabled);
				
				if (!enabled && filterOp != null){
					Date[] d = filterOp.getDates();
					if (d != null){
						if (d.length >=1){
							SmartUtils.initDateDateTimeWidget(startPicker, d[0]);
						}
						if (d.length == 2){
							SmartUtils.initDateDateTimeWidget(endPicker, d[1]);
						}else{
							SmartUtils.initDateDateTimeWidget(endPicker, new Date());
						}
					}
				}
		}});
		
		x = settings.get("org.wcs.smart.report.parameter.dateOp"); //$NON-NLS-1$
		IDateFilter defaultSelection = IDateFilter.DATE_FILTERS[0];
		for (int i = 0; i < IDateFilter.DATE_FILTERS.length; i++){
			if (IDateFilter.DATE_FILTERS[i].getQueryKey().equals(x)){
				defaultSelection = IDateFilter.DATE_FILTERS[i];
				break;
			}
		}		
		cmbDatesOps.setSelection(new StructuredSelection(defaultSelection));
		return param;
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent#getParameters()
	 */
	@Override
	public HashMap<String, Object> getParameters() {

		HashMap<String, Object> params = new HashMap<String, Object>();
		
		IDateFilter op = (IDateFilter) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement();

		if (op.getQueryKey().equals(CustomDateFilter.KEY)){
			//update custom date filter with values from combo boxes
			((CustomDateFilter) op).setDates(SmartUtils.getDate(startPicker), SmartUtils.getDate(endPicker));
		}	
		
		java.sql.Date dates[] = op.getDates();
		if (dates == null) {
			if (op.getQueryKey().equals(AllDatesFilter.INSTANCE.getQueryKey())) {
				params.put(SmartReportParameters.PARAM_START_DATE_KEY,
						new java.sql.Date(-2208998272375l)); // JAN 01 1900

				Session session = HibernateManager.openSession();
				try {
					session.beginTransaction();

					String hql = "SELECT min(startDate) from Patrol WHERE conservationArea = :ca"; //$NON-NLS-1$
					Query q = session.createQuery(hql);
					q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					List<?> data = q.list();
					Date startdate = null;
					if (data != null && data.size() >= 1 && data.get(0) != null) {
						startdate = (java.sql.Timestamp) data.get(0);
						params.put(SmartReportParameters.PARAM_START_DATE_KEY,
								new java.sql.Date(startdate.getTime()));
					}

				} catch (Exception ex) {
					ReportPlugIn
							.log(Messages.SmartDateParameterComponent_EarliestDateError,
									ex);
				} finally {
					if (session.getTransaction().isActive()) {
						session.getTransaction().commit();
					}
					session.close();
				}
				// today + one day
				// add one day just to make sure we get everything
				params.put(SmartReportParameters.PARAM_END_DATE_KEY,
						new java.sql.Date((new Date()).getTime() + 86400000)); 
			} else {
				throw new UnsupportedOperationException(
						MessageFormat
								.format(Messages.SmartDateParameterComponent_DateFilterNotSupported,
										new Object[] { op.getGuiName(Locale.getDefault()) }));
			}
		}else if (dates.length == 1){
			params.put(SmartReportParameters.PARAM_START_DATE_KEY, dates[0]);
			params.put(SmartReportParameters.PARAM_END_DATE_KEY, new java.sql.Date( (new Date()).getTime() + 86400000));  //add one day just to make sure we get everything
		}else if (dates.length == 2){
			params.put(SmartReportParameters.PARAM_START_DATE_KEY, dates[0]);
			params.put(SmartReportParameters.PARAM_END_DATE_KEY, dates[1]);
		}
		params.put("org.wcs.smart.report.parameter.dateOp", op.getQueryKey()); //$NON-NLS-1$
		return params;
	}

	@Override
	public void handleEvent(Event event) {
		IDateFilter op = (IDateFilter) 
				((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement();

		if (op.getQueryKey().equals(CustomDateFilter.KEY)){
			Date start = SmartUtils.getDate(startPicker);
			Date end = SmartUtils.getDate(endPicker);
			
			if (end.before(start)){
				//warning
				cdEnd.setDescriptionText(Messages.SmartDateParameterComponent_DateWarning);
				cdEnd.show();
			}else{
				cdEnd.hide();
			}
			
		}
	}

}
