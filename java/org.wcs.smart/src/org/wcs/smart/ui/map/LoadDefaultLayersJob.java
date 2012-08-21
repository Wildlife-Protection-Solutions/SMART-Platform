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
package org.wcs.smart.ui.map;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.IMap;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.impl.MapImpl;
import net.refractions.udig.project.internal.render.impl.RenderManagerImpl;
import net.refractions.udig.project.ui.ProjectUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;

/**
 * Job and intitializes a map with the
 * default conservation area basemap 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class LoadDefaultLayersJob extends Job{

	private IMap map;
	private boolean zoom;
	
	public LoadDefaultLayersJob(IMap map, boolean zoom){
		super("Load default layers to map");
		this.map = map;
		this.zoom = zoom;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
		SmartService ss = new SmartService(params);
		CatalogPlugin.getDefault().getLocalCatalog().add(ss);
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		
		BasemapDefinition mapDef = getDefaultDefinition();
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    	if (map != null){
    		try {
    			
    			if (mapDef != null){
    				MapSettings settings = MapSettings.getInstance(mapDef);
    				settings.applyTo((Map) map);
    			}else{
					List<IGeoResource> layers = (List<IGeoResource>) ss.resources(null);
    				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    				List<IGeoResource> cleanedGeoResources;
    				cleanedGeoResources = ProjectUtil.cleanDuplicateGeoResources(layers, map);
    				AddLayersCommand alCommand = new AddLayersCommand(cleanedGeoResources, 0);
    				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
//    				
    				//TODO fix performance issues with add layer command
    				((RenderManagerImpl)map.getRenderManager()).disableRendering();
//    				((MapImpl)map).getContextModel().eSetDeliver(false);
    				map.sendCommandSync(alCommand);
    				map.getBlackboard().put(MapSettings.BASEMAP_BLACKBOARD_KEY,alCommand.getLayers());
//    				((MapImpl)map).getContextModel().eSetDeliver(true);
    				((RenderManagerImpl)map.getRenderManager()).enableRendering();
    				
    			}
    			if (zoom){
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					map.sendCommandASync(new ZoomExtentCommand());
				}
			} catch (IOException e) {
				SmartPlugIn.log("Could not add layers to map.", e);
			}
    	}
		return Status.OK_STATUS;
	}
	
	private BasemapDefinition getDefaultDefinition(){
		BasemapDefinition selection = SmartPlugIn.getDefault().getBasemapSelection();
		if (selection != null) return selection;
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			return HibernateManager.getDefaultBasemapDefinition(s);	
		}finally{
			if (s.getTransaction().isActive()){
				s.getTransaction().commit();
			}
			s.close();
		}
		
	}
}
