package org.
wcs.smart.patrol.internal.ui;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;

/**
 * 
 * Patrol Item composites that edit patrol leg specific attributes.
 * Includes transportation type, pilot, leader.  If patrol
 * leg is not set using setPatrolLeg then by default the first
 * leg of the patrol is edited.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class PatrolLegItemComposite extends PatrolItemComposite{

	private  PatrolLeg patrolLeg;
	
	@Override
	public void setValues(Patrol p, Session session) {
		if (this.patrolLeg == null){
			setValues(p.getFirstLeg(), session);
		}else{
			setValues(patrolLeg, session);
		}
	}

	@Override
	public void updatePatrol(Patrol p) {
		if (patrolLeg == null){
			updatePatrol(p.getFirstLeg());
		}else{
			updatePatrol(patrolLeg);
		}
	}


	public void setPatrolLeg(PatrolLeg pleg){
		this.patrolLeg = pleg;
	}
	
	public abstract void setValues(PatrolLeg p, Session session);
	
	public abstract void updatePatrol(PatrolLeg p);

}
