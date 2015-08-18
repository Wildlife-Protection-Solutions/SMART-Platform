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
package org.wcs.smart.plan.report.oda;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.data.oda.smart.impl.QueryDatasetExtensionManager;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Plan patrols query
 * 
 * @author Emily
 *
 */
public class PlanPatrolQuery extends SmartQuery {

	private PlanPatrolParameterMetaData pMetadata;
	
	/**
	 * Creates a new smart query
	 */
	public PlanPatrolQuery(SmartPlanConnection connection) {
		super(connection);
		this.smartQuery = createQuery();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p></p>
	 */
	public void prepare(String queryText) throws OdaException {
		parameters = new HashMap<Object, Object>();
		try {
			this.wrapperObject = QueryDatasetExtensionManager.getInstance().getDatasetHandler(smartQuery.getTypeKey());
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public IParameterMetaData getParameterMetaData() throws OdaException {
		if (pMetadata == null) {
			pMetadata = new PlanPatrolParameterMetaData();
		}
		return pMetadata;
	}
	
	
	/**
	 * Execute the query
	 */
	public IResultSet executeQuery() throws OdaException {
		String[] planUuids = ((String)parameters.get(3)).split(","); //$NON-NLS-1$
		try{
			updateQueryFilter((PatrolQuery)this.smartQuery, planUuids[0]);
		}catch (Exception ex){
			throw new OdaException(ex);
		}
		
		//set query dates
		String hql = "SELECT min(startDate) from Patrol WHERE conservationArea = :ca"; //$NON-NLS-1$
		org.hibernate.Query q = connection.getSession().createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> data = q.list();
		Date startdate = null;
		if (data != null && data.size() >= 1 && data.get(0) != null) {
			startdate = (java.sql.Timestamp) data.get(0);
			super.setObject(SmartParameterMetaData.Parameter.STARTDATE.guiName,
					new java.sql.Date(startdate.getTime()));
		}
	
		// today + one day
		// add one day just to make sure we get everything
		super.setObject(SmartParameterMetaData.Parameter.ENDDATE.guiName, 
				new java.sql.Date((new Date()).getTime() + 86400000)); 
		
		this.wrapperObject.prepare(this);
		return wrapperObject.executeQuery(this, connection);
	}
	
	public static PatrolQuery createQuery() {
		PatrolQuery pq = PatrolQueryFactory.createPatrolQuery();
		pq.updateName(SmartDB.getCurrentLanguage(), Messages.MapPlanEditorPage_QueryName);
		pq.setName(Messages.MapPlanEditorPage_QueryName);
		pq.setDateFilter(new DateFilter(PatrolStartDateField.INSTANCE, AllDatesFilter.INSTANCE));
		return pq;
	}
	
	
	//do not close session as assume it is managed by SmartConnection is BIRT report
	//uses the current session which should be associated with connection
	public static void updateQueryFilter(PatrolQuery pq, String parentPlanUuid) throws Exception{
		if (parentPlanUuid == null || parentPlanUuid.length() == 0){
			pq.setQueryFilter("observation|patrol:uuid equals \"\""); //$NON-NLS-1$
			return;
		}
		
		Session session = HibernateManager.openSession();
		Plan p = (Plan)session.createCriteria(Plan.class)
				.add(Restrictions.eq("uuid",UuidUtils.stringToUuid(parentPlanUuid))).list().get(0); //$NON-NLS-1$
		
		List<String> patrols = new ArrayList<String>();
		
		
		List<Plan> plans = new ArrayList<Plan>();
		plans.add(p);
		while(plans.size() > 0){
			Plan thisplan = plans.remove(0);
			List<PatrolEditorInput> items = PlanHibernateManager.getPatrols(thisplan, session);
			for (PatrolEditorInput in : items){
				patrols.add(UuidUtils.uuidToString(in.getUuid()));
			}
			plans.addAll(thisplan.getChildren());
		}
		
		
		if (patrols.size() == 0){
			pq.setQueryFilter("observation|patrol:uuid equals \"\""); //$NON-NLS-1$
			return ;
		}
		StringBuilder sb = new StringBuilder();
		for(String patrol : patrols){
			if (sb.length() > 0){
				sb.append(" OR "); //$NON-NLS-1$
			}
			sb.append("patrol:uuid equals \"" + patrol + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		pq.setQueryFilter("observation|" + sb.toString());	 //$NON-NLS-1$
		
		return ;
	}

	public Object getParameterKey(int parameterId)  throws OdaException{
		Object x = super.getParameterKey(parameterId);
		if (x != null){
			return x;
		}
		return parameterId;
	}
	
	public Object getParameterKey(String parameterName)  throws OdaException{
		Object x = super.getParameterKey(parameterName);
		if (x != null){
			return x;
		}
		return parameterName;
	}
}