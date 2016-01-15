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

import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.Messages;
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
		
		//check roles for permission
		String queryString = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  ";
		queryString += "WHERE a.role = role AND r.id.username = :username AND ( a.action = :adminAction OR ";
		if (resource == null){
			queryString += " (a.action = :action and a.resource is null)";
		}else {
			queryString += " (a.action = :action and (a.resource is null OR a.resource = :resource))";
		}
		queryString += "  )";

		Query query = s.createQuery(queryString);
		query.setParameter("username",  username);
		query.setParameter("adminAction", AdminAccountAction.KEY);
		query.setParameter("action", action);
		if (resource != null){
			query.setParameter("resource", resource);
		}
		Long cnt = (Long)query.uniqueResult();
		if (cnt >  0){
			return true;
		}
		
		//check actions for permission
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

		Long cnt2 = (Long) c.uniqueResult();
		if (cnt2 > 0){
			return true;
		}
		return false;

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

	
	/**
	 * Ensure there is at least one user with admin action
	 * OR associated with a role with admin action
	 * 
	 * Throws an exception if no admin user found
	 */
	public void validateSingleAdminUser(Session s, Locale l){
		Long adminCnt = (Long) s.createCriteria(SmartUserAction.class)
				.add(Restrictions.eq("action", AdminAccountAction.KEY)) //$NON-NLS-1$
				.setProjection(Projections.rowCount()).uniqueResult();
		if (adminCnt > 0) {
			return;
		}
		
		//a user with a role with admin action
		//check roles for permission
		String queryString = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  ";
		queryString += "WHERE a.role = role AND a.action = :adminAction ";
		Query q= s.createQuery(queryString);
		q.setParameter("adminAction", AdminAccountAction.KEY);
		Long adminRoleCnt = (Long) q.uniqueResult();
		if (adminRoleCnt > 0){
			return;
		}		
		
		throw new SmartConnectException(
				Response.Status.BAD_REQUEST,
				Messages.getString("ConnectUserAction.AdminError", l)); //$NON-NLS-1$
		
	}
}
