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
package org.wcs.smart.i2.birt.map.entity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDataset;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDatasetResultSetMetadata;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * Map layer for entity location attributes
 * 
 * @author Emily
 *
 */
public class EntityAttributeMapLayer implements IBirtMapLayerManager, IBirtLayerStyleProvider {

	public EntityAttributeMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(EntityLocationAttributeDataset.DATASET_TYPE)) {
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(EntityLocationAttributeDataset.DATASET_TYPE)) {
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, EntityLocationAttributeDatasetResultSetMetadata.Column.GEOMETRY.getId());
			List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
			layers.add(def);
			return layers;
		}
		return null;
	}

	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText, Session s) {
		if (extensionId.equals(EntityLocationAttributeDataset.DATASET_TYPE)){
			//return red star style
			StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
			sb.put("org.locationtech.udig.style.sld", EntityManager.INSTANCE.buildRedStarStyle()); //$NON-NLS-1$
			return sb;
		}
		return null;
	}

}
