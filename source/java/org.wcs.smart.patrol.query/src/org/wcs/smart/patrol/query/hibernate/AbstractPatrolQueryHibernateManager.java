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

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * Hibernate utility functions to support 
 * Query module.
 *  
 * @author Emily
 *
 */
public abstract class AbstractPatrolQueryHibernateManager implements IPatrolQueryHibernateManager{

	/**
	 * Get the patrol mandate object.
	 * 
	 * @param session
	 * @param value patrol mandate uuid as hex encoded string
	 * @return the patrol mandate or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getPatrolMandate(Session session, String value) throws Exception{
		return getListItem(session, PatrolMandate.class, UuidUtils.stringToUuid(value));

	}
	
	/**
	 * Gets the station object
	 * @param session
	 * @param value station uuid as hex encoded string
	 * @return the station or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getStation(Session session, String value) throws Exception{
		return getListItem(session, Station.class, UuidUtils.stringToUuid(value));
	}
	
	/**
	 * Gets team object
	 * @param session
	 * @param value team uuid as hex encoded string
	 * @return the team or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getTeam(Session session, String value) throws Exception{
		return getListItem(session, Team.class, UuidUtils.stringToUuid(value));
	}
	
	/**
	 * Gets the transportation types listitem object 
	 * @param session
	 * @param value transportation type uuid as hex encoded string
	 * @return the transport type or null if not found
	 * @throws Exception
	 */
	public ListItem getTransportType(Session session, String value) throws Exception{
		return getListItem(session, PatrolTransportType.class, UuidUtils.stringToUuid(value)); 
	}
	
	/**
	 * Loads the list item for the given clazz and given uuid
	 *  
	 * @param session
	 * @param clazz object type
	 * @param value uuid an encoded hex string
	 * @return resulting list item
	 * @throws Exception
	 */
	private ListItem getListItem(Session session, Class<? extends NamedItem> clazz, UUID uuid) throws Exception{
		NamedItem item = session.get(clazz, uuid);
		return new ListItem(item.getUuid(), item.getName());		
	}
	
	/**
	 * Creates a list item for the given employee uuid
	 * @param session
	 * @param value employee uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getEmployee(Session session, String value) throws Exception{
		Query<Tuple> q = session.createQuery("SELECT uuid, givenName, familyName, id FROM Employee WHERE uuid =:uuid and conservationArea = :ca", Tuple.class); //$NON-NLS-1$		
		q.setParameter("uuid", UuidUtils.stringToUuid(value)); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<Tuple> results = q.list();
		if (results.size() == 1){
			Tuple d = results.get(0);
			return new ListItem( (UUID)d.get(0), (String) d.get(1) + " " + (String)d.get(2) + " [" + (String)d.get(3) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else{
			QueryPlugIn.log(MessageFormat.format(Messages.QueryHibernateManager_LoadEmployeeError, new Object[]{value}), null);
			return null;
		}
	}
	
	

}
