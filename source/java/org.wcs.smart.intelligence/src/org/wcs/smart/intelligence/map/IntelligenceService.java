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
package org.wcs.smart.intelligence.map;

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
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.util.UuidUtils;
/**
 * Intelligence point service.
 * 
 * @author Emily
 *
 */
public class IntelligenceService extends IService{
	
	public static final String SERVICE_ID = "org.wcs.smart.intelligence.udig.catalog.intelligenceService"; //$NON-NLS-1$
	private Map<String, Serializable> params;
	private URL url;
	
	private volatile List<IntelligenceGeoResource> members;
	private IntelligenceDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	private Intelligence intelligence = null;
	
	public IntelligenceService(Map<String, Serializable> params) {
		this.params = params;
		this.url = IntelligenceServiceExtension.createURL(this.params);
		this.intelligence = null;
	}
	
	public IntelligenceService(Intelligence intelligence){
		this.intelligence = intelligence;
		this.params = new HashMap<String, Serializable>();
		this.params.put(IntelligenceDataSourceFactory.INTELL_UUID.key, UuidUtils.uuidToString(this.intelligence.getUuid()));
		this.url = IntelligenceServiceExtension.createURL(this.params);
		
	}
	
	public Intelligence getIntelligenceRecord(){
		return intelligence;
	}
	
	
	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws IOException{
		for (IGeoResource member : resources(monitor)){
			((IntelligenceGeoResourceInfo)member.getInfo(monitor)).computeBounds((IntelligenceGeoResource)member, monitor);
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
					ArrayList<IntelligenceGeoResource> temp = new ArrayList<>();
					temp.add(new IntelligenceGeoResource(this));
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
		return new IntelligenceServiceInfo(this);
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
        for( IntelligenceGeoResource resolve : members ) {
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
	
	
	IntelligenceDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (intelligence != null){
                		ds = new IntelligenceDataSource(intelligence);
                	}else{
                		//use factory
                		IntelligenceDataSourceFactory dsf = new IntelligenceDataSourceFactory();
                    
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.put(IntelligenceDataSourceFactory.INTELL_UUID.key, params.get(IntelligenceServiceExtension.INTEL_UUID_KEY));
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (IntelligenceDataSource) dsf.createDataStore(paramsLocal);
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