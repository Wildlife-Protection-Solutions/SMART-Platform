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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.hibernate.QueryFactory;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Security manager for smart connect.
 * 
 * @author Emily
 *
 */
public enum SecurityManager {

	INSTANCE;

	/*
	 * Determine if the user represented by the username is active
	 */
	private boolean isActive(Session s, String username){
		Long roleCnt = QueryFactory.buildCountQuery(s, SmartUserRole.class, new Object[] {"id.username",username}); //$NON-NLS-1$
		if (roleCnt > 0) return true;
		return false;
	}
	
	public boolean canAccess(Session s, String username, String action, UUID resource){
		//ensure user is active
		if (!isActive(s, username)) return false;
		
		//check roles for permission
		String queryString = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  "; //$NON-NLS-1$
		queryString += "WHERE a.role = role AND r.id.username = :username AND ( a.action = :adminAction OR "; //$NON-NLS-1$
		if (resource == null){
			queryString += " (a.action = :action and a.resource is null)"; //$NON-NLS-1$
		}else {
			queryString += " (a.action = :action and (a.resource is null OR a.resource = :resource))"; //$NON-NLS-1$
		}
		queryString += "  )"; //$NON-NLS-1$

		Query<Long> query = s.createQuery(queryString, Long.class);
		query.setParameter("username",  username); //$NON-NLS-1$
		query.setParameter("adminAction", AdminAccountAction.KEY); //$NON-NLS-1$
		query.setParameter("action", action); //$NON-NLS-1$
		if (resource != null){
			query.setParameter("resource", resource); //$NON-NLS-1$
		}
		Long cnt = query.uniqueResult();
		if (cnt >  0) return true;
		
		//check actions for permission
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<SmartUserAction> from = c.from(SmartUserAction.class);
		c.select(cb.count(from));
		
		Predicate r = null;
		if (resource == null){
			r = cb.and(
					cb.equal(from.get("action"), action), //$NON-NLS-1$
					cb.isNull(from.get("resource"))); //$NON-NLS-1$

		}else{
			r = cb.and(
					cb.equal(from.get("action"), action), //$NON-NLS-1$
					cb.or(
							cb.isNull(from.get("resource")), //$NON-NLS-1$
							cb.equal(from.get("resource"), resource))); //$NON-NLS-1$
			
		}
		c.where(cb.and(
				cb.equal(from.get("username"), username)), //$NON-NLS-1$
				cb.or(cb.equal(from.get("action"), AdminAccountAction.KEY), //$NON-NLS-1$
						r));
		Long cnt2 = s.createQuery(c).uniqueResult();
		if (cnt2 > 0){
			return true;
		}
		
		//if we are checking specifically for Administrator(the real-one, not a CA admin), don't do this check since it will pass when resource = null;
		if(action.equals(AdminAccountAction.KEY)){
			return false;
		}
		//check if CaAdmin role allows access, API and other code checking for access must call canAccess with the CAUUID as the resource for this check to work
		Long cnt3 = QueryFactory.buildCountQuery(s, SmartUserAction.class, 
				new Object[] {"username", username}, //$NON-NLS-1$
				new Object[] {"action", CaAdminAccountAction.KEY}, //$NON-NLS-1$
				new Object[] {"resource", resource}); //$NON-NLS-1$
		if (cnt3 > 0){
			return true;
		}
		return false;

	}
	
	public boolean canAccess(Session s, String username, String action){
		return canAccess(s, username, action, null);
	}
	
