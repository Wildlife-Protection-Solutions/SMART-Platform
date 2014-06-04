package org.wcs.smart.udig.catalog.smart.ui;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import net.refractions.udig.catalog.ui.UDIGConnectionFactory;

import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;

/**
 * Connection factory for smart area connection.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartAreaConnectionFactory extends UDIGConnectionFactory {

	public SmartAreaConnectionFactory() {
	}

	@Override
	public Map<String, Serializable> createConnectionParameters(Object context) {
		if (context instanceof SmartService){
			return ((SmartService)context).getConnectionParams();
		}
		
		if (context instanceof URL ){
			return SmartServiceExtension.createParamsFromUrl((URL)context);
		}
		
		return null;

	}

	@Override
	public URL createConnectionURL(Object context) {
		if (context instanceof URL){
			if (SmartServiceExtension.isValid((URL)context)){
				return (URL)context;
			}
			return null;
		}
		if (context instanceof Map){
			@SuppressWarnings("unchecked")
			Map<String, Serializable> params = (Map<String,Serializable>)context;
			return SmartServiceExtension.createURL(params);
		}
		return null;
	}

}
