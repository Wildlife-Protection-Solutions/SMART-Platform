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
package org.wcs.smart.patrol.query.ui.editor;

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
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.map.udig.QueryService;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * Patrol query map page for viewing the results
 * of the patrol query.
 * @author egouge
 * @since 1.0.0
 */
public class PatrolQueryMapPage  extends SmartMapEditorPart{
	
	private PatrolQueryResultsEditor parentEditor;
	private QueryService queryService = null;
	private IViewportModelListener initListener = null; 
	private LoadDefaultLayersJob loadDefaultLayers = null;
	/*
	 * Job for adding query layer to map
	 */
	private Job addLayerJob = new Job(Messages.PatrolQueryMapPage_AddLayersJobName) {
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			queryService = new QueryService(parentEditor.getQueryInternal());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) queryService.resources(monitor);
	    		AddLayersCommand command = new AddLayersCommand(layers);
	    		if (getMap() == null) return Status.CANCEL_STATUS;
	    		getMap().sendCommandASync(command);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Messages.PatrolQueryMapPage_UnknownStatus, IStatus.ERROR, Messages.PatrolQueryMapPage_ErrorLoadingPage, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.PatrolQueryMapPage_RefreshJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (queryService != null){
				try {
					queryService.refresh(null);
					List<IGeoResource> layers = (List<IGeoResource>) queryService.resources(monitor);
					boolean found = false;
					if (layers.size() > 0){
						IGeoResource patrolLayer = layers.get(0);
						for( ILayer layer : getMap().getLayersInternal() ) {
		                	if(layer.getID().equals(patrolLayer.getIdentifier())){
		                		found = true;
		                		break;
		                	}
		                }
					}
					if (!found){
						addLayerJob.schedule();
					}
				
				} catch (IOException e) {
					SmartPatrolPlugIn.log(Messages.PatrolQueryMapPage_ErrorRefreshing, e);
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
	public PatrolQueryMapPage(PatrolQueryResultsEditor parent) {
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

		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
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
	public void reset() {
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
				PatrolQueryPlugIn.log(ex.getMessage(), ex);
			}
			
			CatalogPlugin.getDefault().getLocalCatalog().remove(queryService);
			queryService.dispose(null);
			queryService = null;
			
			refresh();
		}
		
	}

}
