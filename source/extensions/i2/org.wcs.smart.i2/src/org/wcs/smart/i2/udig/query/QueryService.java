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
package org.wcs.smart.i2.udig.query;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.i2.query.IPagedQueryResultSet;


/**
 * A udig service for a paged results set query
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryService extends IService {

	/**
	 * The query service id
	 */
	public static final String SERVICE_ID = "org.wcs.smart.patrol.udig.catalog.i2.query"; //$NON-NLS-1$
	
	private Map<String, Serializable> params;
	private URL url;	
	private volatile List<QueryGeoResource> members;
	private DataStore ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	private IPagedQueryResultSet results;
	
	private String queryName;
	/**
	 * Creates a new query service .
	 * 
	 * @param query waypoint query
	 */
	public QueryService(IPagedQueryResultSet results, UUID queryUuid, String queryName){
		this.results = results;
		
		this.params = new HashMap<String, Serializable>();
		this.params.put(QueryDataSourceFactory.QUERY_UUID.key, queryUuid);
		this.url = QueryServiceExtension.createURL(this.params);
		this.queryName = queryName;
	}

	
	public String getQueryName(){
		return this.queryName;
	}
	
	/**
	 * @return the query 
	 */
	public IPagedQueryResultSet getResultSet(){
		return this.results;
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

	public boolean isDisposed(){
		return super.isDisposed;
	}
	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		//if we create a new service it needs to have a unique
		//identifier
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
					ArrayList<QueryGeoResource> temp = new ArrayList<QueryGeoResource>();
					temp.add(new QueryGeoResource(this, QueryDataSource.POINT_TYPE.getLocalPart()));
					temp.add(new QueryGeoResource(this, QueryDataSource.POLYGON_TYPE.getLocalPart()));
					this.members = temp;
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
		super.dispose(monitor);
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
                	if (results != null){
                		ds = new QueryDataSource(getResultSet());
                	}else{
                		throw new IOException("Unable to create datastore - query results not provided."); //$NON-NLS-1$
                    }
                }
            } finally {
                dsInstantiationLock.unlock();
            }
        }
        return this.ds;
    }
}
