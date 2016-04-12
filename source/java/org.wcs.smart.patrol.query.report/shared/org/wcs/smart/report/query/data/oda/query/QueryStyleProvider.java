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
package org.wcs.smart.report.query.data.oda.query;

import java.util.List;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.report.birt.map.AbstractQueryStyleProvider;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Query style provider for patrol queries.
 * 
 * @author Emily
 *
 */
public class QueryStyleProvider  extends AbstractQueryStyleProvider{

	@Override
	public StyleBlackboard getStyle(String queryType, UUID queryUuid, Session s) {
		
		String tableName = null;
		String resourceKey = null;
		if (queryType.equals(PatrolGriddedQuery.KEY)){
			tableName = PatrolGriddedQuery.class.getSimpleName(); 
			resourceKey = "Gridded"; //$NON-NLS-1$
		}else if (queryType.equals(PatrolObservationQuery.KEY)){
			tableName = PatrolObservationQuery.class.getSimpleName(); 
			resourceKey = "Waypoint"; //$NON-NLS-1$
		}else if (queryType.equals(PatrolWaypointQuery.KEY)){	
			tableName = PatrolWaypointQuery.class.getSimpleName(); 
			resourceKey = "Waypoint"; //$NON-NLS-1$
		}else if (queryType.equals(PatrolQuery.KEY)){	
			tableName = PatrolQuery.class.getSimpleName(); 
			resourceKey = "Patrol"; //$NON-NLS-1$
		}else{
			return null;
		}
		
		Query query = s.createQuery("SELECT style FROM " + tableName + " WHERE uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		query.setParameter("uuid", queryUuid); //$NON-NLS-1$
		List<?> results = query.list();
		if (results.size() == 0 ) return null;
		
		String stylemap = (String)results.get(0);
		
		try {
			return StyleManager.INSTANCE.fromStringMap(stylemap).get(resourceKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		return null;

	}

}
