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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.ImportOptionsComposite.ImportOption;

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

	private ImportOptionsComposite ops;
		
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
		return ops.getImportOption() == ImportOption.ALL;
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
		
		ImportGpsDataWizard w = ((ImportGpsDataWizard)getWizard());
		ops = new ImportOptionsComposite(center, 
				w.getCurrentDate(),
				w.getType(),
				new ImportOption[]{ImportOption.ALL, ImportOption.DATE},
				new String[]{Messages.ImportFromWaypointWizardPage_OpGenerateAllTracks,
			Messages.ImportFromWaypointWizardPage_OpGenerateDayTracks1});
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ops.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				updateComplete();
			}});
		
		
		
		updateComplete();
		super.setTitle(Messages.ImportFromWaypointWizardPage_PageTitle);
		super.setMessage(Messages.ImportFromWaypointWizardPage_PageMessage);
		super.setControl(comp);
	}
	
	private void updateComplete(){		
		setPageComplete(true);
		((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		getWizard().getContainer().updateButtons();		
	}

	
	@Override
    public IWizardPage getNextPage() {
		return null;
    }
}
