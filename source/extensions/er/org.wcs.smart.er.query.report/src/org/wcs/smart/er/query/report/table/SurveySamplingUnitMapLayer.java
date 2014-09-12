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
package org.wcs.smart.er.query.report.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.map.samplingunit.SamplingUnitServiceExtension;
import org.wcs.smart.er.map.samplingunit.SamplingUnitSourceFactory;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;

/**
 * Map Layers for survey sampling units.
 * 
 * @author Emily
 *
 */
public class SurveySamplingUnitMapLayer implements IBirtMapLayerManager {

	public SurveySamplingUnitMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)) {
			if (odaHandle.getQueryText().startsWith(SurveySamplingUnitTable.SU_PREFIX)){
				return true;
			}
		}
		return false;
	}

	@Override
	public List<IGeoResource> createLayer(DataSetHandle handle,
			IReportContext context) throws Exception {
		if (!(handle instanceof OdaDataSetHandle)){
			return null;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (!odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)){
			return null;
		}
		
		String surveyDesignKey =  odaHandle.getQueryText().split(":")[2]; //$NON-NLS-1$
		SamplingUnit.SamplingUnitType suType = SamplingUnit.SamplingUnitType.valueOf(odaHandle.getQueryText().split(":")[1]); //$NON-NLS-1$
		SurveyDesign sd = null;
		
		//session is managed by running report
		Session s = HibernateManager.openSession();
		Criteria c = s.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", surveyDesignKey)); //$NON-NLS-1$ 
		List<?> data = c.list();
		if (data.size() > 0){
			sd = (SurveyDesign)data.get(0);
		}
		
		if (sd == null){
			return null;
		}
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SamplingUnitSourceFactory.SD_UUID.key, sd.getUuid());
		
		//find fixed entity service
		SamplingUnitService service = null;
		List<IResolve> citems = CatalogPlugin.getDefault().getLocalCatalog().find(SamplingUnitServiceExtension.createURL(params), null);
		for (IResolve i : citems){
			service = (SamplingUnitService) i;
		}
		if (service == null){
			service = (SamplingUnitService) CatalogPlugin.getDefault().getLocalCatalog().acquire(SamplingUnitServiceExtension.createURL(params), null);
		}
		
		//find georesource
		List<IGeoResource> resources = new ArrayList<IGeoResource>();
		List<? extends IGeoResource> items = service.resources(null);
		for (IGeoResource i : items){
			if (suType == SamplingUnit.SamplingUnitType.PLOT && 
					(((SamplingUnitGeoResource)i).getDataType().equals(SamplingUnit.SamplingUnitType.PLOT.name()))){
				resources.add(i);
			}else if (suType == SamplingUnit.SamplingUnitType.TRANSECT && 
					(((SamplingUnitGeoResource)i).getDataType().equals(SamplingUnit.SamplingUnitType.TRANSECT.name()))){
				resources.add(i);
			}
		}
		
		return resources;
	}

}
