package org.wcs.smart.udig.catalog.smart;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.util.SmartUtils;

import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.ServiceExtension;
import net.refractions.udig.core.internal.CorePlugin;

/**
 * Udig service extension for smart conservation area 
 * Area layers.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartServiceExtension implements ServiceExtension {
    public static final String KEY = "org.wcs.smart.udig.catalog.smartService";
   
    /**
     * Service parameter conservation area uuid key
     */
    public static final String CA_UUID_KEY = "cauuid";
    
    /*
     *URLS for smart services are of the form
     *smart://smartdb/CAUUID#TYPE
     */
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(CA_UUID_KEY) && params.get(CA_UUID_KEY) instanceof byte[]) {
            //found it, create the service handle
        	return  new SmartService(params);
        }
        
		return null;
	}

	@Override
	public Map<String, Serializable> createParams(URL url) {	
		return createParams(url);

	}
	
	public static Map<String, Serializable> createParamsFromUrl(URL url){
		/* determine conservation area */
		String scauuid = url.getPath();
		if (scauuid == null){
			return null;
		}
		if (scauuid.charAt(0) == '/'){
			scauuid = scauuid.substring(1);
		}
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		try{
			byte[] buuid = SmartUtils.decodeHex(scauuid);
			params.put(CA_UUID_KEY, buuid);
		}catch (Exception ex){
			//TODO: do something here
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
		if (params.get(CA_UUID_KEY) == null || !(params.get(CA_UUID_KEY) instanceof byte[])){
			return null;
		}
		String url = "smart://smartdb/" + SmartUtils.encodeHex((byte[])params.get(CA_UUID_KEY)) ;
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
