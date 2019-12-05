/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.udig;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.core.internal.CorePlugin;
import org.wcs.smart.util.UuidUtils;

/**
 * Udig service extension for PAWS results layers
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PawsServiceExtension implements ServiceExtension {

	public static final String KEY = "org.wcs.smart.udig.catalog.pawsService"; //$NON-NLS-1$
	/**
	 * SMART service url host
	 */
    private static final String HOST = "smartdb"; //$NON-NLS-1$
    /**
     * SMART service url protocol
     */
	private static final String PROTOCOL = "smart"; //$NON-NLS-1$
	
    /**
     * Service parameter conservation area uuid key
     */
    public static final String CA_UUID_KEY = "cauuid"; //$NON-NLS-1$
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
        
        //check for the property service key
        if (params.containsKey(CA_UUID_KEY) && params.get(CA_UUID_KEY) instanceof UUID ){
            //found it, create the service handle
        	return new PawsService(params);
        }
        
		return null;
	}

	@Override
	public Map<String, Serializable> createParams(URL url) {
		if (isValid(url)){
			return createParamsFromUrl(url);
		}
		return null;

	}
	
	/**
	 * Determines of the URL is a valid smart service URL
	 * 
	 * @param url URL to test
	 * @return <code>true</code> if valid, <code>false</code> otherwise
	 */
	public static boolean isValid(URL url){
		if (url.getProtocol().equals(PROTOCOL)){
			if (url.getHost().equals(HOST) && url.getPath().startsWith("/")){ //$NON-NLS-1$
				//check for valud ca uuid
				String scauuid = url.getPath();
				if (scauuid == null) return false;
				if (scauuid.charAt(0) == '/'){
					scauuid = scauuid.substring(1);
				}
				try{
					UuidUtils.stringToUuid(scauuid);
					return true;
				}catch(Throwable t){
					
				}
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Creates parameter map from URL. Returns null if URL invalid.
	 * 
	 * @param url
	 * @return
	 */
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		if (!isValid(url)){
			return null;
		}
		
		// determine conservation area
		String scauuid = url.getPath();
		if (scauuid == null){
			return null;
		}
		if (scauuid.charAt(0) == '/'){
			scauuid = scauuid.substring(1);
		}
		
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		try{
			UUID buuid = UuidUtils.stringToUuid(scauuid);
			params.put(CA_UUID_KEY, buuid);
		}catch (Throwable ex){
			Logger.getLogger(PawsServiceExtension.class.getName()).log(Level.WARNING, "Invalid Conservation Area UUID provided in URL.", ex); //$NON-NLS-1$
			return null;
		}
		return params;
	}

	/**
	 * Converts parameters into smart service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		if (params.get(CA_UUID_KEY) == null || !(params.get(CA_UUID_KEY) instanceof UUID)){
			return null;
		}
		String url = PROTOCOL + "://" + HOST + "/" + UuidUtils.uuidToString((UUID)params.get(CA_UUID_KEY)) ; //$NON-NLS-1$ //$NON-NLS-2$
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
