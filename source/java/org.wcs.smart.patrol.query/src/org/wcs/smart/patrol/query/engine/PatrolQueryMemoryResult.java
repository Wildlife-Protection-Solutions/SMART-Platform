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
package org.wcs.smart.patrol.query.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Updatable result set for patrol memory query results.
 * @author Emily
 *
 */
public class PatrolQueryMemoryResult extends MemoryQueryResult<PatrolQueryResultItem> implements IUpdateableResultSet {

	public PatrolQueryMemoryResult(List<PatrolQueryResultItem> data) {
		super(data);
	}

	@Override
	public boolean canUpdate(Class<? extends IResultItem> item) {
		return (item.equals(PatrolQueryResultItem.class));
	}

	
	public boolean deletePatrol(UUID patrolUuid){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		final Exception[] error = new Exception []{null};
		try{
			pmd.run(true, false, m->{
				try{
					if (!PatrolManager.getInstance().deletePatrol(patrolUuid, m)){
						error[0] = new Exception(Messages.PatrolQueryMemoryResult_DeleteError);
					}
				}catch (Exception ex){
					error[0] = ex;
				}
			});
		}catch (Exception ex){
			error[0] = ex;
		}
		if (error[0] != null){
			PatrolQueryPlugIn.displayLog(error[0].getMessage(), error[0]);
			return false;
		}
		
		List<PatrolQueryResultItem> toDelete = new ArrayList<>();
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(patrolUuid)){
				toDelete.add(i);
			}
		}
		getData().removeAll(toDelete);
		return true;	
	}
	
	@Override
	public boolean update(QueryColumn column, IResultItem it, Object newValue)
			throws Exception {
		if (!(it instanceof PatrolQueryResultItem)) return false;
		if (!(column instanceof FixedQueryColumn)) return false;
		
		FixedQueryColumn fixedcolumn = (FixedQueryColumn)column;
		PatrolQueryResultItem item = (PatrolQueryResultItem)it;
		Patrol p = null;
		boolean change = false;
		Session s = HibernateManager.openSession();
		try {
			s.getTransaction().begin();
			p = (Patrol) s.get(Patrol.class, item.getPatrolUuid());
			if (p == null) return false; //patrol not found
			
			PatrolLeg pl = null;
			for (PatrolLeg leg : p.getLegs()){
				if (leg.getUuid().equals(item.getPatrolLegUuid())){
					pl = leg;
					break;
				}
			}
			if (pl == null) return false; //patrol leg not found
			switch (fixedcolumn.getColumn()) {
				case PATROL_ID:
					if (newValue instanceof String) {
						if (((String) newValue).length() != 0) { 
							if (!isEqual(newValue, p.getId())) {
								change = true;
								updatePatrolId(p, (String)newValue,s);
							}
						}
					}
					break;
				case PATROL_STATION:
					if (newValue instanceof Station) {
						Station newStation = (Station) newValue;
						if (!isEqual(newStation, p.getStation())){
							change = true;
							updateStation(p, newStation, s);
						}
					}
					break;
				case PATROL_TEAM:
					if (newValue instanceof Team) {
						Team newTeam = (Team) newValue;
						if (!isEqual(newTeam, p.getStation())){
							change = true;
							updateTeam(p, newTeam, s);
						}
					}	
					break;
				case PATROL_OBJETIVE:
					if (newValue instanceof String) {
						String newObj = (String) newValue;
						if (!isEqual(newObj, p.getObjective())){
							change = true;
							updateObjective(p, newObj, s);
						}
					}	
					break;
				case PATROL_MANDATE:
					if (newValue instanceof PatrolMandate) {
						PatrolMandate newMandate = (PatrolMandate) newValue;
						if (!isEqual(newMandate, pl.getMandate())){
							change = true;
							updateMandate(pl, newMandate, s);
						}
					}	
					break;	
				case PATROL_ARMED:
					if (newValue instanceof Boolean) {
						Boolean newArmed = (Boolean) newValue;
						if (!isEqual(newArmed, p.isArmed())){
							change = true;
							updateArmed(p, newArmed, s);
						}
					}	
					break;
				case PATROL_LEG_ID:
					if (newValue instanceof String) {
						String newId = (String)newValue;
						if (!isEqual(newId, pl.getId())){
							change = true;
							updateLegId(pl, newId, s);
						}
					}	
					break;
				case TRANSPORT_TYPE:
					if (newValue instanceof PatrolTransportType) {
						PatrolTransportType newId = (PatrolTransportType)newValue;
						if (!isEqual(newId, pl.getType())){
							change = true;
							updateTransport(pl, newId, s);
						}
					}	
					break;	
				case PATROL_LEG_LEADER:
					if (newValue instanceof Employee) {
						Employee newemployee = (Employee)newValue;
						if (!isEqual(newemployee, pl.getId())){
							change = true;
							updateLegLeader(pl, newemployee, s);
						}
					}	
					break;	
				case PATROL_LEG_PILOT:
					if (newValue instanceof Employee) {
						Employee newemployee = (Employee)newValue;
						if (!isEqual(newemployee, pl.getId())){
							change = true;
							updateLegPilot(pl, newemployee, s);
						}
					}	
					break;						
				default:
					break;
			}
			
			s.getTransaction().commit();
		} catch (Exception ex) {
			s.getTransaction().rollback();
			throw ex;
		} finally {
			s.close();
		}

		if (change) {
			PatrolEventManager.getInstance().patrolSaved(p, true);
			return true;
		}
		return false;
		
		
		
	}

	private void updatePatrolId(Patrol p, String newId, Session session){
		p.setId(newId);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(p.getUuid())){
				i.setPatrolId(newId);
			}
		}
	}

	private void updateStation(Patrol p, Station newStation, Session session){
		p.setStation(newStation);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(p.getUuid())){
				i.setStation(newStation.getName());
			}
		}
	}
	private void updateTeam(Patrol p, Team newTeam, Session session){
		p.setTeam(newTeam);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(p.getUuid())){
				i.setTeam(newTeam.getName());
			}
		}
	}
	
	private void updateObjective(Patrol p, String obj, Session session){
		p.setObjective(obj);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(p.getUuid())){
				i.setObjective(obj);
			}
		}
	}
	private void updateMandate(PatrolLeg leg, PatrolMandate mandate, Session session){
		leg.setMandate(mandate);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolLegUuid().equals(leg.getUuid())){
				i.setMandate(mandate.getName());
			}
		}
	}
	
	private void updateLegId(PatrolLeg leg, String newId, Session session){
		leg.setId(newId);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolLegUuid().equals(leg.getUuid())){
				i.setPatrolLegId(newId);
			}
		}
	}
	
	private void updateLegLeader(PatrolLeg leg, Employee newLeader, Session session){
		for (PatrolLegMember m : leg.getMembers()){
			if (m.getMember().equals(newLeader)){
				m.setIsLeader(true);
			}else{
				m.setIsLeader(false);
			}
		}
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolLegUuid().equals(leg.getUuid())){
				i.setLeader(SmartLabelProvider.getShortLabel(newLeader));
			}
		}
	}
	
	private void updateLegPilot(PatrolLeg leg, Employee newPilot, Session session){
		for (PatrolLegMember m : leg.getMembers()){
			if (m.getMember().equals(newPilot)){
				m.setIsPilot(true);
			}else{
				m.setIsPilot(false);
			}
		}
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolLegUuid().equals(leg.getUuid())){
				i.setPilot(SmartLabelProvider.getShortLabel(newPilot));
			}
		}
	}
	
	private void updateArmed(Patrol p, Boolean newValue, Session session){
		p.setArmed(newValue);
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolUuid().equals(p.getUuid())){
				i.setArmed(newValue);
			}
		}
	}
	
	private void updateTransport(PatrolLeg pl, PatrolTransportType newValue, Session session){
		pl.setType(newValue);
		pl.getPatrol().recalculateType();
		
		for (PatrolQueryResultItem i : getData()){
			if (i.getPatrolLegUuid().equals(pl.getUuid())){
				i.setTransportType(newValue.getName());
			}
			if (i.getPatrolUuid().equals(pl.getPatrol().getUuid())){
				i.setPatrolType(pl.getPatrol().getPatrolType());
			}
		}
	}
	
	public boolean isEqual(Object o1, Object o2){
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
		
	}
}
