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
package org.wcs.smart.incident.ui;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentFeatureFactory;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.map.IncidentMapLineStringAttributeDefaultStyle;
import org.wcs.smart.incident.map.IncidentMapPolygonAttributeDefaultStyle;
import org.wcs.smart.incident.map.IncidentMapWaypointDefaultStyle;
import org.wcs.smart.incident.map.IncidentMapWaypointRawDefaultStyle;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;
import org.wcs.smart.observation.ui.WaypointInfoShellProvider;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.map.tool.IInfoToolShellProvider;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Incident editor map page
 * @author Emily
 *
 */
public class IncidentMapPage extends SmartMapEditorPart {

	private static final String LINESTRING_TYPE_NAME = "LINESTRING"; //$NON-NLS-1$
	private static final String POLYGON_TYPE_NAME = "POLYGON"; //$NON-NLS-1$

	private IncidentEditor parent;

	private SimpleFeatureType featureType;
	private ListFeatureCollection featureCollection;
	
	private FeatureStore<SimpleFeatureType,SimpleFeature> pointStore;
	private Layer pointLayer = null;
	private IGeoResource pointResource;
	
	private SimpleFeatureType prjFeatureType = null;
	private ListFeatureCollection prjFeatureCollection = null;
	private FeatureStore<SimpleFeatureType,SimpleFeature> prjStore = null;
	private Layer prjLayer = null;
	private IGeoResource prjResource = null;
	
	private FeatureStore<SimpleFeatureType,SimpleFeature> attPolygonStore;
	private Layer attPolygonLayer = null;
	private IGeoResource attPolygonResource;
	private SimpleFeatureType attPolygonType;
	
	private FeatureStore<SimpleFeatureType,SimpleFeature> attLinestringStore;
	private Layer attLinestringLayer = null;
	private IGeoResource attLinestringResource;
	private SimpleFeatureType attLinestringType;
	/**
	 * Creates a new map page
	 * @param e parent editor
	 */
	public IncidentMapPage(IncidentEditor e){
		this.parent = e;
		
		List<String> tools = new ArrayList<String>();
		for (String tool : MapToolComposite.DEFAULT_MAP_TOOLS){
			tools.add(tool);
		}
		if (this.parent.canEdit() == null){
			tools.add(MapToolComposite.SEPERATOR_TOOL_ID);
			tools.add(EditPointTool.ID);
			tools.add(UndoTool.ID);
			tools.add(MapToolComposite.SEPERATOR_TOOL_ID);
			
		}
		this.mapTools = tools.toArray(new String[tools.size()]);
		
		
	}
	
	@Override
	public MultiPageEditorPart getParentEditor() {
		return parent;
	}
	
	@Override
	public void dispose() {
		getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, null);
		
		super.dispose();
		
