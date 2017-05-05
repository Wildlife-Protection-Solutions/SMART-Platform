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
package org.wcs.smart.entity.ui.importwizard;

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
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.export.dialog.DelimiterCombo;

/**
 * Wizard page for collecting input file.
 * 
 * @author Emily
 *
 */
public class FileWizardPage extends WizardPage {

	private Text txtFile;
	private DelimiterCombo cmbDelimiter;
	
	protected FileWizardPage() {
		super("File"); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.FileWizardPage_FileLabel);
		
		txtFile = new Text(c, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnBrowse = new Button(c, SWT.PUSH);
		btnBrowse.setText(Messages.FileWizardPage_BrowseLabel);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				
				fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{Messages.FileWizardPage_CsvFile, Messages.FileWizardPage_AllFiles});
				fd.setFilterPath(txtFile.getText());
				
				String x = fd.open();
				if (x != null){
					txtFile.setText(x);
				}
			}
			
		});
		
		l = new Label(c, SWT.NONE);
		l.setText(Messages.FileWizardPage_DelimiterLabel);
		l.setToolTipText(Messages.FileWizardPage_DelimiterTooltip);
		
		cmbDelimiter = new DelimiterCombo(c, SWT.NONE);
		cmbDelimiter.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		setTitle(Messages.FileWizardPage_Title);
		setMessage(Messages.FileWizardPage_Message);
		
		setControl(c);
	}
	
	public String getFile(){
		return this.txtFile.getText();
	}

	public char getDelimiter() throws Exception{
		return cmbDelimiter.getDelimiter();
	}
}
