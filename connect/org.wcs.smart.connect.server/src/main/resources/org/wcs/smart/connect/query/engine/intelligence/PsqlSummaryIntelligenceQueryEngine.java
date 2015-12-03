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
package org.wcs.smart.connect.query.engine.intelligence;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Runs intelligence summary query.
 * @author Emily
 *
 */
public class PsqlSummaryIntelligenceQueryEngine implements IQueryEngine {

	private static Logger logger = Logger.getLogger(PsqlSummaryIntelligenceQueryEngine.class.getName());
	
	@Override
	public boolean canExecute(String querytype) {
		return IntelligenceSummaryQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		final IntelligenceSummaryQuery query = (IntelligenceSummaryQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		try{
			
			Date[] d = query.getDateFilter().getDateFilterOption().getDates();
			ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT count(*) from Intelligence i");  //$NON-NLS-1$
			sb.append(" WHERE i.conservationArea.uuid IN (:ca) "); //$NON-NLS-1$
			if (d != null){
				sb.append(" and i.receivedDate >= :d1 and i.receivedDate <= :d2 "); //$NON-NLS-1$
			}
			sb.append("AND i in (select id.intelligence FROM PatrolIntelligence)"); //$NON-NLS-1$
			org.hibernate.Query q = session.createQuery(sb.toString());
			q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			if (d != null){
				q.setParameter("d1",d[0]); //$NON-NLS-1$
				q.setParameter("d2",d[1]); //$NON-NLS-1$
			}
			Long followedUpOn = (Long)q.uniqueResult(); 
		
		
			sb = new StringBuilder();
			sb.append("SELECT count(*) from Intelligence i"); //$NON-NLS-1$ 
			sb.append(" WHERE i.conservationArea.uuid IN (:ca) "); //$NON-NLS-1$
			if (d != null){
				sb.append(" and i.receivedDate >= :d1 and i.receivedDate <= :d2 "); //$NON-NLS-1$
			}
			sb.append("AND i not in (select id.intelligence FROM PatrolIntelligence)"); //$NON-NLS-1$
			q = session.createQuery(sb.toString());
			q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			if (d != null){
				q.setParameter("d1",d[0]); //$NON-NLS-1$
				q.setParameter("d2",d[1]); //$NON-NLS-1$
			}
			Long notFollowedUpOn = (Long) q.uniqueResult(); 
		
			SummaryQueryResult results = createResultTemplate();
			
			HashMap<SummaryResultKey, Double> data = new HashMap<SummaryResultKey, Double>();
		
			SummaryResultKey key = new SummaryResultKey(IntelligenceSummaryQuery.NUMBER_KEY, new String[]{IntelligenceSummaryQuery.FOLLOW_KEY}); 
			data.put(key, followedUpOn.doubleValue());
		
			key = new SummaryResultKey(IntelligenceSummaryQuery.NUMBER_KEY, new String[]{IntelligenceSummaryQuery.NOT_FOLLOW_KEY}); 
			data.put(key, notFollowedUpOn.doubleValue());
		
			results.setData(data);

			return results;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SQLException(ex);
		}
	}

	@Override
	public String tableNamePrefix(Class<?> clazz) {
		return null;
	}

	@Override
	public String tablePrefix(Class<?> clazz) {
		return null;
	}

	@Override
	public String tableName(Class<?> clazz) {
		return null;
	}

	@Override
	public String addParameterValue(Object parameter) {
		return null;
	}
	

	/**
	 * Creates the template for the results.  These queries
	 * have on value (Number of Intelligence) grouped into either
	 * Followed Up or Not Followed Up.
	 * 
	 * @return
	 */
	public static SummaryQueryResult createResultTemplate(){
		SummaryQueryResult results = new SummaryQueryResult();
		
		results.addValueHeader(
				new SummaryHeader("Number of Intelligence Records", "Number of Intelligence Records", IntelligenceSummaryQuery.NUMBER_KEY, true));
		
		results.addRowHeader(
				new SummaryHeader[]{new SummaryHeader("Followed Up", "Followed Up", IntelligenceSummaryQuery.FOLLOW_KEY, false), 
				new SummaryHeader("Not Followed Up", "Not Followed Up", IntelligenceSummaryQuery.NOT_FOLLOW_KEY, false)}); 
	
		return results;
	}
}
