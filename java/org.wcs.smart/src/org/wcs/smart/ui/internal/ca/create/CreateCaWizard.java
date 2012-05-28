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

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * 
 * A wizard for creating a new conservation area.
 * 
 * 
 * @author Emily Gouge, Refractions Research
 *
 */
public class CreateCaWizard extends Wizard {

	private boolean completedOK = false;
		
	/**
	 * Creates a new wizard.
	 */
	public CreateCaWizard() {
		setWindowTitle("Create Conservation Area Wizard");
		
		
		
	}

	
	@Override
	public void addPages() {
		WizardPage page = new CaWizard_CaDef();
		super.addPage(page);
		
		page = new CaWizard_UserDef();
		super.addPage(page);
	}

	/**
	 * 
	 * @return true if the wizard completed okay with no errors; false if error occured
	 * while finishing wizard
	 */
	public boolean isCompletedOk(){
		return completedOK;
	}

	
	@Override
	public boolean performFinish() {
		completedOK = false;
		
		ConservationArea newCa = SmartUtils.createConservationArea();

		for (int i= 0; i < super.getPageCount(); i ++){
			if (super.getPages()[i] instanceof CaWizardPage){
				((CaWizardPage)super.getPages()[i] ).updateConservationArea(newCa);		
			}
		}
		
		try{
			HibernateManager.saveNewConservationArea(newCa);
			completedOK = true;
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), "Error creating new conservation error: " + ex.getMessage(), ex);
		}
		
		return completedOK;
	}

}
