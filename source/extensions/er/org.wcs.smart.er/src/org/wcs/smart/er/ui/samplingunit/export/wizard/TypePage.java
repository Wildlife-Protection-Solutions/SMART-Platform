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

import java.util.Locale;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;

/**
 * Sampling unit export type page
 * @author Emily
 *
 */
public class TypePage extends WizardPage implements SelectionListener{
	
	private Button opPlots;
	private Button opTransects;
	
	public TypePage(){
		super("TYPE_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		opTransects = new Button(c, SWT.CHECK);
		opTransects.setSelection(true);
		opTransects.setText(GeometryType.TRANSECT.getGuiName(Locale.getDefault()));
		opTransects.addSelectionListener(this);
		
		opPlots = new Button(c, SWT.CHECK);
		opPlots.setSelection(true);
		opPlots.setText(GeometryType.PLOT.getGuiName(Locale.getDefault()));
		opPlots.addSelectionListener(this);
		
		setControl(main);
		
		setTitle(Messages.TypePage1_Title);
		setMessage(Messages.TypePage1_Message);
	}
	
	@Override
	public IWizardPage getNextPage(){
		if (!exportTransect() && !exportPlots()){
			return null;
		}
		return super.getNextPage();
	}
	
	public boolean exportTransect(){
		return opTransects.getSelection();
	}
	
	public boolean exportPlots(){
		return opPlots.getSelection();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		getWizard().getContainer().updateButtons();
		
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
	
}