	/**
	 * check whether there is at least one resource this user can access to
	 * OR associated with a role with access to at least one, so we can show the menu/button/etc if they have at least partial access and hide it otherwise.  
	 */
	public boolean canAccessAtLeastOneResouce(Session s, String username, String action){
		//ensure the user is active
		if (!isActive(s, username)) return false;
		
		
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<SmartUserAction> from = c.from(SmartUserAction.class);
		c.select(cb.count(from));
		List<Predicate> filters = new ArrayList<>();
		
		filters.add(cb.equal(from.get("username"), username)); //$NON-NLS-1$
		
		Predicate actionEq = cb.equal(from.get("action"), action); //$NON-NLS-1$
		if(!action.equals(AdminAccountAction.KEY) && !action.equals(CaAdminAccountAction.KEY) ){ //if we are asking specifically about admin or caAdmin users (probably the menu filter) don't add this, or else it will return true for admin and caAdmin regardless 
			// in summary:   "username"=username" && ("action" == action || ("action"=admin || "action"=caadmin))
			Predicate r2 = cb.or(cb.equal(from.get("action"), AdminAccountAction.KEY), cb.equal(from.get("action"), CaAdminAccountAction.KEY)); //$NON-NLS-1$ //$NON-NLS-2$
			filters.add(cb.or(actionEq, r2));
		}else{
			filters.add(actionEq);
		}
		c.where(cb.and(filters.toArray(new Predicate[filters.size()])));
		Long cnt = s.createQuery(c).uniqueResult();
		
		
		//check if they have permission from a role
		String queryString = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  "; //$NON-NLS-1$
		queryString += "WHERE a.role = role AND a.action = :adminAction AND r.id.username = :username"; //$NON-NLS-1$
		Query<Long> q= s.createQuery(queryString, Long.class);
		q.setParameter("adminAction", action); //$NON-NLS-1$
		q.setParameter("username", username); //$NON-NLS-1$
		Long roleCnt = q.uniqueResult();
		
		Long adminCnt = (long) 0;
		if(!action.equals(AdminAccountAction.KEY) && !action.equals(CaAdminAccountAction.KEY) ){ //if we are asking specifically about admin or caAdmin users (probably the menu filter) don't add this, or else it will return true for admin and caAdmin regardless
			String queryString2 = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  "; //$NON-NLS-1$
			queryString2 += "WHERE a.role = role AND a.action = :adminAction AND r.id.username = :username"; //$NON-NLS-1$
			Query<Long> q2= s.createQuery(queryString2, Long.class);
			q2.setParameter("adminAction", AdminAccountAction.KEY); //$NON-NLS-1$
			q2.setParameter("username", username); //$NON-NLS-1$
			adminCnt = q2.uniqueResult();
		}
		
		if (cnt == 0 && roleCnt == 0 && adminCnt == 0){
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
		Long adminCnt = QueryFactory.buildCountQuery(s, SmartUserAction.class, new Object[] {"action",AdminAccountAction.KEY}); //$NON-NLS-1$
		if (adminCnt > 0) {
			return;
		}
		
		//a user with a role with admin action
		//check roles for permission
		String queryString = "SELECT count(*) FROM SmartUserRole r join r.id.role as role, SmartRoleAction a  "; //$NON-NLS-1$
		queryString += "WHERE a.role = role AND a.action = :adminAction "; //$NON-NLS-1$
		Query<Long> q= s.createQuery(queryString, Long.class);
		q.setParameter("adminAction", AdminAccountAction.KEY); //$NON-NLS-1$
		Long adminRoleCnt = q.uniqueResult();
		if (adminRoleCnt > 0) return;
			
		throw new SmartConnectException(
				Response.Status.BAD_REQUEST,
				Messages.getString("ConnectUserAction.AdminError", l)); //$NON-NLS-1$
		
	}

	//is the user is CaAdmin of any CA?
	public boolean isCaAdmin(Session s, String username) {
		if (!isActive(s, username)) return false;		
		Long count = QueryFactory.buildCountQuery(s, SmartUserAction.class, 
				new Object[] {"action", CaAdminAccountAction.KEY}, //$NON-NLS-1$
				new Object[] {"username", username}); //$NON-NLS-1$
		if (count > 0){
			return true;
		}
		return false;
	}
	
	//is the user is CaAdmin of any CA?
	public ArrayList<UUID> listOfUuidsIsCaAdminOf(Session s, String username) {
		if (!isActive(s, username)) return null;
		ArrayList<UUID> uuids = new ArrayList<UUID>();
		Object[] one = new Object[]{"action",CaAdminAccountAction.KEY}; //$NON-NLS-1$
		Object[] two = new Object[]{"username", username}; //$NON-NLS-1$
		List<SmartUserAction> actions = QueryFactory.buildQuery(s, SmartUserAction.class, one, two ).list();
		for(SmartUserAction a : actions){
			uuids.add(a.getResource());
		}
		return uuids;
	}
}
