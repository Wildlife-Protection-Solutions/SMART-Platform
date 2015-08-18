package org.wcs.smart.udig.catalog.smart;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.core.internal.CorePlugin;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.util.UuidUtils;

/**
 * Udig service extension for smart conservation area 
 * Area layers.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartServiceExtension implements ServiceExtension {
	


	public static final String KEY = "org.wcs.smart.udig.catalog.smartService"; //$NON-NLS-1$
   
    /**
     * Service parameter conservation area uuid key
     */
    public static final String CA_UUID_KEY = "cauuid"; //$NON-NLS-1$
    
    /*
     *URLS for smart services are of the form
     *smart://smartdb/CAUUID#TYPE
     */
	/**
	 * SMART service url host
	 */
    private static final String HOST = "smartdb"; //$NON-NLS-1$
    /**
     * SMART service url protocol
     */
	private static final String PROTOCOL = "smart"; //$NON-NLS-1$
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(CA_UUID_KEY) && params.get(CA_UUID_KEY) instanceof UUID) {
            //found it, create the service handle
        	return  new SmartService(params);
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
	 * Dermines of the url is a valid smart service url
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
	 * Creates parameter map from url. Returns null if url invalid.
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
			SmartPlugIn.log("Error parsing ca uuid.", ex); //$NON-NLS-1$
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
