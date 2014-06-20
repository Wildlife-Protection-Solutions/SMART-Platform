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

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;

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
	
	public static String SIMPLE_DATE_FORMAT = "yyyy-MM-dd G hh:mm:ss z";  //$NON-NLS-1$
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.report.parameters"); //$NON-NLS-1$
	
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
		createButton(parent, IDialogConstants.OK_ID, Messages.ReportParameterDialog_ContinueButton,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		Composite comp = new Composite(parent, SWT.NONE);
		
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		for(IBirtParameterComponent param: params){
			Composite c = param.createComposite(comp, dialogSettings);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 200;
			c.setLayoutData(gd);
		}
		
		super.getShell().setText(Messages.ReportParameterDialog_DialogTitle);
		setMessage(Messages.ReportParameterDialog_DialogMessage);
		setTitle(Messages.ReportParameterDialog_DialogTitle);
		
		return comp;
	}
	
	@Override
	protected void okPressed(){
		try{
			updateValues();
			super.okPressed();
		}catch (Exception ex){
			ReportPlugIn.displayLog(ex.getMessage(), ex);
		}
		
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
		SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
		for(IBirtParameterComponent param: params){
			values.putAll(param.getParameters());
			HashMap<String, Object> values = param.getParameters();
			for (Iterator<Entry<String, Object>> iterator = values.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, Object> type = iterator.next();
				if (Date.class.isAssignableFrom(type.getValue().getClass())){
					String value = sdf.format((Date)type.getValue());
					dialogSettings.put(type.getKey(), value);
				}else{
					dialogSettings.put(type.getKey(), type.getValue().toString());
				}
			}
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
