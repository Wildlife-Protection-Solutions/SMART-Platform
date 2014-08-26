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
package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

public class MissionMapPage extends SmartMapEditorPart {

	private MissionEditor parentEditor;

	private LoadDefaultLayersJob loadDefaultLayers;
	
	private Job addLayerJob = new Job("Add Layers Job") {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
//			patrolService = new PatrolService(parentEditor.getPatrol());
//	    	try {
//	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
//	    		
//	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
//	    		getMap().sendCommandASync(command);
//    		
//	    		final IViewportModelListener initListener = new IViewportModelListener() {
//					@Override
//					public void changed(ViewportModelEvent event) {
//						if (getMap() != null){
//							getMap().getViewportModel().removeViewportModelListener(initListener);
//							getMap().sendCommandASync(new ZoomExtentCommand());
//						}
//						
//					}
//				};
//	    		getMap().getViewportModel().addViewportModelListener(initListener);
//				
//			} catch (IOException e) {
//				return new Status(IStatus.ERROR, Messages.PatrolMapPageEditor_UnknownError, IStatus.ERROR, Messages.PatrolMapPageEditor_Error_LoadingMapPage, e);
//			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job("Refresh Mission Layers"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
//			if (patrolService != null){
//				try {
//					patrolService.refresh(parentEditor.getPatrol(), null);
//				} catch (IOException e) {
//					SmartPatrolPlugIn.log(Messages.PatrolMapPageEditor_Error_RefreshingLayers, e);
//				}
//			}
//			//clear selection
//			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
	
	private ISurveyEventListener missionUpdatedListeners = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			refreshJob.cancel();
			refreshJob.schedule();
		}
	};
	
	public MissionMapPage(MissionEditor parent) {
		this.parentEditor = parent;
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
        addLayers();
        SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, missionUpdatedListeners );
	}

	private void addLayers(){
		addLayerJob.schedule();
		
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();
	}
	
}
