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
package org.wcs.smart.er.ui.samplingunit.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;

/**
 * Import sampling unit wizard, sampling unit type page.
 * 
 * @author Emily
 *
 */
public class TypePage extends WizardPage {

	private Button btnTransect;
	private Button btnPlots;
	
	public TypePage(){
		super("TYPE_PAGE");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Sampling Unit Type:");
		
		Composite ops = new Composite(c, SWT.NONE);
		ops.setLayout(new GridLayout());
		((GridLayout)ops.getLayout()).marginWidth = 20;
		
		btnTransect = new Button(ops, SWT.RADIO);
		btnTransect.setText("Transects (Lines)");
		btnTransect.setSelection(true);
		
		btnPlots = new Button(ops, SWT.RADIO);
		btnPlots.setText("Plots (Points)");
		btnPlots.setSelection(false);
		
		setTitle("Sampling Unit Type");
		setMessage("Select the type of sampling units to import.");
		
		setControl(main);
	}
	
	public SamplingUnitType getType(){
		if (btnTransect.getSelection()){
			return SamplingUnitType.OPEN_TRANSECT;
		}else if (btnPlots.getSelection() ){
			return SamplingUnitType.PLOT;
		}
		return null;
	}

}