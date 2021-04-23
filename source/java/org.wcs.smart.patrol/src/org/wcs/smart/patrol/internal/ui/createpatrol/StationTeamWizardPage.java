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
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;
import org.wcs.smart.patrol.ui.StationComposite;
import org.wcs.smart.patrol.ui.TeamComposite;

/**
 * Wizard page to gather station and
 * team patrol information.
 * @author Emily
 * @since 1.0.0
 */
public class StationTeamWizardPage extends NewPatrolWizardPage {

	private TeamComposite teamList;
	private StationComposite stationList;
	
	/**
	 *
	 */
	public StationTeamWizardPage() {
		super("PatrolStation"); //$NON-NLS-1$
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(2, true));
		
		teamList = new TeamComposite();
		teamList.createComponent(main, SWT.NONE);
		
		stationList = new StationComposite();
		stationList.createComponent(main,  SWT.NONE);
   		
		setTitle(Messages.StationTeamWizardPage_Title);
		setMessage(Messages.StationTeamWizardPage_PageMessage);
		super.setControl(main);
		super.setPageComplete(true);
	}

	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		boolean ret = stationList.updatePatrol(p, session);
		ret = ret & teamList.updatePatrol(p, session);
		return ret;
	}

	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
		teamList.setValues(p, session);
		stationList.setValues(p, session);
	}
}
