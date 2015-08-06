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

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.patrol.query.parser.IExtensionFilter;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
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

	private PlanPatrolQueryOption option = new PlanPatrolQueryOption();

	@Override
	public IExtensionFilter createFilter(String key) {
		return null;
	}

	@Override
	public IExtensionFilter createFilter(String key, Operator op, Object value) {
		String fullKey = "patrol:" + option.getKey(); //$NON-NLS-1$
		if (fullKey.equals(key)) {
			return new PatrolPlanQueryFilter(option, op, value);
		}
		return null;
	}

	
	@Override
	public String asSql(IQueryEngine engine, Session session, IFilter filter){
		if (!(filter instanceof PatrolPlanQueryFilter)){
			return null;
		}
		PatrolPlanQueryFilter qfilter = (PatrolPlanQueryFilter)filter;
		
		String prefix = engine.tablePrefix(qfilter.getOption().getPatrolAttributeClass());
		String v = SharedUtils.stripQuotes((String)qfilter.getValue());
		//if v is empty this means that this is "Any Plan" case
		String planSqlPart = ""; //$NON-NLS-1$
		if (!qfilter.isAnyPlan(v)) {
			List<UUID> planIds = getChildPlanIds(v, session);
			
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
	 * Returns the list of plan uuid that are children for given plan uuid
	 * @param planUuid
	 * @return
	 */
	public static List<UUID> getChildPlanIds(String planUuid, Session session) {
		UUID uuid = UuidUtils.stringToUuid(planUuid);
		return listChildPlanIds(uuid, session);
	}
	
	/**
	 * Returns the list of plan uuid that are children for given plan uuid
	 * @param planUuid
	 * @param session
	 * @return
	 */
	private static List<UUID> listChildPlanIds(UUID planUuid, Session session) {
		List<UUID> ids = new ArrayList<UUID>();
		Query query = session.createQuery("SELECT p.uuid FROM Plan p where p.parent.uuid = :uuid"); //$NON-NLS-1$
		query.setParameter("uuid", planUuid); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<UUID> list = query.list();
		ids.addAll(list);
		for (UUID uuid : list) {
			ids.addAll(listChildPlanIds(uuid, session));
		}
		return ids;
	}
}
