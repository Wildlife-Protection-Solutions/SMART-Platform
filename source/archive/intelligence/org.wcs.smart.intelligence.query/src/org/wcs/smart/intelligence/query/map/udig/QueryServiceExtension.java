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
package org.wcs.smart.intelligence.query.map.udig;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.core.internal.CorePlugin;
import org.wcs.smart.util.UuidUtils;

/**
 * Udig service extension for intelligence record query
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryServiceExtension implements ServiceExtension {
    public static final String KEY = "org.wcs.smart.udig.catalog.SmartQueryService"; //$NON-NLS-1$
   
    /**
     * Service parameter conservation area uuid key
     */
    public static final String QUERY_UUID_KEY = "queryuuid"; //$NON-NLS-1$
   
	
    /**
     * @see org.locationtech.udig.catalog.ServiceExtension#createService(java.net.URL, java.util.Map)
     */
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(QUERY_UUID_KEY) && params.get(QUERY_UUID_KEY) instanceof UUID) {
            //found it, create the service handle
        	return  new QueryService(params);
        }
        
		return null;
	}

	/**
	 * @see org.locationtech.udig.catalog.ServiceExtension#createParams(java.net.URL)
	 */
	@Override
	public Map<String, Serializable> createParams(URL url) {	
		return createParamsFromUrl(url);

	}
	
	/**
	 * Converts url to a set of query service connection parameters
	 * @param url
	 * @return
	 */
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		/* determine conservation area */
		String quuid = url.getPath();
		if (quuid == null){
			return null;
		}
		int pos = quuid.lastIndexOf('/');
		if (pos < 0){
			pos = 0;
		}
		
		quuid = quuid.substring(pos);
		UUID buuid = UuidUtils.stringToUuid(quuid);
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(QUERY_UUID_KEY, buuid);
		return params;
	}

	/**
	 * Converts parameters into smart waypoint query service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		String url = "smart://smartdb/query/"; //$NON-NLS-1$
		if (params.get(QUERY_UUID_KEY) == null || !(params.get(QUERY_UUID_KEY) instanceof UUID)){
			url += System.nanoTime();
		}else{
			url += UuidUtils.uuidToString((UUID)params.get(QUERY_UUID_KEY)) ;
			//we want each service to have a unique identifier
			url += "/" + System.nanoTime(); //$NON-NLS-1$
		}
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
