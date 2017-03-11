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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayerCommand;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelValueItem;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Manages a temporary udig layer for displaying position attributes on a map
 * @author Emily
 *
 */
public class LocationAttributeMapLayer {

	private IGeoResource attributeResource;

	private Map map;
	private String layerName;
	private String uniqueId;

	private Layer layer;

	public LocationAttributeMapLayer(Map map, String layerName, String uniqueId) {
		this.map = map;
		this.layerName = layerName;
		this.uniqueId = uniqueId;
	}

	private SimpleFeatureType createFeatureType() throws SchemaException{
		 return DataUtilities.createType(uniqueId,
					"the_geom:Point:srid=4326,id:String,attribute:String"); //$NON-NLS-1$
	}
	
	public void createLayersRecord(List<IntelRecordAttributeValue> values) {
		createLayers(values);
	}

	public void createValueLayers(List<? extends IntelValueItem> values) {
		createLayers(values);
	}

	public void refreshLayerValue(IntelValueItem value) {
		refreshLayer(value);
	}
	public void refreshLayerRecord(IntelRecordAttributeValue value){
		refreshLayer(value);
	}
	
	public void dispose(){
		//remove layers from map
		if (layer != null){
			DeleteLayerCommand cmd = new DeleteLayerCommand(layer);
			map.executeSyncWithoutUndo(cmd);
		}
	}
	private void refreshLayer(Object value){
		if (attributeResource == null)
			return;
		
		Double number1 = null;
		Double number2 = null;
		IntelAttribute attribute = null;
		if (value instanceof IntelValueItem){
			attribute = ((IntelValueItem) value).getAttribute();
			number1 = ((IntelValueItem)value).getNumberValue();
			number2 = ((IntelValueItem)value).getNumberValue2();
		}else if (value instanceof IntelRecordAttributeValue){
			attribute = ((IntelRecordAttributeValue) value).getAttribute().getAttribute();
			if (((IntelRecordAttributeValue)value).getAttribute().getAttribute() != null
					&& ((IntelRecordAttributeValue)value).getAttribute().getAttribute().getType() == AttributeType.POSITION) {
				number1 = ((IntelRecordAttributeValue)value).getNumberValue();
				number2 = ((IntelRecordAttributeValue)value).getNumberValue2();
			}
		}
		if (attribute == null) return;
		
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		Filter f = ff.equals(ff.property("id"), ff.literal(UuidUtils.uuidToString(attribute.getUuid()))); //$NON-NLS-1$

		try {
			SimpleFeatureStore store = (SimpleFeatureStore) attributeResource
					.resolve(SimpleFeatureStore.class, null);

			if (number1 == null || number2 == null) {
				// remove feature
				try (Transaction transaction = new DefaultTransaction("deletetransaction")) { //$NON-NLS-1$
					store.setTransaction(transaction);
					store.removeFeatures(f);
					transaction.commit();
				}
			} else {
				final com.vividsolutions.jts.geom.Point p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(number1, number2));
				if (store.getFeatures(f).size() == 0) {
					// need to add the feature
					try (Transaction transaction = new DefaultTransaction("addtransaction")) { //$NON-NLS-1$
						store.setTransaction(transaction);
						SimpleFeature sf = SimpleFeatureBuilder.build(store.getSchema(),
								new Object[] {p, UuidUtils.uuidToString(attribute.getUuid()),attribute.getName() }, null);
						store.addFeatures(DataUtilities.collection(sf));
						transaction.commit();
					}
				} else {
					try (Transaction transaction = new DefaultTransaction("edittransaction")) { //$NON-NLS-1$
						store.setTransaction(transaction);
						store.modifyFeatures("the_geom", p, f); //$NON-NLS-1$
						transaction.commit();
					}
				}
			}
		} catch (Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}

		if (layer != null) layer.refresh(null);
	}

	private void createLayers(List<? extends Object> values) {
		// add a layer temporary layer
		try {
			SimpleFeatureType featureType = createFeatureType();

			synchronized (CatalogPlugin.getDefault().getLocalCatalog()) {
				attributeResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			}
			
			List<SimpleFeature> features = new ArrayList<>();
			if (values != null){
				for (Object value : values) {
					IntelAttribute attribute = null;
					if (value instanceof IntelValueItem){
						attribute = ((IntelValueItem) value).getAttribute();
						
					}else if (value instanceof IntelRecordAttributeValue){
						attribute = ((IntelRecordAttributeValue) value).getAttribute().getAttribute();
						
					}
					if (attribute == null || attribute.getType() != IntelAttribute.AttributeType.POSITION){
						continue;
						//not a position attribute
					}
					
					Point p = null;				
					if (value instanceof IntelValueItem){
						p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(((IntelValueItem)value).getNumberValue(), ((IntelValueItem)value).getNumberValue2()));
					}else if (value instanceof IntelRecordAttributeValue){
						p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(((IntelRecordAttributeValue)value).getNumberValue(), ((IntelRecordAttributeValue)value).getNumberValue2()));
					}
					if (p == null) continue;
						
					SimpleFeature sf = SimpleFeatureBuilder.build(
							featureType,
							new Object[] {p, UuidUtils.uuidToString(attribute.getUuid()), attribute.getName() }, null);
					features.add(sf);
					sf.setDefaultGeometry(p);
				}
			}
			attributeResource.resolve(FeatureStore.class, null).addFeatures(DataUtilities.collection(features));

			AddLayersCommand addLayers = new AddLayersCommand(Collections.singleton(attributeResource)) {
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					LocationAttributeMapLayer.this.layer = getLayers().get(0);
					layer.setName(layerName);
					layer.getStyleBlackboard().put("org.locationtech.udig.style.sld", EntityManager.INSTANCE.buildRedStarStyle()); //$NON-NLS-1$
					layer.refresh(null);
				}
			};
			map.sendCommandASync(addLayers);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
