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
package org.wcs.smart.entity.query.report.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.entity.query.map.udig.QueryServiceFactory;
import org.wcs.smart.entity.query.model.type.EntityGridQueryType;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.model.type.EntityWaypointQueryType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.util.SmartUtils;

/**
 * SMART Query Map Layer
 * 
 * @author Emily
 *
 */
public class QueryMapLayer implements IBirtMapLayerManager {

	private static final String SMART_QUERY_ID = "org.wcs.smart.data.oda.smart.smartQueryDataset"; //$NON-NLS-1$
	
	public QueryMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(SMART_QUERY_ID)) {
			String queryText = odaHandle.getQueryText();
			String queryTypeKey = queryText.split(":")[0]; //$NON-NLS-1$
			if (queryTypeKey.equals(EntityGridQueryType.KEY) ||
					queryTypeKey.equals(EntityObservationQueryType.KEY) ||
					queryTypeKey.equals(EntityWaypointQueryType.KEY)){
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
		
		String queryText = ((OdaDataSetHandle)handle).getQueryText();
		byte[] quuid = SmartUtils.decodeHex(queryText.split(":")[1]); //$NON-NLS-1$
		
		String queryType = queryText.split(":")[0]; //$NON-NLS-1$
		IQueryType qtype = QueryTypeManager.getInstance().findQueryType(queryType);
		if (qtype == null){
			return null;
		}
		/* for historic support */
		Query q = null;

		//do not close session as assume it is managed by SmartConnection is BIRT report
		Session session = HibernateManager.openSession();
		q = QueryHibernateManager.getInstance().findQuery(session,quuid, qtype);

		if (q == null){
			return null;
		}
		IService qs = QueryServiceFactory.generateQueryService(q);
		if (context == null){
			List<? extends IGeoResource> resources = qs.resources(null);
			ArrayList<IGeoResource> thisresources = new ArrayList<IGeoResource>();
			thisresources.addAll(resources);
			return thisresources;
		}
		CustomDateFilter cd = new CustomDateFilter();
		cd.setDates((Date) context.getParameterValue(SmartReportParameters.PARAM_START_DATE_KEY),
				(Date) context.getParameterValue(SmartReportParameters.PARAM_END_DATE_KEY));
		DateFilter dateFilter = new DateFilter(
				WaypointDateField.INSTANCE,cd);

		ArrayList<IGeoResource> toAdd = new ArrayList<IGeoResource>();
		if (qs != null) {
			boolean add = true;
			if (Query.class.isAssignableFrom( q.getClass() )){
				q.setDateFilter(dateFilter);
			}
			if (q instanceof SimpleQuery) {
				((SimpleQuery) q).setDateFilter(dateFilter);
				q.executeQuery(new NullProgressMonitor(), session);
			} else if (q instanceof GriddedQuery ){
				((GriddedQuery)q).setDateFilter(dateFilter);
				Collection<GridResultItem> data = (Collection<GridResultItem>) q.executeQuery(new NullProgressMonitor(), session);
				if (data.size() <= 0){
					add = false;
				}
			}
			if (add){
				List<? extends IGeoResource> resources = qs.resources(null);
				if (resources.size() > 0){
					toAdd.add(resources.get(0));
				}
			}						
		
		}
		return toAdd;
		
	}

}
