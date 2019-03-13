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
package org.wcs.smart.er.query.map.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryDataSource extends ContentDataStore{

	public static final String FEATURETYPE_PREFIX = "smart"; //$NON-NLS-1$
	/**
	 * waypoint query data source
	 */
	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	
	/**
	 * mission tracks query data source
	 */
	public static final String WAYPOINT_MISSION_TRACK_TYPE = "WaypointMissionTracks"; //$NON-NLS-1$

	/**
	 * mission tracks query data source
	 */
	public static final String TRACKS_TYPE = "MissionTracks"; //$NON-NLS-1$
	
	private SimpleQuery query;
	private List<QueryColumn> cachedColumns;
	private IProjectionProvider prjProvider;
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public SurveyQueryDataSource(SimpleQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		this.prjProvider = null;
		this.cachedColumns = null;
	}

	public List<QueryColumn> getColumns(){
		return this.cachedColumns;
	}
	
	public SimpleQuery getQuery() {
		return this.query;
	}
	

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> items = new ArrayList<>();
		if (query instanceof MissionQuery || 
			query instanceof MissionTrackQuery){
			items.add(new NameImpl(TRACKS_TYPE));
		}else{
			items.add(new NameImpl(WAYPOINT_TYPE));
			items.add(new NameImpl(WAYPOINT_MISSION_TRACK_TYPE));
		}
		return items;
		
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		if (cachedColumns == null) cachedColumns = query.computeQueryColumns(Locale.getDefault(),  null,  prjProvider);
		return new SurveyQueryFeatureSource(entry);
	}
	
}
