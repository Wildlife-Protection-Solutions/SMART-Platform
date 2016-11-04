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

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.EmployeeComposite;
import org.wcs.smart.user.UserLevelManager;

/**
 * The second page of the create conservation 
 * area wizard which gathers general information 
 * about the first conservation area user.
 * 
 * @author Emily Gouge
 *
 */
public class CaWizard_UserDef extends CaWizardPage{
	
	/* ui fields */

	private EmployeeComposite compEmployee = null;
	/**
	 * Create the wizard.
	 */
	public CaWizard_UserDef() {
		super(Messages.CaWizard_UserDef_PageName);
		setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
		setTitle(Messages.CaWizard_UserDef_PageTitle);
		setDescription(Messages.CaWizard_UserDef_PageDescription);
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new FillLayout());
		compEmployee = new EmployeeCompositeLocal(container, EmployeeComposite.SMART_USER, null);
		setControl(container);
		compEmployee.validate();
	}
	
	

	/**
	 * Updates the conservation area with the information from this wizard page.
	 * 
	 * @param ca Conservation Area object to update
	 */
	public void updateConservationArea(ConservationArea ca) {
		Employee admin = new Employee();
		admin.setConservationArea(ca);
		
		compEmployee.updateEmploye(admin);
		admin.setSmartUserLevel(Collections.singleton(UserLevelManager.ADMIN));

		//remove any previous users
		ca.getEmployees().clear(); 
		ca.getEmployees().add(admin);
	}
	
	/**
	 * Extensions of employee composite
	 * that performs additional tasks when validating
	 * @author Emily
	 *
	 */
	class EmployeeCompositeLocal extends EmployeeComposite{

		public EmployeeCompositeLocal(Composite parent, int localStyle, List<Agency> agencies) {
			super(parent, localStyle, agencies);
			
			//must create a smart user
			super.chSmartUser.setVisible(false);
			super.chSmartUser.setSelection(true);
			enableSmartUser(true);
		}
		
		@Override
		public boolean validate(){
			boolean valid = super.validate();
			CaWizard_UserDef.this.setPageComplete(valid);
			return valid;
		}
		
	}
}
