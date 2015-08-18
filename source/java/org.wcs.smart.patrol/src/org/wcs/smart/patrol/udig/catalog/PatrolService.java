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
package org.wcs.smart.patrol.udig.catalog;

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
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.geotools.PatrolDataSource;
import org.wcs.smart.patrol.geotools.PatrolDataSourceFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.UuidUtils;

/**
 * A udig service for smart
 * conservation area "area" data.
 * 
 * Service exists for a conservation area.  Each
 * area type is a separate georesource.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.patrol.udig.catalog.patrolService"; //$NON-NLS-1$
	private Map<String, Serializable> params;
	private URL url;
//	private Session session = null;
	
	private List<PatrolGeoResource> members;
	private PatrolDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	private Patrol patrol = null;
	
	public PatrolService(Map<String, Serializable> params) {
		this.params = params;
		this.url = PatrolServiceExtension.createURL(this.params);
		this.patrol = null;
	}
	
	public PatrolService(Patrol patrol){
		this.patrol = patrol;
		this.params = new HashMap<String, Serializable>();
		this.params.put(PatrolDataSourceFactory.PATROL_UUID.key, UuidUtils.uuidToString(this.patrol.getUuid()));
		this.url = PatrolServiceExtension.createURL(this.params);
		
	}
	
	public String getPatrolID(){
		if (patrol == null){
			return ""; //$NON-NLS-1$
		}
		return patrol.getId();
	}
	
	public void updatePatrol(Patrol newPatrol){
		this.patrol = newPatrol;
	}
	
	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	
	public void refresh(Patrol patrol, IProgressMonitor monitor) throws IOException{
		this.patrol = patrol;
		this.ds.updatePatrol(patrol);
		
		for (IGeoResource member : resources(monitor)){
			((PatrolGeoResourceInfo)member.getInfo(monitor)).computeBounds((PatrolGeoResource)member, monitor);
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
					members = new ArrayList<PatrolGeoResource>();
					members.add(new PatrolGeoResource(this, PatrolDataSource.TRACK_TYPE));
					members.add(new PatrolGeoResource(this, PatrolDataSource.WAYPOINT_TYPE));
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
		return new SmartServiceInfo(this);
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return params;
	}

	@Override
	public void dispose( IProgressMonitor monitor ) {
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( PatrolGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	SmartPatrolPlugIn.log("Could not dispose Patrol Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	PatrolDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (patrol != null){
                		ds = new PatrolDataSource(patrol);
                	}else{
                		//use factory
                		PatrolDataSourceFactory dsf = new PatrolDataSourceFactory();
                    
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.put(PatrolDataSourceFactory.PATROL_UUID.key, params.get(PatrolServiceExtension.PATROL_UUID_KEY));
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (PatrolDataSource) dsf.createDataStore(paramsLocal);
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
