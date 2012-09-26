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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

/**
 * Wizard page for selecting GPX file to import and
 * associated options.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportGpxWizardPage extends WizardPage { 
	/**
	 * 
	 */
	public static final String PAGE_NAME = "Import Waypoints and Tracks From GPX File";

	private Button opSelect;
	private Button opDate;
	private Button opAll;
	
	private Button btnRemove;
	private Button btnAdd;

	private ListViewer lstFiles;
	private WritableList files = new WritableList();
	
	private boolean importAll;
	
	private ImportWpSelectWizardPage nextPage = null;
	/**
	 * @param pageName
	 */
	protected ImportGpxWizardPage( ImportGpsDataWizard wizard ) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}
	/**
	 * 
	 * @return <code>true</code> if all waypoints are to be imported 
	 * and assigned to the correct day; <code>false</code> if waypoints
	 * are to be imported for only the current day or if waypoints
	 * are to be selected from a list.
	 */
	public boolean getImportAll(){
		return importAll;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Composite center = new Composite(comp, SWT.NONE);
		center.setLayout(new GridLayout(3, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Files:");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		lstFiles = new ListViewer(center, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstFiles.setLabelProvider(new LabelProvider());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 100;
		lstFiles.getList().setLayoutData(gd);
		lstFiles.setInput(files);
		
		Composite buttons = new Composite(center, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = gl.marginBottom = gl.marginHeight = 0;
		buttons.setLayout(gl);		
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		btnAdd = new Button(buttons, SWT.PUSH);
		btnAdd.setText("Add");
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN );
				
				fd.setFilterExtensions(new String[]{"*.gpx", "*.*"});
				fd.setFilterNames(new String[]{"GPX files (*.gpx)", "All Files (*.*)"});
				
				String x = fd.open();
				if (x != null){
					for (int i = 0; i < fd.getFileNames().length; i ++){
						String file = (new File(fd.getFilterPath(),fd.getFileNames()[i])).toString() ;
						if (!files.contains(file)){
							files.add(file);
						}
					}
					lstFiles.refresh();
				}
				updateComplete();
			}		
		});
		
		btnRemove = new Button(buttons, SWT.PUSH);
		btnRemove.setText("Remove");
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = ((IStructuredSelection)lstFiles.getSelection());
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					files.remove(x);
				}
				lstFiles.refresh();
				updateComplete();
			}	
		});
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		ops.setLayout(new GridLayout(1, false));
		
		this.importAll = true;
		opAll = new Button(ops, SWT.RADIO);
		opAll.setText("Import All (and assign to correct day)");
		opAll.setSelection(true);
		opAll.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				updateComplete();
				importAll = true;
			}
		});
		
		opDate = new Button(ops, SWT.RADIO);
		opDate.setText("Import Only " + ((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() + "s for " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(((ImportGpsDataWizard)getWizard()).getCurrentDate()) );
		opDate.setSelection(false);
		opDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateComplete();
				importAll = false;
			}
		});
		
		opSelect = new Button(ops, SWT.RADIO);
		opSelect.setText("Select which " + ((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() + "s to import for " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(((ImportGpsDataWizard)getWizard()).getCurrentDate()));
		opSelect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateComplete();
				importAll = false;
			}
		});
		setPageComplete(false);
		
		super.setMessage("Select the location where you wish to import " +((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() + "s from.");
		super.setControl(comp);
	}
	
	private void updateComplete(){
		if (files.size() == 0){
			setPageComplete(false);
			return;
		}
		
		setPageComplete(true);
		if (opDate.getSelection() || opAll.getSelection() ){
			((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		}else{
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
		}
		getWizard().getContainer().updateButtons();		
	}
	
	/**
	 * 
	 * @return list of files selected by the user
	 */
	@SuppressWarnings("unchecked")
	public List<String> getFiles(){
		List<String> fs = new ArrayList<String>();
		fs.addAll(files);
		return fs;
	}

	public boolean importAll(){
		if (opSelect.getSelection()){
			return true;
		}
		return false;
	}
	
	
	@Override
    public IWizardPage getNextPage() {
		if (opDate.getSelection() || opAll.getSelection() ){
			//not more pages
			return null;
		}else if (opSelect.getSelection()){
			if (nextPage == null){
				nextPage = new ImportWpSelectWizardPage((ImportGpsDataWizard) getWizard());
			}
			return nextPage;
		}
		return null;
    }
}
