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
package org.wcs.smart.intelligence.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.StyleBlackboard;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.intelligence.map.IntelligenceDataSourceFactory;
import org.wcs.smart.intelligence.map.IntelligenceService;
import org.wcs.smart.intelligence.report.oda.IntelligencePointsQuery;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;

/**
 * Converts intelligencew point ID Oda Dataset Handle to a map
 * layer for a SMART Birt map layer. 
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligencePointsMapLayer implements IBirtMapLayerManager {

	@Override
	public StyleBlackboard getDefaultStyle(DataSetHandle handle, IGeoResource resource){
		return null;
	}
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(IntelligencePointsQuery.ID)){
			return true;
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
		if (!odaHandle.getExtensionID().equals(IntelligencePointsQuery.ID)){
			return null;
		}
		
		String uuid = null;
		if(context != null){
			uuid = (String) context.getParameterValue(ReportIntelligence.UUID);
		}
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(IntelligenceDataSourceFactory.INTELL_UUID.key, uuid);
		
		IntelligenceService service = new IntelligenceService(params);
		List<IGeoResource> resources = new ArrayList<IGeoResource>();
		resources.addAll(service.resources(null));
		return resources;
		
	}
	
}
