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

import java.io.File;
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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
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
public class ExportQueryDefLocationPage extends WizardPage {

	private Text txtFile = null;

	private Button btnFolder, btnCa;
	private CheckboxTableViewer caList;
	
	/**
	 * Creates a new query wizard page.
	 */
	public ExportQueryDefLocationPage() {
		super(Messages.ExportQueryLocationPage_PageName);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportQueryDefLocationPage_DestinationLabel);
		
		btnFolder = new Button(main, SWT.RADIO);
		btnFolder.setText(Messages.ExportQueryDefLocationPage_FolderOpLabel);
		
		btnCa = new Button(main, SWT.RADIO);
		btnCa.setText(Messages.ExportQueryDefLocationPage_CaOpLabel);

		
		final Composite stack = new Composite(main, SWT.NONE);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		stack.setLayout(new StackLayout());
		
		final Composite fileComp = new Composite(stack, SWT.NONE);
		fileComp.setLayout(new GridLayout(2, false));
		
		txtFile = new Text(fileComp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();	
			}
		});
		Button btnBrowse = new Button(fileComp, SWT.NONE);
		btnBrowse.setText(Messages.ExportQueryLocationPage_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
				dd.setText(Messages.ExportQueryListPage_DirDialogText);
				dd.setMessage(Messages.ExportQueryListPage_DirDialogMessage);
				if (txtFile.getText().length() > 0) {
					dd.setFilterPath(txtFile.getText());
				}
				String f = dd.open();
				if (f != null) {
					txtFile.setData(f);
				}
				
			}
		});
		
		final Composite caComp = new Composite(stack, SWT.NONE);
		caComp.setLayout(new GridLayout());
		
		
		caList = CheckboxTableViewer.newCheckList(caComp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		caList.setContentProvider(ArrayContentProvider.getInstance());
		caList.setLabelProvider(new ConservationAreaLabelProvider());
		caList.setInput(Messages.ExportQueryDefLocationPage_LoadingLbl);
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
		SelectionAdapter layout = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnFolder.getSelection()){
					((StackLayout)stack.getLayout()).topControl = fileComp;
				}else if (btnCa.getSelection()){
					((StackLayout)stack.getLayout()).topControl = caComp;
				}
				stack.layout();
				validate();
			}
		};
		btnCa.addSelectionListener(layout);
		btnFolder.addSelectionListener(layout);
		
		//initialize widgets
		loadConservationAreas();
		String location = getWizard().getDialogSettings() != null ? getWizard().getDialogSettings().get(ExportQueryWizard.LAST_DIR_KEY) : null;
		if (location == null){
			location = System.getProperty("user.home"); //$NON-NLS-1$
		}
		txtFile.setText(location);
		btnFolder.setSelection(true);
		((StackLayout)stack.getLayout()).topControl = fileComp;
		validate();
		
		setTitle(Messages.ExportQueryDefLocationPage_WizardTitle);
		setMessage(Messages.ExportQueryLocationPage_DialogMessage);
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
		if (btnFolder.getSelection()){
			if (txtFile.getText().isEmpty()){
				error = Messages.ExportQueryDefLocationPage_InvalidFolder;
			}
		}else if (btnCa.getSelection()){
			if (caList.getCheckedElements().length == 0){
				error = Messages.ExportQueryDefLocationPage_InvalidCaSelection;
			}
		}
		
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	/**
	 * 
	 * @return the export folder location or null if should export
	 * to conservation area
	 */
	public File getExportLocation(){
		if (btnFolder.getSelection()){
			return new File(txtFile.getText());
		}
		return null;
	}
	
	/**
	 * 
	 * @return null if should export to file otherwise a list
	 * of conservation areas to export to
	 */
	public List<ConservationArea> getConservationAreasToExport(){
		if (btnCa.getSelection()){
			List<ConservationArea> cas = new ArrayList<ConservationArea>();
			for (Object x : caList.getCheckedElements()){
				if (x instanceof ConservationArea){
					cas.add((ConservationArea)x);
				}
			}
			return cas;
		}
		return null;
	}
}
