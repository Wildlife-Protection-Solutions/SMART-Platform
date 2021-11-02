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
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.WaypointQueryResult;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.model.PatrolWaypointAttachmentResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;

/**
 * Result set of patrol waypoint queries.
 * 
 * @author Emily
 *
 */
public class PatrolWaypointQueryResult extends WaypointQueryResult<PatrolWaypointResultItem> implements IPagedImageResultSet {

	
	public PatrolWaypointQueryResult(PsqlPatrolWaypointEngine engine, int itemcnt, boolean includeUuids){
		super(engine, itemcnt, includeUuids);
	}
	
	@Override
	public String getDefaultSortBy() {
		//default sort by patrol state date, patrol id, waypoint datetime
		StringBuilder sb = new StringBuilder();
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey()));
		sb.append(" DESC, " ); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_ID.getKey()));
		sb.append(","); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
		sb.append(" DESC " ); //$NON-NLS-1$
				
		return sb.toString();
	}
	
	protected PatrolWaypointAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		PatrolWaypointAttachmentResultItem item = new PatrolWaypointAttachmentResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	protected PatrolWaypointResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		PatrolWaypointResultItem item = new PatrolWaypointResultItem();
		setFields(item, rs);
		return item;
	}
	
	@Override
	protected void setFields(PatrolWaypointResultItem it, ResultSet rs) throws SQLException{
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
		
		List<String> patrolAttributes = ((PsqlPatrolWaypointEngine)engine).getPatrolAttributes();
		if (patrolAttributes != null) {
			for (String p : patrolAttributes) {
				it.setPatrolAttribute(p.substring(PatrolAttributeQueryColumn.PREFIX.length() + 1), rs.getObject(p));
			}
		}
		
	}
	
}
