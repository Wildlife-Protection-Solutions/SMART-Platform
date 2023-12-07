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
package org.wcs.smart.ui.map.location;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ISmartPoint;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Map Composite for editing point in dialog 
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PointMapComposite extends MapComposite  {

	private static final String SMART_POINT_SPEC = "fid:String,id:Integer,selected:Boolean,geom:Point:srid=4326"; //$NON-NLS-1$
	private static final String SMART_POINT_TYPE_NAME = "smart.ISmartPoint"; //$NON-NLS-1$

	private SimpleFeatureType featureType;
	private ListFeatureCollection featureCollection;
	private FeatureStore<SimpleFeatureType,SimpleFeature> store;
	private Layer pointLayer = null;
	private IGeoResource pointResource;
	
	private ISmartPointDataProvider dataProvider;
	
	private Style pointStyle = null;
	
	
	/**
	 * @param parent
	 * @param style
	 */
	public PointMapComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	public void setPointStyle(Style pointStyle){
		this.pointStyle = pointStyle;
		if (this.pointLayer != null) {
			pointLayer.getStyleBlackboard().put(SLDContent.ID, pointStyle);			
		}
	}
	
	@Override
	protected void createControls() {
		super.createControls();
		addPointsLayer();
	}
	
	@SuppressWarnings("unchecked")
	private void addPointsLayer() {
        try {
			featureType = DataUtilities.createType(SMART_POINT_TYPE_NAME, SMART_POINT_SPEC);
			featureCollection = new ListFeatureCollection(featureType);
			pointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
		
			//dispose of temporary layer when composite is disposed
			super.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (pointLayer != null){
							CatalogPlugin.getDefault().getLocalCatalog().remove(pointLayer.getGeoResource().service(null));
						}
					}catch (Exception ex){
						SmartPlugIn.log("Error removing service", ex); //$NON-NLS-1$
					}
					
				}
			});
	        store = pointResource.resolve(FeatureStore.class, null);
	        pointResource.resolve(DataStore.class, null);
			List<IGeoResource> layers = new ArrayList<IGeoResource>();
			layers.add(pointResource);
			
			AddLayersCommand command = new AddLayersCommand(layers, 0) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					pointLayer = getLayers().get(0);
					pointLayer.getStyleBlackboard().put(SLDContent.ID, getPointStyling());
					pointLayer.setName("Selected Point");
				}
			};
			getMap().sendCommandASync(command);
        } catch (Exception exception) {
			SmartPlugIn.displayLog(Messages.MapComposite_PointLayer_Add_Error, exception);
		}
		
	}

	public void updatePointsLayer() {
		if (store == null) {
			return; //most likely we failed to add points layer
		}
		try {
			featureCollection.clear();
			featureCollection.addAll(getSmartPointAsFeatures(featureType));
			
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
			SmartPlugIn.displayLog(Messages.MapComposite_PointLayer_Update_Error, e);
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
	
	private List<SimpleFeature> getSmartPointAsFeatures(SimpleFeatureType ftype) {
		if (getDataProvider() == null) {
			return Collections.emptyList();
		}
		List<? extends ISmartPoint> points = getDataProvider().getPoints();
		int size = points.size();
		List<SimpleFeature> features = new ArrayList<SimpleFeature>(size);
		for (int i = 0; i < size; i++) {
			ISmartPoint point = points.get(i);
			Object data[] = new Object[4];
			String name = ftype.getName() + "." + i; //$NON-NLS-1$
			data[0] = name;
			data[1] = i;
			data[2] = getDataProvider().isSelected(point);
			data[3] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(point.getX(), point.getY()));
			features.add(SimpleFeatureBuilder.build(ftype, data, name));
		}
		return features;
	}

	public void setDataProvider(ISmartPointDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}
	
	protected ISmartPointDataProvider getDataProvider() {
		return dataProvider;
	}
	
	private Style getPointStyling() {
		if (pointStyle != null){
			return pointStyle;
		}
		 
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		StyleBuilder sb = new StyleBuilder();
		
		//not selected
		Mark mark = sb.createMark("star",  new Color(255, 0, 0), new Color(255, 0, 0), 1); //$NON-NLS-1$
		Graphic graph2 = sb.createGraphic(null, mark, null, 1, 10, 0);
        PointSymbolizer notselected = sb.createPointSymbolizer(graph2);
        Rule rnotselected = sb.createRule(notselected);
        rnotselected.setFilter(ff.equals(ff.property("selected"), ff.literal(Boolean.FALSE))); //$NON-NLS-1$
        rnotselected.setName("Not Selected");
        
        //selected
        mark = sb.createMark("star",  new Color(255, 255, 0), new Color(255, 255, 0), 1); //$NON-NLS-1$
		graph2 = sb.createGraphic(null, mark, null, 1, 10, 0);
        PointSymbolizer selected = sb.createPointSymbolizer(graph2);
        Rule rselected = sb.createRule(selected);
        rselected.setFilter(ff.equals(ff.property("selected"), ff.literal(Boolean.TRUE))); //$NON-NLS-1$
        rselected.setName("Selected");
        
        FeatureTypeStyle fs = sb.createFeatureTypeStyle(null, rselected, rnotselected);
        Style s = sb.createStyle();
        s.featureTypeStyles().add(fs);
        return s;

	}
	
	
}
