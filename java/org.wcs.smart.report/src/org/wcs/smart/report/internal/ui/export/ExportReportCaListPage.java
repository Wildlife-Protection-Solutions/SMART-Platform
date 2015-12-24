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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

/**
 * Query wizard page to select the export location for
 * exporting query definitions. This page provides two
 * options, one for exporting to a folder and another
 * for exporting to an existing Conservation Area.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportReportCaListPage extends WizardPage {

	public static final String PAGE_NAME = "capage"; //$NON-NLS-1$
	private CheckboxTableViewer caList;
	
	/**
	 * Creates a new query wizard page.
	 */
	public ExportReportCaListPage() {
		super(PAGE_NAME);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Conservation Areas:");

		caList = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		caList.setContentProvider(ArrayContentProvider.getInstance());
		caList.setLabelProvider(new ConservationAreaLabelProvider());
		caList.setInput("Loading...");
		caList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		caList.getTable().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (caList.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)caList.getSelection());
					boolean value = caList.getChecked( selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						caList.setChecked(tp, !value);
					}
					e.doit = false;	
				}
				validate();
			}
		});
		caList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		
		//initialize widgets
		loadConservationAreas();
		validate();
		
		setTitle("Export Report");
		setMessage("Select Conservation Areas to export reports to");
		setControl(main);
	}

	private void loadConservationAreas(){
		Job j = new Job("loadca"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<ConservationArea> cas = null;
				Session s = HibernateManager.openSession();
				try{
					cas = HibernateManager.getConservationAreas(s);
					cas.remove(SmartDB.getCurrentConservationArea());
				}catch (Exception ex){
					QueryPlugIn.displayLog(ex.getMessage(), ex);
					return Status.OK_STATUS;
				}finally{
					s.close();
				}
				final List<ConservationArea> cas1 = cas;
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						caList.setInput(cas1);
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	

	public IWizardPage getNextPage() {
		return null;
	}

	private void validate() {
		String error = null;
		if (caList.getCheckedElements().length == 0){
			error = "At least one Conservation Area must be selected";
		}
		
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	/**
	 * 
	 * @return null if should export to file otherwise a list
	 * of conservation areas to export to
	 */
	public List<ConservationArea> getConservationAreasToExport(){
		List<ConservationArea> cas = new ArrayList<ConservationArea>();
		for (Object x : caList.getCheckedElements()) {
			if (x instanceof ConservationArea) {
				cas.add((ConservationArea) x);
			}
		}
		return cas;
	}
}
