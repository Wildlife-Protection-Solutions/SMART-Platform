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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.DateComposite;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to determine the start and end dates of a patrol
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDateWizardPage extends NewPatrolWizardPage {

	private DateComposite dateComposite = null;

	/**
	 * @param pageName
	 */
	public PatrolDateWizardPage() {
		super("PatrolDates"); //$NON-NLS-1$
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		dateComposite = new DateComposite();
		dateComposite.createComponent(main, SWT.NONE);

		dateComposite.addChangeListener(new IPatrolItemChangeListener() {			
			@Override
			public void itemChanged() {
				setErrorMessage(dateComposite.getErrorMessage());
				if (dateComposite.getErrorMessage() == null){
					PatrolDateWizardPage.this.setPageComplete(true);
				}else{
					PatrolDateWizardPage.this.setPageComplete(false);
				}
			}
		});
		
		setTitle(Messages.PatrolDateWizardPage_Title2);
		setMessage(Messages.PatrolDateWizardPage_PageMessage2);
		super.setControl(main);
		
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		return dateComposite.updatePatrol(p, session);
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
		dateComposite.setValues(p, session);		
	}
}