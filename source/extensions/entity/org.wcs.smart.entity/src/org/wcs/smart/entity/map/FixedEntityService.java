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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;

/**
 * A service for fixed entity types.  A single service provides all entity
 * types for a given conservation area.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.entity.udig.catalog.fixedEntityService"; //$NON-NLS-1$
	private Map<String, Serializable> params;
	private URL url;
	
	private volatile List<FixedEntityGeoResource> members;
	private FixedEntityDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();

	private IEntityListener entityListener = null;
	
	public FixedEntityService(Map<String, Serializable> params) {
		this.params = params;
		this.url = FixedEntityServiceExtension.createURL(this.params);
		
		
		//register listener for different entity types
		entityListener = new IEntityListener() {
			@Override
			public void handleEvent(int eventType, Object source) {
				if (eventType == EntityEventManager.ENTITY_TYPE_ADDED ){
					//add new entity type to members
					EntityType et = (EntityType)source;
					members.add(new FixedEntityGeoResource(FixedEntityService.this, et.getName(), et.getKeyId()));
				}else if (eventType == EntityEventManager.ENTITY_TYPE_DELETED){
					//remove from members array
					EntityType et = (EntityType)source;
					if (members != null){
						for (Iterator<FixedEntityGeoResource> iterator = members.iterator(); iterator.hasNext();) {
							FixedEntityGeoResource gr = (FixedEntityGeoResource) iterator.next();
							if (gr.getEntityTypeKey().equals(et.getKeyId())){
								iterator.remove();
							}
						}
					}
				}
			}
		};
		EntityEventManager.getInstance().addListener(entityListener);
	}
		
	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	
	public void refresh(EntityType entityType, IProgressMonitor monitor) throws IOException{
		getDataStore(monitor).refresh(entityType);
		for (final IGeoResource member : resources(monitor)){
			if (((FixedEntityGeoResource)member).getEntityTypeKey().equals(entityType.getKeyId())){
				//update the bounds
				Job j = new Job("recompute layer bounds") { //$NON-NLS-1$
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							((FixedEntityGeoResourceInfo)member.getInfo(monitor)).computeBounds((FixedEntityGeoResource)member, monitor);
						} catch (IOException e) {
							e.printStackTrace();
						}
						return org.eclipse.core.runtime.Status.OK_STATUS;
					}
				};
				j.setSystem(true);
				j.schedule();
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
					members = new ArrayList<FixedEntityGeoResource>();
					Job j = new Job(Messages.FixedEntityService_LoadingJobName) {
						
						@Override
						protected IStatus run(IProgressMonitor monitor) {
								for (EntityType et : EntityHibernateManager.getInstance().getActiveEntityTypes()){
									if (et.getType() == EntityType.Type.FIXED){
										members.add(new FixedEntityGeoResource(FixedEntityService.this, et.getName(), et.getKeyId()));
									}
								}
							return org.eclipse.core.runtime.Status.OK_STATUS;
						}
					};
					j.schedule();
					try {
						j.join();
					} catch (InterruptedException e) {
						EntityPlugIn.log("Error loading fixed entity types.", e); //$NON-NLS-1$
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
		return new FixedEntityServiceInfo(this);
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
		if (entityListener != null){
			EntityEventManager.getInstance().removeListener(entityListener);
		}
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( FixedEntityGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	EntityPlugIn.log("Could not dispose Fixed Entity Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	FixedEntityDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	FixedEntityDataSourceFactory dsf = new FixedEntityDataSourceFactory();
                    
                	try {
                		Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                		paramsLocal.put(FixedEntityDataSourceFactory.CAUUID.key, params.get(FixedEntityServiceExtension.CAUUID_KEY));
                		if (dsf.canProcess(paramsLocal)) {
                			this.ds = (FixedEntityDataSource) dsf.createDataStore(paramsLocal);
                		}
                	} catch (IOException e) {
                		throw e;
                	}
                }
            } finally {
                dsInstantiationLock.unlock();
            }
        }

        return this.ds;
    }
}
