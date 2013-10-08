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


import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CsvHeader;
import org.wcs.smart.patrol.internal.ui.importwp.csv.ImportCSVDetailsComposite;

public class ImportCsvDetailsWizardPage extends WizardPage{

	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportCsvDetailsWizardPage_0;

	private ImportCSVDetailsComposite ops;
	private CsvHeader[] columnNames;
	private ImportGpsDataWizard wizard;
	
	/**
	 * @param pageName
	 */
	protected ImportCsvDetailsWizardPage( ImportGpsDataWizard wizard, CsvHeader[] columnNames ) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.columnNames = new CsvHeader[columnNames.length];
		this.columnNames = columnNames;		
		this.wizard = wizard;
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
				
		ops = new ImportCSVDetailsComposite(center, columnNames);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		ops.addColumnsListener(new ISelectionChangedListener(){

			@Override
			public void selectionChanged(SelectionChangedEvent event){
				updateComplete();
			}});
		
		
		
		setPageComplete(false);
		
		super.setTitle(Messages.ImportGpxWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(MessageFormat.format(Messages.ImportGpxWizardPage_PageMessage, new Object[]{ ((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() }));
		super.setControl(comp);
	}
	
		
	private void updateComplete(){

		if ( ops.isValid() ){
			setPageComplete(true);
			//we have a next page to go to in this case, can't finish yet.
			if(wizard.showCsvSelection()){
				((ImportGpsDataWizard)getWizard()).setCanFinish(false);
				return;
			}
			((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		}else{
			setPageComplete(false);
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
		}
		getWizard().getContainer().updateButtons();		
	}
	
	
	
	@Override
    public IWizardPage getNextPage() {

		if(wizard.showCsvSelection()){
			ImportWpSelectWizardPage nextPage = new ImportWpSelectWizardPage((ImportGpsDataWizard) getWizard());
			return nextPage;
		}
		return null;
	}

	public String getDateFormat(){
		return ops.getDateFormat();
	}
	
	public int getXColumnNumber(){
		return ops.getXColumnNumber();
	}
	public int getYColumnNumber(){
		return ops.getYColumnNumber();
	}
	public int getTimeColumnNumber(){
		return ops.getTimeColumnNumber();
	}
	public int getIdColumnNumber(){
		return ops.getIdColumnNumber();
	}
	public int getDateColumnNumber(){
		return ops.getDateColumnNumber();
	}
	public int getCommentsColumnNumber(){
		return ops.getCommentsColumnNumber();
	}
	public Projection getProjection(){
		return ops.GetProjection();
	}
	public boolean skipHeaders(){
		return ops.skipHeaders();
	}
}