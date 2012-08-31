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

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayerCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.style.sld.SLDContent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.styling.Style;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GridQueryResultMetadata;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.QueryInput;
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

		@Override
		protected IStatus run(IProgressMonitor monitor) {

	    	try {
	    		// retrieves the last query result
				GriddedQuery query = (GriddedQuery) parentEditor.getQuery(); 
				List<GridResultItem> queryResults = query.getLastResults();
	    		if( (queryResults == null) || queryResults.isEmpty() ) return Status.OK_STATUS;
	    		
	    		Map map = getMap();
	    		if (rasterService == null){
	    			rasterService = new RasterService(map, query.getId(), query);
	    		}
				List<IGeoResource> rasterResourceList = (List<IGeoResource>) rasterService.getGeoResource();
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
		private void updateMap(final Map map, final List<IGeoResource> rasterResourceList) {
			
    		List<IGeoResource> layers = new LinkedList<IGeoResource>();
			layers.addAll(rasterResourceList);

			map.getRenderManagerInternal().disableRendering();
			
			// add the new layers to the map
			AddLayersCommand command = new AddLayersCommand(layers);
    		map.sendCommandSync(command);
    		//setup styles
    		List<Layer> addedlayers = command.getLayers();
    		
    		GridQueryResultMetadata resultsMetadata = ((GriddedQuery)parentEditor.getQuery()).getResultMetadata();
    		for(Layer l : addedlayers){
    			try{
    				String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt; &lt;sld:UserLayer&gt; &lt;sld:LayerFeatureConstraints&gt; &lt;sld:FeatureTypeConstraint/&gt; &lt;/sld:LayerFeatureConstraints&gt; &lt;sld:UserStyle&gt; &lt;sld:Name&gt;000051&lt;/sld:Name&gt; &lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; &lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; &lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; &lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; &lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; &lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;1.0&quot; quantity=&quot;" + resultsMetadata.getMinResultValue() + "&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;1.0&quot; quantity=&quot;" + resultsMetadata.getMaxResultValue() + "&quot;/&gt; &lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>";
    				XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
    				SLDContent c = new SLDContent();
    				Style style = (Style)c.load(memento);
    				l.getStyleBlackboard().put(SLDContent.ID, style);
    				l.getStyleBlackboard().setSelected(new String[]{SLDContent.ID});
    			}catch (Exception ex){
    				//eat me; there is something wrong with styling
    			}
    		}
    		map.getRenderManagerInternal().enableRendering();
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

		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), true);
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
