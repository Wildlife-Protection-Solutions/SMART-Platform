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
package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

/**
 * One service for each entity
 * smart://smartdb/intel2/entity/location/<entityuuid>#TYPE
 * 
 * @author Emily
 */
public class IntelEntityService extends IService {

	private Map<String, Serializable> params;
	private URL url;
	
	private volatile List<IntelEntityGeoResource> members;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	private UUID entityUuid;
	private Exception error;
	
	private IntelEntityDataSource ds = null;
		
	public IntelEntityService(Map<String, Serializable> params) {
		this.params = params;
		this.url = IntelEntityServiceExtension.createURL(this.params);
		try{
			this.entityUuid = UuidUtils.stringToUuid((String) params.get(IntelEntityServiceExtension.ENTITY_UUID_KEY));
		}catch (Exception ex){
			error = ex;
		}
		
		//TODO: listen for changes and configure 
		Job j = new Job("load name"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String recordName = "Intelligence Entity";
				Session s = HibernateManager.openSession();
				try{
					IntelEntity r = (IntelEntity) s.get(IntelEntity.class, entityUuid);
					if (r != null){
						recordName = r.getIdAttributeAsText();
					}
				}catch (Exception ex){
							
				}
				try{
					for (IGeoResource r : resources(monitor)){
						((IntelEntityGeoResourceInfo)r.getInfo(monitor)).setTitle(recordName);
					}
				}catch (Exception ex){
					//TODO:
					ex.printStackTrace();
				}
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	public UUID getEntityUuid(){
		return this.entityUuid;
	}
	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		if (error != null) return Status.BROKEN;
		return Status.CONNECTED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.locationtech.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return error;
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
					members = new ArrayList<IntelEntityGeoResource>();
					//two resources per entity one for points and one for polygons
					members.add(new IntelEntityGeoResource(this, LocationLayerType.POINT));
					members.add(new IntelEntityGeoResource(this, LocationLayerType.POLYGON));
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
		return new IntelEntityServiceInfo(this);
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
        for( IntelEntityGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	Intelligence2PlugIn.log("Could not dispose Intelligence Entity Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	IntelEntityDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (entityUuid != null){
                		ds = new IntelEntityDataSource(entityUuid);
                    }else{
                    	//broken
                    }
                }
            } finally {
                dsInstantiationLock.unlock();
            }
        }
        return this.ds;
    }
}
