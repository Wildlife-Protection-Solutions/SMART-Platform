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
package org.wcs.smart.cybertracker.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.internal.wms.WMSGeoResourceImpl;

/**
 * For managing other map layers in SMART Mobile package
 *  
 * @author Emily
 *
 */
public class PackageMapLayer {

	private static final String LAYER_TYPE_KEY = "type"; //$NON-NLS-1$
	private String type;
	private Map<String, String> properties;
	
	
	public PackageMapLayer(String type) {
		this();
		this.type = type;
	}
	
	public PackageMapLayer() {
		properties = new HashMap<>();
	}
	
	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}
	
	public String getProperty(String key) {
		return this.properties.get(key);
	}
	
	public Map<String,String> getProperties(){
		return this.properties;
	}
	
	public static PackageMapLayer toMapLayer(IGeoResource resource) throws Exception{
		
		if (resource.canResolve(WMSGeoResourceImpl.class)) {
			
			WMSGeoResourceImpl wms = resource.resolve(WMSGeoResourceImpl.class, new NullProgressMonitor());
			
			IService service = wms.resolve(IService.class, new NullProgressMonitor());
			org.geotools.ows.wms.Layer layer = wms.resolve(org.geotools.ows.wms.Layer.class, new NullProgressMonitor());
			
			PackageMapLayer mlayer = new PackageMapLayer("wms"); //$NON-NLS-1$
			mlayer.addProperty("service", service.getIdentifier().toExternalForm()); //$NON-NLS-1$
			mlayer.addProperty("layers", layer.getName()); //$NON-NLS-1$
			
			return mlayer;
		}
		throw new Exception(MessageFormat.format("The resource type {0} is not supported for package map layers", resource.getTitle())); //$NON-NLS-1$

	}
	
	/**
	 * Converts a list of map layers to JSON representation stored in 
	 * database and provided in package
	 * @param layers
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String toJson(List<PackageMapLayer> layers) {
	
		if (layers == null || layers.isEmpty()) return null;
		
		JSONArray items = new JSONArray();
		
		for (PackageMapLayer layer : layers) {
			JSONObject item = new JSONObject();
			item.put(LAYER_TYPE_KEY, layer.getType());
			for (Entry<String,String> prop: layer.getProperties().entrySet()) {
				item.put(prop.getKey(), prop.getValue());
			}
			items.add(item);
		}
		return items.toJSONString();
	}
	
	/**
	 * Converts a json string to a list of PackageMapLayer objects
	 * @param json
	 * @return
	 * @throws ParseException
	 */
	public static List<PackageMapLayer> fromJSON(String json) throws ParseException {
		if (json == null || json.isBlank()) return new ArrayList<>();
		
		JSONParser pp = new JSONParser();
		JSONArray array = (JSONArray) pp.parse(json);
		List<PackageMapLayer> layers = new ArrayList<>();
		
		for (int i = 0; i < array.size(); i ++) {
			JSONObject item = (JSONObject)array.get(i);
			
			PackageMapLayer layer = new PackageMapLayer(item.get(LAYER_TYPE_KEY).toString());
			for (Object okey : item.keySet()) {
				String key = okey.toString();
				if (key.equalsIgnoreCase(LAYER_TYPE_KEY)) continue;
				
				layer.addProperty(key, item.get(key).toString());
			}
			layers.add(layer);
		}
		
		return layers;
	}
	
}
