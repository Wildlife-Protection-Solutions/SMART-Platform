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
package org.wcs.smart.er.ui.surveydesign.importing;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page to select the import source
 * for survey design(s) to import.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportSurveyDesignSourcePage extends WizardPage {

	public static final String PAGENAME = "Source"; //$NON-NLS-1$
	
	private Button opFile;
	private Button opCa;

	/**
	 * Creates a new query wizard page.
	 */
	protected ImportSurveyDesignSourcePage() {
		super(PAGENAME);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));

		opFile = new Button(main, SWT.RADIO);
		opFile.setText("Import from file");
		opFile.setSelection(true);
		
		opCa = new Button(main, SWT.RADIO);
		opCa.setText("Import from another Conservation Area");

		
		setTitle("Import Survey Design(s)");
		setMessage("Select where you want to import survey design(s) from");
		setPageComplete(true);
		setControl(main);
	
	}
	
	@Override
	public IWizardPage getNextPage() {
		if (opFile.getSelection()){
			return getWizard().getPage(ImportSurveyDesignFilesPage.PAGENAME);
		}else if (opCa.getSelection()){
			return getWizard().getPage(ImportSurveyDesignCaPage.PAGENAME);
		}
		return null;
	}
	
}
