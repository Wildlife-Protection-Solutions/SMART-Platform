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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.query.common.model.udig.IQueryService;

/**
 * Tracks services and layers added to a map.
 * 
 * @author Emily
 *
 */
public class MapLayerTracker {

	private List<IQueryService> services;
	private Collection<ILayer> layers;
	
	public MapLayerTracker(){
		services = new ArrayList<IQueryService>();
		layers = new ArrayList<ILayer>();
	}
	
	public void addService(IQueryService service){
		services.add(service);
	}

	public Collection<IQueryService> getServices(){
		return this.services;
	}
	
	public void clearAll(Map map, IProgressMonitor monitor){
		DeleteLayersCommand cmd = new DeleteLayersCommand(layers.toArray(new ILayer[layers.size()]));
		map.sendCommandASync(cmd);
		
		layers.clear();
		for (IQueryService s : services){
			CatalogPlugin.getDefault().getLocalCatalog().remove((IService)s);
			((IService)s).dispose(monitor);
		}
		services.clear();
	}
	public void addLayer(ILayer layer){
		layers.add(layer);
	}
}
