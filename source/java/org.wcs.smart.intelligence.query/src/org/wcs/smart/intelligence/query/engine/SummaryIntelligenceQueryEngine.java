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
package org.wcs.smart.intelligence.query.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryResultKey;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Runs intelligence summary query.
 * @author Emily
 *
 */
public class SummaryIntelligenceQueryEngine  {

	private static final String FOLLOW_KEY = "follow"; //$NON-NLS-1$
	private static final String NOT_FOLLOW_KEY = "notfollow"; //$NON-NLS-1$
	private static final String NUMBER_KEY = "intellcnt"; //$NON-NLS-1$
	
	
	public SummaryQueryResult executeQuery( final IntelligenceSummaryQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {
		try{
			
			Date[] d = query.getDateFilter().getDateFilterOption().getDates();
			
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT count(*) from Intelligence i");  //$NON-NLS-1$
			sb.append(" WHERE i.conservationArea IN (:ca) "); //$NON-NLS-1$
			if (d != null){
				sb.append(" and i.receivedDate >= :d1 and i.receivedDate <= :d2 "); //$NON-NLS-1$
			}
			sb.append("AND i in (select id.intelligence FROM PatrolIntelligence)"); //$NON-NLS-1$
			Query q = session.createQuery(sb.toString());
			q.setParameterList("ca", asList(query.getConservationAreaFilterAsFilter())); //$NON-NLS-1$
			if (d != null){
				q.setParameter("d1",d[0]); //$NON-NLS-1$
				q.setParameter("d2",d[1]); //$NON-NLS-1$
			}
			Long followedUpOn = (Long)q.uniqueResult(); 
		
		
			sb = new StringBuilder();
			sb.append("SELECT count(*) from Intelligence i"); //$NON-NLS-1$ 
			sb.append(" WHERE i.conservationArea IN (:ca) "); //$NON-NLS-1$
			if (d != null){
				sb.append(" and i.receivedDate >= :d1 and i.receivedDate <= :d2 "); //$NON-NLS-1$
			}
			sb.append("AND i not in (select id.intelligence FROM PatrolIntelligence)"); //$NON-NLS-1$
			q = session.createQuery(sb.toString());
			q.setParameterList("ca", asList(query.getConservationAreaFilterAsFilter())); //$NON-NLS-1$
			if (d != null){
				q.setParameter("d1",d[0]); //$NON-NLS-1$
				q.setParameter("d2",d[1]); //$NON-NLS-1$
			}
			Long notFollowedUpOn = (Long) q.uniqueResult(); 
		
			SummaryQueryResult results = createResultTemplate();
			
			HashMap<SummaryResultKey, Double> data = new HashMap<SummaryResultKey, Double>();
		
			SummaryResultKey key = new SummaryResultKey(NUMBER_KEY, new String[]{FOLLOW_KEY}); 
			data.put(key, followedUpOn.doubleValue());
		
			key = new SummaryResultKey(NUMBER_KEY, new String[]{NOT_FOLLOW_KEY}); 
			data.put(key, notFollowedUpOn.doubleValue());
		
			results.setData(data);

			return results;
		}catch (Exception ex){
			IntelligenceQueryPlugIn.log("Error running summary query.", ex); //$NON-NLS-1$
			throw new SQLException(ex);
		}
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
				new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_NumberRecordShortName, Messages.SummaryIntelligenceQueryEngine_NumberRecordLongName, NUMBER_KEY, true));
		
		results.addRowHeader(
				new SummaryHeader[]{new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_FollowedUpHeaderLongName, Messages.SummaryIntelligenceQueryEngine_FollowedUpHeaderShortName, FOLLOW_KEY, false), 
				new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_NotFollowedUpHeaderLongName, Messages.SummaryIntelligenceQueryEngine_NoFollowedUpHeaderShortName, NOT_FOLLOW_KEY, false)}); 

		return results;
	}
	
	/**
	 * Converts conservation area filter ot list of
	 * conservation areas for query.
	 * 
	 * @param filter
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<ConservationArea> asList(ConservationAreaFilter filter) throws SQLException{
		ArrayList<ConservationArea> localFilters = new ArrayList<ConservationArea>();
		if (filter.includeAll()){
			//include all current conservation areas
			if (SmartDB.getConservationAreaConfiguration() != null){
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					localFilters.add(ca);
				}
			}else{
				localFilters.add(SmartDB.getCurrentConservationArea());
			}
		}else{
			//include only selected conservation areas
			for (byte[] ca : filter.getConservationAreaFilterIds()){
				ConservationArea cca = new ConservationArea();
				cca.setUuid(ca);
				localFilters.add(cca);
			}
		}
		return localFilters;
	}
}
