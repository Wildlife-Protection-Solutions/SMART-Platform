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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.er.query.model.SurveyObservationResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResult;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyPagedObservationResult extends ObservationQueryResult<SurveyObservationResultItem> implements ISurveyQueryMissionResult {

	
	public DerbyPagedObservationResult(DerbyObservationEngine engine) {
		super(engine, -1, -1);
	}
	
	@Override
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {
			//default sort by mission start, mission id, waypoint id 
			StringBuilder sb = new StringBuilder();
			sb.append("ORDER BY "); //$NON-NLS-1$
			sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION_START.getKey()));
			sb.append(" DESC, "); //$NON-NLS-1$
			sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION.getKey()));
			sb.append(","); //$NON-NLS-1$
			sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
			sb.append(" DESC "); //$NON-NLS-1$
			return sb.toString();				
		}
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof SurveyQueryColumn) {
			String key = sortColumn.getKey();
			key = SurveyQueryColumn.getDbColumnName(key);
			if (sortColumn.getKey().equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (((SurveyQueryColumn)sortColumn).getKey().equals(SurveyQueryColumn.FixedColumns.OBS_GROUP_ID.getKey())) {
				result = "order by r."+key; //$NON-NLS-1$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		
		}else if (sortColumn instanceof MissionPropertyQueryColumn ||
				sortColumn instanceof SamplingUnitAttributeQueryColumn) {
			
			switch (sortColumn.getType()) {
				case BOOLEAN:
				case NUMBER:
				case INTEGER:
					result = "order by sortKeyDbl"; //$NON-NLS-1$
					break;
				case DATE:
					result = "order by DATE(sortKeyTxt)"; //$NON-NLS-1$
					break;
				default:
					result = "order by UPPER(sortKeyTxt)"; //$NON-NLS-1$
					break;
			}
		}else {
			result = super.getSortColumnString(sortColumn);
		}
		
		if (result != null && !result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	@Override
	protected void updateSortColumn(Session session, Connection c) throws SQLException{
		if (sortColumn instanceof MissionPropertyQueryColumn ||
			sortColumn instanceof SamplingUnitAttributeQueryColumn) {
			
			SurveyPagedResultUtils.processSortColumn(engine.getQueryDataTable(), 
					((DerbyObservationEngine)engine).getObservationLabelTable(),
					((DerbyObservationEngine)engine), hasSortColumns, sortColumn, c, session);
			hasSortColumns = true;
		}else {
			super.updateSortColumn(session,c);
		}
		
		c.commit();
	}

	@Override
	public List<SurveyObservationResultItem> getResults(final Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		final List<SurveyObservationResultItem> items = super.getResults(session, rs, from, pageSize);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SurveyPagedResultUtils.attachMissionProperties(items, c, session);
				SurveyPagedResultUtils.attachSamplingUnitAttributes(items, c, session);
			}
		});
		return items;
	}
		


	@Override
	public boolean isDataColumn(QueryColumn column) {
		if (dataColumns == null) return true;
		if (column instanceof SamplingUnitAttributeQueryColumn) return true;
		if (column instanceof MissionPropertyQueryColumn) return true;
		return dataColumns.contains(column.getKey());
	}
	
	@Override
	public void createTooltip(IAttachmentResultItem data, final Composite parent) {
		SurveyAttachmentTooltipProvider job = new SurveyAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	@Override
	public List<byte[]> getMissionUuids() {
		return SurveyPagedResultUtils.getMissionUuids(engine.getQueryDataTable());
	}

	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		return ((DerbyObservationEngine)engine).getDistinctWaypointQuery(prefix, includeObservation);
	}

	
}