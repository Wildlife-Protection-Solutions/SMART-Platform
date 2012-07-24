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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for collection report parameters.
 * <p>
 * Users add instances of {@link AbstractBirtParameter} to
 * this dialog. One for each report parameter.
 * </p> 
 * @author egouge
 * @since 1.0.0
 */
public class ReportParameterDialog extends TitleAreaDialog {
	
	private List<IBirtParameterComponent> params = new ArrayList<IBirtParameterComponent>();
	private HashMap<String, Object> values = null;
	
	/**
	 * @param parentShel
	 */
	public ReportParameterDialog(Shell parentShell) {
		super(parentShell);
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Continue",
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		for(IBirtParameterComponent param: params){
			Composite c = param.createComposite(comp);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		super.getShell().setText("Report Parameters");
		setMessage("Enter the required report parameters.");
		
		return comp;
	}
	
	@Override
	protected void okPressed(){
		updateValues();
		super.okPressed();
	}
	
	/**
	 * Adds birt parameter component
	 * @param parameter
	 */
	public void addComponent(IBirtParameterComponent parameter){
		params.add(parameter);
	}
	
	/**
	 * 
	 * @return maps of parameter name to parameter value enter by the user
	 */
	public HashMap<String, Object> getValues(){
		return values;
	}
	
	private HashMap<String, Object> updateValues(){
		values = new HashMap<String, Object>();
		
		for(IBirtParameterComponent param: params){
			values.putAll(param.getParameters());
		}
		return values;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable() {
		return true;
	}

}
