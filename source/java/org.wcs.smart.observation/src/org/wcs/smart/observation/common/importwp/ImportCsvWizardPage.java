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

package org.wcs.smart.observation.common.importwp;

import java.io.FileReader;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.common.importwp.csv.CSVImportConfiguration;
import org.wcs.smart.observation.common.importwp.csv.CsvHeader;
import org.wcs.smart.observation.common.importwp.csv.CsvImportEngine;
import org.wcs.smart.observation.common.importwp.csv.ImportCSVOptionsComposite;
import org.wcs.smart.observation.internal.Messages;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Wizard page for selecting CSV file as waypoint import method
 * 
 * @author Jeff
 * @since 2.0
 */

public class ImportCsvWizardPage extends ImportOptionsWizardPage {
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportCsvWizardPage_0;

	private ImportCSVOptionsComposite ops;
	private ImportOptionsComposite ops2;
		
	private ImportCsvDetailsWizardPage nextPage;
	/**
	 * @param pageName
	 */
	public ImportCsvWizardPage( ImportGpsDataWizard wizard ) {
		super(PAGE_NAME, wizard);
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
		
		ops = new ImportCSVOptionsComposite(center);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		ops.addFileModifyListener(new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				updateComplete();
				return;
			}
		});
	
		ops2 = new ImportOptionsComposite(center, getImportType(), getValidOptions(), getOptionLabels(), getWarningMessage());
		ops2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label lbl = new Label(center, SWT.WRAP);
		lbl.setText(Messages.ImportCsvWizardPage_CSVImportMessage);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 100;
		gd.horizontalIndent = 5;
		gd.verticalIndent = 5;
		lbl.setLayoutData(gd);
		
		
		setPageComplete(false);
		
		super.setTitle(Messages.ImportGpxWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(MessageFormat.format(Messages.ImportGpxWizardPage_PageMessage, new Object[]{ ((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() }));
		super.setControl(comp);
	}
	
	private void updateComplete(){
		
		if(!ops.getFileText().equals( Messages.ImportCsvWizardPage_1)){
			setPageComplete(true);
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
		}

	}
	
	
	@Override
    public IWizardPage getNextPage() {
		if (nextPage == null){
			nextPage = new ImportCsvDetailsWizardPage((ImportGpsDataWizard)getWizard());
		}
		return nextPage;
	}

	@Override
	public boolean beforeMoveNext(WizardPage nextPage) {
		if (nextPage instanceof ImportCsvDetailsWizardPage){
			
			((ImportGpsDataWizard)getWizard()).setImportOption(ops2.getImportOption());
			final CSVImportConfiguration config = ((CsvImportEngine)((ImportGpsDataWizard)getWizard()).getImportEngine()).getConfiguration();
			
			try{
				if (ops.getFileText().equals(config.getFilename()) && config.getDelimiter() == ops.getDelimiter()){
					//nothing has changed; leave the last configuration along
					return true;
				}
				//something has changed; reparse
				config.setFileName(ops.getFileText());
				config.setDelimiter(ops.getDelimiter());
			}catch (Exception ex){
				MessageDialog.openError(getShell(), Messages.ImportCsvWizardPage_ErrorDialogTitle, ex.getMessage());
				return false;
			}
			// read header fields
			try {
				CSVReader reader = new CSVReader(new FileReader(
						ops.getFileText()), config.getDelimiter());
				try {
					String[] headers = reader.readNext();
					CsvHeader[] columnNames = new CsvHeader[headers.length];
					for (int i = 0; i < headers.length; i++) {
						columnNames[i] = new CsvHeader(headers[i], i);
					}
					config.setAvailableColumns(columnNames);
				} finally {
					reader.close();
				}
			} catch (Exception e) {
				ObservationPlugIn.displayLog(Messages.ImportCsvWizardPage_2, e);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean init() {
		return true;
	}

}
