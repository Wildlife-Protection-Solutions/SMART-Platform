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
package org.wcs.smart.patrol.internal.ui.editor;

import java.awt.Point;
import java.io.IOException;
import java.text.Collator;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;
import org.wcs.smart.observation.ui.WaypointInfoShellProvider;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.geotools.PatrolDataSource;
import org.wcs.smart.patrol.geotools.PatrolFeatureSource;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.map.style.PatrolMapTrackDefaultStyle;
import org.wcs.smart.patrol.map.style.PatrolMapWaypointDefaultStyle;
import org.wcs.smart.patrol.map.style.PatrolMapWaypointRawDefaultStyle;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.udig.catalog.PatrolGeoResource;
import org.wcs.smart.patrol.udig.catalog.PatrolService;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
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
 * Page for the editor for displaying a map
 * of the waypoints and tracks.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolMapPageEditor extends SmartMapEditorPart {
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolMapEditor"; //$NON-NLS-1$
	
	private PatrolEditor parentEditor; 
	
	private PatrolService patrolService = null;
	private LoadDefaultLayersJob loadDefaultLayers;
	
	private List<IGeoResource> dmAttributeResources = new ArrayList<>();
	
	private Job addLayerJob = new Job(Messages.PatrolMapPageEditor_AddLayersJobName) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			patrolService = new PatrolService(parentEditor.getPatrol());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
	    		
	    		List<IGeoResource> sortedLayers = new ArrayList<>();
	    		Map<String, IGeoResource> toAdd = new HashMap<>();
	    		
	    		for (IGeoResource l : layers) {
	    			String typeName = ((PatrolGeoResource)l).getType();
	    			
	    			if (PatrolDataSource.isGeometryAttribute(typeName)) {
	    				dmAttributeResources.add(l);
	    			}
	    			toAdd.put( ((PatrolGeoResource)l).getType(), l );
	    		}
	    			
	    		String[] orderedLayers = new String[] {
	    				PatrolDataSource.TRACK_PART_TYPE, 
	    				PatrolDataSource.WAYPOINT_PRJ_TYPE, 
	    				PatrolDataSource.WAYPOINT_TYPE,
	    		};
	    		for (String name : orderedLayers) {
	    			sortedLayers.add(toAdd.get(name));
	    			toAdd.remove(name);
	    		}
	    		List<IGeoResource> othersorted = new ArrayList<>();
	    		othersorted.addAll(toAdd.values());
	    		othersorted.sort((a,b)->-Collator.getInstance().compare(a.getTitle(), b.getTitle()));
	    		sortedLayers.addAll(0,othersorted);
	    		
	    		
	    		AddLayersCommand command = new AddLayersCommand(sortedLayers, getMap().getLayersInternal().size()) {
	    			public void run( IProgressMonitor monitor ) throws Exception {
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).disableRendering();
	    				
	    				super.run(monitor);
	    				
	    				Map<String,String> geoIdToStyle = new HashMap<>();
	    				geoIdToStyle.put(PatrolDataSource.TRACK_PART_TYPE,  PatrolMapTrackDefaultStyle.KEY);
	    				geoIdToStyle.put(PatrolDataSource.WAYPOINT_PRJ_TYPE,  PatrolMapWaypointRawDefaultStyle.KEY);
	    				geoIdToStyle.put(PatrolDataSource.WAYPOINT_TYPE,  PatrolMapWaypointDefaultStyle.KEY);
	    				
	    				try(Session session = HibernateManager.openSession()){
		    				for (Layer l : getLayers()) {
		    					
		    					StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(),  l, geoIdToStyle, session, monitor);
		    					
		    					PatrolFeatureSource fs = l.getGeoResource().resolve(PatrolFeatureSource.class, monitor);
		    					if (fs != null) {
		    						l.setVisible(fs.getDefaultVisibility());
		    						l.eNotify(new ENotificationImpl(
		    								(InternalEObject) l, Notification.SET,
		    								ProjectPackage.LAYER__VISIBLE, false, l.isVisible()));	
		    					}
		    				}
	    				}
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).enableRendering();
	    				getMap().getRenderManager().refresh(null);
	    			}
					
	    		};
	    		getMap().sendCommandASync(command);
    		
	    		addInitialZoomFunction();
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Messages.PatrolMapPageEditor_UnknownError, IStatus.ERROR, Messages.PatrolMapPageEditor_Error_LoadingMapPage, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.PatrolMapPageEditor_RefreshPatrolLayers_Job){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (patrolService != null){
				try {
					patrolService.refresh(parentEditor.getPatrol(), null);
				} catch (IOException e) {
					SmartPatrolPlugIn.log(Messages.PatrolMapPageEditor_Error_RefreshingLayers, e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
    
    /** 
     * Listener for patrol events
     * 
     */
    private IPatrolEventListener patrolUpdatedListeners = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			Patrol p = null;
			if (source instanceof Patrol){
				p = (Patrol) source;
			}else if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}
			if (p != null && p.getUuid().equals(parentEditor.getPatrolUuid()) && (
					attributeChanged == PatrolEventManager.PATROL_DATES_LEG ||
					attributeChanged == PatrolEventManager.PATROL_TRACKS ||
					attributeChanged == PatrolEventManager.PATROL_WAYPOINTS)){
//				parentEditor.clearPatrol();
				refresh();
			}
		}
	};
	    
	public PatrolMapPageEditor(PatrolEditor parent){
		this.parentEditor = parent;
		
		List<String> tools = new ArrayList<String>();
		for (String tool : MapToolComposite.DEFAULT_MAP_TOOLS){
			tools.add(tool);
		}
		if (PatrolManager.getInstance().canEditWaypointLocations() == null){
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

	/**
	 * refresh the map and track layers
	 */
	public void refresh() {
    	if (refreshJob != null) {
        	refreshJob.cancel();
        	refreshJob.schedule();
    	}
	}

	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap()) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus r = super.run(monitor);
				if (addLayerJob != null) addLayerJob.schedule();
				return r;
			}
		};
		loadDefaultLayers.schedule();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof PatrolEditorInput)){
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
        addLayers();
        PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        
        if (PatrolManager.getInstance().canEditWaypointLocations() == null){
        	getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, getEditManager());
        	enableTool(UndoTool.ID, false);
        }
        
        getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getMapInfoProvider());
        getMap().getBlackboard().put(IInfoToolShellProvider.BLACKBOARD_KEY, getInfoShellProvider());
	}

    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
    	this.loadDefaultLayers = null;
        this.refreshJob = null;
        this.addLayerJob = null;
        super.dispose();
        
        PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(patrolService);
        patrolService.dispose(null);
        patrolService = null;
        parentEditor = null;
        patrolUpdatedListeners = null;
    }


    private IMapEditManager getEditManager(){
    	return new IMapEditManager() {
			
    		private boolean showWarning = true;
    		@Override
    		public void activate() {
    			if (parentEditor.getOptions().getTrackDistanceDirection() && showWarning) {
    				//down warning 
    				showWarning = false;
    				MessageDialog.openWarning(getSite().getShell(), Messages.PatrolMapPageEditor_EditTitle, Messages.PatrolMapPageEditor_EditWarningMessage);
    			}
    		}
    		
			@Override
			public synchronized void moveFeature(Object feature, int x, int y, IViewportModel vm) {
				if (!(feature instanceof PatrolWaypoint)) return ;
				
				PatrolWaypoint pw = (PatrolWaypoint) feature;
				Coordinate crspx = vm.pixelToWorld(x, y);
				//convert to lat/long
				if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
					try{
						crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					}catch (Exception ex){
						SmartPatrolPlugIn.displayLog(Messages.PatrolMapPageEditor_MoveErrorReproject + ex.getMessage(), ex);
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
							SmartPatrolPlugIn.displayLog(Messages.PatrolMapPageEditor_MoveErrorDb + ex.getMessage(), ex2);
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
					PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, pw.getPatrolLegDay());
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
					PatrolWaypoint toEdit = null;
					for(PatrolLeg pl : parentEditor.getPatrol().getLegs()){
						for (PatrolLegDay pld : pl.getPatrolLegDays()){
							for (PatrolWaypoint pw : pld.getWaypoints()){
								double distance = crspx.distance(new Coordinate(pw.getWaypoint().getX(), pw.getWaypoint().getY()));
								if (distance < max){
									max = distance;
									toEdit = pw;
								}
							}
						}
					}
					Coordinate pnt = ReprojectUtils.reproject(toEdit.getWaypoint().getX(), toEdit.getWaypoint().getY(), SmartDB.DATABASE_CRS, vm.getCRS());
					
					Point exitPnt = vm.worldToPixel(pnt);
					
					if (Math.abs(exitPnt.getX() - x) > 5 || Math.abs(exitPnt.getY() - y) > 5) return null;
					
					StringBuilder sb = new StringBuilder();
					sb.append(MessageFormat.format(Messages.PatrolMapPageEditor_WaypointLbl, toEdit.getWaypoint().getId()));
					sb.append("\n"); //$NON-NLS-1$
					sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(toEdit.getWaypoint().getDateTime()));
					
					return new EditPoint(exitPnt, toEdit, sb.toString());
					
				}catch (Exception ex){
					SmartPatrolPlugIn.log(ex.getMessage(), ex);
					return null;
				}
			}
			
			private List<Object> undoCommands = new ArrayList<>();
			
			private void addUndo(PatrolWaypoint wp, double x, double y, Float direction, Float distance){
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
					try {
						s.beginTransaction();
						Object[] data = (Object[])c;
						
						PatrolWaypoint pw = (PatrolWaypoint) data[0];
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
							PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, pw.getPatrolLegDay());
							WaypointEventManager.getInstance().waypointModified(pw.getWaypoint());
						});
					}catch (Exception ex){
						if (s.getTransaction().isActive()) s.getTransaction().rollback();
						SmartPatrolPlugIn.displayLog(Messages.PatrolMapPageEditor_UndoError + ex.getMessage(), ex);	
					}
				}
				updateToolbar();
			}
			
			private void updateToolbar(){
				Display.getDefault().syncExec(()->{
					enableTool(UndoTool.ID, !undoCommands.isEmpty());
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
					for(PatrolLeg pl : parentEditor.getPatrol().getLegs()) {
						for (PatrolLegDay pld : pl.getPatrolLegDays()) {
							for (PatrolWaypoint pw : pld.getWaypoints()) {
								if (env.contains(pw.getWaypoint().getX(), pw.getWaypoint().getY())) {
									waypoints.add(pw.getWaypoint());
								}
							}
						}
					}
					
					if (!waypoints.isEmpty()) {
						Coordinate px = ReprojectUtils.reproject(waypoints.get(0).getX(), waypoints.get(0).getY(), SmartDB.DATABASE_CRS, vm.getCRS());
						return new InfoPoint(vm.worldToPixel(px), waypoints, null);
					}
					
					//search observation attributes
				
					Object[] found = ObservationAttributeFeatureFactory.findWaypointObservationAttributes(env, dmAttributeResources.toArray(new IGeoResource[dmAttributeResources.size()]));
					List<WaypointObservationAttribute> matched = (List<WaypointObservationAttribute>) found[0];
					Coordinate c = (Coordinate) found[1];
					
					if (!matched.isEmpty()) {						
						Coordinate px = ReprojectUtils.reproject(c.x, c.y, SmartDB.DATABASE_CRS, vm.getCRS());
						return new InfoPoint(vm.worldToPixel(px), matched, null);
					}
					
					return null;
				}catch (Exception ex) {
					SmartPatrolPlugIn.log(ex.getMessage(), ex);					
				}
				return null;
			}
			
		};	
	}
}

