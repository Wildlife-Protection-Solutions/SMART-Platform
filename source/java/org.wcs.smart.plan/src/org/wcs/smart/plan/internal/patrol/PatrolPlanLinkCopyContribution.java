package org.wcs.smart.plan.internal.patrol;

import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.plan.model.PatrolPlan;

public class PatrolPlanLinkCopyContribution implements IPatrolEditContribution {

	@SuppressWarnings("unchecked")
	@Override
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol) {
		Criteria c = s.createCriteria(PatrolPlan.class).add(Restrictions.eq("id.patrol", originalPatrol)); //$NON-NLS-1$ //$NON-NLS-2$
		for(PatrolPlan pp : ((ArrayList<PatrolPlan>)c.list())){
			PatrolPlan newPp = new PatrolPlan();
			newPp.setPatrol(newPatrol);
			newPp.setPlan(pp.getPlan());
			s.saveOrUpdate(newPp);
		}
	}

	@Override
	public void mergePatrols(Patrol mergedPatrol, Patrol[] originalPatrols) {
		// TODO Auto-generated method stub

	}

}
