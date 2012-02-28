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
import org.eclipse.swt.widgets.Text;

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

	private Text txtFile;
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
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		
		Composite fileComp = new Composite(center, SWT.NONE);
		fileComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fileComp.setLayout(new GridLayout(3, false));
		Label lbl = new Label(fileComp, SWT.NONE);
		lbl.setText("File:");
		
		txtFile = new Text(fileComp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.setEditable(false);
		Button btnBrowse = new Button(fileComp, SWT.PUSH);
		btnBrowse.setText("&Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(ImportGpxWizardPage.this.getShell());
				
				fd.setFilterExtensions(new String[]{"*.gpx", "*.*"});
				fd.setFilterNames(new String[]{"GPX files (*.gpx)", "All Files (*.*)"});
				if (txtFile.getText().length() > 0){
					File f = new File(txtFile.getText());
					fd.setFileName(f.getName());
					fd.setFilterPath(f.getAbsolutePath());
				}
				String file = fd.open();
				if (file != null){
					txtFile.setText(file);
					updateComplete();
				}
			}
		});
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
		if (txtFile.getText().isEmpty()){
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
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
	
	public String getFileName(){
		return txtFile.getText();
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
