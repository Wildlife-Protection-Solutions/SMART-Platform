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
package org.wcs.smart.ui.internal.preference;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.map.DefaultMapLayerStylesComposite;
/**
 * Preference page for configuring auto-backups
 * 
 * @author Emily
 *
 */
public class SmartDefaultStylesPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	
	private DefaultMapLayerStylesComposite layersComp;
	
	public SmartDefaultStylesPreferencePage() {
	}

	public SmartDefaultStylesPreferencePage(String title) {
		super(title);
	}

	public SmartDefaultStylesPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public boolean performOk() {
		if (!isEditable()){
			return true;
		}
		try {
			layersComp.save();
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
			return false;
		}
		return true;
	}
	
	@Override
	protected void performDefaults() {
		if (!isEditable()){
			return;
		}
		super.performDefaults();
	}
	
	private boolean isEditable(){
		return PermissionManager.INSTANCE.canConfigureDefaultStyles();
	}
	
	@Override
	public void init(IWorkbench workbench) {
		
	}

	
	@Override
	protected Control createContents(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		layersComp = new DefaultMapLayerStylesComposite(main, SWT.NONE, isEditable());
		layersComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setMessage(Messages.SmartDefaultStylesPreferencePage_Message);
		
		validate();
		
		return parent;
	}
	
	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	private boolean validate() {
		boolean isComplete = true;
		String error = null;	
			
		setErrorMessage(error);
		setValid(isComplete);
		return isComplete;
	}
	
	
}
