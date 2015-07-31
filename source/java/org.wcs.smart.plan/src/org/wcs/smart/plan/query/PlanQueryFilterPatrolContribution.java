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
package org.wcs.smart.plan.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IExtensionFilter;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Plan contribution for the Patrol section of a "Query Filter" view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PlanQueryFilterPatrolContribution implements IQueryFilterPatrolContribution {

	private PatrolPlanOption planOption = new PatrolPlanOption(new PlanPatrolQueryOption());
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution#getOptions()
	 * @return single option for filtering on intelligence
	 */
	@Override
	public List<IExtensionOption> getOptions() {
		List<IExtensionOption> ops = new ArrayList<IExtensionOption>();
		ops.add(planOption);
		return ops;
	}

	@Override
	public IExtensionFilter createFilter(String key) {
		return null;
	}

	@Override
	public IExtensionFilter createFilter(String key, Operator op, Object value) {
		String fullKey = "patrol:" + planOption.getOption().getKey(); //$NON-NLS-1$
		if (fullKey.equals(key)) {
			return new PatrolPlanQueryFilter(planOption.getOption(), op, value);
		}
		return null;
	}

	
	@Override
	public String asSql(IQueryEngine engine, IFilter filter){
		if (!(filter instanceof PatrolPlanQueryFilter)){
			return null;
		}
		PatrolPlanQueryFilter qfilter = (PatrolPlanQueryFilter)filter;
		
		String prefix = engine.tablePrefix(qfilter.getOption().getPatrolAttributeClass());
		String v = SharedUtils.stripQuotes((String)qfilter.getValue());
		//if v is empty this means that this is "Any Plan" case
		String planSqlPart = ""; //$NON-NLS-1$
		if (!qfilter.isAnyPlan(v)) {
			LoadChildPlanIdJob job = new LoadChildPlanIdJob(v);
			job.schedule();
			//This is required as we do NOT want to perform this in current thread in order not to effect its session.
	    	try {
	    		job.join(); //we don't want to proceed until job is finished
			} catch (InterruptedException e) {
				SmartPlanPlugIn.displayLog(Messages.PatrolPlanQueryFilter_FetchPlanChilder_Interrupted, e);
			}
			List<UUID> planIds = job.getResultData();
			StringBuilder planSql = new StringBuilder("AND pa2pl.plan_uuid in (x'"+v+"'"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < planIds.size(); i++) {
				planSql.append(",x'"); //$NON-NLS-1$
				planSql.append(UuidUtils.uuidToString(planIds.get(i)));
				planSql.append("'"); //$NON-NLS-1$
			}
			planSql.append(")"); //$NON-NLS-1$
			planSqlPart = planSql.toString();
		}
		String sql = "EXISTS (SELECT * FROM smart.patrol_plan pa2pl WHERE pa2pl.patrol_uuid = "+prefix+".uuid "+planSqlPart+")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}
	
	/**
	 * Inner class responsible for wrapping fetching of child plan id operation into {@link Job}
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LoadChildPlanIdJob extends Job {
		
		private String id;
		private List<UUID> planIds = new ArrayList<UUID>();
		
		public LoadChildPlanIdJob(String id) {
			super(Messages.PatrolPlanQueryFilter_FetchPlanChilder_JobTitle);
			this.id = id;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			planIds = PlanHibernateManager.getChildPlanIds(id);
			return Status.OK_STATUS;
		}
		
		public List<UUID> getResultData() {
			return planIds;
		}

	}
}
