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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.ui.mision.udig.MissionService;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;

/**
 * Mission editor map page displaying tracks
 * and sampling units.
 * 
 * @author Emily
 *
 */
public class MissionMapPage extends SmartMapEditorPart {

	private MissionEditor parentEditor;
	
	private MissionService missionService;
	private SamplingUnitService suService;

	private LoadDefaultLayersJob loadDefaultLayers;
	
	private Job addLayerJob = new Job(Messages.MissionMapPage_AddLayersJob_Title) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			/* mission track and waypoint layers */
			missionService = new MissionService(parentEditor.getMission());
			suService = new SamplingUnitService(parentEditor.getMission().getSurvey().getSurveyDesign());
			
			try {
				List<IGeoResource> allLayers = new ArrayList<IGeoResource>();
				List<IGeoResource> tmp = (List<IGeoResource>) suService.resources(monitor);
				for (IGeoResource r : tmp){
					if (r instanceof SamplingUnitGeoResource){
						String type = ((SamplingUnitGeoResource)r).getDataType();
						if (type.equals(SamplingUnit.GeometryType.PLOT.name()) ||
								type.equals(SamplingUnit.GeometryType.TRANSECT.name())){
							allLayers.add(r);
						}
					}		
				}
				
				allLayers.addAll((List<IGeoResource>) missionService.resources(monitor));
				
	    		AddLayersCommand command = new AddLayersCommand(allLayers, 0);
	    		getMap().sendCommandASync(command);
    		
	    		addInitialZoomFunction();
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, "unknown", IStatus.ERROR, Messages.MissionMapPage_AddLayersJob_Error, e); //$NON-NLS-1$
			}
	    	return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.MissionMapPage_RefreshLayersJob_Title) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (missionService != null){
				try {
					missionService.refresh(parentEditor.getMission(), monitor);
				} catch (IOException e) {
					EcologicalRecordsPlugIn.log(Messages.MissionMapPage_RefreshLayersJob_Error, e);
				}
			}
			if (suService != null){
				try{
					suService.refresh(monitor);
				}catch (IOException e){
					EcologicalRecordsPlugIn.log(Messages.MissionMapPage_RefreshLayersJob_Error, e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
	
	
	public MissionMapPage(MissionEditor parent) {
		this.parentEditor = parent;
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}
	
	public void refresh() {
    	if (refreshJob != null) {
        	refreshJob.cancel();
        	refreshJob.schedule();
    	}
	}

	@Override
	public void dispose() {
		JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
		super.dispose();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
        addLayers();
	}

	private void addLayers() {
		addLayerJob.schedule();
		
		if (loadDefaultLayers != null) {
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();
	}
	
}
