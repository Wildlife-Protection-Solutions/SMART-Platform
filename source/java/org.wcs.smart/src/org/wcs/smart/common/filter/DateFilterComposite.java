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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
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
		LAST_YEAR(Messages.DateFilterComposite_LastYear), //last year from jan to dec
		LAST_1_YEARS(Messages.DateFilterComposite_LAST_ONE_YEAR),	//last 365 days
		LAST_5_YEARS(Messages.DateFilterComposite_Last5Year);
		
		
		private String guiName;
		
		private DateFilter(String name){
			this.guiName = name;
		}
		public String getGuiName(){
			return this.guiName;
		}
		
		public String getLabel(){
			switch(this){
			case ALL:
				return null;
			case CURRENT_MONTH:
			case MONTH_TO_DATE:
				return DateTimeFormatter.ofPattern("MMM yyyy").format(getEndDate());  //$NON-NLS-1$
			case CURRENT_YEAR:
			case LAST_YEAR:
			case YEAR_TO_DATE:
				return DateTimeFormatter.ofPattern("yyyy").format(getEndDate());  //$NON-NLS-1$
			case CUSTOM:
				return null;
			case LAST_1_YEARS:
			case LAST_30_DAYS:
			case LAST_5_YEARS:
			case LAST_60_DAYS:
			case NEXT_30_DAYS:
			case NEXT_60_DAYS:
			case RANGE_30_DAYS:
			case RANGE_60_DAYS:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(getStartDate()) + "-" + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(getEndDate()); //$NON-NLS-1$
			}
			return null;
		}
		/**
		 * 
		 * @return the start date associated with the filter or
		 * <code>null</code> if start date cannot be computed
		 * for filter value (for custom values).
		 */
		public LocalDate getStartDate(){
			switch(this){
			case LAST_30_DAYS:
			case RANGE_30_DAYS:
				return ChronoUnit.DAYS.addTo(LocalDate.now(), -30);
			case LAST_60_DAYS:
			case RANGE_60_DAYS:
				return ChronoUnit.DAYS.addTo(LocalDate.now(), -60);
			case NEXT_30_DAYS:
			case NEXT_60_DAYS:
				return LocalDate.now();
			case YEAR_TO_DATE:
			case CURRENT_YEAR:
				return LocalDate.of(LocalDate.now().getYear(),1,1);
			case MONTH_TO_DATE:
			case CURRENT_MONTH:
				LocalDate now = LocalDate.now();
				return LocalDate.of(now.getYear(), now.getMonth(), 1);
			case LAST_YEAR:
				return LocalDate.of(LocalDate.now().getYear()-1,1,1);
			case LAST_1_YEARS:
				return ChronoUnit.YEARS.addTo(LocalDate.now(), -1);
			case LAST_5_YEARS:
				return ChronoUnit.YEARS.addTo(LocalDate.now(), -5);
			case ALL: return null;
			case CUSTOM: return null;
			}
			return null;
		}
		
		/**
		 * 
		 * @return the end date associated with the filter or
		 * <code>null</code> if end date cannot be computed
		 * for filter value (for custom values).
		 */
		public LocalDate getEndDate(){
			switch(this){
				case LAST_30_DAYS:
				case LAST_60_DAYS: 
				case YEAR_TO_DATE:
				case MONTH_TO_DATE:
				case LAST_5_YEARS:
				case LAST_1_YEARS:
					return LocalDate.now();
				case LAST_YEAR: 	
					return LocalDate.of(LocalDate.now().getYear() - 1, 12, 31);
				case NEXT_30_DAYS:
				case RANGE_30_DAYS:
					return ChronoUnit.DAYS.addTo(LocalDate.now(), 30);
				case NEXT_60_DAYS:
				case RANGE_60_DAYS:
					return ChronoUnit.DAYS.addTo(LocalDate.now(), 60);
				case CURRENT_YEAR:
					return LocalDate.of(LocalDate.now().getYear(), 12, 31);
				case CURRENT_MONTH:
					LocalDate now = LocalDate.now();
					return YearMonth.of(now.getYear(), now.getMonth()).atEndOfMonth();
				case ALL:
					return null;
				case CUSTOM:
					return null;
			}
			return null;
		}
		
	}
	
	private SmartFilterDialog dialog;
	
	private ComboViewer dateViewer;
	private Label lblStartDateAnd;
	private Label lblStartDateBetween;
	private DateTime dtEnd;
	private DateTime dtStart;
	private Button btnIncludeAll;
	

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
		setLayout(new GridLayout(5, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeAll = new Button(this, SWT.CHECK);
		btnIncludeAll.setText(Messages.DateFilterComposite_IncludeAllDates_Label);
		btnIncludeAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		
		dateViewer = new ComboViewer(this, SWT.READ_ONLY);
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
				setDateRangeControlsEnabled(enabled);
			}
		});
		dateViewer.getCombo().addListener(SWT.Modify, validateListener );
		lblStartDateBetween = new Label(this, SWT.NONE);
		lblStartDateBetween.setText(Messages.DateFilterComposite_ContainsDate_Label_A);
		
		
		dtStart = new DateTime(this, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtStart.addListener(SWT.Selection, validateListener );
		lblStartDateAnd = new Label(this, SWT.NONE);
		lblStartDateAnd.setText(Messages.DateFilterComposite_ContainsDate_Label_B);
		dtEnd = new DateTime(this, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtEnd.addListener(SWT.Selection, validateListener );
		
		btnIncludeAll.addListener(SWT.Selection, e->{
			setFilteringEnabled(!btnIncludeAll.getSelection());
		});
		btnIncludeAll.setSelection(true);

	}

	protected DateFilter[] getDefaultDateViewerInput() {
		return new DateFilter[] {
				DateFilter.ALL,
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

		IStructuredSelection dateSelection = (IStructuredSelection) this.dateViewer.getSelection();
		if (dateSelection == null || dateSelection.isEmpty()){
			dialog.setErrorMessage(Messages.DateFilterComposite_DateRequired_Error);
			return;
		}
		if (dateSelection.getFirstElement().equals(DateFilter.CUSTOM)){
			if (SmartUtils.toDate(dtStart).isAfter(SmartUtils.toDate(dtEnd))){
				dialog.setErrorMessage(Messages.DateFilterComposite_EndDateRange_Error);
				
			}
		}
	}

	public void setDateRangeLabel(String text) {
		lblStartDateBetween.setText(text);
	}
	
	private DateFilter getCurrentDateFilter() {
		if (btnIncludeAll.getSelection()) return null;
		return (DateFilter) ((IStructuredSelection)dateViewer.getSelection()).getFirstElement();
	}

	public void applyState(DateFilter dateFilter, LocalDate startDate, LocalDate endDate) {
		boolean enabled = dateFilter != null;
		if (enabled) {
			dateViewer.setSelection(new StructuredSelection(dateFilter));
			if (dateFilter == DateFilter.CUSTOM) {
				SmartUtils.initDateTimeWidget(dtStart, startDate);
				SmartUtils.initDateTimeWidget(dtEnd, endDate);
			}
		} else {
			dateViewer.setSelection(getDefaultDateViewerSelection());
		}
		//setFilteringEnabled(enabled);
		btnIncludeAll.setSelection(!enabled);
		setFilteringEnabled(!btnIncludeAll.getSelection());

	}
	
	private void setFilteringEnabled(boolean enabled) {
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
		return getCurrentDateFilter();
	}

	public LocalDate getStartDateForModel() {
		if (DateFilter.CUSTOM.equals(getDateFilterForModel())) {
			return SmartUtils.toDate(dtStart);
		}
		return null;
	}

	public LocalDate getEndDateForModel() {
		if (DateFilter.CUSTOM.equals(getDateFilterForModel())) {
			return SmartUtils.toDate(dtEnd);
		}
		return null;
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
