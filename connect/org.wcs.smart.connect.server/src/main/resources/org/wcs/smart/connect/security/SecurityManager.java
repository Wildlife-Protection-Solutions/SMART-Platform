/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.security;

import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.SmartUserAction;

/**
 * Security manager for smart connect.
 * 
 * @author Emily
 *
 */
public enum SecurityManager {

	INSTANCE;

	public boolean canAccess(Session s, String username, String action, UUID resource){
		Criterion r = null;
		if (resource == null){
			r = Restrictions.and(
					Restrictions.eq("action", action), //$NON-NLS-1$
					Restrictions.isNull("resource")); //$NON-NLS-1$

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
	
	public boolean canAccessAtLeastOneResouce(Session s, String username, String action){
		Criterion r = null;
		r = Restrictions.eq("action", action); //$NON-NLS-1$

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

}
