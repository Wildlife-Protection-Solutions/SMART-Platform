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
import java.util.Collection;
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
import org.wcs.smart.er.query.engine.DerbyGridEngine;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumnManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.GridQueryResultMetadata;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.summary.GridQueryDefinition;

/**
 * A class to represent a survey summary query.
 * 
 * @author Emily
 */
@Entity
@Table(name="smart.survey_gridded_query")
public class SurveyGriddedQuery extends GriddedQuery implements ISurveyQuery{
	private Object LOCK = new Object();
	
	protected String surveyDesignKey;
	protected SurveyDesign surveyDesign;
	
	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(SurveyGridQueryType.KEY);
	}
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected GridQueryDefinition parseQuery() throws Exception {
		if (strQuery == null || strQuery.length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(strQuery.getBytes());
		Parser parser = new Parser(is);
		GridQueryDefinition myQuery = parser.GridQuery();
		is.close();
		return myQuery;
	}

	@Transient
	public Collection<GridResultItem> executeQueryInternal(IProgressMonitor monitor, Session session) throws Exception{
		resultMetadata = null;
		Session lsession = session;
		if (lsession == null){
			lsession = HibernateManager.openSession();
			lsession.beginTransaction();
		}
		try{
			
			DerbyGridEngine engine = new DerbyGridEngine();
			Collection<GridResultItem> lastResults = engine.executeQuery(this, lsession, monitor);
			resultMetadata = GridQueryResultMetadata.computeMetadata(lastResults);
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
	@Override
	public SurveyGriddedQuery clone(){
		SurveyGriddedQuery q = new SurveyGriddedQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQuery(getQuery());
		q.setCrsDefinition(getCrsDefinition());
		q.setSurveyDesign(getSurveyDesign());
		return q;
	}

	/**
	 * Loads the query columns
	 */
	protected synchronized void initQueryColumns(){
		QueryColumn[] cols = SurveyQueryColumnManager.getInstance().getGridColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		for (int i = 0; i < cols.length; i ++){
			queryColumns.add(cols[i]);
		}
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
		synchronized (LOCK) {
			this.queryColumns = null;	
		}
		
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
