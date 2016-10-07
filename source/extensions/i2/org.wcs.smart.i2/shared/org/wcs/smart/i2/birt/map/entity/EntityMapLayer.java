package org.wcs.smart.i2.birt.map.entity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDatasetResultSetMetadata;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

public class EntityMapLayer implements IBirtMapLayerManager {

	public EntityMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(EntityLocationDataset.DATASET_TYPE)) {
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(EntityLocationDataset.DATASET_TYPE)) {
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, EntityLocationDatasetResultSetMetadata.Column.GEOM.getId());
			MapLayerInfo def2 = new MapLayerInfo(null, null, LayerType.POLYGON, EntityLocationDatasetResultSetMetadata.Column.GEOM.getId());
			List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
			layers.add(def);
			layers.add(def2);
			return layers;
		}
		return null;
	}

}
