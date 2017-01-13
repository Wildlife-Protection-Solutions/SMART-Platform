package org.wcs.smart.i2.ui.editors;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.opengis.filter.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IWorkingSetMapLayer;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.udig.AddContentFilterLayersCommand;
import org.wcs.smart.i2.udig.IWorkingSetResource;
import org.wcs.smart.i2.udig.entity.IntelEntityDataSource;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.i2.udig.query.QueryGeoResource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.udig.record.IntelRecordService;
import org.wcs.smart.i2.udig.record.IntelRecordServiceExtension;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Adds entity and record layers to working set.  Queries are handled separately in the
 * WorkingSetQueyrLayersJob as they need to be re-run when dates are changed.
 * 
 * @author Emily
 *
 */
public class WorkingSetMapLayersJob extends Job {
	
	public static final String WS_MAP_LAYER_KEY = "org.wcs.smart.i2.ws.map.wslayer"; //$NON-NLS-1$
	
	protected Map map;
	protected ILayerListener[] listeners;
	protected IEclipseContext context;
	
	/*
     * listener that listens for style changes and saves them to the working
     * set item
     */
    protected ILayerListener styleListener = new ILayerListener() {
		
		@Override
		public void refresh(LayerEvent event) {
			if (event.getType() != EventType.STYLE) return;
			
			ILayer layer = event.getSource();
			if (layer == null) return;
			
			Object x = layer.getBlackboard().get(WS_MAP_LAYER_KEY);
			if (x == null) return; //not a working set layer
			if (!((Boolean)x)) return; //not a working set layer
			if (!layer.getGeoResource().canResolve(IWorkingSetResource.class)) return;
			
			IWorkingSetResource resource = null;
			try {
				resource = layer.getGeoResource().resolve(IWorkingSetResource.class, null);
			} catch (IOException e) {
				Intelligence2PlugIn.log(e.getMessage(), e);
			}
			if (resource == null) return;
			IWorkingSetMapLayer workingSetMapLayer = null;
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				if (resource.getResourceType() == IntelWorkingSetCategory.ENTITY){
					//find working set
					org.hibernate.Query q = s.createQuery("FROM IntelWorkingSetEntity i WHERE i.id.entity.uuid = :uuid and i.id.workingSet.uuid = :uuid2");
					q.setParameter("uuid", resource.getResourceId());
					q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet());
					workingSetMapLayer = (IWorkingSetMapLayer) q.uniqueResult();
				}else if (resource.getResourceType() == IntelWorkingSetCategory.RECORD){
					org.hibernate.Query q = s.createQuery("FROM IntelWorkingSetRecord i WHERE i.id.record.uuid = :uuid and i.id.workingSet.uuid = :uuid2");
					q.setParameter("uuid", resource.getResourceId());
					q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet());
					workingSetMapLayer = (IWorkingSetMapLayer) q.uniqueResult();
				}else if (resource.getResourceType() == IntelWorkingSetCategory.QUERIES){
					org.hibernate.Query q = s.createQuery("FROM IntelWorkingSetQuery i WHERE i.id.query.uuid = :uuid and i.id.workingSet.uuid = :uuid2");
					q.setParameter("uuid", resource.getResourceId());
					q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet());
					workingSetMapLayer = (IWorkingSetMapLayer) q.uniqueResult();
				}
				if (workingSetMapLayer != null){
					java.util.Map<String, StyleBlackboard> styles = StyleManager.INSTANCE.fromStringMap(workingSetMapLayer.getMapStyle());
					styles.put(getLayerStyleIdentifier(layer.getGeoResource()).toString(), (StyleBlackboard)layer.getStyleBlackboard());
					try {
						String styleString = StyleManager.INSTANCE.asString(styles);
						workingSetMapLayer.setMapStyle(styleString);
					} catch (IOException e) {
						Intelligence2PlugIn.log(e.getMessage(), e);
					}
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}finally{
				s.close();
			}
			
		}
	};
	
	public WorkingSetMapLayersJob(Map map, IEclipseContext context, ILayerListener... layerlisteners){
		super("loading working set map layers");
		this.map = map;
		this.context = context;
		this.listeners = layerlisteners;
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IntelWorkingSet workingset = null;
		if (WorkingSetManager.INSTANCE.getActiveWorkingSet() != null){
			Session s = HibernateManager.openSession();
			try{
				workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
				
				if (workingset.getEntities() != null){
					workingset.getEntities().forEach(e->e.getEntity().getIdAttributeAsText());
				}
				if (workingset.getRecords() != null){
					workingset.getRecords().forEach(e->e.getRecord().getTitle());
				}
				if (workingset.getQueries() != null){
					workingset.getQueries().forEach(e->e.getQuery().getName());
				}
			}finally{
				s.close();
			}
		}
	
		//remove all layers from map
		List<ILayer> layersToRemove = new ArrayList<ILayer>();
		for (ILayer l : map.getLayersInternal()){
			Object x = l.getBlackboard().get(WS_MAP_LAYER_KEY) ;
			if (x != null && ((Boolean)x)){
				layersToRemove.add(l);
		
				if (l.getGeoResource().canResolve(QueryService.class)){
					try{
						QueryService service = l.getGeoResource().resolve(QueryService.class, monitor);
						if (!service.isDisposed()){
							service.dispose(monitor);
							CatalogPlugin.getDefault().getLocalCatalog().remove(service);
						}
					}catch (Exception ex){
						Intelligence2PlugIn.log(ex.getMessage(), ex);
					}
				}
			}
		}
		if (!layersToRemove.isEmpty()){
			DeleteLayersCommand cmd = new DeleteLayersCommand(layersToRemove.toArray(new ILayer[layersToRemove.size()]));
			map.sendCommandASync(cmd);
		}
					
		if (workingset != null){
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			List<IGeoResource> visible = new ArrayList<IGeoResource>();
			java.util.Map<ID, StyleBlackboard> layerStyles = new HashMap<ID, StyleBlackboard>();
			for (IntelWorkingSetEntity entity: workingset.getEntities()){
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString(entity.getEntity().getUuid()));
				IntelEntityService service = createEntityService(params);
				computeLayers(toAdd, visible, layerStyles, entity, service, monitor);
			}
			
			Date[] dates = null;
			String dateFilter = workingset.getEntityDateFilter();
			try{
				String[] bits = dateFilter.split(":");
				DateFilter initFilter = DateFilter.valueOf(bits[0]);
				if (initFilter == DateFilter.CUSTOM){
					dates = new Date[]{new Date(Long.valueOf(bits[1])), new Date(Long.valueOf(bits[2]))};
				}else{
					dates = new Date[]{initFilter.getStartDate(), initFilter.getEndDate()};
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log("Unable to parse entity date filter for working set : " + dateFilter + ". " + ex.getMessage(), ex);
			}
			
			addLayers(toAdd, visible, layerStyles, true, dates);
			
			toAdd = new ArrayList<IGeoResource>();
			layerStyles = new HashMap<ID, StyleBlackboard>();
			visible = new ArrayList<IGeoResource>();
			for (IntelWorkingSetRecord record: workingset.getRecords()){
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelRecordServiceExtension.RECORD_UUID_KEY, UuidUtils.uuidToString(record.getRecord().getUuid()));
				IntelRecordService service = createRecordService(params);
				computeLayers(toAdd, visible, layerStyles, record, service, monitor);
			}
			addLayers(toAdd, visible, layerStyles, false,  null);
			
			//query layers are dealt with in WorkingSetQueryLayerJob		
		}
		return Status.OK_STATUS;
	}
	
	
	private IntelEntityService createEntityService(HashMap<String, Serializable> params){
		NameChangeListener nameChangeHandler = new NameChangeListener();
		context.get(IEventBroker.class).subscribe(IntelEvents.ENTITY_MODIFIED, nameChangeHandler);
		IntelEntityService service = new IntelEntityService(params){
			public void dispose(IProgressMonitor monitor){
				context.get(IEventBroker.class).unsubscribe(nameChangeHandler);
				super.dispose(monitor);
			}
		};
		nameChangeHandler.setService(service);
		return service;
	}
	private IntelRecordService createRecordService(HashMap<String, Serializable> params){
		NameChangeListener nameChangeHandler = new NameChangeListener();
		context.get(IEventBroker.class).subscribe(IntelEvents.RECORD_MODIFIED, nameChangeHandler);
		IntelRecordService service = new IntelRecordService(params){
			public void dispose(IProgressMonitor monitor){
				context.get(IEventBroker.class).unsubscribe(nameChangeHandler);
				super.dispose(monitor);
			}
		};
		nameChangeHandler.setService(service);
		return service;
	}
	
	protected ID getLayerStyleIdentifier(IGeoResource resource){
		return resource.getID();
	}
	
	
	//compute the layers to add to map from service
	protected void computeLayers(List<IGeoResource> toAdd, List<IGeoResource> visible,
			java.util.Map<ID, StyleBlackboard> layerStyles, 
			IWorkingSetMapLayer item, IService service, IProgressMonitor monitor){
		java.util.Map<String, StyleBlackboard> styles = null;
		try{ 
			styles = StyleManager.INSTANCE.fromStringMap(item.getMapStyle());
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
		try {
			for (IGeoResource r : service.resources(monitor)){
				toAdd.add(r);
				if (item.getIsVisible()){
					visible.add(r);
				}
				
				if (styles != null) layerStyles.put(getLayerStyleIdentifier(r), styles.get(getLayerStyleIdentifier(r).toString()));
			}
		} catch (IOException ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
	}
	
	//add layers to map
	protected void addLayers(List<IGeoResource> toAdd, List<IGeoResource> visibleLayers, java.util.Map<ID, StyleBlackboard> layerStyles, 
			boolean canFilter, Date[] dates){
		if (canFilter){
			Filter f = null;
			 if (dates != null){
				 f = IntelEntityDataSource.createDateFilter(dates[0], dates[1]);
			 }
			AddContentFilterLayersCommand addCmd = new AddContentFilterLayersCommand(toAdd, 1, f){
				 public void run( IProgressMonitor monitor ) throws Exception {
					 super.run(monitor);
					 
					 for (Layer layer : getLayers()){
						 layer.getBlackboard().put(WS_MAP_LAYER_KEY, Boolean.TRUE);
						 layer.addListener(styleListener);
						 for (ILayerListener l : listeners){
							 layer.addListener(l);	 
						 }
						 ID styleId = getLayerStyleIdentifier(layer.getGeoResource());
						 StyleBlackboard bb = layerStyles.get(styleId);
						 if (bb != null){
							 layer.setStyleBlackboard(bb);
						 }
						 
						 boolean visible = false;
						 for(IGeoResource rr : visibleLayers){
							 if (getLayerStyleIdentifier(rr).equals(styleId)){
								 visible = true;
								 break;
							 }
						 }
						 if(!visible) layer.setVisible(visible);
					 }
				 }
			};
			map.sendCommandASync(addCmd);
		}else{
			AddLayersCommand addCmd = new AddLayersCommand(toAdd, 1) {
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					for (Layer layer : getLayers()) {
						layer.getBlackboard().put(WS_MAP_LAYER_KEY,
								Boolean.TRUE);
						layer.addListener(styleListener);
						for (ILayerListener l : listeners) {
							layer.addListener(l);
						}
						ID styleId = getLayerStyleIdentifier(layer.getGeoResource());
						StyleBlackboard bb = layerStyles.get(styleId);
						if (bb != null) {
							layer.setStyleBlackboard(bb);
						}
						boolean visible = false;
						for (IGeoResource rr : visibleLayers) {
							if (getLayerStyleIdentifier(rr).equals(styleId)) {
								visible = true;
								break;
							}
						}
						if (!visible)layer.setVisible(visible);
						
						
					}
				}
			};
			map.sendCommandASync(addCmd);
		}
		
	
	}

	/*
	 * Listens to name changes for record layers and entity layers and
	 * update appropriate map layer names
	 */
	protected class NameChangeListener implements EventHandler{
		
		private IService service;
		
		@Override
		public void handleEvent(Event event) {
			Object data = event.getProperty(IEventBroker.DATA);
			if (service == null) return;
			boolean updateLayers = false;
			Collection<Object> items = null;
			if (data instanceof Collection<?>){
				items = (Collection<Object>)data;
			}else{
				items = new ArrayList<>();
				items.add(data);
			}
			for (Object x : items){
				if (x instanceof IntelRecord && ((IntelRecord)x).getUuid().equals(((IntelRecordService)service).getRecordUuid())){
					((IntelRecordService)service).refreshNames();
					updateLayers = true;
				}else if (x instanceof IntelEntity && ((IntelEntity)x).getUuid().equals(((IntelEntityService)service).getEntityUuid())){
					((IntelEntityService)service).refreshNames();
					updateLayers = true;
				}
			}
			
			if (updateLayers){
				for (Layer l : map.getLayersInternal()){
					IService lservice = null;
					try{
						lservice = l.getGeoResource().resolve(IService.class, null);
					}catch (Exception ex){
						//eat me
					}
					if (lservice == service){
						l.setName(l.getGeoResource().getTitle());
					}
				}
			}
		}
		public void setService(IService s){
			this.service = s;
		}
	};
}
