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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;

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
		super("TYPE_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.TypePage_suTypeLabel);
		
		Composite ops = new Composite(c, SWT.NONE);
		ops.setLayout(new GridLayout());
		((GridLayout)ops.getLayout()).marginWidth = 20;
		
		btnTransect = new Button(ops, SWT.RADIO);
		btnTransect.setText(Messages.TypePage_linesLabel);
		btnTransect.setSelection(true);
		
		btnPlots = new Button(ops, SWT.RADIO);
		btnPlots.setText(Messages.TypePage_pointsLabel);
		btnPlots.setSelection(false);
		
		setTitle(Messages.TypePage_Title);
		setMessage(Messages.TypePage_Message);
		
		setControl(main);
	}
	
	public GeometryType getType(){
		if (btnTransect.getSelection()){
			return GeometryType.TRANSECT;
		}else if (btnPlots.getSelection() ){
			return GeometryType.PLOT;
		}
		return null;
	}

}