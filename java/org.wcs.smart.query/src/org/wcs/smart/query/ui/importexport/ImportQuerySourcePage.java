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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.internal.Messages;

/**
 * Wizard page to select the import source
 * for queries to import.
 * 
 * @author Emily
 *
 */
public class ImportQuerySourcePage extends WizardPage {

	public static final String PAGENAME="Source"; //$NON-NLS-1$
	
	private Button opFile;
	private Button opCa;

	/**
	 * Creates a new query wizard page.
	 */
	protected ImportQuerySourcePage() {
		super(PAGENAME);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));

		Composite inner = new Composite(main, SWT.NONE);
		inner.setLayout(new GridLayout());
		inner.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		opFile = new Button(inner, SWT.RADIO);
		opFile.setText(Messages.ImportQuerySourcePage_OpImportFile);
		opFile.setSelection(true);
		
		opCa = new Button(inner, SWT.RADIO);
		opCa.setText(Messages.ImportQuerySourcePage_OpImportCa);

		
		setTitle(Messages.ImportQuerySourcePage_Title);
		setMessage(Messages.ImportQuerySourcePage_Message);
		setPageComplete(true);
		setControl(main);
	
	}
	
	@Override
	public IWizardPage getNextPage() {
		if (opFile.getSelection()){
			return getWizard().getPage(ImportQueryFilePage.PAGENAME);
		}else if (opCa.getSelection()){
			return getWizard().getPage(ImportQueryCaPage.PAGENAME);
		}
		return null;
	}
	
}
