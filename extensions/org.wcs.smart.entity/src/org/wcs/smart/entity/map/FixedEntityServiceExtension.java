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

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.core.internal.CorePlugin;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * Udig service extension for Fixed Entity Type layers
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityServiceExtension implements ServiceExtension {
    public static final String KEY = "org.wcs.smart.udig.catalog.FixedEntityService"; //$NON-NLS-1$
    
    /**
	 * SMART service url host
	 */
    public static final String HOST = "smartdb"; //$NON-NLS-1$
    /**
     * SMART service url protocol
     */
    public static final String PROTOCOL = "smart"; //$NON-NLS-1$
    /**
     * SMART service url protocol
     */
    public static final String PATH = "/entitytype/fixed"; //$NON-NLS-1$
   
    /**
     * Service parameter for the conservation
     */
    public static final String CAUUID_KEY = "entitytype_cauuid"; //$NON-NLS-1$
    
    /*
     *URLS for smart services are of the form
     *smart://smartdb/entitytype/fixed/CAUUID#ENTITYTYPEUUID
     */
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(CAUUID_KEY) && params.get(CAUUID_KEY) instanceof byte[]) {
            //found it, create the service handle
        	return  new FixedEntityService(params);
        }
        
		return null;
	}

	@Override
	public Map<String, Serializable> createParams(URL url) {	
		return createParamsFromUrl(url);

	}
	
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		if (!url.getHost().equals(HOST) ||
			!url.getProtocol().equals(PROTOCOL) ||
			!url.getPath().startsWith(PATH)){
			return null;
		}
		try {
			/* determine conservation area */
			String scauuid = url.getPath();
			if (scauuid == null){
				return null;
			}
			int i = scauuid.lastIndexOf('/');
			if ( i > 0){
				scauuid = scauuid.substring(i+1);
			}
		
		
			HashMap<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(CAUUID_KEY, SmartUtils.decodeHex(scauuid));
		
			return params;
		} catch (Exception e) {
			EntityPlugIn.log(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Converts parameters into smart service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		if (params.get(CAUUID_KEY) == null || !(params.get(CAUUID_KEY) instanceof byte[])){
			return null;
		}
		String url = PROTOCOL + "://" + HOST + PATH + "/" + SmartUtils.encodeHex((byte[])params.get(CAUUID_KEY)); //$NON-NLS-1$ //$NON-NLS-2$
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
	
	/**
	 * Creates a url using the provided conservation area parameter
	 * @param ca
	 * @return
	 */
	public static URL createURL(ConservationArea ca){
		if (ca == null){
			return null;
		}
		String url = PROTOCOL + "://" + HOST + PATH + "/" + SmartUtils.encodeHex(ca.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
		
	}
}
