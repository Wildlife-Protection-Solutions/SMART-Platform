/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.columns;

import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.ISurveyQueryColumnProvider;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Query column implementation for survey queries.
 * 
 * @author Emily
 *
 */
public class SurveyQueryColumnProvider implements ISurveyQueryColumnProvider {
	
	private static Logger logger = Logger.getLogger(SurveyQueryColumnProvider.class.getName());
	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		List<QueryColumn> cols = null;
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(SurveyObservationQuery.KEY)){
				cols = getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(SurveyWaypointQuery.KEY)){
				cols = getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(SurveyGriddedQuery.KEY)){
				cols = getGriddedQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(MissionQuery.KEY)){
				cols = getMissionQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
				cols = getMissionTrackQueryColumns(query, l, session);
			}
			if (cols != null){
				QueryColumnUtils.filterQueryColumns(cols, query);
				return cols.toArray(new QueryColumn[cols.size()]);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}
	
	
	public List<QueryColumn> getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols;
	}
	
	private List<MissionPropertyQueryColumn> getMissionPropertyColumns(Session session, Locale l, ConservationAreaFilter caFilter, SurveyDesign sd){
		List<MissionPropertyQueryColumn> columns = new ArrayList<MissionPropertyQueryColumn>();
		if (sd == null){
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())).list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				columns.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
		}else{
			for (MissionProperty mp : sd.getMissionProperties()){
				columns.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
		}
		QueryColumnUtils.sortByName(columns, l);
		return columns;
	}
	private List<SamplingUnitAttributeQueryColumn> getSamplingUnitAttributeColumns(Session session, Locale l, ConservationAreaFilter caFilter, SurveyDesign sd){
		List<SamplingUnitAttributeQueryColumn> columns = new ArrayList<SamplingUnitAttributeQueryColumn>();
		if (sd == null){
			List<SamplingUnitAttribute> su = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.list();
			for (SamplingUnitAttribute sua : su){
				columns.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(sua), sua));
			}
		}else{
			List<SurveyDesignSamplingUnitAttribute> atts = session.createCriteria(SurveyDesignSamplingUnitAttribute.class)
				.add(Restrictions.eq("id.surveyDesign", sd)) //$NON-NLS-1$
				.list();
			for (SurveyDesignSamplingUnitAttribute a : atts){
				columns.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(a.getSamplingUnitAttribute()), a.getSamplingUnitAttribute()));
			}
		}
		QueryColumnUtils.sortByName(columns, l);
		return columns;
	}
	
	@SuppressWarnings("unchecked")
	public List<QueryColumn> getMissionQueryColumns(Query query, Locale l, Session session) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}

		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_LEADER, Locale.getDefault()));
		
		//mission property columns
		cols.addAll(getMissionPropertyColumns(session, l, caFilter, sd));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
	
		
		return cols;
	}
	
	@SuppressWarnings("unchecked")
	public  List<QueryColumn> getMissionTrackQueryColumns(Query query, Locale l, Session session) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		
		//mission property columns
		cols.addAll(getMissionPropertyColumns(session, l, caFilter, sd));
				
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		
		return cols;
	}
	
	@SuppressWarnings("unchecked")
	public List<QueryColumn> getObservationQueryColumns(Query query, Locale l, Session session) throws SQLException{
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_LEADER, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_ID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_X, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_Y, Locale.getDefault()));
		if (sd == null || sd.getTrackDistanceDirection()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE, Locale.getDefault()));
		}
		if (sd == null || sd.getTrackObserver()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT, Locale.getDefault()));
				
		//mission property columns
		cols.addAll(getMissionPropertyColumns(session, l, caFilter, sd));
		cols.addAll(getSamplingUnitAttributeColumns(session, l, caFilter, sd));
		
		//data model columns
		for (QueryColumn q : QueryColumnUtils.getDataModelColumns(session, l, AbstractQueryEngine.parseConservationAreaFilter(query))){
			cols.add(q);
		}
		
		return cols;
	}
	
	@SuppressWarnings("unchecked")
	public List<QueryColumn> getWaypointQueryColumns(Query query, Locale l, Session session) throws SQLException{
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);
		
		// survey columns 
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_ID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_X, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_Y, Locale.getDefault()));
		if (sd == null || sd.getTrackDistanceDirection()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT, Locale.getDefault()));
						
		//mission property columns
		cols.addAll(getMissionPropertyColumns(session, l, caFilter, sd));
		cols.addAll(getSamplingUnitAttributeColumns(session, l, caFilter, sd));
		
		return cols;
	}
	
	public static SurveyDesign getSurveyDesign(String key, Session s, ConservationAreaFilter caFilter){
		if (key == null) return null;
		SurveyDesign sd = (SurveyDesign)s.createCriteria(SurveyDesign.class)
			.add(Restrictions.eq("keyId", key))
			.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
			.uniqueResult();
		return sd;
		
	}
	
	
	private String getMissionPropertyColumnName(MissionAttribute ma){
		return "Mission" + "|" + ma.getName();
	}
	
	private String getSamplingUnitAttributeColumnName(SamplingUnitAttribute sua){
		return "Sampling Unit" + "|" + sua.getName();
	}
}
