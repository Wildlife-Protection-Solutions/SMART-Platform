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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.ObservationQueryResult;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.model.PatrolObservationAttachmentResultItem;
import org.wcs.smart.patrol.query.model.PatrolObservationResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;

/**
 * Observation query result for patrol queries.
 * 
 * @author Emily
 *
 */
public class PatrolObservationQueryResult extends ObservationQueryResult<PatrolObservationResultItem> implements IPagedImageResultSet {

	
	public PatrolObservationQueryResult(PsqlPatrolObservationEngine engine, int itemcnt, boolean includeUuids){
		super(engine, itemcnt, includeUuids);
	}

	@Override
	public String getDefaultSortBy() {
		StringBuilder sb = new StringBuilder();
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey()));
		sb.append(" DESC, " ); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_ID.getKey()));
		sb.append(","); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
		sb.append(" DESC " ); //$NON-NLS-1$
		return sb.toString();
		
	}
	
	protected PatrolObservationAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		PatrolObservationAttachmentResultItem item = new PatrolObservationAttachmentResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	protected PatrolObservationResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		PatrolObservationResultItem item = new PatrolObservationResultItem();
		setFields(item, rs);
		return item;
	}
	
	protected void setFields(PatrolObservationResultItem it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		
		it.setPatrolUuid((UUID)rs.getObject("p_uuid")); //$NON-NLS-1$
		it.setPatrolId(rs.getString("p_id")); //$NON-NLS-1$
		it.setPatrolStartDate(rs.getDate("p_startdate").toLocalDate()); //$NON-NLS-1$
		it.setPatrolEndDate(rs.getDate("p_enddate").toLocalDate()); //$NON-NLS-1$
		it.setStation(rs.getString("p_station"));				 //$NON-NLS-1$
		it.setTeam(rs.getString("p_team"));	 //$NON-NLS-1$
		it.setObjective(rs.getString("p_objective")); //$NON-NLS-1$
		it.setMandate(rs.getString("pl_mandate")); //$NON-NLS-1$
		it.setPatrolType(PatrolType.Type.valueOf(rs.getString("p_type"))); //$NON-NLS-1$
		it.setArmed(rs.getBoolean("p_armed")); //$NON-NLS-1$
		it.setTransportType(rs.getString("p_transporttype")); //$NON-NLS-1$
		it.setPatrolLegId(rs.getString("p_legid")); //$NON-NLS-1$
		
		it.setLeader(rs.getString("p_leader")); //$NON-NLS-1$
		it.setPilot(rs.getString("p_pilot")); //$NON-NLS-1$	
	}
	
	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
				"ca_uuid", "ca_id","ca_name","p_uuid","p_id", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"p_startdate","p_enddate","p_station", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"p_team","p_objective","pl_mandate", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"p_type","p_armed","p_transporttype", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"p_legid","p_leader", //$NON-NLS-1$ //$NON-NLS-2$
				"p_pilot","wp_uuid","wp_id", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_x","wp_y","wp_time", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_lastmodified","wp_lastmodifiedbyname", //$NON-NLS-1$ //$NON-NLS-2$
				"wp_direction","wp_distance","wp_comment" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
