package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.udig.query.QueryGeoResource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.ui.editors.query.IntelQueryEditor;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Removes and add query layers to map; rerunning queries as required
 * 
 * @author Emily
 *
 */
public class WorkingSetQueryLayersJob extends WorkingSetMapLayersJob {
	
	public static final String WS_MAP_LAYER_KEY = "org.wcs.smart.i2.ws.map.wslayer"; //$NON-NLS-1$

	public WorkingSetQueryLayersJob(Map map, IEclipseContext context, ILayerListener... layerlisteners){
		super(map, context, layerlisteners);
		setName("loading working set query map layers");
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
	
		//remove all existing query layers from map
		List<ILayer> layersToRemove = new ArrayList<ILayer>();
		for (ILayer l : map.getLayersInternal()){
			Object x = l.getBlackboard().get(WS_MAP_LAYER_KEY) ;
			if (x != null && ((Boolean)x)){
				if (l.getGeoResource().canResolve(QueryService.class)){
					layersToRemove.add(l);
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
						
			for (IntelWorkingSetQuery query: workingset.getQueries()){
				
				RunQueryJob job = new RunQueryJob(query.getQuery()) {
					
					@Override
					protected void onError(Exception ex) {
						
					}
					
					@Override
					protected void onComplete(IPagedQueryResultSet results) {
						if (results == null){
							//Do something better here - show something on tree with error image overlay
							Intelligence2PlugIn.displayLog("Query could not be run.  Results not displayed on map", null);
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
