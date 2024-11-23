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
import org.wcs.smart.patrol.internal.ui.LeaderPilotComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to identify patrol leader and pilot is applicable.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 *
 */
public class PatrolLeaderWizardPage extends NewPatrolWizardPage {


	private LeaderPilotComposite leaderComposite = null;
	
	/**
	 * @param pageName
	 */
	public PatrolLeaderWizardPage() {
		super("PatrolLeaderPilot"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		leaderComposite = new LeaderPilotComposite();
		leaderComposite.createComponent(main, SWT.NONE);
		super.setControl(main);
	}

	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		if (leaderComposite.updatePatrol(p,session)){
			setPageComplete(true);
			return true;
		}else{
			return false;
		}
		
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
		leaderComposite.setValues(p, session);
    	if (p.hasPilot()){
    		setMessage(Messages.PatrolLeaderWizardPage_PageMessage_LeaderPilot2);
    		setTitle(Messages.PatrolLeaderWizardPage_LeaderPilotTitle2);
    	}else{
    		setMessage(Messages.PatrolLeaderWizardPage_PageMessage_Leader2);
    		setTitle(Messages.PatrolLeaderWizardPage_LeaderTitle2);
    	}	
	}
}
