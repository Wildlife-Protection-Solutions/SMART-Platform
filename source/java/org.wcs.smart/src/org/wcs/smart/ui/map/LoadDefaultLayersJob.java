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
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.ui.ProjectUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Job that initializes a map with the default basemap
 * for the current Conservation Area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class LoadDefaultLayersJob extends Job{

	private static final String JOB_NAME = Messages.LoadDefaultLayersJob_JobName;
	private IMap map;
	private UUID basemapUuid = null;
	
	/**
	 * Creates a new job that loads the the session
	 * basemap (if defined), or the default basemap (if defined)
	 * or the default SMART basemap.
	 * 
	 * @param map the map to apply the default basemap too
	 */
	public LoadDefaultLayersJob(IMap map){
		this(map, null);
	}
	
	/**
	 * Creates a new job that loads:<br>
	 *   the basemap with the given uuid if found; or<br>
	 *   the session basemap if found; or<br>
	 *   the ca default basemap if found; or<br>
	 *   the the smart basemap<br>
	 *   
	 * @param map
	 * @param basemapUuid
	 */
	public LoadDefaultLayersJob(IMap map, UUID basemapUuid){
		super(JOB_NAME);
		this.map = map;
		this.basemapUuid = basemapUuid;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
		SmartService ss = new SmartService(params);
		CatalogPlugin.getDefault().getLocalCatalog().add(ss);
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		
		BasemapDefinition mapDef = null;
		mapDef = getDefinition();
		if (mapDef == null){
			mapDef = getDefaultDefinition();
		}
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    	if (map != null){
    		try {
    			if (mapDef != null){
    				MapSettings settings = MapSettings.getInstance(mapDef);
    				settings.applyTo((Map) map);
    				
    				map.sendCommandASync(new ZoomExtentCommand());
    			}else{
					@SuppressWarnings("unchecked")
					List<IGeoResource> layers = (List<IGeoResource>) ss.resources(null);
    				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    				List<IGeoResource> cleanedGeoResources;
    				cleanedGeoResources = ProjectUtil.cleanDuplicateGeoResources(layers, map);
    				AddLayersCommand alCommand = new AddLayersCommand(cleanedGeoResources, 0);
    				if (monitor.isCanceled()) return Status.CANCEL_STATUS;

    				((RenderManagerImpl)map.getRenderManager()).disableRendering();
    				map.sendCommandSync(alCommand);

    				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    				
    				map.getBlackboard().put(MapSettings.BASEMAP_BLACKBOARD_KEY,alCommand.getLayers());
    				((RenderManagerImpl)map.getRenderManager()).enableRendering();
    				((RenderManagerImpl)map.getRenderManager()).refresh(null);
    				Projection prj = getDefaultCrs();
    				if (prj != null){
    					try{
    						CoordinateReferenceSystem crs = ReprojectUtils.stringToCrs(prj.getDefinition());
    						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
    						ChangeCRSCommand cmd = new ChangeCRSCommand(crs);
    						map.sendCommandSync(cmd);
    					}catch(Exception ex){
    						SmartPlugIn.log(Messages.LoadDefaultLayersJob_Error_ParsingCrs + ex.getLocalizedMessage(), ex);
    					}
    				}
    				
    			}
			} catch (IOException e) {
				SmartPlugIn.log(Messages.LoadDefaultLayersJob_Error_AddingLayers, e);
			}
    	}
		return Status.OK_STATUS;
	}
	
	private Projection getDefaultCrs(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			return HibernateManager.getDefaultProjection(s);	
		}finally{
			s.getTransaction().commit();
			s.close();
		}
		
	}
	
	private BasemapDefinition getDefaultDefinition(){
		BasemapDefinition selection = SmartPlugIn.getDefault().getBasemapSelection();
		if (selection != null) return selection;
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			
			return HibernateManager.getDefaultBasemapDefinition(s);	
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
		
	}
	
	private BasemapDefinition getDefinition(){
		if (basemapUuid == null){
			return null;
		}
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			return HibernateManager.getBasemapDefinition(s, basemapUuid);	
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
		
	}
}
