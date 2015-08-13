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
package org.wcs.smart.patrol.model;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Patrol waypoint source.
 * 
 * @author Emily
 *
 */
public class PatrolWaypointSource implements IWaypointSource {

	public static String PATROL_WP_SOURCE_ID = "PATROL"; //$NON-NLS-1$
	
	public PatrolWaypointSource() {
	}

	@Override
	public String getKey() {
		return PATROL_WP_SOURCE_ID;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(this, l);
	}

	public String getDatastoreFileLocation(Patrol p){
		StringBuilder sb = new StringBuilder();
		sb.append(Patrol.PATROL_FILESTORE_LOC);
		sb.append(File.separator);
		sb.append(UuidUtils.getDirectoryPath(p.getUuid()));
		sb.append(File.separator);
		return sb.toString();
	}
	
	@Override
	public String getDatastoreFileLocation(Object source, Session session) throws Exception{
		if (source instanceof Waypoint){
			return getDatastoreFileLocation((Waypoint)source, session);
		}else if (source instanceof Patrol){
			return getDatastoreFileLocation((Patrol)source);
		}
		return null;
	}
	
	public String getDatastoreFileLocation(final Waypoint wp, Session session)  throws Exception{
		if (wp.getUuid() == null){
			return null;
		}
		String patrolDir ;
		StatelessSession temp = session.getSessionFactory().openStatelessSession();
		try{
			Query q = temp.createQuery("SELECT p.uuid from Patrol p join p.legs pl join pl.patrolLegDays pld join pld.waypoints wp where wp.id.waypoint = :wp "); //$NON-NLS-1$
			q.setParameter("wp", wp); //$NON-NLS-1$
	
			List<?> pws = q.list();
		
			if (pws.size() > 0){
				UUID uuid = (UUID) pws.get(0);
				patrolDir = UuidUtils.getDirectoryPath(uuid);
			}else{
				throw new Exception("Could not determine attachment location for patrol waypoint: " + wp.getUuid().toString()); //$NON-NLS-1$
			}
		}finally{
			temp.close();
		}
			
		StringBuilder sb = new StringBuilder();
		sb.append(Patrol.PATROL_FILESTORE_LOC);
		sb.append(File.separator);
		if (patrolDir != null){
			sb.append(patrolDir);
			sb.append(File.separator);
		}
		
		return sb.toString();
	}

}
