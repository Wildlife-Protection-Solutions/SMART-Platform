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
package org.wcs.smart.report.birt.map;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.map.udig.QueryServiceFactory;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.ObservationQuery;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.parser.PatrolQueryOptions.DATE_FILTER_OP;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.report.SmartReportParameters;
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
			QueryType qt = QueryType.valueOf(queryText.split(":")[0]); //$NON-NLS-1$
			if (qt != QueryType.SUMMARY){
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
		QueryType qtype = QueryType.valueOf(queryText.split(":")[0]); //$NON-NLS-1$
		
		Query q = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			q = QueryHibernateManager.getInstance().findQuery(session,quuid, qtype);
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		
		IService qs = QueryServiceFactory.generateQueryService(q);
		if (context == null){
			List<? extends IGeoResource> resources = qs.resources(null);
			ArrayList<IGeoResource> thisresources = new ArrayList<IGeoResource>();
			thisresources.addAll(resources);
			return thisresources;
		}
		DateFilter dateFilter = new DateFilter(
				DateFilter.DATE_FIELD_OP.WAYPOINT, DATE_FILTER_OP.CUSTOM,
				(Date) context.getParameterValue(SmartReportParameters.PARAM_START_DATE_KEY),
				(Date) context.getParameterValue(SmartReportParameters.PARAM_END_DATE_KEY));
			ArrayList<IGeoResource> toAdd = new ArrayList<IGeoResource>();
		if (qs != null) {
			boolean add = true;
			if (Query.class.isAssignableFrom( q.getClass() )){
				q.setDateFilter(dateFilter);
			}
			if (q instanceof ObservationQuery) {
				((ObservationQuery) q).setDateFilter(dateFilter);
				((ObservationQuery) q).getPagedQueryResults(new NullProgressMonitor());
			} else if (q instanceof PatrolQuery) {
				((PatrolQuery) q).setDateFilter(dateFilter);
				((PatrolQuery) q).getQueryResults(new NullProgressMonitor());
			} else if (q instanceof GriddedQuery ){
				((GriddedQuery)q).setDateFilter(dateFilter);
				Collection<GridResultItem> data = ((GriddedQuery) q).getQueryResults(new NullProgressMonitor());
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
