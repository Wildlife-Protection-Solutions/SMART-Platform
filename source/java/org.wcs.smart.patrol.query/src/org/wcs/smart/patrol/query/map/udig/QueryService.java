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
package org.wcs.smart.patrol.query.map.udig;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.geotools.data.DataStore;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.patrol.query.map.geotools.PatrolQueryDataSource;
import org.wcs.smart.patrol.query.map.geotools.QueryDataSource;
import org.wcs.smart.patrol.query.map.geotools.QueryDataSourceFactory;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.model.udig.RasterService;
import org.wcs.smart.query.model.Query;

/**
 * A udig service for a smart waypoint or patrol queries.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryService extends IService implements IQueryService {

	/**
	 * The query service id
	 */
	public static final String SERVICE_ID = "org.wcs.smart.patrol.udig.catalog.queryService"; //$NON-NLS-1$
	
	private Map<String, Serializable> params;
	private URL url;	
	private List<QueryGeoResource> members;
	private DataStore ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	private Query query = null;
	private IProjectionProvider prjProvider;
	
	/**
	 * Creates a new query service .
	 * 
	 * @param query waypoint query
	 */
	public QueryService(SimpleQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
		this.params = new HashMap<String, Serializable>();
		this.params.put(QueryDataSourceFactory.QUERY_UUID.key, this.query.getUuid());
		this.params.put(QueryDataSourceFactory.DATE_UUID.key, query.getDateFilter().asString());
		this.url = QueryServiceExtension.createURL(this.params);
		
	}

	/**
	 * @return the query 
	 */
	public Query getQuery(){
		return this.query;
	}
	
	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws IOException{
		for (IGeoResource member : resources(monitor)){
			((QueryGeoResourceInfo)member.getInfo(monitor)).computeBounds((QueryGeoResource)member, monitor);
		}
		if (ds != null){
			for (String name : ds.getTypeNames()){
				ds.removeSchema(name);
			}
		}
	}	
	
	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/**
	 * @see org.locationtech.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return null;
	}

	/**
	 * @see org.locationtech.udig.catalog.IResolve#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		return this.url;
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
					members = new ArrayList<QueryGeoResource>();
					if (query.getTypeKey().equals(PatrolObservationQuery.KEY) || 
							query.getTypeKey().equals(PatrolWaypointQuery.KEY) ){
						members.add(new QueryGeoResource(this, QueryDataSource.WAYPOINT_TYPE));
					}else if (query.getTypeKey().equals(PatrolQuery.KEY) ){
						members.add(new QueryGeoResource(this, PatrolQueryDataSource.PATROL_TYPE));
					}else if (query.getTypeKey().equals(PatrolGriddedQuery.KEY) ){
						members.add(new QueryGeoResource(this, RasterService.GRIDDED_TYPE));
					}
				}
			}
		}
		return members;
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#createInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return new QueryServiceInfo(this);
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return params;
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#dispose(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void dispose( IProgressMonitor monitor ) {
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( QueryGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	QueryPlugIn.log("Could not dispose query Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	/**
	 * Gets the query data source.
	 * 
	 * @param monitor 
	 * @return the query data source 
	 * @throws IOException
	 */
	public DataStore getDataStore( IProgressMonitor monitor ) throws IOException {
		if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (query != null){
                		if (query.getTypeKey().equals(PatrolObservationQuery.KEY) ){
                			ds = new QueryDataSource((PatrolObservationQuery)query, prjProvider);
                		}else if (query.getTypeKey().equals(PatrolWaypointQuery.KEY) ){
                    		ds = new QueryDataSource((PatrolWaypointQuery)query, prjProvider);
                		}else if (query.getTypeKey().equals(PatrolQuery.KEY) ){
                			ds = new PatrolQueryDataSource((PatrolQuery)query, prjProvider);
                		}
                	}else{
                		//use factory
                		QueryDataSourceFactory dsf = new QueryDataSourceFactory();
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.put(QueryDataSourceFactory.QUERY_UUID.key, params.get(QueryServiceExtension.QUERY_UUID_KEY));
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (QueryDataSource) dsf.createDataStore(paramsLocal);
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
