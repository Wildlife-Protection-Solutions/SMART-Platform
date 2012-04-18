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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.parser.internal.DateFilter;
import org.wcs.smart.query.parser.internal.DateFilter.DATE_FIELD_OP;
import org.wcs.smart.query.parser.internal.DateFilter.DATE_FILTER_OP;
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
	
	
	/**
	 * create new composite 
	 */
	public QueryDateFilterComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginTop = 10;
		layout.marginBottom = 10;
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
		toolkit.adapt(cmbDateField.getControl(), false, true);
		toolkit.adapt(cmbFilterOptions.getControl(), false, true);
		toolkit.adapt(lblDateFilter, false, false);
		toolkit.adapt(lbl1, false, false);
		toolkit.adapt(lbl2, false, false);
		
		toolkit.adapt(dtStart, false, true);
		toolkit.adapt(dtEnd, false, true);
	}
	
	private void createComponent(){
		final Composite main = new Composite(this, SWT.NONE);
		main.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout(7, false);
		//layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblDateFilter = new Label(main, SWT.NONE);
		lblDateFilter.setText("Date:");
		
		cmbDateField = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDateField.setContentProvider(ArrayContentProvider.getInstance());
		cmbDateField.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof DateFilter.DATE_FIELD_OP){
					return ((DateFilter.DATE_FIELD_OP)element).guiName;
				}
				return super.getText(element);
			}
		});
		cmbDateField.setInput(DateFilter.DATE_FIELD_OP.values());
		cmbDateField.getCombo().select(0);
		
		cmbFilterOptions = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbFilterOptions.setContentProvider(ArrayContentProvider.getInstance());
		cmbFilterOptions.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof DateFilter.DATE_FILTER_OP){
					return ((DateFilter.DATE_FILTER_OP)element).guiName;
				}
				return super.getText(element);
			}
		});
		cmbFilterOptions.setInput(DateFilter.DATE_FILTER_OP.values());
	
		cmbFilterOptions.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				lbl1.setText("");
				
				DATE_FILTER_OP filter = (DATE_FILTER_OP) ((IStructuredSelection)cmbFilterOptions.getSelection()).getFirstElement();
				setCustom(filter == DATE_FILTER_OP.CUSTOM);
				java.sql.Date bits[] = DateFilter.getDate(filter);
				if (bits != null){
					if (filter == DATE_FILTER_OP.LAST_30_DAYS ||
							filter == DATE_FILTER_OP.LAST_60_DAYS 
							){
						DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
						lbl1.setText( "[" + formatter.format( bits[0] ) + " - today]");
					}else if (filter == DATE_FILTER_OP.MONTH_TO_DATE || 
							filter == DATE_FILTER_OP.LAST_MONTH){ 
							
						DateFormat formatter = new SimpleDateFormat("MMM yyyy");
						lbl1.setText( "[" + formatter.format( bits[0] ) + "]");
					}else if (filter == DATE_FILTER_OP.YEAR_TO_DATE){
						DateFormat formatter = new SimpleDateFormat("yyyy");
						lbl1.setText( "[" + formatter.format( bits[0] ) + "]");
					}
				}
				main.layout();
			}
		});
		lbl1 = new Label(main, SWT.NONE);
		lbl1.setText("Between");
		dtStart = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
		lbl2 = new Label(main, SWT.NONE);
		lbl2.setText("And");
		dtEnd = new DateTime(main, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);

		cmbFilterOptions.setSelection(new StructuredSelection(DATE_FILTER_OP.values()[0]));
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
			lbl1.setText("Between");
		}
	}
	
	/**
	 * @return a date filter comprised of the components
	 */
	public DateFilter getDateFilter(){
		DATE_FILTER_OP filter = (DATE_FILTER_OP)  ((IStructuredSelection)cmbFilterOptions.getSelection()).iterator().next();
		if (filter == DATE_FILTER_OP.ALL){
			return null;
		}
		DATE_FIELD_OP field = (DATE_FIELD_OP) ((IStructuredSelection)cmbDateField.getSelection()).iterator().next();
		if (filter == DATE_FILTER_OP.CUSTOM){
			java.sql.Date start = new java.sql.Date(SmartUtils.getDate(dtStart).getTime());
			java.sql.Date end = new java.sql.Date(SmartUtils.getDate(dtEnd).getTime());
			return new DateFilter(field, filter, start, end );
		}else{
			return new DateFilter(field, filter);
		}
	}
	
}
