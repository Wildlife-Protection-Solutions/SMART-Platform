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
package org.wcs.smart.query.ui;

import java.util.Locale;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartContext;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * A composite for query date filters.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDateFilterComposite extends Composite {

	private Label lblDateFilter;
	private ComboViewer cmbFilterOptions;
	private ComboViewer cmbDateField;
	
	private DateTime dtStart;
	private DateTime dtEnd;
	private Label lbl1;
	private Label lbl2;
	private Composite main;
	
	private ControlDecoration cdEndDate;
	
	private IDateFieldFilter[] fieldOps;
	private IDateFilter[] filterOps;
	private boolean showLabel;
	/**
	 * create new composite
	 * @param parent parent composite
	 * @param fieldOps date field options; can be null
	 * @param filterOps date filter options 
	 */
	public QueryDateFilterComposite(Composite parent, 
			IDateFieldFilter[] fieldOps, 
			IDateFilter[] filterOps) {
		this(parent, fieldOps, filterOps, false);
	}
	
	/**
	 * create new composite
	 * @param parent parent composite
	 * @param fieldOps date field options; can be null
	 * @param filterOps date filter options 
	 * @param includeLabel  if the "Date" label should be included in the 
	 * composite
	 */
	public QueryDateFilterComposite(Composite parent, 
			IDateFieldFilter[] fieldOps, 
			IDateFilter[] filterOps, boolean includeLabel) {
		
		super(parent, SWT.NONE);
		this.showLabel = includeLabel;
		this.fieldOps = fieldOps;
		this.filterOps = filterOps;
		
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginTop = 0;
		layout.marginBottom = 0;
		setLayout(layout);
		
		createComponent();
	}
	/**
	 * Adapts the components of the composite to the given
	 * form toolkit.
	 * 
	 * @param toolkit
	 */
	public void adapt(FormToolkit toolkit){
		if (cmbDateField != null){
			toolkit.adapt(cmbDateField.getControl(), false, true);
		}
		
		toolkit.adapt(cmbFilterOptions.getControl(), false, true);
		if (lblDateFilter != null){
			toolkit.adapt(lblDateFilter, false, false);
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
		
		GridLayout layout = new GridLayout(7, false);
		//layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if(showLabel){
			lblDateFilter = new Label(main, SWT.NONE);
			lblDateFilter.setText(Messages.QueryDateFilterComposite_DateLabel);
		}
		
		if (fieldOps != null){
			cmbDateField = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbDateField.setContentProvider(ArrayContentProvider.getInstance());
			cmbDateField.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element) {
					if (element instanceof IDateFieldFilter){
						return ((IDateFieldFilter) element).getGuiName(Locale.getDefault());
					}
					return super.getText(element);
				}
			});
	
			cmbDateField.setInput(fieldOps);
			cmbDateField.getCombo().select(0);
		}
		
		
		cmbFilterOptions = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbFilterOptions.setContentProvider(ArrayContentProvider.getInstance());
		cmbFilterOptions.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof IDateFilter){
					return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(
							element,
							Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		cmbFilterOptions.setInput(filterOps);
	
		cmbFilterOptions.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				lbl1.setText(""); //$NON-NLS-1$
				
				IDateFilter filter = (IDateFilter) ((IStructuredSelection)cmbFilterOptions.getSelection()).getFirstElement();
				setCustom(filter instanceof CustomDateFilter);
				lbl1.setText(filter.getLabel());
			
				main.layout();
				
				validate();
			}
		});
		lbl1 = new Label(main, SWT.NONE);
		lbl1.setText(Messages.QueryDateFilterComposite_BetweenLabel);
		dtStart = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		Listener validateListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				validate();
			}};
			
		dtStart.addListener(SWT.Selection, validateListener);
		lbl2 = new Label(main, SWT.NONE);
		lbl2.setText(Messages.QueryDateFilterComposite_AndLabel);
		dtEnd = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER | SWT.DATE);
		dtEnd.addListener(SWT.Selection, validateListener);
		
		cdEndDate = new ControlDecoration(dtEnd, SWT.RIGHT | SWT.TOP);
		cdEndDate.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdEndDate.setShowHover(true);
		cdEndDate.hide();
		
		IDateFilter sel = filterOps[0];
		for (IDateFilter f : filterOps){
			if (f.equals(AllDatesFilter.INSTANCE)){
				sel = f;
				break;
			}
		}
		cmbFilterOptions.setSelection(new StructuredSelection(sel));
	}

	
	/**
	 * Displays the appropriate fields if custom date
	 * operator is chosen.
	 * 
	 * @param isCustom
	 */
	private void setCustom(boolean isCustom){
		dtEnd.setVisible(isCustom);
		dtStart.setVisible(isCustom);
		lbl2.setVisible(isCustom);
		if (isCustom){
			lbl1.setText(Messages.QueryDateFilterComposite_BetweenLabel2);
		}
	}
	
	/**
	 * @return a date filter comprised of the components
	 */
	public DateFilter getDateFilter(){
		IDateFilter filter = (IDateFilter)  ((IStructuredSelection)cmbFilterOptions.getSelection()).iterator().next();
		IDateFieldFilter field = null;
		if (cmbDateField != null){
			field = (IDateFieldFilter) ((IStructuredSelection)cmbDateField.getSelection()).iterator().next();
		}
		
		if (filter instanceof CustomDateFilter){
			java.sql.Date start = new java.sql.Date(SmartUtils.getDate(dtStart).getTime());
			java.sql.Date end = new java.sql.Date(SmartUtils.getDate(dtEnd).getTime());
			((CustomDateFilter) filter).setDates(start, end);
			return new DateFilter(field, filter);
		}else{
			return new DateFilter(field, filter);
		}
	}
	
	public void validate(){
		String error = null;
		
		DateFilter filter = getDateFilter();
		error = filter.getDateFilterOption().validate();
		if (error != null){
			error = Messages.QueryDateFilterComposite_InvalidDate;
			cdEndDate.setDescriptionText(error);
			cdEndDate.show();
		}else{
			cdEndDate.hide();
		}
		
		QuerySourceProvider.getSourceProviderFromContext().setQueryDateValid(error == null, error);
	}
	
}
