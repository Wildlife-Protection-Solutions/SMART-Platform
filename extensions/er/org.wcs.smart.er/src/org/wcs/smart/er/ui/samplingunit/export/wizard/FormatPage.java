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
package org.wcs.smart.er.ui.samplingunit.export.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.export.dialog.DelimiterCombo;

/**
 * Import wizard page, attribute field wizard page.
 * 
 * @author Emily
 *
 */
public class FormatPage extends WizardPage {
	
	private Button opCsv;
	private Button opShp;
	private DelimiterCombo cmbDelimiter;
	private Label lDeleimiter;
	
	public FormatPage(){
		super("FORMAT_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enable();
			}
		};
		opShp = new Button(c, SWT.RADIO);
		opShp.setText(Messages.FormatPage_ShapefileOp);
		opShp.setEnabled(true);
		opShp.addSelectionListener(l);
		
		opCsv = new Button(c, SWT.RADIO);
		opCsv.setText(Messages.FormatPage_DelimitedFileOp);
		opCsv.addSelectionListener(l);
		
		Composite tmp = new Composite(c, SWT.NONE);
		tmp.setLayout(new GridLayout(2, false));
		((GridLayout)tmp.getLayout()).marginLeft = 20;
		lDeleimiter = new Label(tmp, SWT.NONE);
		lDeleimiter.setText(Messages.FormatPage_DelimiterLabel);
		cmbDelimiter = new DelimiterCombo(tmp, SWT.NONE);
		
		setControl(main);
		
		setTitle(Messages.FormatPage_Title);
		setMessage(Messages.FormatPage_Message);
	}
	
	private void enable(){
		cmbDelimiter.getControl().setEnabled(!opShp.getSelection());
		lDeleimiter.setEnabled(!opShp.getSelection());
	}
	
	public boolean isShapefile(){
		return opShp.getSelection();
	}
	
	public Character getDelimiter(){
		if (cmbDelimiter.getControl().isEnabled()){
			try {
				return cmbDelimiter.getDelimiter();
			} catch (Exception e) {
				EcologicalRecordsPlugIn.log(e.getMessage(), e);
				return ',';
			}
		}
		return null;
	}
}