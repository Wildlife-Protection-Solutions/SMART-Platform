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

import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationObservationAttributeDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationObservationDetailsDataset;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Record locations map layer
 * 
 * @author Emily
 *
 */
public class ProfileMapLayers implements IBirtMapLayerManager {

	public ProfileMapLayers() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(RecordLocationDataset.DATASET_TYPE)) {
			return true;
		}
		if (odaHandle.getExtensionID().equals(RecordLocationObservationDetailsDataset.DATASET_TYPE)) {
			return true;
		}
		if (odaHandle.getExtensionID().equals(EntityLocationDataset.DATASET_TYPE)) {
			return true;
		}
		if (odaHandle.getExtensionID().equals(EntityDataset.DATASET_TYPE)) {
			return true;
		}
		if (odaHandle.getExtensionID().equals(EntityLocationAttributeDataset.DATASET_TYPE)) {
			return true;
		}
		if (odaHandle.getExtensionID().equals(EntityLocationObservationAttributeDataset.DATASET_TYPE)) {
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		if (handle instanceof OdaDataSetHandle){
			return this.findGeometryColumnsInResultSet((OdaDataSetHandle) handle);
		}
		return null;
	}

}
