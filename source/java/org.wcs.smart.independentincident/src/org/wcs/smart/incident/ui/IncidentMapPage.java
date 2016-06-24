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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.style.Style;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Incident editor map page
 * @author Emily
 *
 */
public class IncidentMapPage extends SmartMapEditorPart {

	private IncidentEditor parent;
	private static final String SMART_POINT_SPEC = "fid:String,id:Integer,geom:Point:srid=4326"; //$NON-NLS-1$
	private static final String SMART_POINT_TYPE_NAME = "smart.independentincident"; //$NON-NLS-1$
	
	private SimpleFeatureType featureType;
	private ListFeatureCollection featureCollection;
	private FeatureStore<SimpleFeatureType,SimpleFeature> store;
	private Layer pointLayer = null;
	private IGeoResource pointResource;
	
	/**
	 * Creates a new map page
	 * @param e parent editor
	 */
	public IncidentMapPage(IncidentEditor e){
		this.parent = e;
	}
	
	@Override
	public MultiPageEditorPart getParentEditor() {
		return parent;
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		LoadDefaultLayersJob loadDefaultLayers = new LoadDefaultLayersJob(getMap());
		loadDefaultLayers.schedule();
		
		addPointsLayer();
		updatePointsLayer();
	}

	/**
	 * Creates the incident layer
	 */
	private void addPointsLayer() {
        try {
			featureType = DataUtilities.createType(SMART_POINT_TYPE_NAME, SMART_POINT_SPEC);
			featureCollection = new ListFeatureCollection(featureType);
			pointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			
		
			//dispose of temporary layer when composite is disposed
			super.mapViewer.getControl().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (pointLayer != null){
							CatalogPlugin.getDefault().getLocalCatalog().remove(pointLayer.getGeoResource().service(null));
						}
					}catch (Exception ex){
						IncidentPlugIn.log("Error removing incident service", ex); //$NON-NLS-1$
					}
					
				}
			});
	        store = pointResource.resolve(FeatureStore.class, null);

			List<IGeoResource> layers = new ArrayList<IGeoResource>();
			layers.add(pointResource);
			
			AddLayersCommand command = new AddLayersCommand(layers, 0) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					Layer pointLayerEx = getLayers().get(0);
					pointLayerEx.setName(Messages.IncidentMapPage_MapLayerName);
					String sld = getStylingConfig();
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
					Style style = (Style)c.load(memento);
					pointLayerEx.getStyleBlackboard().put(SLDContent.ID, style);
					
				}
			};
			getMap().sendCommandASync(command);
			
			addInitialZoomFunction();
			
        } catch (Exception exception) {
			IncidentPlugIn.displayLog(Messages.IncidentMapPage_Error1, exception);
		}
		
	}

	/**
	 * Updates the incident layer
	 */
	public void updatePointsLayer() {
		if (store == null) {
			return; //most likely we failed to add points layer
		}
		try {
			featureCollection.clear();
			featureCollection.add(createIncidentFeature(featureType));
			try{
				store.removeFeatures(Filter.INCLUDE);
				store.addFeatures(featureCollection);
			}catch (ConcurrentModificationException ex){
				//try again - this should only happen once (udig removes listener)
				//see SMART bug 1672
				store.removeFeatures(Filter.INCLUDE);
				store.addFeatures(featureCollection);
			}
			
			
		} catch (IOException e) {
			IncidentPlugIn.displayLog(Messages.IncidentMapPage_Error2, e);
		}
		//refresh map - only refresh point layer 
		if (pointLayer == null){
			for (ILayer layer : getMap().getMapLayers()){
				if (layer.getGeoResource().getID().equals(pointResource.getID())){
					pointLayer = (Layer)layer;
				}
			}
		}
		if (pointLayer != null){
			pointLayer.refresh(null);
		}
		return;
	}
	
	
	/**
	 * Creates a feature from the incident
	 * @param ftype
	 * @return
	 */
	private SimpleFeature createIncidentFeature(SimpleFeatureType ftype) {
		Waypoint incident = parent.getIncident();
		Object data[] = new Object[3];
		String name = ftype.getName() + "." + UuidUtils.uuidToString(incident.getUuid()); //$NON-NLS-1$
		data[0] = name;
		data[1] = incident.getId();
		data[2] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(incident.getX(), incident.getY()));
		SimpleFeature f = SimpleFeatureBuilder.build(ftype, data, name);
		return f;
	}

	
	/**
	 * Default style for the layer
	 * @return
	 */
	private String getStylingConfig() {
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
		"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
		"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
		"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
		"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
		"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
		"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		//rule for default style
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
		"								&lt;sld:CssParameter name=\"fill\"&gt;#FF0000&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Stroke /&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Size&gt;10.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
			"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
		"</styleEntry>"; //$NON-NLS-1$
	
	}
}
