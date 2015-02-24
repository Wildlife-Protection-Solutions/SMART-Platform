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
package org.wcs.smart.udig.catalog.smart;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.ui.UDIGDisplaySafeLock;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.geotools.data.smart.SmartDataSource;
import org.wcs.smart.geotools.data.smart.SmartDataSourceFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

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
public class SmartService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.udig.catalog.smartService"; //$NON-NLS-1$
	private Map<String, Serializable> params;
	private URL url;
	
	private List<SmartGeoResource> members;
	private SmartDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	
	public SmartService(Map<String, Serializable> params) {
		this.params = params;
		this.url = SmartServiceExtension.createURL(this.params);
	}
	
	
	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		if (SmartDB.isMultipleAnalysis()){
			return Status.RESTRICTED_ACCESS;
		}
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
					members = new ArrayList<SmartGeoResource>();
					//these are only valid for single-cas
					if (!SmartDB.isMultipleAnalysis()){
						for (int i = 0; i < Area.AreaType.values().length; i ++){
							members.add(new SmartGeoResource(this, Area.AreaType.values()[i]));
						}
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

        int steps = (int) ((double) 99 / (double) members.size());
        for( SmartGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	SmartPlugIn.log(Messages.SmartService_Error_DisposingService, e);
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	

	
	SmartDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	SmartDataSourceFactory dsf = new SmartDataSourceFactory();
                    
                    try {
                        Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                        paramsLocal.put(SmartDataSourceFactory.CA_UUID.key, params.get(SmartServiceExtension.CA_UUID_KEY));
                        if (dsf.canProcess(paramsLocal)) {
                            this.ds = (SmartDataSource) dsf.createDataStore(paramsLocal);
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
