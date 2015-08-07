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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.engine.ISurveyQueryMissionResult;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.query.common.model.WaypointQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * Survey observation query.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.survey_waypoint_query")
public class SurveyWaypointQuery extends WaypointQuery implements ISurveyQuery{

	protected String surveyDesignKey;
	
	private Object LOCK = new Object();
	public static final String KEY = "surveywaypoint"; //$NON-NLS-1$
	
	@Transient
	public List<byte[]> getMissions() throws Exception{
		if (getCachedResults() == null) return Collections.emptyList();
		return ((ISurveyQueryMissionResult) getCachedResults()).getMissionUuids();
	}
	
	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	@Override
	public SurveyWaypointQuery clone(Employee newEmployee){
		SurveyWaypointQuery q = new SurveyWaypointQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newEmployee);
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		q.setStyle(getStyle());
		q.setSurveyDesign(getSurveyDesign());
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
	protected void initQueryColumns() {
		synchronized (LOCK) {
			if (this.queryColumns != null) return;
			ArrayList<QueryColumn> temp = new ArrayList<QueryColumn>();
			QueryColumn[] cols = SmartContext.INSTANCE.getClass(ISurveyQueryColumnProvider.class).getQueryColumns(this);
			for (QueryColumn q : cols){
				temp.add(q);
			}	
			this.queryColumns = temp; 
		}
		
		HashSet<String> visible = null;
		if (visibleColumns != null){
			String[] bits = visibleColumns.split(","); //$NON-NLS-1$
			visible = new HashSet<String>();
			for (int i = 0; i < bits.length; i ++){
				visible.add(bits[i]);
			}
		}
		for (QueryColumn q : queryColumns){
			if (visible == null || visible.contains(q.getKey())){
				q.setVisible(true);
			}else{
				q.setVisible(false);
			}
		}
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
			QueryFilter myQuery = parser.QueryFilter();
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
		}
	}

}