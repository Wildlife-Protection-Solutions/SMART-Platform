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

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Wizard page shown when user chooses to import tracks
 * from waypoints.  This page allows users to select
 * if they only want to import the current patrol leg day
 * or if they want to import all days.
 * @author egouge
 * @since 1.0.0
 */
public class ImportFromWaypointWizardPage extends WizardPage { 
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportFromWaypointWizardPage_PageName;

	private Button opDate;
	private Button opAll;

	private boolean importAll;
	
	/**
	 * @param pageName
	 */
	protected ImportFromWaypointWizardPage( ImportGpsDataWizard wizard ) {
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
		
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ops.setLayout(new GridLayout(1, false));
		
		this.importAll = true;
		opAll = new Button(ops, SWT.RADIO);
		opAll.setText(Messages.ImportFromWaypointWizardPage_OpGenerateAllTracks);
		opAll.setSelection(true);
		opAll.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				updateComplete();
				importAll = true;
			}
		});
		
		opDate = new Button(ops, SWT.RADIO);
		opDate.setText(MessageFormat.format(
			Messages.ImportFromWaypointWizardPage_OpGenerateDayTracks,
			new Object[]{DateFormat.getDateInstance(DateFormat.MEDIUM).format(((ImportGpsDataWizard)getWizard()).getCurrentDate())}));
		opDate.setSelection(false);
		opDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateComplete();
				importAll = false;
			}
		});
		
		updateComplete();
		super.setMessage(Messages.ImportFromWaypointWizardPage_PageMessage);
		super.setControl(comp);
	}
	
	private void updateComplete(){		
		setPageComplete(true);
		((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		getWizard().getContainer().updateButtons();		
	}

	public boolean importAll(){
		if (opAll.getSelection()){
			return true;
		}
		return false;
	}
	
	@Override
    public IWizardPage getNextPage() {
		return null;
    }
}
