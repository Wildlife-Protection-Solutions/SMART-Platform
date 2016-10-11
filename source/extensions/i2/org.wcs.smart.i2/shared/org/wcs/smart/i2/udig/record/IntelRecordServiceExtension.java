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

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.core.internal.CorePlugin;

/**
 * 
 * @author Emily
 */
public class IntelRecordServiceExtension implements ServiceExtension {
  
    /**
     * Service parameter conservation area uuid key
     */
    public static final String RECORD_UUID_KEY = "intelrecorduuid"; //$NON-NLS-1$
    
    /*
     *URLS for smart services are of the form
     *smart://smartdb/CAUUID#TYPE
     */
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(RECORD_UUID_KEY) && params.get(RECORD_UUID_KEY) instanceof String) {
            //found it, create the service handle
        	return  new IntelRecordService(params);
        }
        
		return null;
	}

	@Override
	public Map<String, Serializable> createParams(URL url) {	
		return createParamsFromUrl(url);

	}
	
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		/* determine conservation area */
		String uuid = url.getPath();
		if (uuid == null){
			return null;
		}
		int pos = uuid.lastIndexOf('/');
		if (pos < 0){
			pos = 0;
		}
		
		uuid = uuid.substring(pos);
		
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(RECORD_UUID_KEY, uuid);
		return params;
	}

	/**
	 * Converts parameters into smart service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		if (params.get(RECORD_UUID_KEY) == null || !(params.get(RECORD_UUID_KEY) instanceof String)){
			return null;
		}
		String url = "smart://smartdb/intel2/record/" + (String)params.get(RECORD_UUID_KEY); //$NON-NLS-1$
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
