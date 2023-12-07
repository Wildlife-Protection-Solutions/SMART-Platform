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
package org.wcs.smart.i2.ui.editors;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.ui.WorkbenchException;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.locationtech.udig.project.internal.impl.ContextModelImpl;
import org.opengis.filter.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.WorkingSetManager.LayerStatus;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.map.style.WsEntityDataModelObservationDefaultStyle;
import org.wcs.smart.i2.map.style.WsEntityPositionAttributeDefaultStyle;
import org.wcs.smart.i2.map.style.WsEntityRecordPointObservationDefaultStyle;
import org.wcs.smart.i2.map.style.WsEntityRecordPolygonObservationDefaultStyle;
import org.wcs.smart.i2.map.style.WsRecordObservationQueryPointDefaultStyle;
import org.wcs.smart.i2.map.style.WsRecordObservationQueryPolygonDefaultStyle;
import org.wcs.smart.i2.map.style.WsRecordPointObservationDefaultStyle;
import org.wcs.smart.i2.map.style.WsRecordPolygonObservationDefaultStyle;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IWorkingSetMapLayer;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.query.QueryManager;
import org.wcs.smart.i2.udig.IWorkingSetResource;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.i2.udig.entity.IntelEntityDataSource;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.i2.udig.query.QueryDataSource;
import org.wcs.smart.i2.udig.query.QueryGeoResource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.udig.record.IntelRecordService;
import org.wcs.smart.i2.udig.record.IntelRecordServiceExtension;
import org.wcs.smart.udig.AddContentFilterLayersCommand;
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
	
	private static HashMap<String,String> defaultEntityStyles = new HashMap<>();
	static {
		defaultEntityStyles.put(LocationLayerType.POINT.name(), WsEntityRecordPointObservationDefaultStyle.KEY);
		defaultEntityStyles.put(LocationLayerType.POLYGON.name(), WsEntityRecordPolygonObservationDefaultStyle.KEY);
		defaultEntityStyles.put(LocationLayerType.DM_OBS.name(), WsEntityDataModelObservationDefaultStyle.KEY);
		defaultEntityStyles.put(LocationLayerType.ATTRIBUTE.name(), WsEntityPositionAttributeDefaultStyle.KEY);
	}
	private static HashMap<String,String> defaultRecordStyles = new HashMap<>();
	static {
		defaultRecordStyles.put(LocationLayerType.POINT.name(), WsRecordPointObservationDefaultStyle.KEY);
		defaultRecordStyles.put(LocationLayerType.POLYGON.name(), WsRecordPolygonObservationDefaultStyle.KEY);
	}
	
	private static HashMap<String,String> defaultQueryStyles = new HashMap<>();
	static {
		defaultQueryStyles.put(QueryDataSource.POINT_TYPE.getLocalPart(), WsRecordObservationQueryPointDefaultStyle.KEY);
		defaultQueryStyles.put(QueryDataSource.POLYGON_TYPE.getLocalPart(), WsRecordObservationQueryPolygonDefaultStyle.KEY);
	}
	
	private ISchedulingRule MUTEX = new ISchedulingRule() {
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	};
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
			try(Session s = HibernateManager.openSession()){
				try{
					s.beginTransaction();
					if (resource.getResourceType() == IntelWorkingSetCategory.ENTITY){
						//find working set
						org.hibernate.query.Query<?> q = s.createQuery("FROM IntelWorkingSetEntity i WHERE i.id.entity.uuid = :uuid and i.id.workingSet.uuid = :uuid2", IntelWorkingSetEntity.class); //$NON-NLS-1$
						q.setParameter("uuid", resource.getResourceId()); //$NON-NLS-1$
						q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet()); //$NON-NLS-1$
						workingSetMapLayer = (IWorkingSetMapLayer) q.uniqueResult();
					}else if (resource.getResourceType() == IntelWorkingSetCategory.RECORD){
						org.hibernate.query.Query<?> q = s.createQuery("FROM IntelWorkingSetRecord i WHERE i.id.record.uuid = :uuid and i.id.workingSet.uuid = :uuid2", IntelWorkingSetRecord.class); //$NON-NLS-1$
						q.setParameter("uuid", resource.getResourceId()); //$NON-NLS-1$
						q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet()); //$NON-NLS-1$
						workingSetMapLayer = (IWorkingSetMapLayer) q.uniqueResult();
					}else if (resource.getResourceType() == IntelWorkingSetCategory.QUERIES){
						org.hibernate.query.Query<?> q = s.createQuery("FROM IntelWorkingSetQuery i WHERE i.id.query.uuid = :uuid and i.id.workingSet.uuid = :uuid2", IntelWorkingSetQuery.class); //$NON-NLS-1$
						q.setParameter("uuid", resource.getResourceId()); //$NON-NLS-1$
						q.setParameter("uuid2", WorkingSetManager.INSTANCE.getActiveWorkingSet()); //$NON-NLS-1$
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
				}
			}
			
		}
	};
	
	public WorkingSetMapLayersJob(Map map, IEclipseContext context, ILayerListener... layerlisteners){
		super("loading working set map layers"); //$NON-NLS-1$
		this.map = map;
		this.context = context;
		this.listeners = layerlisteners;
		setRule(MUTEX);
	}
	
	public static void removeLayers(IMap map, List<ILayer> toRemove){
		if (toRemove == null || toRemove.isEmpty()) return;
		for (ILayer l : toRemove){
			
			if (!l.getGeoResource().canResolve(QueryService.class)) continue;
			try{
				QueryService service = l.getGeoResource().resolve(QueryService.class, new NullProgressMonitor());
				if (!service.isDisposed()){
					service.dispose(new NullProgressMonitor());
					CatalogPlugin.getDefault().getLocalCatalog().remove(service);
				}
				l.getGeoResource().dispose(new NullProgressMonitor());			
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		DeleteLayersCommand cmd = new DeleteLayersCommand(toRemove.toArray(new ILayer[toRemove.size()]));
		map.sendCommandSync(cmd);		
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		HashMap<IntelWorkingSetQuery, AbstractIntelQuery> queriesToAdd = new HashMap<>();
		
		IntelWorkingSet workingset = null;
		
		
		if (WorkingSetManager.INSTANCE.getActiveWorkingSet() != null){
			try(Session s = HibernateManager.openSession()){
				workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
				if (workingset == null) return Status.OK_STATUS; //ws not found
				if (workingset.getEntities() != null){
					workingset.getEntities().forEach(e->e.getEntity().getIdAttributeAsText());
				}
				if (workingset.getRecords() != null){
					workingset.getRecords().forEach(e->e.getRecord().getTitle());
				}
				if (workingset.getQueries() != null){
					workingset.getQueries().forEach(e->{
						AbstractIntelQuery q = QueryManager.INSTANCE.findQuery(s, e.getQuery(), e.getQueryType());
						if (q != null && WorkingSetManager.INSTANCE.canViewItem(e, q) == LayerStatus.OK) {
							queriesToAdd.put(e, q);
						}
					});
				}
			}
		}
		if (workingset == null){
			//delete all working set layers 
			List<ILayer> layersToRemove = new ArrayList<ILayer>();
			for (ILayer l : map.getLayersInternal()){
				Object x = l.getBlackboard().get(WS_MAP_LAYER_KEY) ;
				if (x != null && ((Boolean)x)) layersToRemove.add(l);
			}
			removeLayers(map, layersToRemove);
			return Status.OK_STATUS;
		}
		
					
		//parse date filter
		LocalDate[] dates = null;
		try{
			dates = WorkingSetManager.INSTANCE.parseEntityDateFilter(workingset);
		}catch (Exception ex){
			Intelligence2PlugIn.log(Messages.WorkingSetMapLayersJob_ParseError + workingset.getEntityDateFilter() + ". " + ex.getMessage(), ex); //$NON-NLS-1$
		}
			
		List<LayerInfo> toAdd = new ArrayList<LayerInfo>();
		java.util.Map<ID, StyleBlackboard> layerStyles = new HashMap<ID, StyleBlackboard>();
		for (IntelWorkingSetEntity layer : workingset.getEntities()){
			if (WorkingSetManager.INSTANCE.canViewItem(layer, null) == LayerStatus.OK) {
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString( layer.getEntity().getUuid()));
				IntelEntityService service = createEntityService(params);
				computeLayers(toAdd, layerStyles, layer, service, true, monitor);
			}
		}
			
		for (IntelWorkingSetRecord layer : workingset.getRecords()){
			if (WorkingSetManager.INSTANCE.canViewItem(layer, null) == LayerStatus.OK) {
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelRecordServiceExtension.RECORD_UUID_KEY, UuidUtils.uuidToString(layer.getRecord().getUuid()));
				IntelRecordService service = createRecordService(params);
				computeLayers(toAdd, layerStyles, layer, service, false, monitor);
			}
			
		}
		
		for (IntelWorkingSetQuery i : workingset.getQueries()){
			if (queriesToAdd.containsKey(i)) {
				AbstractIntelQuery query = queriesToAdd.get(i);
				String name = query.getName();
				if (name == null) name = Messages.WorkingSetMapLayersJob_QueryNotFound;
				QueryService service = new QueryService(null, query.getUuid(), name);
				computeLayers(toAdd, layerStyles, i, service, false, monitor);	
			}
			
		}
		
		
		//filter layers to add based on what's already on the map
		List<ILayer> layersToRemove = new ArrayList<ILayer>();
		for (ILayer l : map.getLayersInternal()){
			Object x = l.getBlackboard().get(WS_MAP_LAYER_KEY) ;
			if (x != null && ((Boolean)x)){
				boolean found = false;
				for(LayerInfo f : toAdd){
					
					if (f.resource instanceof QueryGeoResource){
						if (l.getGeoResource().canResolve(QueryGeoResource.class)){
							try{
								if (((QueryGeoResource)f.resource).getFixedID().equals(l.getGeoResource().resolve(QueryGeoResource.class, null).getFixedID())){
									found = true;
									toAdd.remove(f);	//already exists we don't need to add it again
									break;			
								}
							}catch (Exception ex){
								ex.printStackTrace();
							}
						}
					}else{
						if (f.resource.getID().equals(l.getGeoResource().getID())){
							found = true;
							toAdd.remove(f);	//already exists we don't need to add it again
							break;
						}
					}
				}
				if (!found){
					layersToRemove.add(l);
				}
			}
		}
		removeLayers(map, layersToRemove);
		addLayers(toAdd, layerStyles, dates);
		//query layers are dealt with in WorkingSetQueryLayerJob		
		
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
	protected void computeLayers(List<LayerInfo> toAdd,
			java.util.Map<ID, StyleBlackboard> layerStyles, 
			IWorkingSetMapLayer item, IService service, boolean canFilter, IProgressMonitor monitor){
		java.util.Map<String, StyleBlackboard> styles = null;
		try{ 
			styles = StyleManager.INSTANCE.fromStringMap(item.getMapStyle());
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
		try {
			for (IGeoResource r : service.resources(monitor)){
				if (service.canResolve(IntelRecordService.class) && 
						!service.resolve(IntelRecordService.class, null).canAddToWorkingSet(r)) {						
					continue;
				}
				toAdd.add(new LayerInfo(r,item, canFilter));
				if (styles != null) layerStyles.put(getLayerStyleIdentifier(r), styles.get(getLayerStyleIdentifier(r).toString()));
			}
		} catch (IOException ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
	}
	
	//add layers to map
	protected void addLayers(List<LayerInfo> toAdd, java.util.Map<ID, StyleBlackboard> layerStyles, LocalDate[] dates){
		Set<IService> services = new HashSet<IService>();
		IProgressMonitor m = new NullProgressMonitor();
		for (LayerInfo r : toAdd){
			try {
				IService s = (IService)r.resource.resolve(IService.class, m);
				if (s != null) services.add(s);
			} catch (IOException e) {
				Intelligence2PlugIn.log(e.getMessage(), e);
			}
		}
		
		ICatalog c = CatalogPlugin.getDefault().getLocalCatalog();
		services.forEach(x -> c.remove(x));
		
		List<IGeoResource> filterResources = new ArrayList<>();
		List<IGeoResource> noFilterResources = new ArrayList<>();
		for (LayerInfo l : toAdd){
			if (l.canFilter){
				filterResources.add(l.resource);
			}else{
				noFilterResources.add(l.resource);
			}
		}
		
		map.getRenderManagerInternal().disableRendering();
		map.eSetDeliver(false);
		map.getContextModel().eSetDeliver(false); 
		map.getLayerFactory().eSetDeliver(false);
		map.getViewportModelInternal().eSetDeliver(false);
		map.getEditManagerInternal().eSetDeliver(false);
		List<Layer> allLayers = new ArrayList<>();
		try {
			if (!filterResources.isEmpty()){
				Filter f = null;
				if (dates != null) {
					f = IntelEntityDataSource.createDateTimeFilter(dates[0] == null ? null : dates[0].atStartOfDay(), dates[1] == null ? null : dates[1].atTime(LocalTime.MAX));
				}
				AddContentFilterLayersCommand addCmd = new AddContentFilterLayersCommand(filterResources, 1, f){
					 public void run( IProgressMonitor monitor ) throws Exception {
						 super.run(monitor);
						 
						 for (Layer layer : getLayers()){
							 allLayers.add(layer);
							 layer.getBlackboard().put(WS_MAP_LAYER_KEY, Boolean.TRUE);
							 
							 //configure style and visibility
							 ID styleId = getLayerStyleIdentifier(layer.getGeoResource());
							 StyleBlackboard bb = layerStyles.get(styleId);
							 if (bb != null){
								 layer.setStyleBlackboard(bb);
							 }else {
								 applyDefaultStyle(monitor, layer);
							 }
							 
							 boolean visible = false;
							 for(LayerInfo rr : toAdd){
								 if (getLayerStyleIdentifier(rr.resource).equals(styleId)){
									visible = rr.layerObject.getIsVisible();
									break;
								 }
							 }
							 if(!visible) layer.setVisible(visible);
	
							 //then add listeners
							 layer.addListener(styleListener);
							 for (ILayerListener l : listeners){
								 layer.addListener(l);	 
							 }
						 }
					 }
				};
				map.sendCommandSync(addCmd);
			}
			if (!noFilterResources.isEmpty()){
				AddLayersCommand addCmd = new AddLayersCommand(noFilterResources, 1) {
					public void run(IProgressMonitor monitor) throws Exception {
						super.run(monitor);
						for (Layer layer : getLayers()) {
							allLayers.add(layer);
							layer.getBlackboard().put(WS_MAP_LAYER_KEY, Boolean.TRUE);
							
							//configure style and visibility
							ID styleId = getLayerStyleIdentifier(layer.getGeoResource());
							StyleBlackboard bb = layerStyles.get(styleId);
							if (bb != null) {
								layer.setStyleBlackboard(bb);
							}else {
								applyDefaultStyle(monitor, layer);
							}
							boolean visible = false;
							for(LayerInfo rr : toAdd){
								if (getLayerStyleIdentifier(rr.resource).equals(styleId)){
									visible = rr.layerObject.getIsVisible();
									break;
								}
							}
							if(!visible) layer.setVisible(visible);
							
							//then add listeners
							layer.addListener(styleListener);
							for (ILayerListener l : listeners) {
								layer.addListener(l);
							}
						}
					}
				};
				map.sendCommandSync(addCmd);
			}
		}finally {
        	map.getRenderManagerInternal().enableRendering();
        	map.eSetDeliver(true);
    		map.getContextModel().eSetDeliver(true); 
    		map.getLayerFactory().eSetDeliver(true);
    		map.getViewportModelInternal().eSetDeliver(true);
    		map.getEditManagerInternal().eSetDeliver(true);
//    		
//    		map.getViewportModelInternal().eNotify(notification);
        	
        	
        	 ENotificationImpl notification = new ENotificationImpl((ContextModelImpl)map.getContextModel(),
                     Notification.ADD_MANY, ProjectPackage.CONTEXT_MODEL__LAYERS,
                     null, allLayers);
        	 map.getContextModel().eNotify(notification);
        	 
        	 map.getRenderManager().refresh(null);
		}
	}
	
	private void applyDefaultStyle(IProgressMonitor monitor, Layer layer)
			throws WorkbenchException, IOException {
		//attempt to apply default style
		 if (layer.getGeoResource().canResolve(IntelRecordService.class)) {
			 try(Session session = HibernateManager.openSession()){
				 StyleManager.INSTANCE.applyDefaultStyleToMapLayer(
					 SmartDB.getCurrentConservationArea(), 
					 layer, defaultRecordStyles, session, monitor);
			 }
		 }else if (layer.getGeoResource().canResolve(IntelEntityService.class)) {
			 try(Session session = HibernateManager.openSession()){
				 StyleManager.INSTANCE.applyDefaultStyleToMapLayer(
					 SmartDB.getCurrentConservationArea(), 
					 layer, defaultEntityStyles, session, monitor);
			 }
		 }else if (layer.getGeoResource().canResolve(QueryService.class)) {
			 try(Session session = HibernateManager.openSession()){
				 StyleManager.INSTANCE.applyDefaultStyleToMapLayer(
					 SmartDB.getCurrentConservationArea(), 
					 layer, defaultQueryStyles, session, monitor);
			 }
		 }
	}

	/*
	 * Listens to name changes for record layers and entity layers and
	 * update appropriate map layer names
	 */
	protected class NameChangeListener implements EventHandler{
		
		private IService service;
		
		@SuppressWarnings("unchecked")
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
	
	class LayerInfo{
		IGeoResource resource;
		IWorkingSetMapLayer layerObject;
		boolean canFilter;
		public LayerInfo(IGeoResource resource, IWorkingSetMapLayer layerObject, boolean canFilter){
			this.resource = resource;
			this.layerObject = layerObject;
			this.canFilter = canFilter;
		}
	};
}
