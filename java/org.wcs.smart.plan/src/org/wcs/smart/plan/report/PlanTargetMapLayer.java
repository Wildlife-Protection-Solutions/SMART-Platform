package org.wcs.smart.plan.report;

import java.util.List;

import net.refractions.udig.catalog.IGeoResource;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.plan.report.oda.PlanTargetQuery;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;

public class PlanTargetMapLayer implements IBirtMapLayerManager {

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(PlanTargetQuery.SMART_PLAN_TARGET_ID)){
			return true;
		}
		return false;
	}

	@Override
	public List<IGeoResource> createLayer(DataSetHandle handle,
			IReportContext context) throws Exception {
		
		
		
		
		return null;
	}

}
