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
package org.wcs.smart.patrol.query.engine;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResult;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class DerbyPagedWaypointResult extends WaypointQueryResult<PatrolWaypointResultItem> implements IWaypointUpdateableResultSet {

	
	private UpdateableResultSet updater = null;
	
	public DerbyPagedWaypointResult(DerbyWaypointEngine engine) {
		super(engine, -1);
		updater = new UpdateableResultSet(engine);
	}
	
	@Override
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {
			//default sort by patrol state date, patrol id, waypoint datetime
			
			StringBuilder sb = new StringBuilder();
			sb.append("ORDER BY "); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey()));
			sb.append(" DESC, " ); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.PATROL_ID.getKey()));
			sb.append(","); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
			sb.append(" DESC " ); //$NON-NLS-1$
			return sb.toString();
		}
			
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);
			if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}else if (sortColumn instanceof PatrolAttributeQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);
			if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}
		
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	
	@Override
	public void createTooltip(IAttachmentResultItem data, final Composite parent) {
		PatrolAttachmentTooltipProvider job = new PatrolAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	@Override
	public boolean canUpdate(Class<? extends IResultItem> item) {
		return updater.canUpdate(item);
	}

	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception {
		return updater.update(column, item, newValue);
	}

	@Override
	public boolean deleteWaypoint(UUID waypointUuid) throws Exception {
		return updater.deleteWaypoint(waypointUuid);
	}

	@Override
	public boolean updateWaypointPosition(IPatrolQueryResultItem pw, Double x, Double y, Float distance,
			Float direction) throws Exception {
		return updater.updateWaypointPosition(pw, x, y, distance, direction);
	}

	
}
