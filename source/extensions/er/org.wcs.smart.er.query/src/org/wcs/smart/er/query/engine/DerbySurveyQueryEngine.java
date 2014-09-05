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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DerbySurveyQueryEngine extends AbstractQueryEngine {
	
	protected HashMap<IFilter, String> filterTables = new HashMap<IFilter, String>();
	
	static {
		tablePrefix.put(SurveyDesign.class, "sd"); //$NON-NLS-1$
		tablePrefix.put(Survey.class, "s"); //$NON-NLS-1$
		tablePrefix.put(Mission.class, "m"); //$NON-NLS-1$
		tablePrefix.put(MissionAttribute.class, "ma"); //$NON-NLS-1$
		tablePrefix.put(MissionAttributeListItem.class, "mali"); //$NON-NLS-1$
		tablePrefix.put(MissionProperty.class, "mp"); //$NON-NLS-1$
		tablePrefix.put(MissionTrack.class, "t"); //$NON-NLS-1$
		tablePrefix.put(MissionMember.class, "mm"); //$NON-NLS-1$
		tablePrefix.put(MissionPropertyValue.class, "mpv"); //$NON-NLS-1$
		tablePrefix.put(SurveyWaypoint.class, "sw"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnit.class, "su"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttribute.class, "sua"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttributeValue.class, "suav"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(SurveyDesign.class, "smart.survey_design"); //$NON-NLS-1$
		tableNames.put(Survey.class, "smart.survey"); //$NON-NLS-1$
		tableNames.put(Mission.class, "smart.mission"); //$NON-NLS-1$
		tableNames.put(MissionAttribute.class, "smart.mission_attribute"); //$NON-NLS-1$
		tableNames.put(MissionAttributeListItem.class, "smart.mission_attribute_list"); //$NON-NLS-1$
		tableNames.put(MissionProperty.class, "smart.mission_property"); //$NON-NLS-1$
		tableNames.put(MissionTrack.class, "smart.mission_track"); //$NON-NLS-1$
		tableNames.put(MissionMember.class, "smart.mission_member"); //$NON-NLS-1$
		tableNames.put(MissionPropertyValue.class, "smart.mission_property_value"); //$NON-NLS-1$
		tableNames.put(SurveyWaypoint.class, "smart.survey_waypoint"); //$NON-NLS-1$
		tableNames.put(SamplingUnit.class, "smart.sampling_unit"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttribute.class, "smart.sampling_unit_attribute"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttributeValue.class, "smart.sampling_unit_attribute_value"); //$NON-NLS-1$
	}

	/**
	 * Create the select statement to populate the temporary table
	 * containing observation data for the query engine.
	 * 
	 * @param includeObservations if observation information should be included
	 * in the output table (ob_uuid).
	 * 
	 * @return
	 */
	protected abstract String getTemporaryTableSelectClause(boolean includeObservations);
	
	/**
	 * Converts the a row in the temporary table select clause to
	 * a result item
	 * @param rs result set item to convert to the queryresultitem
	 * @param session current database connection
	 * @return
	 * @throws SQLException
	 */
	protected abstract IResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException;
	
	/**
	 * Create the temporary table for hold observation data
	 * for querying
	 * 
	 * @param tableName temporary table name
	 * @return 
	 */
	protected abstract String getTemporaryTableCreateClause(String tableName);
	
	/**
	 * A string to append to the from clause of the select
	 * statement to create the temporary table.
	 * <p>Depending on the select clause additional tables may
	 * be required.  See {@link DerbySurveyQueryEngine#getTemporaryTableCreateClause(String)}. </p> 
	 * @param tables List of tables already included in the from clause
	 * @return
	 */
	protected String appendFromClause(HashSet<Class<?>> tables){
		return ""; //$NON-NLS-1$
	}
	
	
	/**
	 * By default creates an index on the ob_uuid field.  This method can be overwritten to 
	 * create additional indexes.
	 * 
	 * @param c database connection
	 * @param tableName temporary table to create indexes on
	 * @throws SQLException
	 */
	protected void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
	
	}
	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	protected IFilterProcessor getFilterProcessor(IFilter.FilterType filterType, 
			String queryDataTable,
			SurveyDesignFilter designFilter){
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, designFilter);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, designFilter);
		}
	}
}
