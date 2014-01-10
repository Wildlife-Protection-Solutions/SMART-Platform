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
package org.wcs.smart.entity.ui.editor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ID;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.fixed.map.EntityQueryService;
import org.wcs.smart.entity.fixed.map.FixedEntityGeoResource;
import org.wcs.smart.entity.fixed.map.FixedEntityService;
import org.wcs.smart.entity.fixed.map.FixedEntityServiceExtension;
import org.wcs.smart.entity.model.EntityType.Type;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
/**
 * Map page that contains the latest sighting query and
 * the fixed positiong for fixed entity types.
 * 
 * @author Emily
 *
 */
public class SightingMapPage extends SmartMapEditorPart implements IEntityTypeEditorPage {

	private FixedEntityService entityService ;
	private EntityQueryService queryService;
	
	private EntityTypeEditor parentEditor;
	private IViewportModelListener initListener = null; 
	private LoadDefaultLayersJob loadDefaultLayers;
	
	private Job addLayerJob = new Job("Add Entity Location Layer") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {  
			try {
				URL url = FixedEntityServiceExtension.createURL(SmartDB.getCurrentConservationArea());
				
				//find existing service
				FixedEntityService entityService = (FixedEntityService) CatalogPlugin
				.getDefault()
				.getLocalCatalog().getById(IService.class, new ID(url), monitor);
				
				if (entityService == null){
					//if not found, create a new one
					entityService = (FixedEntityService) CatalogPlugin
							.getDefault()
							.getLocalCatalog()
							.acquire(url, monitor);	
				}
				
				 
				FixedEntityGeoResource geoResource = new FixedEntityGeoResource(
						entityService, parentEditor.getEntityType().getName(), parentEditor
								.getEntityType().getUuid());
				
				entityService.refresh(parentEditor.getEntityType(), monitor);
				AddLayersCommand command = new AddLayersCommand(
						Collections.singleton(geoResource), 0);
				getMap().sendCommandASync(command);
				
				

				SightingMapPage.this.entityService = entityService;
			} catch (Exception e) {
				return new Status(IStatus.ERROR, "Unknown", IStatus.ERROR,
						"Unknown error creating entity type map layer.", e);
			}
			return Status.OK_STATUS;

		}
		
	};
	
	private Job addQueryReusltsJob = new Job("Adding Query Results Layer"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (parentEditor.getCurrentQuery() == null){
				return Status.OK_STATUS;
			}
			queryService = new EntityQueryService(parentEditor.getCurrentQuery());
			try {
	    		List<IGeoResource> layers = (List<IGeoResource>) queryService.resources(monitor);
	    		AddLayersCommand command = new AddLayersCommand(layers);
	    		if (getMap() == null) return Status.CANCEL_STATUS;
	    		getMap().sendCommandASync(command);
	    		
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, IStatus.ERROR, "Unknown error adding sightings to map", e);
			}
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job updateLayerJob = new Job("Updating Location Layer") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {  
			if (queryService != null){
				try {
					queryService.refresh(parentEditor.getCurrentQuery());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (entityService != null){				
				try {
					entityService.refresh(parentEditor.getEntityType(), monitor);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * Creates a new map page
	 * @param e parent editor
	 */
	public SightingMapPage(EntityTypeEditor e){
		this.parentEditor = e;
	}
	
	@Override
	public MultiPageEditorPart getParentEditor() {
		return parentEditor;
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), true);
		loadDefaultLayers.schedule();
		
		addPointLayers();
		
		//add initial zoom to extents command
		initListener = new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if (getMap() != null) {
					getMap().getViewportModel()
							.removeViewportModelListener(initListener);
					getMap().sendCommandASync(new ZoomExtentCommand());
				}

			}
		};
		getMap().getViewportModel().addViewportModelListener(
				initListener);
	}

	/**
	 * Creates the incident layer
	 */
	private void addPointLayers() {
		//for fixed entities add fixed location entities
		if (parentEditor.getEntityType().getType() == Type.FIXED){
			addLayerJob.schedule();
		}
		
	}

	/**
	 * Updates the incident layer
	 */
	public void updateFixedEntityLayer() {
		if (parentEditor.getEntityType().getType() == Type.FIXED){
			updateLayerJob.schedule(100);
		}
	}

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		if (queryService == null){
			addQueryReusltsJob.schedule();
		}
		updateFixedEntityLayer();
	}
	
	
	@Override
	public void dispose(){
		super.dispose();
		
		loadDefaultLayers.cancel();
		addLayerJob.cancel();
		addQueryReusltsJob.cancel();
		updateLayerJob.cancel();
	    
		if (queryService != null){
			CatalogPlugin.getDefault().getLocalCatalog().remove(queryService);
	        queryService.dispose(null);
	           queryService = null;
	    }
		
	}
}