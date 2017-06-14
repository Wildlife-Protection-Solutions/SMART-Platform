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

import java.awt.Point;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.udig.MissionService;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

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
		
		List<String> tools = new ArrayList<String>();
		for (String tool : MapToolComposite.DEFAULT_MAP_TOOLS){
			tools.add(tool);
		}
		if (SurveyPermissionManager.INSTANCE.canEditWaypointLocations() == null){
			tools.add(MapToolComposite.SEPERATOR_TOOL_ID);
			tools.add(EditPointTool.ID);
			tools.add(UndoTool.ID);
			tools.add(MapToolComposite.SEPERATOR_TOOL_ID);
			
		}
		this.mapTools = tools.toArray(new String[tools.size()]);
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
		
		CatalogPlugin.getDefault().getLocalCatalog().remove(missionService);
		missionService.dispose(null);
		missionService = null;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
        addLayers();
        
        if (SurveyPermissionManager.INSTANCE.canEditWaypointLocations() == null){
        	getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, getEditManager());
        	tools.getTool(UndoTool.ID).setEnabled(false);
        }
	}

	private void addLayers() {
		addLayerJob.schedule();
		
		if (loadDefaultLayers != null) {
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap());
		loadDefaultLayers.schedule();
	}
	
	
	private IMapEditManager getEditManager(){
    	return new IMapEditManager() {
			
			@Override
			public synchronized void moveFeature(Object feature, int x, int y, IViewportModel vm) {
				if (!(feature instanceof SurveyWaypoint)) return ;
				
				SurveyWaypoint pw = (SurveyWaypoint) feature;
				Coordinate crspx = vm.pixelToWorld(x, y);
				//convert to lat/long
				if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
					try{
						crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					}catch (Exception ex){
						EcologicalRecordsPlugIn.displayLog(Messages.MissionMapPage_MoveReprojectError + ex.getMessage(), ex);
						return;
					}
				}
				
				double origx = pw.getWaypoint().getX();
				double origy = pw.getWaypoint().getY();
				
				boolean modified = false;
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					pw.getWaypoint().setX(crspx.x);
					pw.getWaypoint().setY(crspx.y);
					s.update(pw.getWaypoint());
					s.getTransaction().commit();
					modified = true;
				}catch (Exception ex){
					try{
						if (s.getTransaction().isActive()) s.getTransaction().rollback();
					}catch (Exception ex2){
						EcologicalRecordsPlugIn.displayLog(Messages.MissionMapPage_MoveDbError + ex.getMessage(), ex);
						return;
					}
					pw.getWaypoint().setX(origx);
					pw.getWaypoint().setY(origy);
				}finally{
					s.close();
				}
				if (modified){
					addUndo(pw, origx, origy);
					SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, pw.getMissionDay().getMission());
					WaypointEventManager.getInstance().waypointModified(pw.getWaypoint());
				}
			}
			
			@Override
			public EditPoint findFeature(int x, int y, IViewportModel vm) {
				try{
					Coordinate crspx = vm.pixelToWorld(x, y);
					//convert to lat/long
					if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
						crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					}
					double max = Double.MAX_VALUE;
					SurveyWaypoint toEdit = null;
					for(MissionDay md : parentEditor.getMission().getMissionDays()){
						for (SurveyWaypoint pw : md.getWaypoints()){
							double distance = crspx.distance(new Coordinate(pw.getWaypoint().getX(), pw.getWaypoint().getY()));
							if (distance < max){
								max = distance;
								toEdit = pw;
							}
						}
					}
					Coordinate pnt = ReprojectUtils.reproject(toEdit.getWaypoint().getX(), toEdit.getWaypoint().getY(), SmartDB.DATABASE_CRS, vm.getCRS());					
					Point exitPnt = vm.worldToPixel(pnt);
					if (Math.abs(exitPnt.getX() - x) > 5 || Math.abs(exitPnt.getY() - y) > 5) return null;
					
					StringBuilder sb = new StringBuilder();
					sb.append(MessageFormat.format(Messages.MissionMapPage_WaypointLbl, toEdit.getWaypoint().getId()));
					sb.append("\n"); //$NON-NLS-1$
					sb.append(DateFormat.getDateTimeInstance().format(toEdit.getWaypoint().getDateTime()));
					
					return new EditPoint(exitPnt, toEdit, sb.toString());
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
					return null;
				}
			}
			
			private List<Object> undoCommands = new ArrayList<>();
			
			private void addUndo(SurveyWaypoint wp, double x, double y){
				undoCommands.add(0, new Object[]{wp, x, y});
				if (undoCommands.size() > 100){
					undoCommands.remove(undoCommands.size() - 1);
				}
				updateToolbar();
			}
			
			
			@Override
			public synchronized void undo() {
				if (undoCommands.isEmpty()) return;
				
				Session s = HibernateManager.openSession();
				try{
					Object c = undoCommands.remove(0);
					s.beginTransaction();
					Object[] data = (Object[])c;
					
					SurveyWaypoint pw = (SurveyWaypoint) data[0];
					double x = (double) data[1];
					double y = (double) data[2];
					
					pw.getWaypoint().setX(x);
					pw.getWaypoint().setY(y);
					s.update(pw.getWaypoint());
					s.getTransaction().commit();
					
					Display.getDefault().syncExec(()->{
						SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, pw.getMissionDay().getMission());
						WaypointEventManager.getInstance().waypointModified(pw.getWaypoint());
					});
				}catch (Exception ex){
					if (s.getTransaction().isActive()) s.getTransaction().rollback();
					EcologicalRecordsPlugIn.displayLog(Messages.MissionMapPage_UndoError + ex.getMessage(), ex);	
				}finally{
					s.close();
				}
				updateToolbar();
			}
			
			private void updateToolbar(){
				Display.getDefault().syncExec(()->{
					ToolItem ti = tools.getTool(UndoTool.ID);
					if (ti != null) ti.setEnabled(!undoCommands.isEmpty());
				});
			}
			
			@Override
			public boolean canUndo() {
				return !undoCommands.isEmpty();
			}
		};
    }
}
