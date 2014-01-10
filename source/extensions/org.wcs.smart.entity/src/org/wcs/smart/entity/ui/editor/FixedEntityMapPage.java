package org.wcs.smart.entity.ui.editor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ID;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.core.internal.CorePlugin;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.fixed.map.FixedEntityGeoResource;
import org.wcs.smart.entity.fixed.map.FixedEntityService;
import org.wcs.smart.entity.fixed.map.FixedEntityServiceExtension;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.SmartUtils;

public class FixedEntityMapPage extends SmartMapEditorPart implements IEntityTypeEditorPage {

	private FixedEntityService entityService ;
	private EntityTypeEditor parent;
	private IViewportModelListener initListener = null; 
	
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
						entityService, parent.getEntityType().getName(), parent
								.getEntityType().getUuid());
				
				entityService.refresh(parent.getEntityType(), monitor);
				AddLayersCommand command = new AddLayersCommand(
						Collections.singleton(geoResource), 0);
				getMap().sendCommandASync(command);
				
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

				FixedEntityMapPage.this.entityService = entityService;
			} catch (Exception e) {
				return new Status(IStatus.ERROR, "Unknown", IStatus.ERROR,
						"Unknown error creating entity type map layer.", e);
			}
			return Status.OK_STATUS;

		}
		
	};
	
	
	private Job updateLayerJob = new Job("Updating Location Layer") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {  
			if (entityService != null){				
				try {
					entityService.refresh(parent.getEntityType(), monitor);
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
	public FixedEntityMapPage(EntityTypeEditor e){
		this.parent = e;
	}
	
	@Override
	public MultiPageEditorPart getParentEditor() {
		return parent;
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		LoadDefaultLayersJob loadDefaultLayers = new LoadDefaultLayersJob(getMap(), true);
		loadDefaultLayers.schedule();
		
		addPointsLayer();
	}

	/**
	 * Creates the incident layer
	 */
	private void addPointsLayer() {
		addLayerJob.schedule();
		
	}

	/**
	 * Updates the incident layer
	 */
	public void updatePointsLayer() {
		updateLayerJob.schedule(100);
	}

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		updatePointsLayer();
	}
	
}

