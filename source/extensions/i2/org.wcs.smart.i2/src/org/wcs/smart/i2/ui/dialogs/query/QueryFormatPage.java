/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs.query;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.export.CsvEntitySummaryQueryExporter;
import org.wcs.smart.i2.query.export.CsvRecordQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter.ExportOption;
import org.wcs.smart.i2.query.export.ShpRecordQueryExporter;;

/**
 * Export query wizard page for selection query format
 * 
 * @author Emily
 *
 */
public class QueryFormatPage extends WizardPage {

	private static final String LAST_EXPORT_FORMAT = "org.wcs.smart.i2.query.export.format"; //$NON-NLS-1$
	
	private TableViewer outputOptions;
	
	/**
	 * @param pageName
	 */
	protected QueryFormatPage() {
		super("Export_Format"); //$NON-NLS-1$
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(1, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.QueryFormatPage_FormatOp);
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayout(new TableColumnLayout());
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		outputOptions = new TableViewer(wrapper, SWT.BORDER | SWT.SINGLE );
		outputOptions.setContentProvider(ArrayContentProvider.getInstance());
		outputOptions.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof IQueryExporter){
					IQueryExporter exp = (IQueryExporter) element;
					String name = exp.getName(Locale.getDefault());
					String ext = exp.getExtension();
					if (ext != null){
						name = name  + " (*." + ext + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return name;					
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		});
		TableColumn tc = new TableColumn(outputOptions.getTable(), SWT.NONE);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(tc, new ColumnWeightData(100));
		
		List<IQueryExporter> exports = new ArrayList<>();
		exports.add(new CsvRecordQueryExporter());
		exports.add(new ShpRecordQueryExporter());
		exports.add(new CsvEntitySummaryQueryExporter());
		
		for (Iterator<IQueryExporter> iterator = exports.iterator(); iterator.hasNext();) {
			IQueryExporter iQueryExporter = iterator.next();
			if (!iQueryExporter.canExport(((ExportQueryWizard)getWizard()).getQuery().getTypeKey())) iterator.remove();
		}
		
		Collections.sort(exports, new Comparator<IQueryExporter>() {
			@Override
			public int compare(IQueryExporter o1, IQueryExporter o2) {
				return Collator.getInstance().compare(o1.getName(Locale.getDefault()), o2.getName(Locale.getDefault()));
			}
		});
		outputOptions.setInput(exports);
		
		outputOptions.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(!outputOptions.getSelection().isEmpty());
			}
		});
		outputOptions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (!outputOptions.getSelection().isEmpty()){
					setPageComplete(true);
					((ExportQueryWizard)getWizard()).getContainer().showPage( getNextPage() );
				}
				
			}
		});
		
		String id = getWizard().getDialogSettings().get(LAST_EXPORT_FORMAT);
		IQueryExporter defaultExport = null;
		for (IQueryExporter export : exports){
			if (export.getClass().getName().equals(id)){
				defaultExport = export;
			}
		}
		if (defaultExport != null){
			outputOptions.setSelection(new StructuredSelection(defaultExport));
		}
		
		setTitle(MessageFormat.format(Messages.QueryFormatPage_Title, ((ExportQueryWizard)getWizard()).getQuery().getName()));
		setMessage(Messages.QueryFormatPage_Message);
		setControl(main);
		setPageComplete(!outputOptions.getSelection().isEmpty());
	}

	/**
	 * @return the exporter for the selected export option
	 */
	public IQueryExporter getQueryExporter(){
		 return (IQueryExporter) ((IStructuredSelection)outputOptions.getSelection()).getFirstElement();
	}
	
	public void performFinish(){
		try{
			IQueryExporter f = getQueryExporter();
			getWizard().getDialogSettings().put(LAST_EXPORT_FORMAT, f.getClass().getName());
		}catch (Exception ex){
			//eatme
		}
	}
	@Override
    public IWizardPage getNextPage() {
		ExportQueryWizard wizard = (ExportQueryWizard) getWizard();
		IQueryExporter exporter = getQueryExporter();
		if (!exporter.supportsOption(ExportOption.GEOMETRY_COLUMN)) {
			return getWizard().getPage(QueryFormatOptionPage.PAGE_NAME);
		} else {
			List<IQueryColumn> columns = wizard.getGeometryColumns(exporter);
			if (columns != null && columns.size() > 1) {
				return getWizard().getPage(ExportQueryGeometryColumnPage.PAGE_NAME);
			}else {
				return getWizard().getPage(QueryFormatOptionPage.PAGE_NAME);
			}
		}
	}
}

