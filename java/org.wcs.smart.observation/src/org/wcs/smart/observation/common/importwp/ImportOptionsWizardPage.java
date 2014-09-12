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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.internal.Messages;

/**
 * Import page that allows to select some options
 * 
 * @author elitvin
 * @since 3.0.0
 */
public abstract class ImportOptionsWizardPage extends WizardPage implements IImportWizardPage {

	private ImportOption[] validOptions;
	private String[] labels;
	private String warningMessage = null;
	
	
	public ImportOptionsWizardPage(String pageName, ImportGpsDataWizard wizard) {
		super(pageName);
		wizard.addPage(this);
		initDefaultOptions(wizard.getDateOption());
	}

	protected void initDefaultOptions(Date date) {
		String dateStr = date != null ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(date) : ""; //$NON-NLS-1$
		validOptions = new ImportOption[]{ImportOption.ALL, ImportOption.DATE, ImportOption.SELECT};
		labels = new String[]{Messages.ImportGpxWizardPage_ImportAllOp,
				MessageFormat.format(Messages.ImportGpxWizardPage_ImportSingleDayOp, new Object[]{getImportType().guiName.toLowerCase(), dateStr}),
				MessageFormat.format(Messages.ImportGpxWizardPage_ImportSelectionOp, new Object[]{getImportType().guiName.toLowerCase(), dateStr})
		};
	}
	
	public ImportOption[] getValidOptions() {
		return validOptions;
	}
	
	public void setOptions(ImportOption[] validOptions, String[] labels) {
		this.validOptions = validOptions;
		this.labels = labels;
	}
	
	public String[] getOptionLabels() {
		return labels;
	}
	
	public String getWarningMessage() {
		return warningMessage;
	}
	public void setWarningMessage(String warningMessage) {
		this.warningMessage = warningMessage;
	}
	
	protected ImportType getImportType() {
		return ((ImportGpsDataWizard)getWizard()).getType();		
	}
}
