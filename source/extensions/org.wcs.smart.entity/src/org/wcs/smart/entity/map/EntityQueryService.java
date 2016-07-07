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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.core.internal.CorePlugin;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.query.EntitySightingQuery;

/**
 * Service for entity sightings query.
 * 
 * @author Emily
 *
 */
public class EntityQueryService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.entity.udig.catalog.queryEntityService"; //$NON-NLS-1$
	
	private EntitySightingQuery query;
	private URL url;
	
	private volatile List<EntityQueryGeoResource> members;
	private EntityQueryDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();

	private EntityQueryGeoResource geoResource;
	
	private IServiceInfo info = null;
	
	public EntityQueryService(EntitySightingQuery query) {
		this.query = query;
		
		String  url = "smart://smartdb/entitytype/query/" + query.getEntityType().getKeyId()  ; //$NON-NLS-1$
		try {
			this.url = new URL(null, url, CorePlugin.RELAXED_HANDLER);
		} catch (MalformedURLException e) {
			
		}
	}
	
	/**
	 * Refreshes the query results and associated
	 * bounds.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(EntitySightingQuery newQuery ) throws IOException{
		this.query = newQuery;
		if (ds != null){
			ds.refresh(newQuery);	
		}
		if (geoResource != null){
			geoResource.refresh(new NullProgressMonitor());
		}
	}	
	

	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
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
					
					members = new ArrayList<EntityQueryGeoResource>();
					geoResource = new EntityQueryGeoResource(this);
					members.add(geoResource);
					
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
		if (info == null){
			info = new IServiceInfo(Messages.EntityQueryService_ServiceName, Messages.EntityQueryService_ServiceDescription, 
				null, null, null, null,new String[]{Messages.EntityQueryService_Keyword1, Messages.EntityQueryService_Keyword2}, null);
		}
		return info;
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return null;
	}

	@Override
	public void dispose( IProgressMonitor monitor ) {
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( EntityQueryGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	EntityPlugIn.log("Could not dispose Sighting Query Entity Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	EntityQueryDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	ds = new EntityQueryDataSource(query);
                }
            } finally {
                dsInstantiationLock.unlock();
            }
        }

        return this.ds;
    }
}
