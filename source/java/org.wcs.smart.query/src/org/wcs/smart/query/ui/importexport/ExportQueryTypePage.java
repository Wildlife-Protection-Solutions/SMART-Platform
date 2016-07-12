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

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.internal.Messages;

/**
 * Query page for the query export wizard
 * that lists the export type options.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryTypePage extends WizardPage {

	private static final String LAST_EXPORT_FORMAT = "QUERY_EXPORT_FORMAT"; //$NON-NLS-1$
	
	private TableViewer outputOptions;
	
	/**
	 * @param pageName
	 */
	protected ExportQueryTypePage() {
		super(Messages.ExportQueryTypePage_PageName);
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(1, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportQueryTypePage_SelectFormatLabel);
		
		outputOptions = new TableViewer(main, SWT.BORDER | SWT.SINGLE );
		outputOptions.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outputOptions.setContentProvider(ArrayContentProvider.getInstance());
		outputOptions.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof IQueryExporter){
					IQueryExporter exp = (IQueryExporter) element;
					String name = exp.getName();
					String ext = exp.getDefaultExtension();
					if (ext != null){
						name = name  + " (*." + ext + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return name;					
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		});
		List<IQueryExporter> exports = QueryExportEngine.getQueryExports(((ExportQueryWizard)getWizard()).getQuery() );
		Collections.sort(exports, new Comparator<IQueryExporter>() {

			@Override
			public int compare(IQueryExporter o1, IQueryExporter o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
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
			if (export.getId().equals(id)){
				defaultExport = export;
			}
		}
		if (defaultExport != null){
			outputOptions.setSelection(new StructuredSelection(defaultExport));
		}
		
		setTitle(Messages.ExportQueryLocationPage_PageTitle + ": " + ((ExportQueryWizard)getWizard()).getQuery().getName()); //$NON-NLS-1$
		setMessage(Messages.ExportQueryTypePage_DialogMessage);
		setControl(main);
		setPageComplete(!outputOptions.getSelection().isEmpty());
	}

	/**
	 * @return the exporter for the selected export option
	 */
	public IQueryExporter getQueryExporter(){
		 return   (IQueryExporter) ((IStructuredSelection)outputOptions.getSelection()).getFirstElement();
	}
	
	public void performFinish(){
		try{
			IQueryExporter f = getQueryExporter();
			getWizard().getDialogSettings().put(LAST_EXPORT_FORMAT, f.getId());
		}catch (Exception ex){
			//eatme
		}
	}
	
    /* (non-Javadoc)
     * Method declared on IWizardPage.
     * The default behavior is to ask the wizard for the next page.
     */
    public IWizardPage getNextPage() {
    	IQueryExporter exporter = getQueryExporter();
    	if (exporter.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
    		return getWizard().getPage(ExportQueryListPage.PAGE_NAME);
    	}else{
    		return getWizard().getNextPage(this);
    	}
    }
}
