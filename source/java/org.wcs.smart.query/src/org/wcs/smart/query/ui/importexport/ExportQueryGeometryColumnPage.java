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
package org.wcs.smart.query.ui.importexport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query page for the query export wizard
 * that lists the export type options.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryGeometryColumnPage extends WizardPage {

	public static final String PAGE_NAME = "ExportLayers"; //$NON-NLS-1$
	
	private CheckboxTableViewer outputOptions;
	
	/**
	 * @param pageName
	 */
	protected ExportQueryGeometryColumnPage() {
		super(PAGE_NAME);
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(1, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportQueryGeometryColumnPage_GeometryLabel);
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		wrapper.setLayout(new TableColumnLayout());
		
		outputOptions = CheckboxTableViewer.newCheckList(wrapper, SWT.BORDER | SWT.MULTI );
		
		outputOptions.setContentProvider(ArrayContentProvider.getInstance());
		outputOptions.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof QueryColumn qc) return qc.getName();
				return super.getText(element);
			}
		});
		TableColumn tc = new TableColumn(outputOptions.getTable(), SWT.NONE);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(tc, new ColumnWeightData(100));
		
		outputOptions.addSelectionChangedListener(e->
			setPageComplete(outputOptions.getCheckedElements().length != 0)
		);
		
		setTitle(Messages.ExportQueryLocationPage_PageTitle + ": " + ((ExportQueryWizard)getWizard()).getQuery().getName()); //$NON-NLS-1$
		setMessage(Messages.ExportQueryGeometryColumnPage_PageMessage);
		setControl(main);
	}

	public void initValues() {
		List<QueryColumn> geoms = ((ExportQueryWizard)getWizard()).getGeometryColumns(((ExportQueryWizard)getWizard()).getQueryExporter());
		outputOptions.setInput( geoms );
		outputOptions.setAllChecked(false);
		for (QueryColumn qc : geoms) {
			if (qc.isDefaultGeometryColumn()) {
				outputOptions.setChecked(qc, true);
			}
		}
		setPageComplete(outputOptions.getCheckedElements().length != 0);
	}
	
	/**
	 * @return the exporter for the selected export option
	 */
	public List<QueryColumn> getGeometryColumns(){
		List<QueryColumn> values = new ArrayList<>();
		for (Object x : outputOptions.getCheckedElements()) {
			values.add((QueryColumn) x);
		}
		return values;
	}
	
	public void performFinish(){
	}
	
	@Override
    public IWizardPage getNextPage() {
    	return getWizard().getNextPage(this);    	
    }
}
