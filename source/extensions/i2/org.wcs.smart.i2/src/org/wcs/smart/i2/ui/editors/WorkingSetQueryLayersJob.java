package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
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
import org.wcs.smart.i2.udig.query.QueryDataSourceFactory;
import org.wcs.smart.i2.udig.query.QueryGeoResource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.udig.query.QueryService.State;
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
				QueryService existing = null;
				for (ILayer l : map.getMapLayers()){
					if (l.getGeoResource().canResolve(QueryService.class)){
						try{
							QueryService currentService = l.getGeoResource().resolve(QueryService.class, new NullProgressMonitor());
							if (currentService.getConnectionParams().get(QueryDataSourceFactory.QUERY_UUID.key).equals(query.getQuery().getUuid())){
								existing = currentService;
								break;
							}
							
						}catch (Exception ex){
							ex.printStackTrace();
						}
					}
				}
				if (existing == null) continue;
				
				//if we request specific queries we want to make sure they are done
				//regardless of if results existing already or not
				if (queriesToUpdate == null && existing.getState() != State.NO_RESULTS) continue;	
				existing.setState(State.SCHEDULED);
				
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
						UUID ws = WorkingSetManager.INSTANCE.getActiveWorkingSet();
						if (ws == null) return;
						
						
						//ensure the query is still part of the working set; it might have been deleted by the user
						boolean add = false;
						Session s = HibernateManager.openSession();
						try{
							IntelWorkingSet temp = (IntelWorkingSet) s.get(IntelWorkingSet.class, ws);
							if (temp.getQueries() != null){
								for (IntelWorkingSetQuery tq : temp.getQueries()){
									if (tq.equals(query)){
										add = true;
										break;
									}
								}
							}
						}finally{
							s.close();
						}
						if (!add) return;
						
						
						if (results == null){
							//Do something better here - show something on tree with error image overlay
							Intelligence2PlugIn.displayLog(Messages.WorkingSetQueryLayersJob_QueryRunError, null);
							return;
						}
						//add a new query service with associated layers
						List<LayerInfo> toAdd = new ArrayList<>();
						HashMap<ID, StyleBlackboard>layerStyles = new HashMap<>();
						
						
						//lets find the layers on the map and update;
						//I tried to set the results in the query service but the layer caches all sorts of information
						//that I cannot clear out; so we are going to remove and add back the layers;
						//this is a bit annoying as it changes the layer order
						List<ILayer> toRemove = new ArrayList<ILayer>();
						for (ILayer l : map.getMapLayers()){
							if (l.getGeoResource().canResolve(QueryService.class)){
								try{
									QueryService currentService = l.getGeoResource().resolve(QueryService.class, new NullProgressMonitor());
									if (currentService.getConnectionParams().get(QueryDataSourceFactory.QUERY_UUID.key).equals(query.getQuery().getUuid())){
										toRemove.add(l);
									}
									
								}catch (Exception ex){
									ex.printStackTrace();
								}
							}
						}
						removeLayers(map, toRemove);
						
						
						QueryService service = new QueryService(results, query.getQuery().getUuid(), query.getQuery().getName());
						computeLayers(toAdd, layerStyles, query, service, false, monitor);
						service.setState(State.RESULTS);
						for (LayerInfo r : toAdd){
							ID styleId = getLayerStyleIdentifier(r.resource);
							if (layerStyles.get(styleId) == null){
								//look for query style
								String style = query.getQuery().getStyle();
								java.util.Map<String, StyleBlackboard> styles = null;
								if (style != null){
									try{
										styles = StyleManager.INSTANCE.fromStringMap(style);
										if (styles.get(r.resource.getIdentifier().getRef()) != null){
											layerStyles.put(styleId, styles.get(r.resource.getIdentifier().getRef()));
										}
									}catch (Exception ex){
										Intelligence2PlugIn.log(ex.getMessage(), ex);
									}
								}
							}
						}
						addLayers(toAdd, layerStyles,  null);
					}
				};
				//only run one at a time so that we don't consume all db connections running queries
				job.setRule(WS_QUERY_MUTEX);
				job.setDateFilter(dates);
				job.schedule();
			}
		}
		return Status.OK_STATUS;
	}
	
	private final static ISchedulingRule WS_QUERY_MUTEX = new ISchedulingRule() {
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	};
	
}
