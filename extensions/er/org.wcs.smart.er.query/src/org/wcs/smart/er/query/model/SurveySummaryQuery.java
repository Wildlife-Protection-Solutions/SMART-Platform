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
package org.wcs.smart.er.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.engine.DerbySummaryEngine;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.survey_summary_query")
public class SurveySummaryQuery extends SummaryQuery implements ISurveyQuery{

	protected String surveyDesignKey;
	
	protected SurveyDesign surveyDesign;
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected SumQueryDefinition parseQuery() throws Exception {
		if (getQuery() == null || getQuery().length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(getQuery().getBytes());
		Parser parser = new Parser(is);
		SumQueryDefinition myQuery = parser.SumQuery();
		is.close();
		return myQuery;
	}


	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(SurveySummaryQueryType.KEY);
	}
	
	@Transient
	public SummaryQueryResult executeQueryInternal(IProgressMonitor monitor, Session session) throws Exception{
		Session lsession = session;
		if (session == null){
			lsession = HibernateManager.openSession();
			lsession.beginTransaction();
		}
		try{
			DerbySummaryEngine engine = new DerbySummaryEngine();
			SummaryQueryResult lastResults = engine.executeQuery(this, lsession, monitor);
			return lastResults;
		}finally{
			if (session == null && lsession.isOpen()){
				lsession.getTransaction().commit();
				lsession.close();
			}
		}
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public SurveySummaryQuery clone(){
		SurveySummaryQuery q = SurveyQueryFactory.createSummaryQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQuery(getQuery());
		q.setSurveyDesign(getSurveyDesign());
		return q;
	}
	
	/**
	 * Validates the query parts.  Assumes the query
	 * definition is formed from a valid string.
	 * <p>
	 * This validates the items in the query.
	 * </p>
	 * 
	 * @param def the summary query definition
	 * @return error string or null if query validates okay
	 */
	public static String validateQueryParts(SumQueryDefinition def){
		List<IGroupBy> groupBys = new ArrayList<IGroupBy>();
		if (def.getRowGroupByPart() != null){
			groupBys.addAll(def.getRowGroupByPart().getGroupBys());
		}
		if (def.getColumnGroupByPart().getGroupBys() != null){
			groupBys.addAll(def.getColumnGroupByPart().getGroupBys());
		}
		
//		for (IValueItem valueIt : def.getValuePart().getValueItems()){
//			if (valueIt instanceof PatrolValueItem){
//				PatrolValueItem pIt = (PatrolValueItem) valueIt;
//				if (pIt.getOption() == PatrolValueOption.NUM_NIGHTS){
//					//cannot group by patrol leader, patrol memeber, time period, or transport
//					for (IGroupBy groupBy : groupBys){
//						if (groupBy instanceof CategoryGroupBy ){
//							return MessageFormat.format(
//									Messages.SummaryQuery_CannotGroupByCategory, new Object[]{pIt.getOption().getGuiName()});
//									
//						}else if (groupBy instanceof AttributeGroupBy){
//							return MessageFormat.format(
//									Messages.SummaryQuery_CannotGroupByAttribute, new Object[]{pIt.getOption().getGuiName()});
//						}
//					}
//				}else if (pIt.getOption() == PatrolValueOption.MAN_DAYS ||
//						pIt.getOption() == PatrolValueOption.MAN_HOURS || 
//						pIt.getOption() == PatrolValueOption.NUM_MEMBERS ){
//					
//					for (IGroupBy groupBy : groupBys){
//						if (groupBy instanceof PatrolGroupBy){
//							if (((PatrolGroupBy)groupBy).getOption() == PatrolQueryOption.EMPLOYEE){
//								return MessageFormat.format(
//										Messages.SummaryQuery_GroupByError3 , new Object[]{pIt.getOption().getGuiName(), ((PatrolGroupBy)groupBy).getOption().getGuiName()});
//							}
//						}
//					}
//				}
//			}
//		}
		return null;
	}


	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setSurveyDesign(String key){
		this.surveyDesignKey = key;
		this.surveyDesign = null;		
	}
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "surveydesign_key")
	public String getSurveyDesign(){
		return this.surveyDesignKey;
	}
	
	@Transient
	public void setSurveyDesign(SurveyDesign design){
		if (design == null){
			setSurveyDesign((String)null);
		}else{
			setSurveyDesign(design.getKeyId());
			this.surveyDesign = design;
		}
	}
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	@Transient
	public SurveyDesign getSurveyDesignAsObject(){
		if (surveyDesign == null){
			synchronized (this) {
				if (surveyDesign == null){
					if (surveyDesignKey == null) return null;
					
					Job j = new Job(Messages.SurveyObservationQuery_loadingDesignJobName){

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							Session s = HibernateManager.openSession();
							List<?> results = s.createCriteria(SurveyDesign.class)
								.add(Restrictions.eq("keyId", surveyDesignKey)) //$NON-NLS-1$
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
							if (results.size() > 0){
								surveyDesign = (SurveyDesign) results.get(0);
							}
							return Status.OK_STATUS;
						}};
					j.setSystem(true);
					j.schedule();
					try {
						j.join();
					} catch (InterruptedException e) {
						ERQueryPlugIn.log(e.getMessage(), e);
					}
				}
				
			}
		}
		return surveyDesign;
	}
	
}
