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
package org.wcs.smart.intelligence.ui.editor;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.render.IViewportModelListener;
import org.locationtech.udig.project.render.ViewportModelEvent;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.map.IntelligenceService;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;

/**
 * Intelligence editor map page.
 * @author Emily
 *
 */
public class IntelligenceEditorMapPage extends SmartMapEditorPart {

	
	private IntelligenceService intelService;
	private IntelligenceEditor parentEditor;
	private LoadDefaultLayersJob loadDefaultLayersJob;
	
	
	private Job addLayerJob = new Job(Messages.IntelligenceEditorMapPage_AddLayerJobName) {
		private IViewportModelListener initListener;	
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			intelService = new IntelligenceService(parentEditor.getIntelligence());
	    	try {
	    		@SuppressWarnings("unchecked")
				List<IGeoResource> layers = (List<IGeoResource>) intelService.resources(monitor);
	    		
	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
	    		getMap().sendCommandASync(command);
    		
	    		initListener = new IViewportModelListener() {
					@Override
					public void changed(ViewportModelEvent event) {
						if (getMap() != null){
							getMap().getViewportModel().removeViewportModelListener(initListener);
							getMap().sendCommandASync(new ZoomExtentCommand());
						}
						
					}
				};
	    		getMap().getViewportModel().addViewportModelListener(initListener);
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, e.getLocalizedMessage(), IStatus.ERROR,Messages.IntelligenceEditorMapPage_ErrorLoadingMaps, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.IntelligenceEditorMapPage_RefreshJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (intelService != null){
				try {
					intelService.refresh(null);
				} catch (IOException e) {
					SmartPatrolPlugIn.log(Messages.IntelligenceEditorMapPage_ErrorRefreshingMapPage, e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
    
	    
	public IntelligenceEditorMapPage(IntelligenceEditor parent){
		this.parentEditor = parent;
	}
	
	   public MultiPageEditorPart getParentEditor(){
		   return this.parentEditor;
	   }
		


	private void addLayers(){
		addLayerJob.schedule();
		
		if (loadDefaultLayersJob != null){
			loadDefaultLayersJob.cancel();			
		}
		loadDefaultLayersJob = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayersJob.schedule();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
        addLayers();
	}

    public void refresh(){
    	if (refreshJob != null) {
        	refreshJob.cancel();
        	refreshJob.schedule();
    	}
    }

    @Override
    public void dispose() {
		JobUtil.stopJobs(loadDefaultLayersJob, addLayerJob, refreshJob);
		loadDefaultLayersJob = null;
		refreshJob = null;
    	
        super.dispose();
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(intelService);
        intelService.dispose(null);
        intelService = null;
    }

}
