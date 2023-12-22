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
package org.wcs.smart.er.query.map;

import java.io.IOException;
import java.util.List;

import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.map.QueryDataSource;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryDataSource extends QueryDataSource{

	public static Name MISSION_TRACK = new NameImpl("mission", "waypointtrack"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public SurveyQueryDataSource(SimpleQuery query, IProjectionProvider prjProvider){
		super(query, prjProvider);
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
	}
	

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = super.createTypeNames();
		
		if (query instanceof SurveyObservationQuery || 
				query instanceof SurveyWaypointQuery) {		
			names.add(MISSION_TRACK);
		}
		return names;
		
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		if (isMissionTrack(entry.getName())) {
			return new SurveyQueryFeatureSource(entry);	
		}
		return super.createFeatureSource(entry);
		
	}
	
	public static boolean isMissionTrack(Name name) {
		return name.getLocalPart().equalsIgnoreCase(MISSION_TRACK.getLocalPart());
			
	}
	
}
