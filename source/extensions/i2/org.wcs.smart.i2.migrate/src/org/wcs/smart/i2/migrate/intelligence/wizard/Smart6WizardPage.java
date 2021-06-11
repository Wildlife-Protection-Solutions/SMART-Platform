/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.i2.migrate.internal.Messages;

/**
 * Wizard page to collect SMART 6 backup file details
 * 
 * @author Emily
 *
 */
public class Smart6WizardPage extends WizardPage {

	private Text txtFile;
	
	public Smart6WizardPage() {
		super("SMART6Page"); //$NON-NLS-1$
	}

	public String getFile() {
		return txtFile.getText();
	}
	
	@Override
	public void createControl(Composite parent) {
	
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite temp = new Composite(outer, SWT.NONE);
		temp.setLayout(new GridLayout(3, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(temp, SWT.NONE);
		l.setText(Messages.Smart6WizardPage_lblFileName);
		
		txtFile = new Text(temp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnBrowse = new Button(temp, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnBrowse.getLayoutData()).heightHint = txtFile.computeSize(SWT.DEFAULT,  SWT.DEFAULT).y;
		
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
			fd.setFileName(txtFile.getText());
			fd.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.Smart6WizardPage_ZipFiles, Messages.Smart6WizardPage_AllFiles});
			String fname = fd.open();
			if (fname == null) return;
			txtFile.setText(fname);
					
		});
		
		setControl(outer);
		
		setTitle(Messages.Smart6WizardPage_Title);
		setMessage(Messages.Smart6WizardPage_Message);
		
	}

	
}
