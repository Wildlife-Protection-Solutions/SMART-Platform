/*
' * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.query.engine.ObservationQueryResult;
import org.wcs.smart.er.query.model.SurveyObservationAttachmentResultItem;
import org.wcs.smart.er.query.model.SurveyObservationResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;

/**
 * Survey observation query results.
 * 
 * @author Emily
 *
 */
public class ErObservationQueryResult extends ObservationQueryResult<SurveyObservationResultItem> implements IPagedImageResultSet {

	
	public ErObservationQueryResult(PsqlErObservationEngine engine, int itemcnt, boolean includeUuids){
		super(engine, itemcnt, includeUuids);;
	}

	/**
	 * Gets results from the given result set.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	@Override
	public List<SurveyObservationResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {

		List<SurveyObservationResultItem> items = super.getResults(session, rs, from, pageSize);
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				ErSurveyQueryResultSet.attachMissionProperties(items, c, session);
				ErSurveyQueryResultSet.attachSamplingUnitAttributes(items, c, session);
			}
			
		});
		return items;
	}
	

	
	@Override
	public String getDefaultSortBy() {
		StringBuilder sb = new StringBuilder();
		sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION_START.getKey()));
		sb.append(" DESC, "); //$NON-NLS-1$
		sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION.getKey()));
		sb.append(","); //$NON-NLS-1$
		sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
		sb.append(" DESC "); //$NON-NLS-1$
		return sb.toString();
	}

	
	@Override
	protected SurveyObservationAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyObservationAttachmentResultItem item = new SurveyObservationAttachmentResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	@Override
	protected SurveyObservationResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		SurveyObservationResultItem item = new SurveyObservationResultItem();
		setFields(item, rs);
		return item;
	}
	
	@Override
	protected void setFields(SurveyObservationResultItem it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		
		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_enddate").toLocalDate()); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_startdate").toLocalDate()); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
	
		it.setSamplingUnitUuid((UUID)rs.getObject("samplingunit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
	}
	

	@Override
	public String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
				"ca_id","ca_name","mission_uuid","mission_enddate", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"mission_id","mission_startdate","mission_leader", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"surveydesign_name","surveydesign_enddate", //$NON-NLS-1$ //$NON-NLS-2$
				"surveydesign_startdate","survey_id","survey_enddate", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"survey_startdate","samplingunit_uuid","samplingunit_id",  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				"wp_uuid","wp_id","wp_x","wp_y","wp_time","wp_direction", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				"wp_distance","wp_comment","wp_lastmodified","wp_lastmodifiedbyname" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		};
		for (String s : selectFields) {
			sb.append(prefix);
			sb.append(s);
			sb.append(","); //$NON-NLS-1$
		}
		
		if (includeObservation) {
			sb.append(prefix);
			sb.append("ob_observer,"); //$NON-NLS-1$
			sb.append(prefix);
			sb.append("ob_uuid,"); //$NON-NLS-1$
			sb.append(prefix);
			sb.append("wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(","); //$NON-NLS-1$
				sb.append(prefix);
				sb.append("category_" + i); //$NON-NLS-1$
			}
		
		}else {
			sb.append("cast(null as varchar(32000)) as ob_observer,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
}


