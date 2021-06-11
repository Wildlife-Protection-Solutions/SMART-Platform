package org.wcs.smart.incident.birt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.incident.birt.details.IncidentDataset;
import org.wcs.smart.incident.birt.details.IncidentDatasetResultSetMetadata;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

public class IncidentMapLayer implements IBirtMapLayerManager {

	public IncidentMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(IncidentDataset.DATASET_TYPE)){
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle) throws Exception {
		MapLayerInfo position = new MapLayerInfo("Position", null, LayerType.POINT, IncidentDatasetResultSetMetadata.GEOM_COLUMN_NAME);
		MapLayerInfo raw = new MapLayerInfo("Raw Position", null, LayerType.POINT, IncidentDatasetResultSetMetadata.RAW_GEOM_COLUMN_NAME);
		
		List<MapLayerInfo> items = new ArrayList<>();
		items.add(position);
		items.add(raw);
		return items;
	}

}
