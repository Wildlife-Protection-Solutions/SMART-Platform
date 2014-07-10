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
package org.wcs.smart.plan.map.udig;

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
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.map.geotools.PlanTargetDataSource;
import org.wcs.smart.plan.map.geotools.PlanTargetDataSourceFactory;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.util.SmartUtils;

/**
 * A udig service for plan targets.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PlanTargetService extends IService {

	public static final String SERVICE_ID = "org.wcs.smart.plan.udig.catalog.planService"; //$NON-NLS-1$
	
	private Map<String, Serializable> params;
	private URL url;
	
	private List<PlanTargetGeoResource> members;
	private PlanTargetDataSource ds = null;
	
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	private Plan plan = null;
	private Boolean subPlans = false;
	
	public PlanTargetService(Map<String, Serializable> params) {
		this.params = params;
		this.url = PlanTargetServiceExtension.createURL(this.params);
		
		this.plan = null;
		this.subPlans = (Boolean) params.get(PlanTargetDataSourceFactory.SUB_PLANS.key);
	}
	
	/**
	 * 
	 * @param plan plan
	 * @param subPlans if only subplan targets to be included 
	 */
	public PlanTargetService(Plan plan, Boolean subPlans){
		this.plan = plan;
		this.subPlans = subPlans;
		this.params = new HashMap<String, Serializable>();
		this.params.put(PlanTargetDataSourceFactory.PLAN_UUID.key, SmartUtils.encodeHex(this.plan.getUuid()));
		this.params.put(PlanTargetDataSourceFactory.SUB_PLANS.key, subPlans);
		this.url = PlanTargetServiceExtension.createURL(this.params);
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

	public String getName(){
		if (!subPlans){
			if (plan != null){
				return Messages.PlanTargetService_PlanTargetsLayerName + " (" + plan.getLabel() + ")";   //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				return Messages.PlanTargetService_PlanTargetsLayerName;
			}
		}else{
			if (plan != null){
				return Messages.PlanTargetService_SubPlanTargetsLayerName + " (" + plan.getLabel() + ")";  //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				return Messages.PlanTargetService_SubPlanTargetsLayerName;
			}
		}
	}
	
	public Boolean getSubPlans(){
		return subPlans;
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
					members = new ArrayList<PlanTargetGeoResource>();
					members.add(new PlanTargetGeoResource(this));
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
		return new PlanTargetServiceInfo(this);
	}

	/**
	 * @see net.refractions.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return params;
	}

	/**
	 * Refreshes the bounds for each resource.
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	
	public void refresh(Plan plan, IProgressMonitor monitor) throws IOException{
		this.plan = plan;
		if (this.ds != null){
			this.ds.updatePlan(plan);
			for (IGeoResource member : resources(monitor)){
				((PlanTargetGeoResourceInfo)member.getInfo(monitor)).computeBounds((PlanTargetGeoResource)member, monitor);
			}
		}
	}	
	
	
	@Override
	public void dispose( IProgressMonitor monitor ) {
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( PlanTargetGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	SmartPlanPlugIn.log("Could not dispose plan service", e); //$NON-NLS-1$
            }
        }
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
	
	
	public PlanTargetDataSource getDataStore( IProgressMonitor monitor ) throws IOException {
        if (this.ds == null) {
            dsInstantiationLock.lock();
            try {
                if (ds == null) {
                	if (plan != null){
                		ds = new PlanTargetDataSource(plan, subPlans);
                	}else{
                		//use factory
                		PlanTargetDataSourceFactory dsf = new PlanTargetDataSourceFactory();
                		try {
                			if (dsf.canProcess(params)) {
                				this.ds = (PlanTargetDataSource) dsf.createDataStore(params);
                				this.plan = ((PlanTargetDataSource)ds).getPlan();
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
