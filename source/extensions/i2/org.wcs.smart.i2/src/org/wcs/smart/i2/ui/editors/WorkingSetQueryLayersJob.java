package org.wcs.smart.i2.ui.editors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.udig.query.QueryGeoResource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.udig.query.QueryServiceExtension;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Removes and add query layers to map; rerunning queries as required
 * 
 * @author Emily
 *
 */
public class WorkingSetQueryLayersJob extends WorkingSetMapLayersJob {
	
	public static final String WS_MAP_LAYER_KEY = "org.wcs.smart.i2.ws.map.wslayer"; //$NON-NLS-1$

	private List<IntelRecordObservationQuery> queriesToUpdate = null;
	
	/*
	 * simple mutex so this job is only run once at a time
	 */
	private static final ISchedulingRule MUTEX = new ISchedulingRule() {
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return (rule == MUTEX);
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return (rule == MUTEX);
		}
	};
	
	public WorkingSetQueryLayersJob(Map map, IEclipseContext context, ILayerListener... layerlisteners){
		super(map, context, layerlisteners);
		setName("loading working set query map layers"); //$NON-NLS-1$
		setRule(MUTEX);
	}
	
	public WorkingSetQueryLayersJob clone(){
		return new WorkingSetQueryLayersJob(map, context, listeners);
	}
	
	public void setQueriesToUpdate(List<IntelRecordObservationQuery> queriesToUpdate){
		this.queriesToUpdate = queriesToUpdate;
	}
	
	
	@Override
	protected ID getLayerStyleIdentifier(IGeoResource resource){
		try{
			return resource.resolve(QueryGeoResource.class, null).getFixedID();
		}catch (Exception ex){
			return null;
		}
	}
	
	private boolean canDelete(ILayer layer, IProgressMonitor monitor) throws IOException{
		
		Object x = layer.getBlackboard().get(WS_MAP_LAYER_KEY) ;
		if (x != null && ((Boolean)x)){
			if (layer.getGeoResource().canResolve(QueryService.class)){
				if (queriesToUpdate == null) return true;
				
				QueryService service = layer.getGeoResource().resolve(QueryService.class, monitor);
				for (IntelRecordObservationQuery q : queriesToUpdate){
					if (q.getUuid().equals(service.getConnectionParams().get(QueryServiceExtension.QUERY_UUID_KEY))){
						return true;
					}
				}
			}
		}
		return false;
		
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IntelWorkingSet workingset = null;
		if (WorkingSetManager.INSTANCE.getActiveWorkingSet() != null){
			Session s = HibernateManager.openSession();
			try{
				workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
				if (workingset.getQueries() != null){
					workingset.getQueries().forEach(e->e.getQuery().getName());
				}
			}finally{
				s.close();
			}
		}
	
		//remove all existing query layers from map
		List<ILayer> layersToRemove = new ArrayList<ILayer>();
		List<ILayer> currentMapLayers = new ArrayList<ILayer>();
		currentMapLayers.addAll(map.getMapLayers());
		for (ILayer l : currentMapLayers){
			try{
				if (canDelete(l, monitor)){
					layersToRemove.add(l);
					//dispose of query service
					QueryService service = l.getGeoResource().resolve(QueryService.class, monitor);
					if (!service.isDisposed()){
						service.dispose(monitor);
						CatalogPlugin.getDefault().getLocalCatalog().remove(service);
					}
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		if (!layersToRemove.isEmpty()){
			DeleteLayersCommand cmd = new DeleteLayersCommand(layersToRemove.toArray(new ILayer[layersToRemove.size()]));
			map.sendCommandASync(cmd);
		}
					
		if (workingset != null){
			Date[] dates = null;
			String dateFilter = workingset.getEntityDateFilter();
			try{
				String[] bits = dateFilter.split(":"); //$NON-NLS-1$
				DateFilter initFilter = DateFilter.valueOf(bits[0]);
				if (initFilter == DateFilter.CUSTOM){
					dates = new Date[]{new Date(Long.valueOf(bits[1])), new Date(Long.valueOf(bits[2]))};
				}else{
					dates = new Date[]{initFilter.getStartDate(), initFilter.getEndDate()};
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log("Unable to parse entity date filter for working set : " + dateFilter + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
						
			List<IntelWorkingSetQuery> queries = null;
			if (queriesToUpdate != null){
				queries = new ArrayList<IntelWorkingSetQuery>();
				for (IntelRecordObservationQuery q : queriesToUpdate){
					for (IntelWorkingSetQuery wq : workingset.getQueries()){
						if (wq.getQuery().equals(q)){
							queries.add(wq);
						}
					}
					
				}
			}else{
				queries= workingset.getQueries();
				
			}
			for (IntelWorkingSetQuery query: queries){
				
				RunQueryJob job = new RunQueryJob(query.getQuery()) {
					
					@Override
					protected void onError(Exception ex) {
						//TODO: it might be possible to iconify the layer in the tree						
					}
					
					@Override
					protected void onCancel() {
						//TODO: it might be possible to iconify the layer in the tree
					}
					
					@Override
					protected void onComplete(IPagedQueryResultSet results) {
						if (results == null){
							//Do something better here - show something on tree with error image overlay
							Intelligence2PlugIn.displayLog(Messages.WorkingSetQueryLayersJob_QueryRunError, null);
							return;
						}
						//add a new query service with associated layers
						List<IGeoResource> toAdd = new ArrayList<>();
						HashMap<ID, StyleBlackboard>layerStyles = new HashMap<>();
						List<IGeoResource> visible = new ArrayList<>();
						
						QueryService service = new QueryService(results, query.getQuery().getUuid(), query.getQuery().getName());
						computeLayers(toAdd, visible, layerStyles, query, service, monitor);
						
						for (IGeoResource r : toAdd){
							ID styleId = getLayerStyleIdentifier(r);
							if (layerStyles.get(styleId) == null){
								//look for query style
								String style = query.getQuery().getStyle();
								java.util.Map<String, StyleBlackboard> styles = null;
								if (style != null){
									try{
										styles = StyleManager.INSTANCE.fromStringMap(style);
										if (styles.get(r.getIdentifier().getRef()) != null){
											layerStyles.put(styleId, styles.get(r.getIdentifier().getRef()));
										}
									}catch (Exception ex){
										Intelligence2PlugIn.log(ex.getMessage(), ex);
									}
								}
							}
						}
						addLayers(toAdd, visible, layerStyles, false,  null);
					}
				};
				job.setDateFilter(dates);
				job.schedule();
			}
		}
		return Status.OK_STATUS;
	}
	
	
}
