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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.ui.ObjectiveComposite;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Wizard page to gather patrol objective wizard page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolObjectiveWizardPage extends NewPatrolWizardPage {

	
	private ObjectiveComposite objectiveComp = null;


	/**
	 * 
	 */
	protected PatrolObjectiveWizardPage() {
		super("Patrol Objective");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		objectiveComp = new ObjectiveComposite();
		setMessage("Enter the patrol objective and select how well this objective was met.");
		super.setControl(objectiveComp.createComponent(parent, SWT.NONE));
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#updateModel()
	 */
	@Override
	void updateModel(Patrol p) {
		objectiveComp.updatePatrol(p);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	void initModel(Patrol p, Session session) {
		objectiveComp.setValues(p, session);
	}
}