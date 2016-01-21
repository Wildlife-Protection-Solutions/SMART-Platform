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
package org.wcs.smart.connect;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Utility class to load common objects from the database.
 * 
 * @author Emily
 *
 */
public class ConnectHibernateManager {

	/**
	 * Gets the connect server associated with the current conservation area
	 * or null if none found
	 * @param session
	 * @return
	 */
	public static ConnectServer getConnectServer(Session session){
		return getConnectServer(session, SmartDB.getCurrentConservationArea());
	}
	
	/**
	 * Gets the connect server associated with the given conservation area
	 * or null if none found
	 * @param session
	 * @return
	 */
	public static ConnectServer getConnectServer(Session session, ConservationArea ca){
		ConnectServer server = (ConnectServer) session.createCriteria(ConnectServer.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.uniqueResult();
		return server;
	}
	/**
	 * Gets the connect user associated with the given employee or null
	 * if none found
	 * @param e
	 * @param session
	 * @return
	 */
	public static ConnectUser getConnectUser(Employee e, Session session){
		return (ConnectUser)session.createCriteria(ConnectUser.class, "u") //$NON-NLS-1$
				.createAlias("u.server", "s") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("u.uuid", e.getUuid())) //$NON-NLS-1$
				.add(Restrictions.eq("s.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				
				.uniqueResult();
	}
	
	/**
	 * Gets the connect server status associated with the current conservatoin
	 * area or null if none found.
	 * @param session
	 * @return
	 */
	public static ConnectServerStatus getConnectServerStatus(Session session){
		return (ConnectServerStatus)session.get(ConnectServerStatus.class, 
				SmartDB.getCurrentConservationArea().getUuid());
		
	}
}
