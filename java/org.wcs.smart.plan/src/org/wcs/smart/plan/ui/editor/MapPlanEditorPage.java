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
package org.wcs.smart.plan.ui.editor;

import java.io.IOException;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
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
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.map.udig.PlanTargetService;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * Map page for Plan editor.  Displays spatial targets on a map
 * for given plan and children plans.
 * @author Emily
 *
 */
public class MapPlanEditorPage extends SmartMapEditorPart {

	private PlanEditor parentEditor;

	private IViewportModelListener initListener = null; 
	
	private PlanTargetService planTargetService = null;
	private PlanTargetService subPlanTargetService = null;
	private LoadDefaultLayersJob loadDefaultLayers;

	//plan object with all subplans loaded
	//for supporting the subplan spatial targets
	//layer
	private Plan subPlanLayer = null;
	
	/*
	 * Job for adding or refreshing the
	 * subplan spatial target layer.  It makes
	 * use of the subPlanLayer Plan object which will have
	 * all children plans loaded from the database.
	 * It waits for both the defaultlayers job  and the
	 * addlayers job to finish. 
	 */
	private Job refreshSubPlanLayer = new Job(Messages.MapPlanEditorPage_SubplanJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Plan lPlan = subPlanLayer;
			if (lPlan == null){
				return Status.OK_STATUS;
			}
			try {
				if (subPlanTargetService != null){
					subPlanTargetService.refresh(lPlan, monitor);
				}else{
					//wait until plan target layer is added first
					loadDefaultLayers.join();
					addLayerJob.join();
					subPlanTargetService = new PlanTargetService(lPlan, true);
	    			List<IGeoResource> layers = (List<IGeoResource>) subPlanTargetService.resources(monitor);
	    			AddLayersCommand command = new AddLayersCommand(layers, getMap().getLayersInternal().size());
	    			getMap().sendCommandASync(command);
				}
			} catch (Exception e) {					
				SmartPlanPlugIn.displayLog(Messages.MapPlanEditorPage_SubPlanError + e.getLocalizedMessage(), e);
				
			}
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * Job for adding plan spatial target layer and
	 * zooming the map.  This job waits until
	 * the load default layers job is finished to add
	 * the plan target layer to the map.
	 */
	private Job addLayerJob = new Job(Messages.MapPlanEditorPage_PlanJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			planTargetService = new PlanTargetService(parentEditor.getPlan(), false);
			
	    	try {
	    		loadDefaultLayers.join();
	    		
	    		List<IGeoResource> layers = (List<IGeoResource>) planTargetService.resources(monitor);
	    		AddLayersCommand command = new AddLayersCommand(layers, getMap().getLayersInternal().size());
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
				
			} catch (Exception e) {
				return new Status(IStatus.ERROR, 
						Messages.MapPlanEditorPage_UnknownError, 
						IStatus.ERROR, 
						Messages.MapPlanEditorPage_ErrorInitializePlanTargets, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /*
     * Job to refresh the plan target service and map layer 
     */
    private Job refreshJob = new Job(Messages.MapPlanEditorPage_RefreshMapJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (planTargetService != null){
				try {
					planTargetService.refresh(parentEditor.getPlan(), null);
				} catch (IOException e) {
					SmartPlanPlugIn.log(Messages.MapPlanEditorPage_RefreshError, e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
        
    /**
     * Creates a new map page
     * @param editor
     */
	public MapPlanEditorPage(PlanEditor editor){
		super();
		this.parentEditor = editor;
		
	}
	
	/**
	 * Updates the sub plan targets.
	 * 
	 * @param loadedPlan plan with all subplans loaded from database
	 */
	public void updateSubplanTargetLayer(Plan loadedPlan){
		this.subPlanLayer = loadedPlan;
		refreshSubPlanLayer.schedule();
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	/**
	 * refresh the map and track layers
	 */
	public void refreshPlanTargets(){
		refreshJob.cancel();
		refreshJob.schedule();
	}

	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();	
		
		addLayerJob.schedule();
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

    

    @Override
    public void dispose() {
        super.dispose();
        if (loadDefaultLayers != null){
        	loadDefaultLayers.cancel();
        	loadDefaultLayers = null;
        }
        addLayerJob.cancel();
        addLayerJob = null;
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(planTargetService);
        planTargetService.dispose(null);
        planTargetService = null;
        CatalogPlugin.getDefault().getLocalCatalog().remove(subPlanTargetService);
        subPlanTargetService.dispose(null);
        subPlanTargetService = null;
        
        refreshJob.cancel();
        refreshJob = null;
    }


}
