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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.parser.filter.EmptyFilter;
import org.wcs.smart.query.parser.filter.Operator;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * "Patrol is a part of a plan" Query Filter
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolPlanQueryFilter extends EmptyFilter {

	private IPatrolQueryOption option;
	private Operator op;	
	private Object value;

	public PatrolPlanQueryFilter(IPatrolQueryOption option, Operator op, Object value) {
		this.option = option;
		this.op = op;
		this.value = value;
	}

	@Override
	public String asString() {
		return "patrol:" + option.getKey() + " " + op.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String prefix = tableMapping.get(option.getPatrolAttributeClass());
		String v = SmartUtils.stripQuotes((String)value);
		//if v is empty this means that this is "Any Plan" case
		String planSqlPart = ""; //$NON-NLS-1$
		if (!isAnyPlan(v)) {
			LoadChildPlanIdJob job = new LoadChildPlanIdJob(v);
			job.schedule();
			//This is required as we do NOT want to perform this in current thread in order not to effect its session.
	    	try {
	    		job.join(); //we don't want to proceed until job is finished
			} catch (InterruptedException e) {
				SmartPlanPlugIn.displayLog(Messages.PatrolPlanQueryFilter_FetchPlanChilder_Interrupted, e);
			}
			List<byte[]> planIds = job.getResultData();
			StringBuilder planSql = new StringBuilder("AND pa2pl.plan_uuid in (x'"+v+"'"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < planIds.size(); i++) {
				planSql.append(",x'"); //$NON-NLS-1$
				planSql.append(SmartUtils.encodeHex(planIds.get(i)));
				planSql.append("'"); //$NON-NLS-1$
			}
			planSql.append(")"); //$NON-NLS-1$
			planSqlPart = planSql.toString();
		}
		String sql = "EXISTS (SELECT * FROM smart.patrol_plan pa2pl WHERE pa2pl.patrol_uuid = "+prefix+".uuid "+planSqlPart+")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		DropItem it = DropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		String id = SmartUtils.stripQuotes((String)value);
		ListItem listItem = isAnyPlan(id) ? PlanPatrolQueryOption.ANY_PATROL_ITEM : PlanHibernateManager.getPlan(session, id);
		it.initializeData(listItem);
		return new DropItem[]{it};
	}

	private boolean isAnyPlan(String v) {
		return v == null || v.isEmpty();
	}
	
	/**
	 * Inner class responsible for wrapping fetching of child plan id operation into {@link Job}
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LoadChildPlanIdJob extends Job {
		
		private String id;
		private List<byte[]> planIds = new ArrayList<byte[]>();
		
		public LoadChildPlanIdJob(String id) {
			super(Messages.PatrolPlanQueryFilter_FetchPlanChilder_JobTitle);
			this.id = id;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			planIds = PlanHibernateManager.getChildPlanIds(id);
			return Status.OK_STATUS;
		}
		
		public List<byte[]> getResultData() {
			return planIds;
		}

	}
	
}
