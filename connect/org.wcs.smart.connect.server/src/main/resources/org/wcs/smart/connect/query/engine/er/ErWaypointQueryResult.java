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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.query.engine.WaypointQueryResult;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyWaypointAttachmentResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;

/**
 * Survey waypoint query results.
 * 
 * @author Emily
 *
 */
public class ErWaypointQueryResult extends WaypointQueryResult<SurveyWaypointResultItem> implements IPagedImageResultSet {

	public ErWaypointQueryResult(PsqlErWaypointEngine engine, int itemcnt, boolean includeUuids){
		super(engine, itemcnt, includeUuids);
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
	public List<SurveyWaypointResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<SurveyWaypointResultItem> items = super.getResults(session, rs, from, pageSize);
		
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
	protected SurveyWaypointAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyWaypointAttachmentResultItem item = new SurveyWaypointAttachmentResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	@Override
	protected SurveyWaypointResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		SurveyWaypointResultItem item = new SurveyWaypointResultItem();
		setFields(item, rs);
		return item;
	}
	
	@Override
	protected void setFields(SurveyWaypointResultItem it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$

		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_startdate").toLocalDate()); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_enddate").toLocalDate()); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSamplingUnitUuid((UUID)rs.getObject("samplingunit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$		
	}
	
	
	@Override
	public void updateSortColumn(Session session) throws SQLException {
		//pass in the specific column name prefixes and suffixes to customize the SQL to this type of Query
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".wp_", "_mLIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

}