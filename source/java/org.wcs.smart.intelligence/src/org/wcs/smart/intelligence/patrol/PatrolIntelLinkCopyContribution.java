package org.wcs.smart.intelligence.patrol;

import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.intelligence.model.PatrolIntelligence;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.model.Patrol;

public class PatrolIntelLinkCopyContribution implements IPatrolEditContribution {

	@Override
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol) {
		Criteria c = s.createCriteria(PatrolIntelligence.class).add(Restrictions.eq("id.patrol", originalPatrol)); //$NON-NLS-1$ //$NON-NLS-2$
		@SuppressWarnings("unchecked")
		ArrayList<PatrolIntelligence> list = (ArrayList<PatrolIntelligence>)c.list();
		for(PatrolIntelligence pi : list){
			PatrolIntelligence newPp = new PatrolIntelligence();
			newPp.setPatrol(newPatrol);
			newPp.setIntelligence(pi.getIntelligence());
			s.saveOrUpdate(newPp);
		}
	}

	@Override
	public void mergePatrols(Patrol mergedPatrol, Patrol[] originalPatrols) {
		// TODO Auto-generated method stub

	}

}
