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

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.ServiceExtension;
import net.refractions.udig.core.internal.CorePlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Udig service extension for smart queries
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SamplingUnitServiceExtension implements ServiceExtension {
   
	public static final String KEY = "org.wcs.smart.udig.er.catalog.SmartSamplingUnitService"; //$NON-NLS-1$
   
   
	/**
	 * SMART service url host
	 */
    private static final String HOST = "smartdb"; //$NON-NLS-1$
    /**
     * SMART service url protocol
     */
	private static final String PROTOCOL = "smart"; //$NON-NLS-1$
	
    /**
     * @see net.refractions.udig.catalog.ServiceExtension#createService(java.net.URL, java.util.Map)
     */
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(SamplingUnitSourceFactory.SD_UUID.key )
        		&& params.get(SamplingUnitSourceFactory.SD_UUID.key) instanceof byte[]) {
            //found it, create the service handle
        	//in a separate job load survey design
        	final byte[] uuid = (byte[]) params.get(SamplingUnitSourceFactory.SD_UUID.key);
        	final SurveyDesign[] sd = new SurveyDesign[1];
        	Job j = new Job(Messages.SamplingUnitServiceExtension_jobName){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						SurveyDesign sds = (SurveyDesign) s.load(SurveyDesign.class, uuid);
						sds.getName();
						sd[0] = sds;
					}finally{
						s.close();
					}
					return Status.OK_STATUS;
				}};
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				EcologicalRecordsPlugIn.displayLog(e.getMessage(), e);
			}
			
			if (sd[0] != null){
				return new SamplingUnitService(sd[0]);
			}
        }
        
		return null;
	}

	/**
	 * @see net.refractions.udig.catalog.ServiceExtension#createParams(java.net.URL)
	 */
	@Override
	public Map<String, Serializable> createParams(URL url) {	
		if (isValid(url)){
			return createParamsFromUrl(url);
		}
		return null;
	}
	
	/**
	 * Create service url from survey design uuid
	 * @param surveyDesignUuid
	 * @return
	 */
	public static URL createURL(byte[] surveyDesignUuid) {
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SamplingUnitSourceFactory.SD_UUID.key, surveyDesignUuid);
		return createURL(params);

	}
	
	/**
	 * Determine if the url is a valid SMART sampling unit service
	 * 
	 * @param url url to test
	 * @return <code>true</code> if valid, <code>false</code> otherwise
	 */
	public static boolean isValid(URL url){
		if (url.getProtocol().equals(PROTOCOL)){
			if (url.getHost().equals(HOST) && url.getPath().equals("/")){ //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Converts url to a set of query service connection parameters
	 * @param url
	 * @return
	 */
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		if (!isValid(url)){
			return null;
		}
		
		/* determine survey design uuid */
		String sduuid = url.getPath();
		if (sduuid == null){
			return null;
		}
		int pos = sduuid.lastIndexOf('/');
		if (pos < 0){
			pos = 0;
		}else{
			pos++;
		}
		
		try{
			sduuid = sduuid.substring(pos);
			byte[] buuid = SmartUtils.decodeHex(sduuid);
			HashMap<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(SamplingUnitSourceFactory.SD_UUID.key, buuid);
			return params;
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return null;
		
	}

	/**
	 * Converts parameters into smart waypoint query service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		String url = PROTOCOL + "://" + HOST + "/er/"; //$NON-NLS-1$ //$NON-NLS-2$
		if (params.get(SamplingUnitSourceFactory.SD_UUID.key) == null ||
			!(params.get(SamplingUnitSourceFactory.SD_UUID.key) instanceof byte[])){
			url += System.nanoTime();
		}else{
			url += SmartUtils.encodeHex((byte[])params.get(SamplingUnitSourceFactory.SD_UUID.key)) ;
		}
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
