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
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.map.style.MissionMapSamplingUnitLinearDefaultStyle;
import org.wcs.smart.er.map.style.MissionMapSamplingUnitPointDefaultStyle;
import org.wcs.smart.er.map.style.MissionMapTrackDefaultStyle;
import org.wcs.smart.er.map.style.MissionMapWaypointDefaultStyle;
import org.wcs.smart.er.map.style.MissionMapWaypointRawDefaultStyle;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.udig.MissionDataSource;
import org.wcs.smart.er.ui.mision.udig.MissionFeatureSource;
import org.wcs.smart.er.ui.mision.udig.MissionGeoResource;
import org.wcs.smart.er.ui.mision.udig.MissionService;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.ui.WaypointInfoShellProvider;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.map.tool.IInfoToolShellProvider;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.ReprojectUtils;

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
				
				List<IGeoResource> sortedLayers = new ArrayList<>();
				List<? extends IGeoResource> layers = missionService.resources(monitor);
	    		for (IGeoResource l : layers) if (((MissionGeoResource)l).getType().equals(MissionDataSource.MISSIONTRACK_TYPE)) sortedLayers.add(l);
	    		for (IGeoResource l : layers) if (((MissionGeoResource)l).getType().equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)) sortedLayers.add(l);
	    		for (IGeoResource l : layers) if (((MissionGeoResource)l).getType().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) sortedLayers.add(l);
	    		
				allLayers.addAll(sortedLayers);
				
	    		AddLayersCommand command = new AddLayersCommand(allLayers, 0) {
	    			public void run( IProgressMonitor monitor ) throws Exception {
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).disableRendering();
	    				try {
		    				super.run(monitor);
		    				

		    				Map<String,String> geoIdToStyle = new HashMap<>();
		    				geoIdToStyle.put(MissionDataSource.MISSIONTRACK_TYPE,  MissionMapTrackDefaultStyle.KEY);
		    				geoIdToStyle.put(MissionDataSource.MISSIONRAWWAYPOINT_TYPE,  MissionMapWaypointRawDefaultStyle.KEY);
		    				geoIdToStyle.put(MissionDataSource.MISSIONWAYPOINT_TYPE,  MissionMapWaypointDefaultStyle.KEY);
		    				geoIdToStyle.put(SamplingUnit.GeometryType.TRANSECT.name(),  MissionMapSamplingUnitLinearDefaultStyle.KEY);
		    				geoIdToStyle.put(SamplingUnit.GeometryType.PLOT.name(),  MissionMapSamplingUnitPointDefaultStyle.KEY);

		    				for (Layer l : getLayers()) {
		    					
		    					StyleManager.INSTANCE.applyDefaultStyleToMapLayer(l, geoIdToStyle, monitor);
		    				
		    					MissionFeatureSource fs = l.getGeoResource().resolve(MissionFeatureSource.class, monitor);
		    					if (fs == null) continue;
		    					
		    					l.setName(fs.getLayerName());
		    					l.setVisible(fs.getDefaultVisibility());
		    					l.eNotify(new ENotificationImpl(
		    							(InternalEObject) l, Notification.SET,
		    							ProjectPackage.LAYER__VISIBLE, false, l.isVisible()));	
		    					
		    				}
	    				}finally {
	    					((RenderManagerImpl)getMap().getRenderManagerInternal()).enableRendering();
	    				}
	    				getMap().getRenderManager().refresh(null);
	    			}
	    		};
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
        
        getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getMapInfoProvider());
        getMap().getBlackboard().put(IInfoToolShellProvider.BLACKBOARD_KEY, getInfoShellProvider());
        
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
			
    		private boolean showWarning = true;
    		@Override
    		public void activate() {
    			if (parentEditor.getMission().getSurvey().getSurveyDesign().getTrackDistanceDirection() && showWarning) {
    				//down warning 
    				showWarning = false;
    				MessageDialog.openWarning(getSite().getShell(), Messages.MissionMapPage_EditMsgTitle, Messages.MissionMapPage_EditMsg);
    			}
    		}
    		
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
				
				double origx = pw.getWaypoint().getRawX();
				double origy = pw.getWaypoint().getRawY();
				Float origdistance = pw.getWaypoint().getDistance();
				Float origdirection = pw.getWaypoint().getDirection();
				
				double newx = origx;
				double newy = origy;
				Float newdistance = pw.getWaypoint().getDistance();
				Float newdirection = pw.getWaypoint().getDirection();
				if (pw.getWaypoint().getDirection() != null && pw.getWaypoint().getDistance() != null) {
					//change the distance and direction not the coordinate
					//but warn the user somehow
					Float[] d = Waypoint.computeDistanceBearing(new Coordinate(origx, origy), crspx);
					newdistance = d[0];
					newdirection = d[1];
				}else {
					newx = crspx.x;
					newy = crspx.y;
				}
				
				boolean modified = false;
				try(Session s = HibernateManager.openSession()){
					try{
						s.beginTransaction();
						pw.getWaypoint().setRawX(newx);
						pw.getWaypoint().setRawY(newy);
						pw.getWaypoint().setDirection(newdirection);
						pw.getWaypoint().setDistance(newdistance);
						s.merge(pw.getWaypoint());
						s.getTransaction().commit();
						modified = true;
					}catch (Exception ex){
						try{
							if (s.getTransaction().isActive()) s.getTransaction().rollback();
						}catch (Exception ex2){
							EcologicalRecordsPlugIn.displayLog(Messages.MissionMapPage_MoveDbError + ex.getMessage(), ex);
							return;
						}
						pw.getWaypoint().setRawX(origx);
						pw.getWaypoint().setRawY(origy);
						pw.getWaypoint().setDirection(origdirection);
						pw.getWaypoint().setDistance(origdistance);
					}
				}
				if (modified){
					addUndo(pw, origx, origy, origdirection, origdistance);
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
					sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(toEdit.getWaypoint().getDateTime()));
					
					return new EditPoint(exitPnt, toEdit, sb.toString());
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
					return null;
				}
			}
			
			private List<Object> undoCommands = new ArrayList<>();
			
			private void addUndo(SurveyWaypoint wp, double x, double y, Float direction, Float distance){
				undoCommands.add(0, new Object[]{wp, x, y, distance, direction});
				if (undoCommands.size() > 100){
					undoCommands.remove(undoCommands.size() - 1);
				}
				updateToolbar();
			}
			
			
			@Override
			public synchronized void undo() {
				if (undoCommands.isEmpty()) return;
				
				try(Session s = HibernateManager.openSession()){
					Object c = undoCommands.remove(0);
					s.beginTransaction();
					try {
						Object[] data = (Object[])c;
						
						SurveyWaypoint pw = (SurveyWaypoint) data[0];
						double x = (double) data[1];
						double y = (double) data[2];
						Float distance = (Float)data[3];
						Float direction = (Float)data[4];
						
						pw.getWaypoint().setRawX(x);
						pw.getWaypoint().setRawY(y);
						pw.getWaypoint().setDirection(direction);
						pw.getWaypoint().setDistance(distance);
						s.merge(pw.getWaypoint());
						s.getTransaction().commit();
						
						Display.getDefault().syncExec(()->{
							SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, pw.getMissionDay().getMission());
							WaypointEventManager.getInstance().waypointModified(pw.getWaypoint());
						});
					}catch (Exception ex){
						if (s.getTransaction().isActive()) s.getTransaction().rollback();
						EcologicalRecordsPlugIn.displayLog(Messages.MissionMapPage_UndoError + ex.getMessage(), ex);
					}
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
	
	 private IInfoToolShellProvider getInfoShellProvider() {
	    	return new WaypointInfoShellProvider(getSite().getShell(), super.mapViewer.getControl());
	    }
	    
	    private IInfoToolProvider getMapInfoProvider(){
			return new IInfoToolProvider(){
				@Override
				public InfoPoint findFeature(int x, int y, IViewportModel vm) {
					try{
						int xll = x - 5;
						int yll = y - 5;
						int xur = x + 5;
						int yur = y + 5;
						
						Coordinate worldll = vm.pixelToWorld(xll, yll);
						Coordinate worldur = vm.pixelToWorld(xur, yur);
						
						Coordinate dbll = ReprojectUtils.reproject(worldll.x, worldll.y, vm.getCRS(), SmartDB.DATABASE_CRS);
						Coordinate dbur = ReprojectUtils.reproject(worldur.x, worldur.y, vm.getCRS(), SmartDB.DATABASE_CRS);
						
						Envelope env = new Envelope(dbll,  dbur);

						//find all waypoints in bounding box
						List<Waypoint> waypoints = new ArrayList<>();
						for(MissionDay md : parentEditor.getMission().getMissionDays()) {
							for (SurveyWaypoint pw : md.getWaypoints()) {
								if (env.contains(pw.getWaypoint().getX(), pw.getWaypoint().getY())) {
									waypoints.add(pw.getWaypoint());
								}
							}
						}
						
						if (waypoints.isEmpty()) return null;
						Coordinate px = ReprojectUtils.reproject(waypoints.get(0).getX(), waypoints.get(0).getY(), SmartDB.DATABASE_CRS, vm.getCRS());
						return new InfoPoint(vm.worldToPixel(px), waypoints, null);	
					}catch (Exception ex) {
						EcologicalRecordsPlugIn.log(ex.getMessage(), ex);					
					}
					return null;
				}
				
			};	
		}
}
