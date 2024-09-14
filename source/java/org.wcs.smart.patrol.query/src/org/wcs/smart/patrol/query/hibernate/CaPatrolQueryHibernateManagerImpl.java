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
package org.wcs.smart.patrol.query.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportGroup;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
/**
 * Utility functions for supporting query module
 * when logged in as a single conservation area
 *  
 * @author Emily
 *
 */
public class CaPatrolQueryHibernateManagerImpl extends AbstractPatrolQueryHibernateManager {

	@Override
	public List<ListItem> getAgencies(Session session) {
		List<Agency> teams = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (Agency t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}
	
	@Override
	public List<ListItem> getActiveTeams(Session session) {
		List<Team> teams = PatrolHibernateManager.getActiveTeams(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (Team t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}

	@Override
	public List<ListItem> getActiveMandates(Session session) {
		List<PatrolMandate> teams = PatrolHibernateManager.getActiveMandates(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (PatrolMandate t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}
	@Override
	public List<ListItem> getActiveTransportTypes(Session session) {
		List<PatrolTransportType> teams = PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (PatrolTransportType t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}

	@Override
	public List<ListItem> getActivePatrolTypes(Session session) {
		List<PatrolType> teams = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (PatrolType t : teams){
			items.add(new ListItem(null, t.getName(), t.getKeyId()));
		}
		return items;
	}
	
	@Override
	public List<ListItem> getActiveTransportGroups(Session session) {
		List<PatrolType> types = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
		List<ListItem> items = new ArrayList<ListItem>();
		for (PatrolType type : types) {
			if (type.getTransportGroups() == null) continue;
			for (PatrolTransportGroup group : type.getTransportGroups()) {				
				items.add(new ListItem(null, group.getGroupTypeLabel(), group.getKeyId()));
			}
		}
		return items;
	}
	/**
	 * Gets the patrol type types listitem object 
	 * @param session
	 * @param value the key representing the patrol type
	 * @return the transport type or null if not found
	 * @throws Exception
	 */
	public ListItem getPatrolType(Session session, String value) throws Exception{
		PatrolType type = session.createQuery("FROM PatrolType WHERE conservationArea = :ca and keyId = :key", PatrolType.class) //$NON-NLS-1$
		.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
		.setParameter("key", value).uniqueResult(); //$NON-NLS-1$
		if (type == null) return null;
		return new ListItem(null, type.getName(), type.getKeyId());		
	}
	
	@Override
	public List<PatrolAttribute> getCustomPatrolAttributes(Session session) {
		List<PatrolAttribute> pas = QueryFactory.buildQuery(session, PatrolAttribute.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		return pas;
	}

	@Override
	public PatrolAttribute getPatrolAttribute(Session session, String value) {
		return QueryFactory.buildQuery(session, PatrolAttribute.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", value}).uniqueResult(); //$NON-NLS-1$
	}
}
