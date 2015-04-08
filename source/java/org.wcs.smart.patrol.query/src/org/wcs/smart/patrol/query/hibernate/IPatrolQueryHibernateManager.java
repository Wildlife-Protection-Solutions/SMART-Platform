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

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Hibernate utility functions to support 
 * Query module.
 *  
 * @author Emily
 *
 */
public interface IPatrolQueryHibernateManager {
	
	/**
	 * Get the patrol mandate object.
	 * 
	 * @param session
	 * @param value patrol mandate uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getPatrolMandate(Session session, String value)throws Exception;
	
	/**
	 * Gets the station object
	 * @param session
	 * @param value station uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getStation(Session session, String value)throws Exception;
	
	/**
	 * Gets team object
	 * @param session
	 * @param value team uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getTeam(Session session, String value)throws Exception;
	
	/**
	 * Gets the transportation types listitem object 
	 * @param session
	 * @param value transportation type uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getTransportType(Session session, String value)throws Exception;
	
		
	/**
	 * Creates a list item for the given employee uuid
	 * @param session
	 * @param value employee uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getEmployee(Session session, String value) throws Exception;
	
	/**
	 * 
	 * @return  list of all active teams as list items
	 */
	public List<ListItem> getActiveTeams(Session session);
	/**
	 * 
	 * @return  list of all active patrol mandates
	 */
	public List<ListItem> getActiveMandates(Session session);
	/**
	 * 
	 * @return  list of all active transport types
	 */
	public List<ListItem> getActiveTransportTypes(Session session);
}
