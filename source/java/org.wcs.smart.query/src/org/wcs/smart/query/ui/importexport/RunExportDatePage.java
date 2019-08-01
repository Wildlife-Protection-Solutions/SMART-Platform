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
package org.wcs.smart.query.ui.importexport;

import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.QueryDateFilterComposite;

/**
 * Wizard page for selecting date range
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class RunExportDatePage extends WizardPage {

	public static final String NAME = "DATEPAGE"; //$NON-NLS-1$
	
	private QueryDateFilterComposite dateComposite;
	
	protected RunExportDatePage() {
		super(NAME);
	}

	public RunExportWizard getWizardInternal() {
		return (RunExportWizard)getWizard();
	}
	
	public DateFilter getDateFilters() {
		return dateComposite.getDateFilter();
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite core = new Composite(parent , SWT.NONE);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;

		Composite dateFilterComp = new Composite(core, SWT.NONE);
		dateFilterComp.setLayout(new GridLayout());
		dateFilterComp.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		((GridLayout)dateFilterComp.getLayout()).marginWidth = 0;
		((GridLayout)dateFilterComp.getLayout()).marginHeight = 0;

		List<IDateFieldFilter> fields = getWizardInternal().getDateFilters();
		dateComposite = new QueryDateFilterComposite(dateFilterComp, 
				fields.toArray(new IDateFieldFilter[fields.size()]), IDateFilter.DATE_FILTERS);
		//hack to get 2line layout
		Composite d = (Composite) dateComposite.getChildren()[0];
		d.setLayout(new GridLayout());
		Composite top = new Composite(d, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		Composite bottom = new Composite(d, SWT.NONE);
		bottom.setLayout(new GridLayout(4, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		
		d.getChildren()[0].setParent(top);
		d.getChildren()[0].setParent(top);
		d.getChildren()[0].setParent(bottom);
		d.getChildren()[0].setParent(bottom);
		d.getChildren()[0].setParent(bottom);
		d.getChildren()[0].setParent(bottom);

		dateComposite.addChangeListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				core.layout(true);
			}
		});
		setPageComplete(true);
		setControl(core);
		
		setTitle(Messages.RunExportDatePage_PageName);
		setMessage(Messages.RunExportDatePage_PageMessage);
	}
	
	@Override
	public IWizardPage getNextPage() {
		((RunExportOptionsPage)getWizard().getNextPage(this)).updateControls();
		return super.getNextPage();
	}

}
