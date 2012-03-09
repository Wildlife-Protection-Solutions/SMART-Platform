package org.wcs.smart.patrol.udig.catalog;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
public class PatrolServiceExtension implements ServiceExtension {
    public static final String KEY = "org.wcs.smart.udig.catalog.smartService";
   
    /**
     * Service parameter conservation area uuid key
     */
    public static final String PATROL_UUID_KEY = "patroluuid";
    
    /*
     *URLS for smart services are of the form
     *smart://smartdb/CAUUID#TYPE
     */
	
    @Override
	public IService createService(URL id, Map<String, Serializable> params) {
        if (params == null)
            return null;
            
        //check for the property service key
        if (params.containsKey(PATROL_UUID_KEY) && params.get(PATROL_UUID_KEY) instanceof byte[]) {
            //found it, create the service handle
        	return  new PatrolService(params);
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
		int pos = scauuid.lastIndexOf('/');
		if (pos < 0){
			pos = 0;
		}
		
		scauuid = scauuid.substring(pos);
		byte[] buuid = scauuid.getBytes();
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(PATROL_UUID_KEY, buuid);
		return params;
	}

	/**
	 * Converts parameters into smart service url.
	 * 
	 * @param params smart service connection parameters
	 * @return url generated from connection parameters
	 */
	public static URL createURL(Map<String, Serializable> params){
		if (params.get(PATROL_UUID_KEY) == null || !(params.get(PATROL_UUID_KEY) instanceof byte[])){
			return null;
		}
		String url = "smart://smartdb/patrol/" + new String((byte[])params.get(PATROL_UUID_KEY)) ;
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Throwable t){
			return null;
		}
	}
}
