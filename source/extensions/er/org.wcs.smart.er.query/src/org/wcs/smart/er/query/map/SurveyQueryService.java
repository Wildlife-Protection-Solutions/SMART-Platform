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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.locationtech.udig.catalog.IGeoResource;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.map.QueryGeoResource;
import org.wcs.smart.query.map.QueryService;
import org.wcs.smart.query.map.QueryServiceExtension;
import org.wcs.smart.query.model.Query;

/**
 * A udig service for a ecological record queries.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryService extends QueryService {

	
	public SurveyQueryService(Map<String, Serializable> params) {
		super(params);
	}

	public SurveyQueryService(Query query, IProjectionProvider projProvider){
		super(query,projProvider);
	}
	
	/**
	 * 
	 * @see
	 * org.locationtech.udig.catalog.IService#resources(org.eclipse.core.runtime
	 * .IProgressMonitor)
	 */
	@Override
	public List<? extends IGeoResource> resources(IProgressMonitor monitor)
			throws IOException {
		
		if (members == null){
			synchronized (this) {
				if (members == null){
					SurveyQueryDataSource ds = (org.wcs.smart.er.query.map.SurveyQueryDataSource) getDataStore(monitor);
					
					ArrayList<QueryGeoResource> temp = new ArrayList<>();
					for (Name n : ds.getNames()) {
						if (SurveyQueryDataSource.isMissionTrack(n)) {
							QueryGeoResource r = new QueryGeoResource(this, n.getLocalPart(), Messages.SurveyQueryService_MissionTrackGeoResource);
							temp.add(r);
						}else {
							String name = ds.getLayerName(n);
							QueryGeoResource r = new QueryGeoResource(this, n.getLocalPart(), name);
							temp.add(r);
						}
					}
					this.members = temp;
				}
			}
		}
		return members;
	}

	/**
	 * Gets the query data source.
	 * 
	 * @param monitor 
	 * @return the query data source 
	 * @throws IOException
	 */
	@Override
	public DataStore getDataStore( IProgressMonitor monitor ) throws IOException {
		if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (query != null){
                		if (query.getTypeKey().equals(SurveyObservationQuery.KEY) ||
                			query.getTypeKey().equals(SurveyWaypointQuery.KEY) ||
                				query.getTypeKey().equals(MissionQuery.KEY) || 
                				query.getTypeKey().equals(MissionTrackQuery.KEY)){
                			ds = new SurveyQueryDataSource((SimpleQuery)query, prjProvider);
                		}
                	}else{
                		//use factory
                		SurveyDataSourceFactory dsf = new SurveyDataSourceFactory();
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.put(SurveyDataSourceFactory.QUERY_UUID.key, params.get(QueryServiceExtension.QUERY_UUID_KEY));
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (SurveyQueryDataSource) dsf.createDataStore(paramsLocal);
                			}
                		} catch (IOException e) {
                			throw e;
                		}
                    }
                }
            } finally {
                dsInstantiationLock.unlock();
            }
        }
        return this.ds;
    }
}
