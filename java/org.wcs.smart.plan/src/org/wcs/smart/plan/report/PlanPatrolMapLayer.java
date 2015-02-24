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
package org.wcs.smart.plan.report;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.query.map.udig.QueryServiceFactory;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.plan.report.oda.PlanPatrolQuery;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;

/**
 * Converts Patrol Plan Query into a map layer
 * for adding to smart plan map. 
 * 
 * @author Emily
 *
 */
public class PlanPatrolMapLayer implements IBirtMapLayerManager {

	public PlanPatrolMapLayer() {
	}

	@Override
	public StyleBlackboard getDefaultStyle(DataSetHandle handle, IGeoResource resource){
		return null;
	}
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		//only support queries without any query strings
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(PlanPatrolQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			if (queryText.length() == 0){
				return true;
			}
		}
		return false;
	}

	@Override
	public List<IGeoResource> createLayer(DataSetHandle handle, IReportContext context) throws Exception {
		if (!(handle instanceof OdaDataSetHandle)){
			return null;
		}
		
		PatrolQuery q = PlanPatrolQuery.createQuery();
		if (context == null){
			PlanPatrolQuery.updateQueryFilter(q, ""); //$NON-NLS-1$
		}else{
			String bits = (String) context.getParameterValue(ReportPlan.PLAN_UUID);
			if (bits == null || bits.length() == 0){
				PlanPatrolQuery.updateQueryFilter(q, ""); //$NON-NLS-1$
			}else{
				PlanPatrolQuery.updateQueryFilter(q, bits.split(",")[0]); //$NON-NLS-1$
			}
		}
		//do not close session as assume it is managed by SmartConnection is BIRT report
		Session session = HibernateManager.openSession();
		IService qs = QueryServiceFactory.generateQueryService(q);
		ArrayList<IGeoResource> toAdd = new ArrayList<IGeoResource>();
		if (qs != null) {
			q.executeQuery(new NullProgressMonitor(), session);
			List<? extends IGeoResource> resources = qs.resources(null);
			if (resources.size() > 0){
				toAdd.add(resources.get(0));
			}
		}
		return toAdd;
		
	}
	

}
