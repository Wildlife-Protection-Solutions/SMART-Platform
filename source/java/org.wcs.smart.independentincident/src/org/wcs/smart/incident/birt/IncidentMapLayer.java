/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.FilterFactory;
import org.opengis.style.FeatureTypeStyle;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.incident.birt.details.IncidentDataset;
import org.wcs.smart.incident.birt.observations.IncidentObservationAttributeDataset;
import org.wcs.smart.incident.birt.observations.IncidentObservationAttributeDatasetResultSetMetadata;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.udig.style.StyleManager;

/**
 * MapLayer for adding incident dataset to BIRT map.
 * 
 * @author Emily
 *
 */
public class IncidentMapLayer implements IBirtMapLayerManager, IBirtLayerStyleProvider {

	public IncidentMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(IncidentDataset.DATASET_TYPE) || 
			odaHandle.getExtensionID().equals(IncidentObservationAttributeDataset.DATASET_TYPE) ){
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle) throws Exception {
		if (handle instanceof OdaDataSetHandle) {
			return findGeometryColumnsInResultSet((OdaDataSetHandle) handle);
		}
		return Collections.emptyList();		
	}

	@Override
	public StyleBlackboard getStyle(String extensionId, 
			String queryText, MapLayerInfo info, ConservationArea ca,
			Session s) {
		
		if (extensionId.equals(IncidentObservationAttributeDataset.DATASET_TYPE)) {
			//get all geometry attributes
			Attribute.AttributeType type = null;
			if (info.getLayerType() == LayerType.MULTILINE || info.getLayerType() == LayerType.LINE) {
				type = Attribute.AttributeType.LINE;
			}
			if (info.getLayerType() == LayerType.MULTIPOLYGON || info.getLayerType() == LayerType.POLYGON) {
				type = Attribute.AttributeType.POLYGON;
			}
			if (type == null) return null;
			
			return StyleManager.INSTANCE.buildThemedGeometryAttributeStyle(
					IncidentObservationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE.name, 
					type, s, ca, false);			
		}
		return null;
	}
}
