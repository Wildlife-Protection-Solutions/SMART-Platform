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
			sb.put("org.locationtech.udig.style.sld", EntityManager.INSTANCE.buildRedStarStyle());
			return sb;
		}
		return null;
	}

}
