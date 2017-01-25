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
package org.wcs.smart.i2.udig.record;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

/**
 * A udig service for intelligence entity record locations
 * 
 * @author Emily
 */
public class IntelRecordService extends IService {

	private Map<String, Serializable> params;
	private URL url;
	
	private volatile List<IntelRecordGeoResource> members;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	private UUID recordUuid;
	private Exception error;
	
	private IntelRecordDataSource ds = null;
	
	/*
	 * this jobs configures the names of the geo resources
	 * associated with this service
	 */
	private Job configureResourceNames = new Job("load name"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String recordName = "Intelligence Record";
			Session s = HibernateManager.openSession();
			try{
				IntelRecord r = (IntelRecord) s.get(IntelRecord.class, recordUuid);
				if (r != null){
					recordName = r.getTitle();
				}	
			}catch (Exception e){
				Logger.getLogger(IntelRecordService.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}finally{
				s.close();
			}
			
			try{
				for (IGeoResource theseresources : resources(monitor)){
					((IntelRecordGeoResourceInfo)theseresources.getInfo(monitor)).setTitle(recordName);
				}
			}catch (Exception e){
				Logger.getLogger(IntelRecordService.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
	

	public IntelRecordService(Map<String, Serializable> params) {
		this.params = params;
		this.url = IntelRecordServiceExtension.createURL(this.params);
		try{
			this.recordUuid = UuidUtils.stringToUuid((String) params.get(IntelRecordServiceExtension.RECORD_UUID_KEY));
		}catch (Exception ex){
			error = ex;
		}

		configureResourceNames.schedule();
	}
	
	/**
	 * The record uuid represented by this service.
	 * 
	 * @return
	 */
	public UUID getRecordUuid(){
		return this.recordUuid;
	}
	
	/**
	 * Schedule the job to refresh the resource names
	 */
	public void refreshNames(){
		configureResourceNames.schedule();
		try {
			configureResourceNames.join();
		} catch (InterruptedException e) {
			Logger.getLogger(IntelRecordService.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
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
					List<IntelRecordGeoResource> list = new ArrayList<>();
					//two resources per entity one for points and one for polygons
					list.add(new IntelRecordGeoResource(this, LocationLayerType.POINT));
					list.add(new IntelRecordGeoResource(this, LocationLayerType.POLYGON));
					members = list;
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
		return new IntelRecordServiceInfo(this);
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
        super.dispose(monitor);
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	IntelRecordDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (recordUuid != null){
                		ds = new IntelRecordDataSource(recordUuid);
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