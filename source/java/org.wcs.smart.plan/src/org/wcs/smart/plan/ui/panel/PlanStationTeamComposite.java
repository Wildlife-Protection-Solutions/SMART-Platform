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
package org.wcs.smart.plan.ui.panel;

import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.ui.StationComposite;
import org.wcs.smart.patrol.ui.TeamComposite;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;

/**
 * Composite for collecting the plan description information
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanStationTeamComposite extends PlanComposite {

	private TeamComposite teamList;
	private StationComposite stationList;


	/**
	 * @param parent
	 * @param style
	 */
	public PlanStationTeamComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.PlanStationTeamComposite_Message);
		createControls();
	}

	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	        	
		stationList = new StationComposite();
		Composite compStations = stationList.createComponent(this,  SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 250;
		compStations.setLayoutData(gd);
		stationList.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireInputChangeListeners();	
			}
		});
		
		teamList = new TeamComposite();
		Composite teamComp = teamList.createComponent(this, SWT.NONE);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd2.widthHint = 250;
		teamComp.setLayoutData(gd2);
		
		teamList.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireInputChangeListeners();	
			}
		});
		
		
		
		        
	}
	
	@Override
	protected boolean updateModelInternal(Plan plan) {
		plan.setStation(stationList.getSelectedStation());
		plan.setTeam(teamList.getSelectedTeam());
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		//Set team values,
		Session session = HibernateManager.openSession();
				
			List<? extends Object> teams = null;
			List<? extends Object> stations = null;
			try{
				
				teams =  PatrolHibernateManager.getActiveTeams(plan.getConservationArea(), session);
				stations = PatrolHibernateManager.getActiveStations(plan.getConservationArea(), session);
			}catch (Exception ex){
				SmartPatrolPlugIn.displayLog(Messages.PlanStationTeamComposite_TeamStation_NotFound_Error, ex);
				
			}finally{
				session.close();
			}
			
			teamList.setInput(teams, plan.getTeam());
			stationList.setInput(stations, plan.getStation());		
			
			try{
				teamList.setSelectedTeam(plan.getTeam());			
			}catch (Exception e){
				//do nothing, probably just no template so we can't set the values to anything
			}
			try{
				stationList.setSelectedStation(plan.getStation() );
			}catch (Exception e){
				//eat me
			}
	}

	@Override
	public String getTitle() {
		return Messages.PlanStationTeamComposite_Title;
	}
	
}
