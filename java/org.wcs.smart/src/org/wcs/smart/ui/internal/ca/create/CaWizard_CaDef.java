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
package org.wcs.smart.ui.internal.ca.create;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.CaInfoComposite;

/**
 * The first page of the create conservation 
 * area wizard which gathers general information 
 * about a given conservation area.
 * 
 * @author Emily Gouge
 *
 */
public class CaWizard_CaDef extends CaWizardPage  {
	

	/* ui fields */
	private CaInfoComposite composite = null;
	
	/**
	 * Create the wizard.
	 */
	public CaWizard_CaDef() {
		super(Messages.CaWizard_CaDef_CaDef_PageName);
		setImageDescriptor(JFaceResources.getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
		setTitle(Messages.CaWizard_CaDef_PageTitle);
		setDescription(Messages.CaWizard_CaDef_PageDescription);
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	public void createControl(Composite parent) {
		
		composite = new CaInfoComposite(parent,  SWT.NULL, null);
		composite.addValidationListener(new CaInfoComposite.IValidationListener() {
			@Override
			public void validate() {
				CaWizard_CaDef.this.validate();
			}
		});
		setControl(composite);
		
		validate();
	}

	/**
	 * Validate the input fields
	 */
	private void validate() {
		super.setErrorMessage(null);
		
		boolean isComplete = true;
		if (!composite.isValid()){
			isComplete = false;
		}	
		
		super.setPageComplete(isComplete);
	}

	/**
	 * Updates the conservation area with the information from this wizard page.
	 * 
	 * @param ca Conservation Area object to update
	 */
	public void updateConservationArea(ConservationArea ca) {
		composite.updateConservationArea(ca);
	}
	
}
