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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.query.common.model.IQueryColumnProvider;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * Mission track query object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.survey_mission_track_query")
public class MissionTrackQuery extends SimpleQuery implements IPagedQuery, ISurveyQuery{

	protected String surveyDesignKey;
	
	public static final String KEY = "surveymissiontrack"; //$NON-NLS-1$
	
	/**
	 * Creates a new patrol query with the default
	 * conservation area filter and no date filter
	 */
	protected MissionTrackQuery(){
		super();
	}
	

	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public MissionTrackQuery clone(Employee newEmployee){
		MissionTrackQuery q = new MissionTrackQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newEmployee);
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		q.setSurveyDesign(getSurveyDesign());
		q.setStyle(getStyle());
		return q;
	}



	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}
	
	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		try(InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes())){
			Parser parser = new Parser(is);
			QueryFilter myQuery = new QueryFilter(parser.ExpressionPart());
			queryFilter = myQuery;
			return myQuery;
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
		if ((this.surveyDesignKey != null && this.surveyDesignKey.equals(key)) ||
			(surveyDesignKey == null && key == null)  ){
			//nothing has changed so we don't want to clear information
			return;
		}
		this.surveyDesignKey = key;
//		synchronized (this) {
//			this.queryColumns = null;	
//		}
		
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
		}
	}

	@Override
	@Transient
	protected Class<? extends IQueryColumnProvider> getColumnProviderClass() {
		return ISurveyQueryColumnProvider.class;
	}
}

