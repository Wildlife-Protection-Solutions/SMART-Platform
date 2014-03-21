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
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayerCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayerCommand;
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
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.map.EntityQueryService;
import org.wcs.smart.entity.map.FixedEntityGeoResource;
import org.wcs.smart.entity.map.FixedEntityService;
import org.wcs.smart.entity.map.FixedEntityServiceExtension;
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
	
	private Job addLayerJob = new Job(Messages.SightingMapPage_AddEntityLayerJobName) {

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
								.getEntityType().getKeyId());
				
				entityService.refresh(parentEditor.getEntityType(), monitor);
				AddLayersCommand command = new AddLayersCommand(
						Collections.singleton(geoResource), 0);
				getMap().sendCommandASync(command);
				
				

				SightingMapPage.this.entityService = entityService;
			} catch (Exception e) {
				return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, IStatus.ERROR,
						Messages.SightingMapPage_ErrorDescription, e);
			}
			return Status.OK_STATUS;

		}
		
	};
	
	private Layer queryLayer;
	private Job addQueryReusltsJob = new Job(Messages.SightingMapPage_AddQueryJobName){

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
	    		getMap().sendCommandSync(command);
	    		queryLayer = command.getLayers().get(0);
	    		
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, IStatus.ERROR, Messages.SightingMapPage_ErrorDescriptionQuery, e);
			}
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job updateLayerJob = new Job(Messages.SightingMapPage_UpdateLayersJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {  
			if (parentEditor.getCurrentQuery() == null){
				if (queryLayer != null){
					if (getMap().getLayersInternal().contains(queryLayer)){
						DeleteLayerCommand delete = new DeleteLayerCommand(queryLayer);
						getMap().sendCommandSync(delete);
					}
				}
			}else{		
				if (queryService != null){
					try {
						queryService.refresh(parentEditor.getCurrentQuery());
						if (queryLayer != null){
							//add back the query layer
							AddLayerCommand add = new AddLayerCommand(queryLayer);
							getMap().sendCommandSync(add);
						}
					
					} catch (IOException e) {
						EntityPlugIn.log(Messages.SightingMapPage_QueryRefreshError1, e);
					}
				}else{
					addQueryReusltsJob.schedule();
				}
			}
			
			if (entityService != null){				
				try {
					entityService.refresh(parentEditor.getEntityType(), monitor);
				} catch (IOException e) {
					EntityPlugIn.log(Messages.SightingMapPage_QueryRefreshError2, e);
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

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		updateLayerJob.schedule(100);
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