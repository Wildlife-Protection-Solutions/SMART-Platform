package org.wcs.smart.connect.security;

import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.SmartUserAction;

public enum SecurityManager {

	INSTANCE;

	public boolean canAccess(Session s, String username, String action, UUID resource){
		Criterion r = null;
		if (resource == null){
			r = Restrictions.eq("action", action); //$NON-NLS-1$
		}else{
			r = Restrictions.and(
					Restrictions.eq("action", action), //$NON-NLS-1$
					Restrictions.or(
							Restrictions.isNull("resource"), //$NON-NLS-1$
							Restrictions.eq("resource", resource))); //$NON-NLS-1$
			
		}
		Criteria c = s.createCriteria(SmartUserAction.class);
				c.add(Restrictions.eq("username", username)) //$NON-NLS-1$
				.add(Restrictions.or(
						Restrictions.eq("action", AdminAccountAction.KEY), //$NON-NLS-1$
						r))
				.setProjection(Projections.rowCount());

		Long cnt = (Long) c.uniqueResult();
		if (cnt == 0){
			return false;
		}else{
			return true;
		}
	}
	
	public boolean canAccess(Session s, String username, String action){
		return canAccess(s, username, action, null);
	}
}
