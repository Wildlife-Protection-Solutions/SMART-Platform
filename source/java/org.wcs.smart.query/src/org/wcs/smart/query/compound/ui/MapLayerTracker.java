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
package org.wcs.smart.query.compound.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.locationtech.udig.project.ILayer;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.model.Query;

/**
 * Tracks services and layers added to a map.
 * 
 * @author Emily
 *
 */
public class MapLayerTracker {

	private HashMap<UUID, IQueryService> services;
	private HashMap<IQueryService, Collection<ILayer>> layers;
	
	public MapLayerTracker(){
		services = new HashMap<UUID, IQueryService>();
		layers = new HashMap<IQueryService, Collection<ILayer>>();
	}
	public void setService(Query query, IQueryService service){
		services.put(query.getUuid(), service);
	}
	
	public IQueryService getService(UUID query){
		return services.get(query);
	}
	
	public Set<UUID> getQueries(){
		return services.keySet();
		
	}
	
	public void clearAll(UUID queryUuid){
		IQueryService s = services.get(queryUuid);
		if (s != null){
			layers.remove(s);
		}
		services.remove(queryUuid);
		
	}
	public void addLayer(IQueryService service, ILayer layer){
		Collection<ILayer> ls = layers.get(service);
		if (ls == null){
			ls = new ArrayList<ILayer>();
			layers.put(service, ls);
		}
		ls.add(layer);
	}
	
	public Collection<ILayer> getLayers(IQueryService service){
		return layers.get(service);
	}
}
