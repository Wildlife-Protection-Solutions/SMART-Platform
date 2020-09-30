/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.er.ui;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.memory.MemoryServiceExtensionImpl;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.udig.SurveyFeatureFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.er.internal.Messages;
import org.wcs.smart.qa.ui.view.EditWaypointDetailsDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Extends of edit waypoint dialog specific to editing patrol waypoints.
 * 
 * @author Emily
 *
 */
public class ErEditWaypointDialog extends EditWaypointDetailsDialog {

	private SurveyWaypoint editWaypoint;
	private SimpleFeatureType wpSchema;
	private FeatureStore<SimpleFeatureType, SimpleFeature> editStore;
	private FeatureStore<SimpleFeatureType, SimpleFeature> prjStore;

	
	private SurveyWaypoint previousWaypoint;
	private SurveyWaypoint nextWaypoint;
	
	private ToolItem btnInterpolate ; 
	
	public ErEditWaypointDialog(Shell parentShell, UUID wpUuid) {
		super(parentShell, wpUuid);
	}

	@Override
	protected void fireEvents(Waypoint modified){
		SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.MISSION_MODIFIED, editWaypoint.getMissionDay().getMission());
	}
	
	private void interpolate(){
		if (previousWaypoint == null || nextWaypoint == null) return;
		
		double x2 = nextWaypoint.getWaypoint().getX();
		double y2 = nextWaypoint.getWaypoint().getY();
		
		double x1 = previousWaypoint.getWaypoint().getX();
		double y1 = previousWaypoint.getWaypoint().getY();

		double t2t1diff = ChronoUnit.MILLIS.between(nextWaypoint.getWaypoint().getDateTime(), previousWaypoint.getWaypoint().getDateTime());
		double t2tndiff = ChronoUnit.MILLIS.between(nextWaypoint.getWaypoint().getDateTime(), waypoint.getDateTime());
		
		double newX =  x2 - ( (t2tndiff) / (t2t1diff) ) * (x2 - x1);
		double newY =  y2 - ( (t2tndiff) / (t2t1diff) ) * (y2 - y1);
		
		if (newX < -180) newX = -180;
		if (newX > 180) newX = 180;
		if (newY < -180) newY = -90;
		if (newY > 180) newY = 90;
		
		updateWaypointLocation(new Coordinate(newX, newY), null, null);
		updateLabels();
	}
	
	@Override
	protected void addToolbarContributions(ToolBar toolBar){
		btnInterpolate = new ToolItem(toolBar, SWT.PUSH);
		btnInterpolate.setToolTipText(Messages.ErEditWaypointDialog_interpolateTooltip);
		btnInterpolate.setImage(QaPlugIn.getDefault().getImageRegistry().get(QaPlugIn.ICON_INTERPOLATE));
		btnInterpolate.addListener(SWT.Selection, e->interpolate());
	}

	@Override
	protected void updateFeature(){
		editWaypoint.getWaypoint().setRawX(waypoint.getRawX());
		editWaypoint.getWaypoint().setRawY(waypoint.getRawY());
		editWaypoint.getWaypoint().setDirection(waypoint.getDirection());
		editWaypoint.getWaypoint().setDistance(waypoint.getDistance());
		try {
			
			try{
				editStore.removeFeatures(Filter.INCLUDE);
			}catch (ConcurrentModificationException ex){
				editStore.removeFeatures(Filter.INCLUDE);
			}
			editStore.addFeatures(DataUtilities.collection(Collections.singletonList(SurveyFeatureFactory.createWaypointFeature(wpSchema, editWaypoint))));
			
			if (prjStore != null) {
				try{
					prjStore.removeFeatures(Filter.INCLUDE);
				}catch (ConcurrentModificationException ex){
					prjStore.removeFeatures(Filter.INCLUDE);
				}
				prjStore.addFeatures(DataUtilities.collection(Collections.singletonList(SurveyFeatureFactory.createWaypointPrjFeature(prjStore.getSchema(), editWaypoint))));
			}
			
			getMap().getRenderManager().refresh(null);
		} catch (IOException e) {
			QaPlugIn.log(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initBackgroundLayers() {
		List<SurveyWaypoint> waypoints = null;
		List<MissionTrack> tracks = null;
		List<IGeoResource> referenceLayers = new ArrayList<>();	
		IGeoResource editResource = null;
		IGeoResource waypointResource  = null;
		IGeoResource prjResource  = null;
		ReferencedEnvelope zoomEnv = null;	
		
		
		try(Session s = HibernateManager.openSession()){
			SurveyWaypoint pw = QueryFactory.buildQuery(s, SurveyWaypoint.class, "id.waypoint.uuid", waypointUuid).uniqueResult(); //$NON-NLS-1$
			editWaypoint = pw;
			if (pw == null){
				setErrorMessage(Messages.ErEditWaypointDialog_NotFoundError);
				return;
			}
			waypoints = pw.getMissionDay().getWaypoints();
			waypoints.remove(pw);
			
			//lazy loading
			pw.getMissionDay().equals(null);
			pw.getMissionDay().getMission().equals(null);
			
			//find previous and next waypoint for interpolation
			double prevDiff = Double.POSITIVE_INFINITY;
			double nextDiff = Double.POSITIVE_INFINITY;
			LocalDateTime wpTime = editWaypoint.getWaypoint().getDateTime();
			for (SurveyWaypoint ww : waypoints){
				if (ww.equals(editWaypoint)) continue;
				
				LocalDateTime time = ww.getWaypoint().getDateTime();
				if (time.isBefore(wpTime) && (previousWaypoint == null || ChronoUnit.MILLIS.between(wpTime,  time) < prevDiff)){
					prevDiff = ChronoUnit.MILLIS.between(wpTime,  time);
					previousWaypoint = ww;
				}
				
				if (time.isAfter(wpTime)  && (nextWaypoint == null || ChronoUnit.MILLIS.between(time,wpTime) < nextDiff)){
					nextDiff = ChronoUnit.MILLIS.between(time, wpTime);
					nextWaypoint = ww;
				}
				
			}
			
			tracks = pw.getMissionDay().getTracks();
			
			//create a layer for the track
			try{
				if (tracks != null && !tracks.isEmpty()){
					SimpleFeatureType schema = SurveyFeatureFactory.createTrackSchema();
					List<SimpleFeature> features = new ArrayList<>();
					for (MissionTrack t : tracks){
						features.add(SurveyFeatureFactory.createTrackFeature(schema, t));
					}
					
					IGeoResource track = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(schema);
					FeatureStore<SimpleFeatureType, SimpleFeature> fs = track.resolve(FeatureStore.class, new NullProgressMonitor());
					fs.addFeatures(DataUtilities.collection(features));
					referenceLayers.add(track);
				}
			}catch (Exception ex){
				QaPlugIn.log(ex.getMessage(), ex);
			}
			
			//create a layer for the waypoints
			if (waypoints != null){
				try{
					SimpleFeatureType schema = SurveyFeatureFactory.createWaypointSchema();
					List<SimpleFeature> features = new ArrayList<>();
					for (SurveyWaypoint w : waypoints){
						features.add(SurveyFeatureFactory.createWaypointFeature(schema, w));
					}
					
					waypointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(schema);
					FeatureStore<SimpleFeatureType, SimpleFeature> fs = waypointResource.resolve(FeatureStore.class, new NullProgressMonitor());
					fs.addFeatures(DataUtilities.collection(features));

					referenceLayers.add(waypointResource);
				}catch (Exception ex){
					QaPlugIn.log(ex.getMessage(), ex);
				}
			}
			
			//create edit feature
			try{
				wpSchema = SurveyFeatureFactory.createWaypointSchema();
				SimpleFeature editFeature = SurveyFeatureFactory.createWaypointFeature(wpSchema, pw);
				
				double offset = 0.01;
				zoomEnv = new ReferencedEnvelope(pw.getWaypoint().getX() - offset, pw.getWaypoint().getX() + offset,  pw.getWaypoint().getY() - offset, pw.getWaypoint().getY() + offset, SmartDB.DATABASE_CRS);
				
				editResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(wpSchema);
				editStore = editResource.resolve(FeatureStore.class, new NullProgressMonitor());
				editStore.addFeatures(DataUtilities.collection(Collections.singletonList(editFeature)));
				referenceLayers.add(editResource);
				
				if (editWaypoint.getWaypoint().getDirection() != null && editWaypoint.getWaypoint().getDistance() != null) {
					SimpleFeatureType ftype = SurveyFeatureFactory.createWaypointPrjSchema();
					SimpleFeature sfeature = SurveyFeatureFactory.createWaypointPrjFeature(ftype, editWaypoint);
					prjResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(ftype);
					prjStore = prjResource.resolve(FeatureStore.class, new NullProgressMonitor());
					prjStore.addFeatures(DataUtilities.collection(Collections.singletonList(sfeature)));
					referenceLayers.add(prjResource);
					
					zoomEnv.expandToInclude(new Coordinate(editWaypoint.getWaypoint().getRawX(), editWaypoint.getWaypoint().getRawY()));
					zoomEnv.expandBy(zoomEnv.getWidth() * 0.5);
				}
			}catch (Exception ex){
				QaPlugIn.log(ex.getMessage(), ex);
			}
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
		}
		
		IGeoResource eResource = editResource;
		IGeoResource pResource = prjResource;
		IGeoResource wResource = waypointResource;
		ReferencedEnvelope eZoom = zoomEnv;
		if (!referenceLayers.isEmpty()){
			AddLayersCommand cmd = new AddLayersCommand(referenceLayers){
				 public void run( IProgressMonitor monitor ) throws Exception {
					 super.run(monitor);
					 for (Layer l : getLayers()){
						 if (l.getGeoResource().getIdentifier().equals(eResource.getIdentifier())){
							 Style style = getStylingConfig();
							 l.getStyleBlackboard().put(SLDContent.ID, style);
						 }else if (pResource != null && l.getGeoResource().getIdentifier().equals(pResource.getIdentifier())) {
							 l.getStyleBlackboard().put(SLDContent.ID, SmartUtils.getDefaultPrjWaypointStyle());
						 }else if (wResource != null && l.getGeoResource().getIdentifier().equals(wResource.getIdentifier())) {
							 l.getStyleBlackboard().put(SLDContent.ID, getOtherWaypointStyle());
						 }
					 }
					 if (eZoom != null) getMap().sendCommandASync(new SetViewportBBoxCommand(new ReferencedEnvelope(eZoom)));
				 }
				 
			};
			getMap().sendCommandASync(cmd);
		}
		getShell().addListener(SWT.Dispose, e->{
			List<IResolve> items = CatalogPlugin.getDefault().getLocalCatalog().find(MemoryServiceExtensionImpl.URL, new NullProgressMonitor());
			for (IResolve i : items) {
				if (i.canResolve(IService.class)) {
					try {
						CatalogPlugin.getDefault().getLocalCatalog().remove(i.resolve(IService.class, new NullProgressMonitor()));
					} catch (Exception e1) {
						EcologicalRecordsPlugIn.log(e1.getMessage(), e1);
					}
				}
			}
		});		
		
		if (previousWaypoint == null || nextWaypoint == null || previousWaypoint.equals(nextWaypoint)){
			btnInterpolate.setEnabled(false);
		}
	}
}
