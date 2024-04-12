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
package org.wcs.smart.entity.report;

import java.util.Collections;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.MemberHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * Map Layers for fixed entity types.
 * 
 * @author Emily
 *
 */
public class FixedEntityMapLayer implements IBirtMapLayerManager {

	public FixedEntityMapLayer() {
	}

	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)) {
			if (odaHandle.getQueryText().startsWith(EntityTable.ENTITYKEY_PREFIX)){
				String type = odaHandle.getQueryText().split(":")[1]; //$NON-NLS-1$
				if (type.equals(EntityType.Type.FIXED.name())){
					return true;
				}
			}
		}
		return false;
	}
	

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		MemberHandle metadata = handle.getCachedMetaDataHandle().getResultSet();
		if (metadata.getListValue() != null) {
			for (int i=0; i < metadata.getListValue().size(); i++) {
				ResultSetColumnHandle resultSetColumn=(ResultSetColumnHandle)metadata.getAt(i);
				if (resultSetColumn.getColumnName().equals(EntityTable.GEOMETRY_COL_KEY)){
					MapLayerInfo info = new MapLayerInfo(null, null, LayerType.POINT, resultSetColumn.getColumnName());
					return Collections.singletonList(info);
				}
			}
		}
		return null;
	}

}
