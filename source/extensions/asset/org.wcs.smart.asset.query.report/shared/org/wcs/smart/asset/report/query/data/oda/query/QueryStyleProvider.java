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
package org.wcs.smart.asset.report.query.data.oda.query;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.report.birt.map.AbstractQueryStyleProvider;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Query style provider for asset queries.
 * 
 * @author Emily
 *
 */
public class QueryStyleProvider  extends AbstractQueryStyleProvider{

	@Override
	public StyleBlackboard getStyle(String queryType, UUID queryUuid, MapLayerInfo.LayerType layerType,  Session s) {
		if (queryUuid == null) return null;
		String tableName = null;
		String resourceKey = null;
		if (queryType.equals(AssetObservationQuery.KEY)){
			tableName = AssetObservationQuery.class.getSimpleName(); 
			resourceKey = "Waypoint"; //$NON-NLS-1$
		}else if (queryType.equals(AssetWaypointQuery.KEY)){	
			tableName = AssetWaypointQuery.class.getSimpleName(); 
			resourceKey = "Waypoint"; //$NON-NLS-1$
		}else{
			return null;
		}
		
		List<String> results = s.createQuery("SELECT style FROM " + tableName + " WHERE uuid = :uuid", String.class) //$NON-NLS-1$ //$NON-NLS-2$
				.setParameter("uuid", queryUuid) //$NON-NLS-1$
				.list();
		
		if (results.size() == 0 ) return null;
		String stylemap = (String)results.get(0);
		try {
			return StyleManager.INSTANCE.fromStringMap(stylemap).get(resourceKey);
		} catch (Exception e) {
			Logger.getLogger(QueryStyleProvider.class.getName()).log(Level.WARNING, "Could not parse query style.", e); //$NON-NLS-1$
		}
		return null;

	}

}
