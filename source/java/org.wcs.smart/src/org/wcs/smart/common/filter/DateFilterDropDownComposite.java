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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * A composite for query date filters.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateFilterDropDownComposite extends Composite {

	private ComboViewer cmbFilter;
	private DateTime dtStart;
	private DateTime dtEnd;
	private Label lbl1;
	private Label lbl2;

	private Composite main;
	private ControlDecoration cdEndDate;
	
	private List<ISelectionChangedListener> listeners;

	private DateFilterComposite.DateFilter[] filters;
	private DateFilter defaultValue;
	private boolean showDateRangeLabel;
	private Composite customComp;
	
	/**
	 * create new composite
	 * @param parent parent composite
	 * @param fieldOps date field options; can be null
	 * @param filterOps date filter options 
	 * @param showDateRangeLabel true if the date range should be displayed next to the drop down
	 */
	public DateFilterDropDownComposite(Composite parent, 
			DateFilterComposite.DateFilter[] filters, DateFilter defaultValue, boolean showDateRangeLabel) {
		super(parent, SWT.NONE);
		this.showDateRangeLabel = showDateRangeLabel;
		this.listeners = new ArrayList<ISelectionChangedListener>();
		this.filters = filters;
		this.defaultValue = defaultValue;
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);
		createComponent();
	}
	public DateFilterDropDownComposite(Composite parent, 
			DateFilterComposite.DateFilter[] filters, DateFilter defaultValue) {
		this(parent, filters, defaultValue, false);
	}
	private ISelectionChangedListener fireListener = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			fireListeners(event);
		}
	};

	
	
	public void addChangeListener(ISelectionChangedListener listener){
		this.listeners.add(listener);
	}
	
	public void removeChangeListener(ISelectionChangedListener listener){
		this.listeners.remove(listener);
	}
	
	private void fireListeners(SelectionChangedEvent event){
		for (ISelectionChangedListener listener: listeners){
			listener.selectionChanged(event);
		}
	}
	
	/**
	 * Adapts the components of the composite to the given
	 * form toolkit.
	 * 
	 * @param toolkit
	 */
	public void adapt(FormToolkit toolkit){
		if (cmbFilter != null){
			toolkit.adapt(cmbFilter.getControl(), false, true);
		}
		
		toolkit.adapt(lbl1, false, false);
		toolkit.adapt(lbl2, false, false);
		toolkit.adapt(dtStart, false, true);
		toolkit.adapt(dtEnd, false, true);
		toolkit.adapt(main, false, true);
		toolkit.adapt(this, false, true);
	}
	
	public void setBackgroundColor(Color color){
		cmbFilter.getControl().setBackground(color);
	}
	
	private void createComponent(){
		main = new Composite(this, SWT.NONE);
		
		int cols = 4;
		
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		//use ccombo to have white background
		CCombo combo = new CCombo(main, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		combo.setBackground(combo.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		cmbFilter = new ComboViewer(combo);
	
		cmbFilter.setContentProvider(ArrayContentProvider.getInstance());
		cmbFilter.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof DateFilterComposite.DateFilter){
					return ((DateFilterComposite.DateFilter) element).getGuiName();
				}
				return super.getText(element);
			}
		});
		
		lbl1 = new Label(main, SWT.NONE);
		lbl1.setText(Messages.DateFilterDropDownComposite_Between);
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lbl1.getLayoutData()).horizontalIndent = 2;
		
		customComp = new Composite(main, SWT.NONE);
		customComp.setLayout(new GridLayout(3, false));
		((GridLayout)customComp.getLayout()).marginHeight = 0;
		customComp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		
		
		dtStart = new DateTime(customComp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.DATE);
		dtStart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		Listener validateListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				validate();
			}};
		dtStart.addListener(SWT.Selection, validateListener);
		
		lbl2 = new Label(customComp, SWT.NONE);
		lbl2.setText(Messages.DateFilterDropDownComposite_And);
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		dtEnd = new DateTime(customComp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtEnd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		dtEnd.addListener(SWT.Selection, validateListener);
		
		cdEndDate = new ControlDecoration(dtEnd, SWT.RIGHT | SWT.TOP);
		cdEndDate.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdEndDate.setShowHover(true);
		cdEndDate.hide();
		
		
		cmbFilter.setInput(filters);
		cmbFilter.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				DateFilterComposite.DateFilter filter = (DateFilterComposite.DateFilter) ((IStructuredSelection)cmbFilter.getSelection()).getFirstElement();
				//lbl1.setText(filter.getGuiName());
				setCustom(filter == DateFilterComposite.DateFilter.CUSTOM);
				String ll = filter.getLabel();
				if (showDateRangeLabel && ll != null){
					lbl1.setText("[" + ll + "]"); //$NON-NLS-1$ //$NON-NLS-2$
					getParent().layout(true);
				}
				main.layout(true, true);
				validate();
			}
		});
		cmbFilter.addSelectionChangedListener(fireListener);
		
		
		
		DateFilterComposite.DateFilter sel = filters[0];
		if (defaultValue != null){
			sel = defaultValue;
		}
		cmbFilter.setSelection(new StructuredSelection(sel));
		
		SelectionListener dSelection = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireListeners(null);
			}
		};
		dtStart.addSelectionListener(dSelection);
		dtEnd.addSelectionListener(dSelection);
	}

	
	/**
	 * Displays the appropriate fields if custom date
	 * operator is chosen.
	 * 
	 * @param isCustom
	 */
	private void setCustom(boolean isCustom){
		if (isCustom){
			lbl1.setText(Messages.DateFilterDropDownComposite_Between);
			((GridData)customComp.getLayoutData()).widthHint = customComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}else{
			lbl1.setText(""); //$NON-NLS-1$
			((GridData)customComp.getLayoutData()).widthHint = 0;
		}
		customComp.setVisible(isCustom);
		getParent().layout(true, true);		
	}
	
	/**
	 * @return a date filter comprised of the components
	 */
	public DateFilterComposite.DateFilter getDateFilter(){
		DateFilterComposite.DateFilter filter = (DateFilterComposite.DateFilter)  ((IStructuredSelection)cmbFilter.getSelection()).iterator().next();
		return filter;
	}
	
	public LocalDate getCustomStartDate() {
		return SmartUtils.toDate(dtStart);
	}

	public LocalDate getCustomEndDate() {
		return SmartUtils.toDate(dtEnd);
	}
	
	public void setDateFilter(DateFilterComposite.DateFilter filter, LocalDate[] customDates){
		cmbFilter.setSelection(new StructuredSelection(filter));
		if (filter == DateFilter.CUSTOM){
			SmartUtils.initDateTimeWidget(dtStart, customDates[0]);
			SmartUtils.initDateTimeWidget(dtEnd, customDates[1]);
		}
	}
	
	
	public void validate(){
		String error = null;
		
		DateFilterComposite.DateFilter filter = getDateFilter();
		if (filter.getEndDate() != null && filter.getStartDate() != null && 
				filter.getEndDate().isBefore(filter.getStartDate())){
			error = Messages.DateFilterDropDownComposite_InvalidDate;
		}
		
		if (error != null){
			cdEndDate.setDescriptionText(error);
			cdEndDate.show();
		}else{
			cdEndDate.hide();
		}
		
		//eg: I think this is a bug, but this ensures the x disappears when hidden;
		//without it the x stays visible in some situations 
		cdEndDate.getControl().getParent().getParent().getParent().layout();
	}
	
}
