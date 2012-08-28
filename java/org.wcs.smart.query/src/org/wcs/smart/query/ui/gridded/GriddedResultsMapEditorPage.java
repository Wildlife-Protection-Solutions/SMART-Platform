package org.wcs.smart.query.ui.gridded;

import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.IMap;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.ui.ApplicationGIS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.gridded.RasterService;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * Presents the map with the GridCoverage which result of queryS
 * 
 * @author Mauricio Pazos
 */
public class GriddedResultsMapEditorPage extends SmartMapEditorPart{
	
	private GriddedEditor parentEditor;
	/// private QueryService queryService = null;  it was replaced by rasterService
	private RasterService rasterService = null; 
	private IViewportModelListener initListener = null; 
	private LoadDefaultLayersJob loadDefaultLayers = null;
	/*
	 * Job for adding Raster layer to map
	 */
	private Job addLayerJob = new Job("Add Query Raster Layers") {

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {

	    	try {
	    		// retrieves the last query result
				GriddedQuery query = (GriddedQuery) parentEditor.getQuery(); 
				List<QueryResultItem> queryResults = query.getLastResults();
	    		if( (queryResults == null) || queryResults.isEmpty() ) return Status.OK_STATUS;
	    		
	    		// creates a raster georesource using the query result
				IMap activeMap = ApplicationGIS.getActiveMap();
				rasterService = new RasterService(activeMap, query.getId(), queryResults);
				List<IGeoResource> rasterResourceList = (List<IGeoResource>) rasterService.getGeoResource();
				assert !rasterResourceList.isEmpty();
				
				Map map = getMap();
	    		if (map == null) return Status.CANCEL_STATUS;
	    		
				updateMap(map, rasterResourceList);
				
				
			} catch (Exception e) {
				
				return new Status(IStatus.ERROR, "unknown", IStatus.ERROR, "Error loading pages", e);
			}
			return Status.OK_STATUS;
		}

		/**
		 * Adds the new raster resource to the mapraster it will be deleted.
		 * 
		 * @param map
		 * @param rasterResourceList
		 */
		private void updateMap(final Map map, final List<IGeoResource> rasterResourceList) {
			
    		List<IGeoResource> layers = new LinkedList<IGeoResource>();
    		
			layers.addAll(rasterResourceList);
			
			// add the new layers to the map
			AddLayersCommand command = new AddLayersCommand(layers);
    		map.sendCommandASync(command);
    		initListener = new IViewportModelListener() {
				@Override
				public void changed(ViewportModelEvent event) {
					map.getViewportModel().removeViewportModelListener(initListener);
					map.sendCommandASync(new ZoomExtentCommand());
				}
			};
    		map.getViewportModel().addViewportModelListener(initListener);
    		
    		map.getRenderManager().refresh(null);
		}
	};
	
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job("Raster Refresh Job"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (rasterService != null){
				try {
					rasterService.refresh(null);
					
					List<IGeoResource> layers = (List<IGeoResource>) rasterService.resources(monitor);
					boolean found = false;
					if (layers.size() > 0){
						IGeoResource rasterLayers = layers.get(0);
						for( ILayer layer : getMap().getLayersInternal() ) {
		                	if(layer.getID().equals(rasterLayers.getIdentifier())){
		                		found = true;
		                		break;
		                	}
		                }
					}
					if (!found){
						addLayerJob.schedule();
					}
					
				
				} catch (Exception e) {
					QueryPlugIn.log("Error refreshing raster service.", e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}

    };
    
	private List<ILayer> mapContainsRaster(Map map,	List<IGeoResource> rasterResourceList) {

		String rasterName = rasterResourceList.get(0).getIdentifier().getFile().toString();
		List<ILayer> toRemove = new LinkedList<ILayer>();
		
        List<ILayer> mapLayers = map.getMapLayers();
        for( int i = 0; i < mapLayers.size(); i++ ) {
            String layerName = mapLayers.get(i).getName();
            if (rasterName.equals(layerName)) {
                // remove it from layer list
                toRemove.add(mapLayers.get(i));
            }
        }
		return toRemove;
	}
  
	/**
	 * Creates a new query map editor page
	 * 
	 * @param parent
	 *            parent editor
	 */
	public GriddedResultsMapEditorPage(GriddedEditor parent) {
		this.parentEditor = parent;
	}

	/**
	 * @see org.wcs.smart.ui.map.SmartMapEditorPart#getParentEditor()
	 */
	@Override
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof QueryInput)){
			throw new RuntimeException("Invalid editor input.");
		}
		super.init(site, input);
	}

	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();
	}

    

    /**
     * @see org.wcs.smart.ui.map.SmartMapEditorPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        
        if (loadDefaultLayers != null){
        	loadDefaultLayers.cancel();
        	loadDefaultLayers = null;
        }
        addLayerJob.cancel();
        
        if (rasterService != null){
        	this.rasterService.dispose(null);
        	this.rasterService = null;
        }
        refreshJob.cancel();
        refreshJob = null;
    }
    
    /**
     * Refresh the service on the map
     */
    public void refresh(){
    	if (rasterService == null){
    		addLayerJob.schedule();
    	}else{
    		refreshJob.schedule();
    	}
    }

}