		if (pointResource != null) {
			pointResource.dispose(new NullProgressMonitor());
			
			try {
				IService service = pointResource.service(new NullProgressMonitor());
				if (service != null) CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			} catch (IOException e) {
				IncidentPlugIn.log(e.getMessage(), e);
			}
			
		}
		featureCollection = null;
		pointStore = null;
		this.pointResource = null;
		this.parent = null;
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		LoadDefaultLayersJob loadDefaultLayers = new LoadDefaultLayersJob(getMap()) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus s = super.run(monitor);
				Display.getDefault().asyncExec(()->{
    				((RenderManagerImpl)getMap().getRenderManager()).disableRendering();
    				try {
						addPointsLayer();
						updatePointsLayer();
						updateObservationLayers();
    				}finally {
    					((RenderManagerImpl)getMap().getRenderManager()).enableRendering();
    				}
				});
				return s;
			}
		};
		loadDefaultLayers.schedule();
		
		addInitialZoomFunction();
		
        getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getMapInfoProvider());
        getMap().getBlackboard().put(IInfoToolShellProvider.BLACKBOARD_KEY, getInfoShellProvider());
        
		if (this.parent.canEdit() == null){
        	getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, getEditManager());
        	tools.getTool(UndoTool.ID).setEnabled(false);
        }
	}

	/**
	 * Creates the incident layer
	 */
	@SuppressWarnings("unchecked")
	private void addPointsLayer() {
        try {
			List<IGeoResource> layers = new ArrayList<IGeoResource>();

			//normal point layer
			featureType = IncidentFeatureFactory.createSimpleIncidentSchema(IncidentFeatureFactory.SMART_POINT_TYPE_NAME);
			featureCollection = new ListFeatureCollection(featureType);
			pointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			pointStore = pointResource.resolve(FeatureStore.class, null);
			layers.add(pointResource);
			
			if (parent.getOptions().getTrackDistanceDirection()) {
				//projected point layer
				prjFeatureType = IncidentFeatureFactory.createSimpleIncidentSchema(IncidentFeatureFactory.SMART_POINT_PRJ_TYPE_NAME);
				prjFeatureCollection = new ListFeatureCollection(prjFeatureType);
				prjResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(prjFeatureType);
				prjStore = prjResource.resolve(FeatureStore.class, null);
				layers.add(0, prjResource);
			}
		
			attPolygonType = ObservationAttributeFeatureFactory.createObservationPolygonSchema(POLYGON_TYPE_NAME);
			attPolygonResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(attPolygonType);
			attPolygonStore = attPolygonResource.resolve(FeatureStore.class, null);
			layers.add(0, attPolygonResource);
			
			attLinestringType = ObservationAttributeFeatureFactory.createObservationLineStringSchema(LINESTRING_TYPE_NAME);
			attLinestringResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(attLinestringType);
			attLinestringStore = attLinestringResource.resolve(FeatureStore.class, null);
			layers.add(0, attLinestringResource);
			
			//dispose of temporary layer when composite is disposed
			super.mapViewer.getControl().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (pointLayer != null){
							CatalogPlugin.getDefault().getLocalCatalog().remove(pointLayer.getGeoResource().service(null));
						}
						if (prjLayer != null) {
							CatalogPlugin.getDefault().getLocalCatalog().remove(prjLayer.getGeoResource().service(null));
						}
					}catch (Exception ex){
						IncidentPlugIn.log("Error removing incident service", ex); //$NON-NLS-1$
					}
					
				}
			});
	      ;
			
			AddLayersCommand command = new AddLayersCommand(layers, getMap().getLayersInternal().size()) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					Map<String,String> geoIdToStyle = new HashMap<>();
    				geoIdToStyle.put(IncidentFeatureFactory.SMART_INCIDENT_TYEPNAME,  IncidentMapWaypointDefaultStyle.KEY);
    				geoIdToStyle.put(IncidentFeatureFactory.SMART_INCIDENT_PRJ_TYEPNAME,  IncidentMapWaypointRawDefaultStyle.KEY);
    				geoIdToStyle.put(POLYGON_TYPE_NAME,  IncidentMapPolygonAttributeDefaultStyle.KEY);
    				geoIdToStyle.put(LINESTRING_TYPE_NAME,  IncidentMapLineStringAttributeDefaultStyle.KEY);

    				Map<String, Consumer<Layer>> defaultStyles = new HashMap<>();
    				defaultStyles.put(IncidentMapWaypointDefaultStyle.KEY, (l)->{
    					l.getStyleBlackboard().put(SLDContent.ID, getStylingConfig());
   					});
    				defaultStyles.put(IncidentMapWaypointRawDefaultStyle.KEY, (l)->{
    					l.getStyleBlackboard().put(SLDContent.ID, getPrjStylingConfig());
   					});

    				for (Layer l : getLayers()) {
    					FeatureType type = l.getGeoResource().resolve(FeatureType.class, null);
    					if (type.getName().equals(featureType.getName())) {
    						pointLayer = l;
    						pointLayer.setName(Messages.IncidentMapPage_MapLayerName);
    						pointLayer.setVisible(true);
    					}else if (prjFeatureType != null && type.getName().equals(prjFeatureType.getName())) {
    						prjLayer = l;
    					}else if (type.getName().equals(attPolygonType.getName())) {
    						attPolygonLayer = l;
    					}else if (type.getName().equals(attLinestringType.getName())) {
    						attLinestringLayer = l;
    					}
    				}
    				
					

					try(Session session = HibernateManager.openSession()){
	   					StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(), pointLayer, geoIdToStyle, defaultStyles, session, monitor);
						
						if (prjLayer != null) {
							prjLayer.setName(Messages.IncidentMapPage_RawPointLayerName);
							prjLayer.setVisible(false);
							StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(), prjLayer, geoIdToStyle, defaultStyles, session, monitor);
						}
						if (attPolygonLayer != null) {
							attPolygonLayer.setName(SmartLabelProvider.POLYGON_ATTRIBUTE_LAYER);
							StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(), attPolygonLayer, geoIdToStyle, defaultStyles, session, monitor);
						}
						if (attLinestringLayer != null) {
							attLinestringLayer.setName(SmartLabelProvider.LINESTRING_ATTRIBUTE_LAYER);
							StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(), attLinestringLayer, geoIdToStyle, defaultStyles, session, monitor);
						}
					}						
					pointLayer.eNotify(new ENotificationImpl(
							(InternalEObject) pointLayer, Notification.SET,
							ProjectPackage.LAYER__VISIBLE, false, true));
				}
			};
			getMap().sendCommandSync(command);
			
        } catch (Exception exception) {
			IncidentPlugIn.displayLog(Messages.IncidentMapPage_Error1, exception);
		}
		
	}
	
	public void updateObservationLayers() {
		if (this.attLinestringLayer == null || this.attPolygonLayer == null) return;
		List<SimpleFeature> polys = new ArrayList<>();
		List<SimpleFeature> lines = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			Waypoint wp = session.get(Waypoint.class, parent.getIncident().getUuid());
			for (WaypointObservation wo : wp.getAllObservations()) {
				for (WaypointObservationAttribute a : wo.getAttributes()) {
					if (a.getGeom() == null) continue;
					if (!a.getAttribute().getType().isGeometry()) continue;
					
					boolean hasArea = a.getAttribute().getType() == Attribute.AttributeType.POLYGON;
					SimpleFeatureType type = a.getAttribute().getType() == AttributeType.POLYGON ? attPolygonType : attLinestringType;
					SimpleFeature sf = ObservationAttributeFeatureFactory.getObservationAttributeAsGeometry(type, hasArea, a);
					if (hasArea) {
						polys.add(sf);
					}else {
						lines.add(sf);
					}
				}
			}
		}
		doTwice(()->{
			try {
				attLinestringStore.removeFeatures(Filter.INCLUDE);
				attLinestringStore.addFeatures(new ListFeatureCollection(attLinestringType, lines));
			}catch (IOException ex) {
				IncidentPlugIn.log(ex.getMessage(), ex);
			}
		});
		
		doTwice(()->{
			try {
				attPolygonStore.removeFeatures(Filter.INCLUDE);
				attPolygonStore.addFeatures(new ListFeatureCollection(attPolygonType, polys));
			}catch (IOException ex) {
				IncidentPlugIn.log(ex.getMessage(), ex);
			}
		});
		
		if (attLinestringLayer != null) attLinestringLayer.refresh(null);
		if (attPolygonLayer != null) attPolygonLayer.refresh(null);
	}

	private void doTwice(Runnable r) {
		try {
			r.run();
		}catch (ConcurrentModificationException ex) {
			r.run();
			//try again - this should only happen once (udig removes listener)
			//see SMART bug 1672
		}
	}
	
	/**
	 * Updates the incident layer
	 */
	public void updatePointsLayer() {
		if (pointStore == null) {
			return; //most likely we failed to add points layer
		}

		featureCollection.clear();
		featureCollection.add(IncidentFeatureFactory.createSimpleIncidentFeature(featureType, parent.getIncident()));
			
		doTwice(()->{
			try{
				pointStore.removeFeatures(Filter.INCLUDE);
				pointStore.addFeatures(featureCollection);
			}catch (IOException ex){
				IncidentPlugIn.log(ex.getMessage(), ex);
			}
		});
			
			
		if (prjFeatureCollection != null) {
			prjFeatureCollection.clear();
			prjFeatureCollection.add(IncidentFeatureFactory.createSimpleIncidentFeature(prjFeatureType, parent.getIncident()));
				
			doTwice(()->{
				try{
					prjStore.removeFeatures(Filter.INCLUDE);
					prjStore.addFeatures(prjFeatureCollection);
				}catch (IOException ex){
					IncidentPlugIn.log(ex.getMessage(), ex);
				}
			});

				
		}
		
		//refresh map - only refresh point layer 
		if (pointLayer != null) pointLayer.refresh(null);
		if (prjLayer != null) prjLayer.refresh(null);
		return;
	}
	
	


	
	/**
	 * Default style for the layer
	 * @return
	 */
	private Style getStylingConfig() {
		return SmartUtils.getDefaultWaypointStyle();
	}
	
	private Style getPrjStylingConfig() {
		return SmartUtils.getDefaultPrjWaypointStyle();
	}
	
	
	private IMapEditManager getEditManager(){
    	return new IMapEditManager() {
    		private List<Object> undoCommands = new ArrayList<>();
			
    		private boolean showWarning = true;
    		@Override
    		public void activate() {
    			if (parent.getOptions().getTrackDistanceDirection() && showWarning) {
    				//down warning 
    				showWarning = false;
    				MessageDialog.openWarning(parent.getSite().getShell(), Messages.IncidentMapPage_EditWarningMsgTitle, Messages.IncidentMapPage_EditWarningMsg);
    			}
    		}
    		@Override
			public synchronized void moveFeature(Object feature, int x, int y, IViewportModel vm) {
				if (!(feature instanceof Waypoint)) return ;
				
				Waypoint pw = (Waypoint) feature;
				Coordinate crspx = vm.pixelToWorld(x, y);
				//convert to lat/long
				if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
					try{
						crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					}catch (Exception ex){
						IncidentPlugIn.displayLog(Messages.IncidentMapPage_reprojectionError + ex.getMessage(), ex);
						return;
					}
				}
				
				double origx = pw.getRawX();
				double origy = pw.getRawY();
				Float origdistance = pw.getDistance();
				Float origdirection = pw.getDirection();
				
				double newx = origx;
				double newy = origy;
				Float newdistance = pw.getDistance();
				Float newdirection = pw.getDirection();
				if (pw.getDirection() != null && pw.getDistance() != null) {
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
						pw.setRawX(newx);
						pw.setRawY(newy);
						pw.setDirection(newdirection);
						pw.setDistance(newdistance);
						s.merge(pw);
						s.getTransaction().commit();
						modified = true;
					}catch (Exception ex){
						try{
							if (s.getTransaction().isActive()) s.getTransaction().rollback();
						}catch (Exception ex2){
							IncidentPlugIn.displayLog(Messages.IncidentMapPage_SaveError + ex.getMessage(), ex);
							return;
						}
						pw.setRawX(origx);
						pw.setRawY(origy);
						pw.setDirection(origdirection);
						pw.setDistance(origdistance);
					}
				}
				if (modified){
					addUndo(pw, origx, origy, origdirection, origdistance);
					WaypointEventManager.getInstance().waypointModified(pw);
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
					Coordinate pnt = ReprojectUtils.reproject(parent.getIncident().getX(), parent.getIncident().getY(), SmartDB.DATABASE_CRS, vm.getCRS());
					Point exitPnt = vm.worldToPixel(pnt);
					if (Math.abs(exitPnt.getX() - x) > 5 || Math.abs(exitPnt.getY() - y) > 5) return null;
					
					return new EditPoint(exitPnt, parent.getIncident());
				}catch (Exception ex){
					IncidentPlugIn.log(ex.getMessage(), ex);
					return null;
				}
			}
			
			private void addUndo(Waypoint wp, double x, double y, Float direction, Float distance){
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
					try{
						Object c = undoCommands.remove(0);
						s.beginTransaction();
						Object[] data = (Object[])c;
						
						Waypoint pw = (Waypoint) data[0];
						double x = (double) data[1];
						double y = (double) data[2];
						Float distance = (Float)data[3];
						Float direction = (Float)data[4];
						
						pw.setRawX(x);
						pw.setRawY(y);
						pw.setDirection(direction);
						pw.setDistance(distance);
						s.merge(pw);
						s.getTransaction().commit();
						
						Display.getDefault().syncExec(()->{
							WaypointEventManager.getInstance().waypointModified(pw);
						});
					}catch (Exception ex){
						if (s.getTransaction().isActive()) s.getTransaction().rollback();
						IncidentPlugIn.displayLog(Messages.IncidentMapPage_UndonError + ex.getMessage(), ex);
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
	    
	private IInfoToolProvider getMapInfoProvider() {
		return new IInfoToolProvider() {
			@Override
			public InfoPoint findFeature(int x, int y, IViewportModel vm) {
				try {
					int xll = x - 5;
					int yll = y - 5;
					int xur = x + 5;
					int yur = y + 5;

					Coordinate worldll = vm.pixelToWorld(xll, yll);
					Coordinate worldur = vm.pixelToWorld(xur, yur);

					Coordinate dbll = ReprojectUtils.reproject(worldll.x, worldll.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					Coordinate dbur = ReprojectUtils.reproject(worldur.x, worldur.y, vm.getCRS(), SmartDB.DATABASE_CRS);

					Envelope env = new Envelope(dbll, dbur);

					// find all waypoints in bounding box
					List<Waypoint> waypoints = new ArrayList<>();
					if (env.contains(parent.getIncident().getX(), parent.getIncident().getY())) {
						waypoints.add(parent.getIncident());
					}

					if (!waypoints.isEmpty()) {
						
						Coordinate px = ReprojectUtils.reproject(waypoints.get(0).getX(), waypoints.get(0).getY(),
							SmartDB.DATABASE_CRS, vm.getCRS());
						return new InfoPoint(vm.worldToPixel(px), waypoints, null);
					}
					
					//check observation details
					Object[] found = ObservationAttributeFeatureFactory.findWaypointObservationAttributes(env, new IGeoResource[]{attLinestringResource, attPolygonResource});
					List<WaypointObservationAttribute> matched = (List<WaypointObservationAttribute>) found[0];
					Coordinate c = (Coordinate) found[1];
					
					if (!matched.isEmpty()) {						
						Coordinate px = ReprojectUtils.reproject(c.x, c.y, SmartDB.DATABASE_CRS, vm.getCRS());
						return new InfoPoint(vm.worldToPixel(px), matched, null);
					}
					
					return null;
					
				} catch (Exception ex) {
					IncidentPlugIn.log(ex.getMessage(), ex);
				}
				return null;
			}

		};
	}
}
