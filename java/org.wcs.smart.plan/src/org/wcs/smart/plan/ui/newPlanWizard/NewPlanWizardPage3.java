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
package org.wcs.smart.plan.ui.newPlanWizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Station;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.ui.StationComposite;
import org.wcs.smart.patrol.ui.TeamComposite;
import org.wcs.smart.plan.model.Plan;




/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage3 extends NewPlanWizardPage {

	
	private TeamComposite teamList;
	private StationComposite stationList;
	//private ComboViewer team = null;
	//private ComboViewer station= null;
	private Text unavailable;

	/**
	 * 
	 */
	protected NewPlanWizardPage3() {
		super("Plan Station/Team");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
				
		teamList = new TeamComposite();
		teamList.createComponent(center, SWT.NONE);
		
		stationList = new StationComposite();
		stationList.createComponent(center,  SWT.NONE);
		
		setControl(center);
		setMessage("Select the associated Team and/or Station for this plan, if applicable:");

	}
	

	@Override
	public boolean updateModel(Plan p) {
		p.setStation(stationList.getSelectedStation());
		p.setTeam(teamList.getSelectedTeam());
		
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {
		
		//Set team values, 
		List<? extends Object> teams = null;
		try{
			teams =  PatrolHibernateManager.getActiveTeams(p.getConservationArea(), session);

		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Could not load teams.", ex);
			session.close();
		}
		
		teamList.setInput(teams, p.getTeam());
		
		List<? extends Object> stations = PatrolHibernateManager.getActiveStations(p.getConservationArea(), session);
		stationList.setInput(stations, p.getStation());		
		
		
	}
}