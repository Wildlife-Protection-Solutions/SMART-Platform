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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CSVImportConfiguration;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CsvHeader;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CsvImportEngine;
import org.wcs.smart.patrol.internal.ui.importwp.csv.ImportCSVDetailsComposite;

/**
 * Wizard page for gathering CSV column to 
 * field mappings. 
 * 
 * @author Jeff
 * @author Emily
 *
 */
public class ImportCsvDetailsWizardPage extends WizardPage implements IImportWizardPage{

	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportCsvDetailsWizardPage_0;

	private ImportCSVDetailsComposite ops;
	private ImportGpsDataWizard wizard;
	
	private ImportWpSelectWizardPage nextPage ;
	private CsvHeader[] lastHeaders;
	
	/**
	 * @param pageName
	 */
	protected ImportCsvDetailsWizardPage( ImportGpsDataWizard wizard) {
		super(PAGE_NAME);
		wizard.addPage(this);
		
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
				
		ops = new ImportCSVDetailsComposite(center);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		ops.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateComplete();
			}
		});
		ops.setConfigData(((CsvImportEngine)wizard.getImportEngine()).getConfiguration());
		lastHeaders = ((CsvImportEngine)wizard.getImportEngine()).getConfiguration().getAvailableColumns();
		((GridData)ops.getLayoutData()).widthHint = 350;
		
		setPageComplete(false);
		
		super.setTitle(Messages.ImportGpxWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(Messages.ImportCsvDetailsWizardPage_PageMessage);
		super.setControl(comp);
	}
	
		
	private void updateComplete(){

		if ( ops.isValid() ){
			setPageComplete(true);
			//we have a next page to go to in this case, can't finish yet.
			if(wizard.getImportOption()==ImportOption.SELECT){
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

	
	/**
	 * updates the configuration object with the options
	 * selected on the gui
	 * 
	 * @param config
	 */
	public void updateConfiguration(CSVImportConfiguration config){
		config.setDateFormat(ops.getDateFormat());
		config.setXColumn(ops.getXColumnNumber());
		config.setYColumn(ops.getYColumnNumber());
		config.setTimeColumn(ops.getTimeColumnNumber());
		config.setIdColumn(ops.getIdColumnNumber());
		config.setDateColumn(ops.getDateColumnNumber());
		config.setCommentsColumn(ops.getCommentsColumnNumber());
		config.setProjection(ops.getProjection());
		config.setSkipHeaders(ops.skipHeaders());
	}
	
	@Override
    public IWizardPage getNextPage() {

		if(wizard.getImportOption()==ImportOption.SELECT){
			if (nextPage == null){
				nextPage = new ImportWpSelectWizardPage((ImportGpsDataWizard) getWizard());
			}
			return nextPage;
		}
		return null;
	}


	@Override
	public boolean beforeMoveNext(WizardPage nextPage) {
		CSVImportConfiguration config = ((CsvImportEngine)((ImportGpsDataWizard)getWizard()).getImportEngine()).getConfiguration();
		updateConfiguration(config);
		
		SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_DELIMITER_KEY, String.valueOf(config.getDelimiter()));
		return true;
	}


	@Override
	public boolean init() {
		if (ops != null) {
			CSVImportConfiguration config = ((CsvImportEngine)((ImportGpsDataWizard)getWizard()).getImportEngine()).getConfiguration();
			if (lastHeaders != config.getAvailableColumns()){
				lastHeaders = config.getAvailableColumns();
				ops.setConfigData(config);
			}
			ops.validate();
			updateComplete();
		}
		return true;
	}

}