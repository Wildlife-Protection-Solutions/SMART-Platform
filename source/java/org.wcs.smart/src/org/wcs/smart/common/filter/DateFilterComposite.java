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
package org.wcs.smart.common.filter;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite with required controls for date filtering. Used inside filter dialogs
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class DateFilterComposite extends Composite {

	public enum DateFilter {
		
		LAST_30_DAYS(Messages.DateFilter_Last30Days),
		LAST_60_DAYS(Messages.DateFilter_Last60Days),
		NEXT_30_DAYS(Messages.DateFilter_Next30Days),
		NEXT_60_DAYS(Messages.DateFilter_Next60Days),
		YEAR_TO_DATE(Messages.DateFilter_YearToDate),
		MONTH_TO_DATE(Messages.DateFilter_MonthToDate),
		RANGE_30_DAYS(Messages.DateFilterComposite_30DayRangeOption),
		RANGE_60_DAYS(Messages.DateFilterComposite_60DayRangeOption),
		CURRENT_YEAR(Messages.DateFilterComposite_CurrentYear),
		CURRENT_MONTH(Messages.DateFilterComposite_CurrentMonth),
		CUSTOM(Messages.DateFilter_Custom),
		ALL(Messages.DateFilterComposite_All),
		LAST_YEAR(Messages.DateFilterComposite_LastYear),
		LAST_5_YEARS(Messages.DateFilterComposite_Last5Year);
		
		private String guiName;
		
		private DateFilter(String name){
			this.guiName = name;
		}
		public String getGuiName(){
			return this.guiName;
		}
		
		/**
		 * 
		 * @return the start date associated with the filter or
		 * <code>null</code> if start date cannot be computed
		 * for filter value (for custom values).
		 */
		public Date getStartDate(){
			Calendar cal = Calendar.getInstance();
			if (this == LAST_30_DAYS || this == RANGE_30_DAYS){
				cal.add(Calendar.DAY_OF_MONTH, -30);
				return cal.getTime(); 	
			}else if (this == LAST_60_DAYS || this == RANGE_60_DAYS){
				cal.add(Calendar.DAY_OF_MONTH, -60);
				return cal.getTime();
			}else if (this == NEXT_30_DAYS || this == NEXT_60_DAYS){
				return cal.getTime();
			}else if (this == YEAR_TO_DATE || this == CURRENT_YEAR){
				cal.set(cal.get(Calendar.YEAR), 0, 01, 0, 0, 0);
				return cal.getTime();
			}else if (this == MONTH_TO_DATE || this == CURRENT_MONTH){
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 01, 0, 0, 0);
				return cal.getTime();
			}else if (this == LAST_YEAR){
				cal.add(Calendar.YEAR, -1);
				return cal.getTime();
			}else if (this == LAST_5_YEARS){
				cal.add(Calendar.YEAR, -5);
				return cal.getTime();
			}
			return null;
		}
		
		/**
		 * 
		 * @return the end date associated with the filter or
		 * <code>null</code> if end date cannot be computed
		 * for filter value (for custom values).
		 */
		public Date getEndDate(){
			Calendar cal = Calendar.getInstance();
			if (this == LAST_30_DAYS || this == LAST_60_DAYS 
					|| this == YEAR_TO_DATE
					|| this == MONTH_TO_DATE
					|| this == LAST_YEAR
					|| this == LAST_5_YEARS){
				return cal.getTime(); 	
			}else if (this == NEXT_30_DAYS || this == RANGE_30_DAYS){
				cal.add(Calendar.DAY_OF_MONTH, 30);
				return cal.getTime();
			}else if (this == NEXT_60_DAYS || this == RANGE_60_DAYS){
				cal.add(Calendar.DAY_OF_MONTH, 60);
				return cal.getTime();
			}else if (this == CURRENT_YEAR){
				cal.set(cal.get(Calendar.YEAR), 11, 31, 23, 59, 59);
				return cal.getTime();
			}else if (this == CURRENT_MONTH){
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.MONTH), 23, 59, 59);
				return cal.getTime();
			}
			return null;
		}
		
	}
	
	private SmartFilterDialog dialog;
	
	private Button btnFilterDate;
	private Button btnIncludeAllDate;
	private ComboViewer dateViewer;
	private Label lblStartDateAnd;
	private Label lblStartDateBetween;
	private DateTime dtEnd;
	private DateTime dtStart;

	private Listener validateListener = new Listener() {

		@Override
		public void handleEvent(Event event) {
			validate();
		}
	};

	public DateFilterComposite(Composite parent, int style, SmartFilterDialog dialog) {
		super(parent, style);
		this.dialog = dialog;
		createControls();
	}

	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnIncludeAllDate = new Button(this, SWT.RADIO);
		btnIncludeAllDate.setText(Messages.DateFilterComposite_IncludeAllDates_Label);
		btnIncludeAllDate.addListener(SWT.Selection, validateListener);
		btnIncludeAllDate.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (btnIncludeAllDate.getSelection()){
					dtEnd.setEnabled(false);
					dtStart.setEnabled(false);
				}
			}
		});
		btnFilterDate = new Button(this, SWT.RADIO);
		btnFilterDate.setText(Messages.DateFilterComposite_FilterDates_Label);
		btnFilterDate.addListener(SWT.Selection, validateListener);
		btnFilterDate.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				DateFilter ff = (DateFilter)((StructuredSelection)dateViewer.getSelection()).getFirstElement();
				boolean enabled =  (ff != null && ff == DateFilter.CUSTOM);
				dtStart.setEnabled(enabled);
				dtEnd.setEnabled(enabled);
				lblStartDateAnd.setEnabled(enabled);
				lblStartDateBetween.setEnabled(enabled);
			}
		});
		
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateViewer = new ComboViewer(comp, SWT.READ_ONLY);
		dateViewer.setContentProvider(ArrayContentProvider.getInstance());
		dateViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof DateFilter){
					return ((DateFilter)element).getGuiName();
				}
				return super.getText(element);
			}
		});
		dateViewer.setInput(getDefaultDateViewerInput());
		dateViewer.setSelection(getDefaultDateViewerSelection());		
		dateViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				DateFilter ff = (DateFilter)((StructuredSelection)dateViewer.getSelection()).getFirstElement();
				boolean enabled =  (ff != null && ff == DateFilter.CUSTOM);
				dtStart.setEnabled(enabled);
				dtEnd.setEnabled(enabled);
				lblStartDateAnd.setEnabled(enabled);
				lblStartDateBetween.setEnabled(enabled);
			}
		});
		dateViewer.getCombo().addListener(SWT.Modify, validateListener );
		lblStartDateBetween = new Label(comp, SWT.NONE);
		lblStartDateBetween.setText(Messages.DateFilterComposite_ContainsDate_Label_A);
		
		
		dtStart = new DateTime(comp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtStart.addListener(SWT.Selection, validateListener );
		lblStartDateAnd = new Label(comp, SWT.NONE);
		lblStartDateAnd.setText(Messages.DateFilterComposite_ContainsDate_Label_B);
		dtEnd = new DateTime(comp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtEnd.addListener(SWT.Selection, validateListener );
		
		btnFilterDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dateViewer.getCombo().setEnabled(true);
			}
		});
		btnIncludeAllDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dateViewer.getCombo().setEnabled(false);
			}
		});
	}

	protected DateFilter[] getDefaultDateViewerInput() {
		return new DateFilter[] {
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.YEAR_TO_DATE,
				DateFilter.MONTH_TO_DATE,
				DateFilter.CUSTOM
		};
	}

	protected ISelection getDefaultDateViewerSelection() {
		return new StructuredSelection(DateFilter.LAST_30_DAYS);
	}
	
	protected void validate(){
		dialog.setErrorMessage(null);
		if (btnFilterDate.getSelection()){
			IStructuredSelection dateSelection = (IStructuredSelection) this.dateViewer.getSelection();
			if (dateSelection == null || dateSelection.isEmpty()){
				dialog.setErrorMessage(Messages.DateFilterComposite_DateRequired_Error);
				return;
			}
			if (dateSelection.getFirstElement().equals(DateFilter.CUSTOM)){
				if (SmartUtils.getDate(dtStart).after(SmartUtils.getDate(dtEnd))){
					dialog.setErrorMessage(Messages.DateFilterComposite_EndDateRange_Error);
				}
			}
		}
	}

	public void setIncludeAllRadioLabel(String text) {
		btnIncludeAllDate.setText(text);
	}

	public void setFilterRadioLabel(String text) {
		btnFilterDate.setText(text);
	}
	
	public void setDateRangeLabel(String text) {
		lblStartDateBetween.setText(text);
	}
	
	private DateFilter getCurrentDateFilter() {
		IStructuredSelection selection = (IStructuredSelection) dateViewer.getSelection();
		if (selection != null) {
			return (DateFilter) selection.getFirstElement();
		}
		return null;
	}

	public void applyState(DateFilter dateFilter, Date startDate, Date endDate) {
		boolean enabled = dateFilter != null;
		if (enabled) {
			dateViewer.setSelection(new StructuredSelection(dateFilter));
			if (dateFilter == DateFilter.CUSTOM) {
				SmartUtils.initDateDateTimeWidget(dtStart, startDate);
				SmartUtils.initDateDateTimeWidget(dtEnd, endDate);
			}
		} else {
			dateViewer.setSelection(getDefaultDateViewerSelection());
		}
		setFilteringEnabled(enabled);
		
	}
	
	private void setFilteringEnabled(boolean enabled) {
		btnFilterDate.setSelection(enabled);
		btnIncludeAllDate.setSelection(!enabled);
		dateViewer.getControl().setEnabled(enabled);
		setDateRangeControlsEnabled(enabled && DateFilter.CUSTOM.equals(getCurrentDateFilter()));
	}
	
	private void setDateRangeControlsEnabled(boolean enabled) {
		dtStart.setEnabled(enabled);
		dtEnd.setEnabled(enabled);
		lblStartDateAnd.setEnabled(enabled);
		lblStartDateBetween.setEnabled(enabled);
	}

	public DateFilter getDateFilterForModel() {
		if (btnFilterDate.getSelection()) {
			return (DateFilter) ((IStructuredSelection)dateViewer.getSelection()).getFirstElement();
		}
		return null;
	}

	public Date getStartDateForModel() {
		if (DateFilter.CUSTOM.equals(getDateFilterForModel())) {
			return SmartUtils.getDate(dtStart);
		}
		return null;
	}

	public Date getEndDateForModel() {
		if (DateFilter.CUSTOM.equals(getDateFilterForModel())) {
			return SmartUtils.getDate(dtEnd);
		}
		return null;
	}
	
	public Button getBtnFilterDate() {
		return btnFilterDate;
	}

	public Button getBtnIncludeAllDate() {
		return btnIncludeAllDate;
	}

	public ComboViewer getDateViewer() {
		return dateViewer;
	}

	public DateTime getDtEnd() {
		return dtEnd;
	}

	public DateTime getDtStart() {
		return dtStart;
	}
	
}
