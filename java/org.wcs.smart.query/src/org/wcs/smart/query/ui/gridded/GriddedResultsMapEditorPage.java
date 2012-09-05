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

package org.wcs.smart.query.ui.gridded;

import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayerCommand;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.map.udig.RasterService;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * Presents the map with the GridCoverage which result of queryS
 * 
 * @author Mauricio Pazos
 */
public class GriddedResultsMapEditorPage extends SmartMapEditorPart{
	
	private GriddedEditor parentEditor;
	private RasterService rasterService = null; 
	private LoadDefaultLayersJob loadDefaultLayers = null;
	
	/*
	 * Job for adding Raster layer to map
	 */
	private Job addLayerJob = new Job("Add Query Raster Layers") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

	    	try {
	    		// retrieves the last query result
				GriddedQuery query = (GriddedQuery) parentEditor.getQuery(); 
				List<GridResultItem> queryResults = query.getLastResults();
	    		if( (queryResults == null) || queryResults.isEmpty() ) return Status.OK_STATUS;
	    		
	    		Map map = getMap();
	    		if (rasterService == null){
	    			String id = query.getId();
	    			if (id == null){
	    				id = String.valueOf(System.nanoTime());
	    			}
	    			rasterService = new RasterService(query);
	    		}
				List<? extends IGeoResource> rasterResourceList = rasterService.resources(monitor);
				assert !rasterResourceList.isEmpty();
				
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
		private void updateMap(final Map map, final List<? extends IGeoResource> rasterResourceList) {
			
    		List<IGeoResource> layers = new LinkedList<IGeoResource>();
			layers.addAll(rasterResourceList);

			map.getRenderManagerInternal().disableRendering();
			// add the new layers to the map
			AddLayersCommand command = new AddLayersCommand(layers);
    		map.sendCommandSync(command);
    		//setup styles
    		map.getRenderManagerInternal().enableRendering();
    		map.getRenderManager().refresh(null);
    		
    		try{
    			CatalogPlugin.getDefault().getLocalCatalog().remove(rasterService);
    		}catch (Exception ex){}
    		
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
					
					List<? extends IGeoResource> layers = rasterService.resources(monitor);
					if (layers.size() > 0){
						IGeoResource rasterLayers = layers.get(0);
						for( ILayer layer : getMap().getLayersInternal() ) {
		                	//if(layer.getID().equals(rasterLayers.getIdentifier() + "@type@geotiff")){
							if (layer.getID().sameFile(rasterLayers.getIdentifier())){
		                		DeleteLayerCommand cmd = new DeleteLayerCommand((Layer)layer);
		                		getMap().sendCommandASync(cmd);
		                		break;
		                	}
		                }
					}
					addLayerJob.schedule();				
				} catch (Exception e) {
					QueryPlugIn.log("Error refreshing raster service.", e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}

    };
    
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
		
		
		final IPageChangedListener initZoom = new IPageChangedListener() {
			
			@Override
			public void pageChanged(PageChangedEvent event) {
				if (event.getSelectedPage() == GriddedResultsMapEditorPage.this){
					getMap().sendCommandASync(new ZoomExtentCommand());
					parentEditor.removePageChangedListener(this);
				}
				
			}
		};
		parentEditor.addPageChangedListener(initZoom);
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
