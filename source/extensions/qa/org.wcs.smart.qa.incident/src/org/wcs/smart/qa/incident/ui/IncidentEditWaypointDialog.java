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
package org.wcs.smart.qa.incident.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentFeatureFactory;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.ui.view.EditWaypointDetailsDialog;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Dialog for editing independent incidents.
 * 
 * @author Emily
 *
 */
public class IncidentEditWaypointDialog extends EditWaypointDetailsDialog {

	private Waypoint editWaypoint;
	private SimpleFeatureType wpSchema;
	private FeatureStore<SimpleFeatureType, SimpleFeature> editStore;
	
	public IncidentEditWaypointDialog(Shell parentShell, UUID wpUuid) {
		super(parentShell, wpUuid);
	}

	@Override
	protected void fireEvents(Waypoint modified){
		IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, modified);
	}
	
	@Override
	protected void updateFeature(Coordinate newPosition){
		try {
			editWaypoint.setX(newPosition.x);
			editWaypoint.setY(newPosition.y);
			try{
				editStore.removeFeatures(Filter.INCLUDE);
			}catch (ConcurrentModificationException ex){
				editStore.removeFeatures(Filter.INCLUDE);
			}
			editStore.addFeatures(DataUtilities.collection(Collections.singletonList(IncidentFeatureFactory.createSimpleIncidentFeature(wpSchema, editWaypoint))));
			getMap().getRenderManager().refresh(null);
		} catch (IOException e) {
			QaPlugIn.log(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initBackgroundLayers() {
		
		List<IGeoResource> referenceLayers = new ArrayList<>();	
		IGeoResource editResource = null;
		ReferencedEnvelope zoomEnv = null;	
		
		Session s = HibernateManager.openSession();
		try{
			Waypoint pw = (Waypoint) s.get(Waypoint.class, waypointUuid);
			editWaypoint = pw;
			if (pw == null){
				setErrorMessage("Waypoint not found.  Close dialog an re-run validation routines.");
				return;
			}

			//create edit feature
			try{
				wpSchema = IncidentFeatureFactory.createSimpleIncidentSchema();
				SimpleFeature editFeature = IncidentFeatureFactory.createSimpleIncidentFeature(wpSchema, pw);
				
				double offset = 0.01;
				zoomEnv = new ReferencedEnvelope(pw.getX() - offset, pw.getX() + offset,  pw.getY() - offset, pw.getY() + offset, SmartDB.DATABASE_CRS);
				
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
	}
	
}
