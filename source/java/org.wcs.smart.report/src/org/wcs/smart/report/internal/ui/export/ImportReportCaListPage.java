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
package org.wcs.smart.report.internal.ui.export;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.ui.ReportContentProvider;
import org.wcs.smart.report.ui.ReportContentProvider.RootType;
import org.wcs.smart.report.ui.ReportLabelProvider;

/**
 * Wizard page to select queries from a different Conservation Area
 * to import.
 * 
 * @author Emily
 *
 */
public class ImportReportCaListPage extends WizardPage {
	
	public static final String PAGENAME = "CaReports"; //$NON-NLS-1$
	
	private CheckboxTreeViewer chReports;
	private ConservationArea currentCa;
	
	public ImportReportCaListPage(){
		super(PAGENAME);
	}
	
	
	public List<Report> getReports(){
		Object[] checked = chReports.getCheckedElements();
		List<Report> reports = new ArrayList<Report>();
		for (Object selection : checked){
			if (selection instanceof Report){
				reports.add((Report)selection);
			}
		}
		return reports;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ImportReportCaListPage_ReportLabel);
		
		chReports = new CheckboxTreeViewer(main, SWT.MULTI | SWT.BORDER);
		chReports.setLabelProvider(new ReportLabelProvider());
		chReports.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		chReports.setCheckStateProvider(new ICheckStateProvider() {
			
			@Override
			public boolean isGrayed(Object element) {
				return element instanceof ReportFolder;
			}
			
			@Override
			public boolean isChecked(Object element) {
				return element instanceof ReportFolder;
			}
		});
		chReports.getTree().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (chReports.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)chReports.getSelection());
					selection.getFirstElement();
					boolean value = chReports.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						chReports.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		chReports.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(false);
				for (Object x : chReports.getCheckedElements()){
					if (x instanceof Report){
						setPageComplete(true);
					}
				}
				getContainer().updateButtons();
			}
		});
		
		setControl(main);
		setTitle(Messages.ImportReportCaListPage_Title);
		setMessage(Messages.ImportReportCaListPage_Message);

	}
	
	public void initValues(){
		ConservationArea caa = currentCa;
		currentCa = ((ImportReportCaPage)getWizard().getPage(ImportReportCaPage.PAGENAME)).getConservationArea();
		if (caa == currentCa){
			return;
		}
		
		chReports.setContentProvider(new ReportContentProvider(RootType.SHARED_ONLY, currentCa));
		chReports.setInput(new String[]{Messages.ImportReportCaListPage_LoadingLabel});
		chReports.refresh();
		chReports.expandToLevel(2);
		
		setPageComplete(false);
	}
	
	@Override
	public IWizardPage getNextPage() {
		return getWizard().getPage(ImportReportFolderPage.PAGENAME);
		
	}

}
