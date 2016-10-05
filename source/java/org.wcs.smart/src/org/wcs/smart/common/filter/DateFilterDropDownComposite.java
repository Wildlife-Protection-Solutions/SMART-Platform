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

import java.util.ArrayList;
import java.util.Date;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
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
	/**
	 * create new composite
	 * @param parent parent composite
	 * @param fieldOps date field options; can be null
	 * @param filterOps date filter options 
	 */
	public DateFilterDropDownComposite(Composite parent, 
			DateFilterComposite.DateFilter[] filters, DateFilter defaultValue) {
		super(parent, SWT.NONE);
		
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
	
	
	private void createComponent(){
		main = new Composite(this, SWT.NONE);
		
		int cols = 6;
		
		GridLayout layout = new GridLayout(cols, false);
		//layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cmbFilter = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
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
		cmbFilter.setInput(filters);
	
		cmbFilter.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				DateFilterComposite.DateFilter filter = (DateFilterComposite.DateFilter) ((IStructuredSelection)cmbFilter.getSelection()).getFirstElement();
				//lbl1.setText(filter.getGuiName());
				setCustom(filter == DateFilterComposite.DateFilter.CUSTOM);
				main.layout();
				validate();
			}
		});
		cmbFilter.addSelectionChangedListener(fireListener);
		
		lbl1 = new Label(main, SWT.NONE);
		lbl1.setText("Between");
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		dtStart = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Listener validateListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				validate();
			}};
		dtStart.addListener(SWT.Selection, validateListener);
		
		lbl2 = new Label(main, SWT.NONE);
		lbl2.setText("And");
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		dtEnd = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dtEnd.addListener(SWT.Selection, validateListener);
		
		cdEndDate = new ControlDecoration(dtEnd, SWT.RIGHT | SWT.TOP);
		cdEndDate.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdEndDate.setShowHover(true);
		cdEndDate.hide();
		
		
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
			((GridData)dtStart.getLayoutData()).widthHint = dtStart.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			((GridData)dtEnd.getLayoutData()).widthHint = dtEnd.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			((GridData)lbl2.getLayoutData()).widthHint = lbl2.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}else{
			((GridData)dtStart.getLayoutData()).widthHint = 0;
			((GridData)dtEnd.getLayoutData()).widthHint = 0;
			((GridData)lbl2.getLayoutData()).widthHint = 0;
		}
		dtEnd.setVisible(isCustom);
		dtStart.setVisible(isCustom);
		lbl2.setVisible(isCustom);
		
		if (isCustom){
			lbl1.setText("Between");
		}else{
			lbl1.setText("");
		}
		((GridData)lbl1.getLayoutData()).widthHint = lbl1.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
	}
	
	/**
	 * @return a date filter comprised of the components
	 */
	public DateFilterComposite.DateFilter getDateFilter(){
		DateFilterComposite.DateFilter filter = (DateFilterComposite.DateFilter)  ((IStructuredSelection)cmbFilter.getSelection()).iterator().next();
		return filter;
	}
	
	public Date getCustomStartDate() {
		return SmartUtils.getDate(dtStart);
	}

	public Date getCustomEndDate() {
		return SmartUtils.getDate(dtEnd);
	}
	
	public void setDateFilter(DateFilterComposite.DateFilter filter, Date[] customDates){
		cmbFilter.setSelection(new StructuredSelection(filter));
		if (filter == DateFilter.CUSTOM){
			SmartUtils.initDateDateTimeWidget(dtStart, customDates[0]);
			SmartUtils.initDateDateTimeWidget(dtEnd, customDates[1]);
		}
	}
	
	
	public void validate(){
		String error = null;
		
		DateFilterComposite.DateFilter filter = getDateFilter();
		if (filter.getEndDate() != null && filter.getStartDate() != null && filter.getEndDate().before(filter.getStartDate())){
			error = "Invalid end date";
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
