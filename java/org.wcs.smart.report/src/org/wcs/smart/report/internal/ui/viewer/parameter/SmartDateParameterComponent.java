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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.wcs.smart.query.parser.PatrolQueryOptions.DATE_FILTER_OP;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.util.SmartUtils;

/**
 * Special Parameter component for SMART report start and end dates.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartDateParameterComponent implements IBirtParameterComponent{

	
	private DateTime startPicker = null;
	private DateTime endPicker = null;
	
	private Label lblStart;
	private Label lblEnd;
	private ComboViewer cmbDatesOps;
	
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
		lbl.setText("Date Range: ");
		
		cmbDatesOps = new ComboViewer(param, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbDatesOps.setContentProvider(ArrayContentProvider.getInstance());
		cmbDatesOps.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof DATE_FILTER_OP){
					return ((DATE_FILTER_OP)element).guiName;
				}
				return super.getText(element);
			}
		});
		cmbDatesOps.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblStart = new Label(param, SWT.NONE);
		lblStart.setText("Start Date:");
		startPicker = new DateTime(param, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER );
		
		SimpleDateFormat sdf = new SimpleDateFormat(ReportParameterDialog.SIMPLE_DATE_FORMAT);
		String x = settings.get(SmartReportParameters.PARAM_START_DATE_KEY);
		if (x != null){
			try{
				Date d = sdf.parse(x);
				Calendar cal = Calendar.getInstance();
				cal.setTime(d);
				startPicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			}catch (Exception ex){
				//eat me
			}
		}
		
		lblEnd = new Label(param, SWT.NONE);
		lblEnd.setText("End Date:");
		endPicker = new DateTime(param, SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER );
		x = settings.get(SmartReportParameters.PARAM_END_DATE_KEY);
		if (x != null){
			try{
				Date d = sdf.parse(x);
				Calendar cal = Calendar.getInstance();
				cal.setTime(d);
				endPicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			}catch (Exception ex){
				//eat me
			}
		}
		
		cmbDatesOps.getCombo().addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				DATE_FILTER_OP filterOp = (DATE_FILTER_OP) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement(); 
				boolean enabled = (filterOp == DATE_FILTER_OP.CUSTOM);
				lblStart.setEnabled(enabled);
				lblEnd.setEnabled(enabled);
				startPicker.setEnabled(enabled);
				endPicker.setEnabled(enabled);
				
				if (!enabled && filterOp != null){
					Date[] d = filterOp.getDates();
					if (d != null){
						Calendar cal = GregorianCalendar.getInstance();
						if (d.length >=1){
							cal.setTime(d[0]);
							startPicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
						}
						if (d.length == 2){
							cal.setTime(d[1]);
							endPicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
						}else{
							cal.setTime(new Date());
							endPicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
						}
					}
				}
		}});
		
		cmbDatesOps.setInput(DATE_FILTER_OP.values());
		
		x = settings.get("org.wcs.smart.report.parameter.dateOp");
		if (x != null){
			cmbDatesOps.setSelection(new StructuredSelection(DATE_FILTER_OP.valueOf(x)));
		}else{
			cmbDatesOps.setSelection(new StructuredSelection(DATE_FILTER_OP.values()[0]));
		}
		return param;
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent#getParameters()
	 */
	@Override
	public HashMap<String, Object> getParameters() {

		HashMap<String, Object> params = new HashMap<String, Object>();
		
		DATE_FILTER_OP op = (DATE_FILTER_OP) ((IStructuredSelection)cmbDatesOps.getSelection()).getFirstElement();
		
		java.sql.Date dates[] = op.getDates();
		if (dates == null){
			if (op == DATE_FILTER_OP.CUSTOM){
				params.put(SmartReportParameters.PARAM_START_DATE_KEY, new java.sql.Date(SmartUtils.getDate(startPicker).getTime()));
				params.put(SmartReportParameters.PARAM_END_DATE_KEY, new java.sql.Date(SmartUtils.getDate(endPicker).getTime()));	
			}else if (op == DATE_FILTER_OP.ALL){
				params.put(SmartReportParameters.PARAM_START_DATE_KEY, new java.sql.Date(-2208998272375l));	//JAN 01 1900  
				
				Session session = HibernateManager.openSession();
				try{
					session.beginTransaction();
				
					String hql = "SELECT min(startDate) from Patrol WHERE conservationArea = :ca";
					Query q = session.createQuery(hql);
					q.setParameter("ca", SmartDB.getCurrentConservationArea());
					List<?> data = q.list();
					Date startdate = null;
					if (data != null && data.size() >= 1 && data.get(0) != null){
						startdate = (java.sql.Timestamp)data.get(0);
					}
					params.put(SmartReportParameters.PARAM_START_DATE_KEY, new java.sql.Date(startdate.getTime()));	//JAN 01 1900  
				}catch (Exception ex){					
					ReportPlugIn.log("Error retriving earliest data date for report paraemeters", ex);
				}finally{
					if (session.getTransaction().isActive()){
						session.getTransaction().commit();
					}
					session.close();
				}
				//today + one day
				params.put(SmartReportParameters.PARAM_END_DATE_KEY, new java.sql.Date( (new Date()).getTime() + 86400000));  //add one day just to make sure we get everything	
			}else{
				throw new UnsupportedOperationException("CDate file " + op.guiName + " not supported for reports.");
			}
			
		}else if (dates.length == 1){
			params.put(SmartReportParameters.PARAM_START_DATE_KEY, dates[0]);
			params.put(SmartReportParameters.PARAM_END_DATE_KEY, new java.sql.Date( (new Date()).getTime() + 86400000));  //add one day just to make sure we get everything
		}else if (dates.length == 2){
			params.put(SmartReportParameters.PARAM_START_DATE_KEY, dates[0]);
			params.put(SmartReportParameters.PARAM_END_DATE_KEY, dates[1]);
		}
		params.put("org.wcs.smart.report.parameter.dateOp", op.toString());
		return params;
	}

}
