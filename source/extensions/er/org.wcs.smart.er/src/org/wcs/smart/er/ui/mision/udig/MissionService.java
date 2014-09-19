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
package org.wcs.smart.er.ui.mision.udig;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.IServiceInfo;
import net.refractions.udig.ui.UDIGDisplaySafeLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.util.SmartUtils;

/**
 * Mission point service
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.er.mision.udig.missionService"; //$NON-NLS-1$
	private Map<String, Serializable> params;
	private URL url;
	
	private List<MissionGeoResource> members;
	private MissionDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	private Mission mission = null;
	
	public MissionService(Map<String, Serializable> params) {
		this.params = params;
		this.url = MissionServiceExtension.createURL(this.params);
		this.mission = null;
	}
	
	public MissionService(Mission mission){
		this.mission = mission;
		this.params = new HashMap<String, Serializable>();
		this.params.put(MissionDataSourceFactory.MISSION_UUID.key, SmartUtils.encodeHex(this.mission.getUuid()));
		this.url = MissionServiceExtension.createURL(this.params);
		
	}
	
	public Mission getMissionRecord(){
		return mission;
	}
	
	
	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws IOException{
		for (IGeoResource member : resources(monitor)){
			((MissionGeoResourceInfo)member.getInfo(monitor)).computeBounds((MissionGeoResource)member, monitor);
		}
	}	
	
	
	/**
	 * @see net.refractions.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.refractions.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return null;
	}

	/**
	 * @see net.refractions.udig.catalog.IResolve#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		return this.url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.refractions.udig.catalog.IService#resources(org.eclipse.core.runtime
	 * .IProgressMonitor)
	 */
	@Override
	public List<? extends IGeoResource> resources(IProgressMonitor monitor)
			throws IOException {
		if (members == null){
			synchronized (this) {
				if (members == null){
					members = new ArrayList<MissionGeoResource>();
					members.add(new MissionGeoResource(this, MissionDataSource.MISSIONWAYPOINT_TYPE));
					members.add(new MissionGeoResource(this, MissionDataSource.MISSIONTRACK_TYPE));
				}
			}
		}
		return members;
	}

	/**
	 * @see net.refractions.udig.catalog.IService#createInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return new MissionServiceInfo(this);
	}

	/**
	 * @see net.refractions.udig.catalog.IService#getConnectionParams()
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
        for( MissionGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	EcologicalRecordsPlugIn.log("Could not dispose Patrol Service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	MissionDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (mission != null){
                		ds = new MissionDataSource(mission);
                	}else{
                		//use factory
                		MissionDataSourceFactory dsf = new MissionDataSourceFactory();
                    
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.put(MissionDataSourceFactory.MISSION_UUID.key, params.get(MissionServiceExtension.MISSION_UUID_KEY));
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (MissionDataSource) dsf.createDataStore(paramsLocal);
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
