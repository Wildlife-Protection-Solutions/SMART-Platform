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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.udig.catalog.PatrolService;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
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
		
	private Job addLayerJob = new Job(Messages.PatrolMapPageEditor_AddLayersJobName) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			patrolService = new PatrolService(parentEditor.getPatrol());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
	    		
	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
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
				p = (Patrol) p;
			}else if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}
			if (p != null && p.equals(parentEditor.getPatrol()) && (
					attributeChanged == PatrolEventManager.PATROL_DATES_LEG ||
					attributeChanged == PatrolEventManager.PATROL_TRACKS ||
					attributeChanged == PatrolEventManager.PATROL_WAYPOINTS)){
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
		addLayerJob.schedule();
		
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap());
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
	}

    

    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
    	loadDefaultLayers = null;
        refreshJob = null;

        super.dispose();
        
        PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(patrolService);
        patrolService.dispose(null);
        patrolService = null;
    }


    private IMapEditManager getEditManager(){
    	return new IMapEditManager() {
			
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
						SmartPatrolPlugIn.displayLog(Messages.PatrolMapPageEditor_MoveErrorDb + ex.getMessage(), ex);
						return;
					}
					pw.getWaypoint().setX(origx);
					pw.getWaypoint().setY(origy);
				}finally{
					s.close();
				}
				if (modified){
					addUndo(pw, origx, origy);
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
					sb.append(DateFormat.getDateTimeInstance().format(toEdit.getWaypoint().getDateTime()));
					
					return new EditPoint(exitPnt, toEdit, sb.toString());
					
				}catch (Exception ex){
					SmartPatrolPlugIn.log(ex.getMessage(), ex);
					return null;
				}
			}
			
			private List<Object> undoCommands = new ArrayList<>();
			
			private void addUndo(PatrolWaypoint wp, double x, double y){
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
					
					PatrolWaypoint pw = (PatrolWaypoint) data[0];
					double x = (double) data[1];
					double y = (double) data[2];
					
					pw.getWaypoint().setX(x);
					pw.getWaypoint().setY(y);
					s.update(pw.getWaypoint());
					s.getTransaction().commit();
					
					Display.getDefault().syncExec(()->{
						PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, pw.getPatrolLegDay());
						WaypointEventManager.getInstance().waypointModified(pw.getWaypoint());
					});
				}catch (Exception ex){
					if (s.getTransaction().isActive()) s.getTransaction().rollback();
					SmartPatrolPlugIn.displayLog(Messages.PatrolMapPageEditor_UndoError + ex.getMessage(), ex);	
				}finally{
					s.close();
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
}

