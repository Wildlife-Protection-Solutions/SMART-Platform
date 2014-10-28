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
package org.wcs.smart.query.common.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayersCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;

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
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * Query editor page for displaying query results
 * on a map.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryMapPageEditor extends SmartMapEditorPart{
	
	private QueryResultsEditor parentEditor;
	private IQueryService queryService = null;
	private LoadDefaultLayersJob loadDefaultLayers = null;
	private IViewportModelListener initListener ;
	/*
	 * Job for adding query layer to map
	 */
	private Job addLayerJob = new Job(Messages.QueryMapPageEditor_AddLayerJobName) {
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			queryService = parentEditor.createQueryService();
			if (queryService == null){
				return Status.OK_STATUS;
			}
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) queryService.resources(monitor);
	    		AddLayersCommand command = new AddLayersCommand(layers);
	    		if (getMap() == null) return Status.CANCEL_STATUS;

	    		getMap().sendCommandASync(command);
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Messages.QueryMapPageEditor_UnknownStatus, IStatus.ERROR, Messages.QueryMapPageEditor_ErrorLoadingPages, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.QueryMapPageEditor_RefreshJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (queryService != null){
				try {
					queryService.refresh(null);
					List<IGeoResource> layers = (List<IGeoResource>) queryService.resources(monitor);
					boolean found = false;
					for (IGeoResource w : layers){
						for( ILayer layer : getMap().getLayersInternal() ) {
							if(layer.getID().equals(w.getIdentifier())){
								found = true;
								break;
							}
	                	}
					}
					if (!found){
						addLayerJob.schedule();
					}
				
				} catch (IOException e) {
					QueryPlugIn.log(Messages.QueryMapPageEditor_ErrorRefreshing, e);
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
	public QueryMapPageEditor(QueryResultsEditor parent) {
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
		if (!(input instanceof QueryEditorInput)){
			throw new RuntimeException("Invalid editor input."); //$NON-NLS-1$
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
		initListener = new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				getMap().getViewportModel().removeViewportModelListener(initListener);
				getMap().sendCommandASync(new ZoomExtentCommand());
			}
		};
		getMap().getViewportModel().addViewportModelListener(initListener); 
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
        
        if (queryService != null){
        	CatalogPlugin.getDefault().getLocalCatalog().remove(queryService);
        	queryService.dispose(null);
            queryService = null;
        }
        
        refreshJob.cancel();
        refreshJob = null;
    }
    
    /**
     * Refresh the service on the map
     */
    public void refresh(){
    	if (queryService == null){
    		addLayerJob.schedule();
    	}else{
    		refreshJob.schedule();
    	}
    }

    /**
     * Dispose of current query service
     * and refresh to create a new one as required.
     */
    public void reset(boolean refresh) {
		if (queryService != null) {
			// remove layers
			try{
				List<ILayer> toRemove = new ArrayList<ILayer>();
				for (ILayer layer : getMap().getLayersInternal()){
					if (  ((IService)layer.getGeoResource().resolve(IService.class,null)) == queryService){
						toRemove.add(layer);
					}
				}
				if (toRemove.size() > 0) {
					getMap().sendCommandSync(
							new DeleteLayersCommand(toRemove.toArray(new ILayer[toRemove.size()])));
				}
			}catch (Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
			}
			
			CatalogPlugin.getDefault().getLocalCatalog().remove(queryService);
			queryService.dispose(null);
			queryService = null;
			
			if (refresh){
				refresh();
			}
		}
		
	}
}
