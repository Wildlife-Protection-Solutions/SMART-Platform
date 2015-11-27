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
import java.util.ArrayList;
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
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(SurveyObservationQuery.KEY)){
				return getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(SurveyWaypointQuery.KEY)){
				return getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(SurveyGriddedQuery.KEY)){
				return getGriddedQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(MissionQuery.KEY)){
				return getMissionQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
				return getMissionTrackQueryColumns(query, l, session);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}
	
	
	public QueryColumn[] getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
	
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public  QueryColumn[] getMissionQueryColumns(Query query, Locale l, Session session) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}

		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_LEADER, Locale.getDefault()));
		
		//mission property columns
		if (sd == null){
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())).list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
		}else{
			SurveyDesign sd2 = (SurveyDesign) session.load(SurveyDesign.class, sd.getUuid());
			for (MissionProperty mp : sd2.getMissionProperties()){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
		}
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
	
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public  QueryColumn[] getMissionTrackQueryColumns(Query query, Locale l, Session session) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
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
		if (sd == null){
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())).list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
		}else{
			SurveyDesign sd2 = (SurveyDesign) session.load(SurveyDesign.class, sd.getUuid());
			for (MissionProperty mp : sd2.getMissionProperties()){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
		}
				
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public QueryColumn[] getObservationQueryColumns(Query query, Locale l, Session session) throws SQLException{
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);

		// survey columns 
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
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
		if (sd == null){
			//TODO: this won't work for ccaa queries; need to merge returned rows by key
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
					.add(Restrictions.in("conservationArea.uuid",caFilter.getConservationAreaFilterIds()))
					.list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
			//TODO: same as above won't work for ccaa queries
			List<SamplingUnitAttribute> su = session.createCriteria(SamplingUnitAttribute.class)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
					.list();
			for (SamplingUnitAttribute sua : su){
				cols.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(sua), sua));
			}
		}else{
			//TODO: won't work for ccaa queries
			for (MissionProperty mp : sd.getMissionProperties()){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
			@SuppressWarnings("unchecked")
			List<SurveyDesignSamplingUnitAttribute> atts = session
				.createCriteria(SurveyDesignSamplingUnitAttribute.class)
				.add(Restrictions.eq("id.surveyDesign", sd)) //$NON-NLS-1$
				.list();
			for (SurveyDesignSamplingUnitAttribute a : atts){
				cols.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(a.getSamplingUnitAttribute()), a.getSamplingUnitAttribute()));
			}			
		}
		
		//data model columns
		for (QueryColumn q : DataModelColumnProvider.getDataModelColumns(session, l, query)){
			cols.add(q);
		}
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public QueryColumn[] getWaypointQueryColumns(Query query, Locale l, Session session) throws SQLException{
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		SurveyDesign sd = getSurveyDesign(((ISurveyQuery)query).getSurveyDesign(), session, caFilter);
		
		// survey columns 
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
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
		if (sd == null){
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())).list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
			List<SamplingUnitAttribute> su = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.list();
			for (SamplingUnitAttribute sua : su){
				cols.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(sua), sua));
			}
		}else{
			for (MissionProperty mp : sd.getMissionProperties()){
				cols.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
			List<SurveyDesignSamplingUnitAttribute> atts = session.createCriteria(SurveyDesignSamplingUnitAttribute.class)
				.add(Restrictions.eq("id.surveyDesign", sd)) //$NON-NLS-1$
				.list();
			for (SurveyDesignSamplingUnitAttribute a : atts){
				cols.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(a.getSamplingUnitAttribute()), a.getSamplingUnitAttribute()));
			}
		}
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public SurveyDesign getSurveyDesign(String key, Session s, ConservationAreaFilter caFilter){
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
