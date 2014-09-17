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
package org.wcs.smart.er.map.samplingunit;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.MessageFormat;
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
import org.geotools.data.DataStore;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;

/**
 * Service for sampling units. Must be associated
 * with the survey design
 * 
 * @author Emily
 *
 */
public class SamplingUnitService extends IService {

	/**
	 * The query service id
	 */
	public static final String SERVICE_ID = "org.wcs.smart.er.udig.catalog.queryService"; //$NON-NLS-1$
	
	private Map<String, Serializable> params;
	private URL url;	
	private List<SamplingUnitGeoResource> members;
	private SamplingUnitDataSource ds = null;
	private Lock dsInstantiationLock = new UDIGDisplaySafeLock();
	
	private SurveyDesign sd = null;
	private String cachedName = null;
	
	private SamplingUnitServiceInfo info = null;
	
	
	/**
	 * Creates a sampling unit service.
	 * 
	 * @param sd the survey design
	 */
	public SamplingUnitService(SurveyDesign sd){
		this.sd = sd;
		this.cachedName = sd.getName();
		this.params = new HashMap<String, Serializable>();
		this.params.put(SamplingUnitSourceFactory.SD_UUID.key, this.sd.getUuid());
		this.url = SamplingUnitServiceExtension.createURL(this.params);
		
	}

	/**
	 * @return the query 
	 */
	public SurveyDesign getSurveyDesign(){
		return this.sd;
	}
	
	/**
	 * The cached survey name.
	 * @return
	 */
	public String getCachedName(){
		return this.cachedName;
	}
	
	/**
	 * Refreshes the sampling unit layers associated with the service
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws IOException{
		for (IGeoResource member : resources(monitor)){
			((SamplingUnitGeoResourceInfo)member.getInfo(monitor)).computeBounds((SamplingUnitGeoResource)member, monitor);
		}
	}
	
	
	/**
	 * @see net.refractions.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/**
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

	/**
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
					members = new ArrayList<SamplingUnitGeoResource>();
					//add two types
					members.add(new SamplingUnitGeoResource(this, SamplingUnitType.PLOT.name()));
					members.add(new SamplingUnitGeoResource(this, SamplingUnitType.TRANSECT.name()));
					members.add(new SamplingUnitGeoResource(this, SamplingUnitType.RECON.name()));
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
		if (info == null){
			info = new SamplingUnitServiceInfo(this);
		}
		return info;
	}

	/**
	 * @see net.refractions.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return params;
	}

	/**
	 * @see net.refractions.udig.catalog.IService#dispose(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void dispose( IProgressMonitor monitor ) {
        if (members == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
        int steps = (int) ((double) 99 / (double) members.size());
        for( SamplingUnitGeoResource resolve : members ) {
            try {
                SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
                resolve.dispose(subProgressMonitor);
                subProgressMonitor.done();
            } catch (Throwable e) {
            	EcologicalRecordsPlugIn.log("Could not dispose query Service", e); //$NON-NLS-1$
            }
        }
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
                	if (sd != null){
                		ds = new SamplingUnitDataSource(sd);
                	}else{
                		SamplingUnitSourceFactory dsf = new SamplingUnitSourceFactory();
                		try {
                			Map<String, Serializable> paramsLocal = new HashMap<String, Serializable>();
                			paramsLocal.putAll(params);
                			if (dsf.canProcess(paramsLocal)) {
                				this.ds = (SamplingUnitDataSource) dsf.createDataStore(paramsLocal);
                				this.sd = ds.getDesign();
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
	
	
	public class SamplingUnitServiceInfo extends IServiceInfo{

		public SamplingUnitServiceInfo(SamplingUnitService service){
			this.description = Messages.SamplingUnitService_Description;
			this.icon = EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SAMPLING_UNIT_ICON);
			this.keywords = new String[]{Messages.SamplingUnitService_Keyword1, Messages.SamplingUnitService_Keyword2, Messages.SamplingUnitService_Keyword3, Messages.SamplingUnitService_Keyword4, Messages.SamplingUnitService_Keyword5};
			this.title = MessageFormat.format(Messages.SamplingUnitService_title, new Object[]{service.getSurveyDesign().getName()});
		}
		
	}
}
