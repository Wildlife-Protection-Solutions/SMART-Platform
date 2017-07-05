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
package org.wcs.smart.qa.er;

import java.io.IOException;
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
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.udig.SurveyFeatureFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.ui.view.EditWaypointDetailsDialog;

import com.vividsolutions.jts.geom.Coordinate;

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
		double x1 = previousWaypoint.getWaypoint().getX();
		double y2 = nextWaypoint.getWaypoint().getY();
		double y1 = previousWaypoint.getWaypoint().getY();
		
		double t2 = nextWaypoint.getWaypoint().getDateTime().getTime();
		double t1 = previousWaypoint.getWaypoint().getDateTime().getTime();
		
		double tn = waypoint.getDateTime().getTime();
		
		double newX =  x2 - ( (t2 - tn) / (t2 - t1) ) * (x2 - x1);
		double newY =  y2 - ( (t2 - tn) / (t2 - t1) ) * (y2 - y1);
		
		if (newX < -180) newX = -180;
		if (newX > 180) newX = 180;
		if (newY < -180) newY = -90;
		if (newY > 180) newY = 90;
		
		updateWaypointLocation(new Coordinate(newX, newY));
		updateLabels();
	}
	
	@Override
	protected void addToolbarContributions(ToolBar toolBar){
		btnInterpolate = new ToolItem(toolBar, SWT.PUSH);
		btnInterpolate.setToolTipText("interpolate a new position based on previous and next waypoints");
		btnInterpolate.setImage(QaPlugIn.getDefault().getImageRegistry().get(QaPlugIn.ICON_INTERPOLATE));
		btnInterpolate.addListener(SWT.Selection, e->interpolate());
	}

	@Override
	protected void updateFeature(Coordinate newPosition){
		try {
			editWaypoint.getWaypoint().setX(newPosition.x);
			editWaypoint.getWaypoint().setY(newPosition.y);
			try{
				editStore.removeFeatures(Filter.INCLUDE);
			}catch (ConcurrentModificationException ex){
				editStore.removeFeatures(Filter.INCLUDE);
			}
			editStore.addFeatures(DataUtilities.collection(Collections.singletonList(SurveyFeatureFactory.createWaypointFeature(wpSchema, editWaypoint))));
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
		ReferencedEnvelope zoomEnv = null;	
		
		Session s = HibernateManager.openSession();
		try{
			SurveyWaypoint pw = (SurveyWaypoint) s.createCriteria(SurveyWaypoint.class)
					.add(Restrictions.eq("id.waypoint.uuid", waypointUuid)) //$NON-NLS-1$
					.uniqueResult();
			editWaypoint = pw;
			if (pw == null){
				setErrorMessage("Waypoint not found.  Close dialog an re-run validation routines.");
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
			long wpTime = editWaypoint.getWaypoint().getDateTime().getTime();
			for (SurveyWaypoint ww : waypoints){
				if (ww.equals(editWaypoint)) continue;
				long time = ww.getWaypoint().getDateTime().getTime();
				if (time <= wpTime && (previousWaypoint == null || (wpTime - time) < prevDiff)){
					prevDiff = wpTime - time;
					previousWaypoint = ww;
				}
				
				if (time >= wpTime && (nextWaypoint == null || (time - wpTime) < nextDiff)){
					nextDiff = time = wpTime;
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
					
					IGeoResource waypointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(schema);
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
			}catch (Exception ex){
				QaPlugIn.log(ex.getMessage(), ex);
			}
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
		}finally{
			s.close();
		}
		
		IGeoResource eResource = editResource;
		ReferencedEnvelope eZoom = zoomEnv;
		if (!referenceLayers.isEmpty()){
			AddLayersCommand cmd = new AddLayersCommand(referenceLayers){
				 public void run( IProgressMonitor monitor ) throws Exception {
					 super.run(monitor);
					 for (Layer l : getLayers()){
						 if (l.getGeoResource().getIdentifier().equals(eResource.getIdentifier())){
							 Style style = getStylingConfig();
							 l.getStyleBlackboard().put(SLDContent.ID, style);
						 }
					 }
					 if (eZoom != null) getMap().sendCommandASync(new SetViewportBBoxCommand(new ReferencedEnvelope(eZoom)));
				 }
				 
			};
			getMap().sendCommandASync(cmd);
		}
		
		if (previousWaypoint == null || nextWaypoint == null || previousWaypoint.equals(nextWaypoint)){
			btnInterpolate.setEnabled(false);
		}
	}
}
