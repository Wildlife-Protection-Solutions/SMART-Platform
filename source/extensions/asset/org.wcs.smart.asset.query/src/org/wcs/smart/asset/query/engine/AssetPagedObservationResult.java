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
package org.wcs.smart.asset.query.engine;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.asset.query.model.AssetObservationResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResult;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 */
public class AssetPagedObservationResult extends ObservationQueryResult<AssetObservationResultItem> implements IWaypointUpdateableResultSet, IPagedImageResultSet  {
	
	private UpdateableResultSet updater;
	
	public AssetPagedObservationResult(AssetObservationEngine engine) {
		super(engine, -1, -1);
		updater = new UpdateableResultSet(engine);
	}
	
	
	@Override
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {
			//default to waypoint date/time, location, station
			StringBuilder sb = new StringBuilder();
			sb.append("ORDER BY "); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
			sb.append(" DESC, "); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.LOCATION.getKey()));
			sb.append(","); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.STATION.getKey()));
			sb.append(" DESC "); //$NON-NLS-1$
			return sb.toString();				
		}
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);			
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (((FixedQueryColumn)sortColumn).getKey().equals(FixedQueryColumn.FixedColumns.OBS_GROUP_ID.getKey())){
				result = "order by r.wp_group_uuid"; //$NON-NLS-1$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r." + key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}else {
			result = super.getSortColumnString(sortColumn);
		}
		
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	

	@Override
	public void createTooltip(IAttachmentResultItem data, final Composite parent) {
		AssetAttachmentTooltipProvider job = new AssetAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	@Override
	public int getImageCount() {
		return imageResults.getImageCount();
	}


	@Override
	public boolean canUpdate(Class<? extends IResultItem> item) {
		return item.equals(AssetObservationResultItem.class);
	}


	@Override
	public boolean deleteWaypoint(UUID waypointUuid) throws Exception {
		return updater.deleteWaypoint(this, waypointUuid);
	}

	public boolean deleteObservation(UUID observationUuid) throws Exception {
		return updater.deleteObservation(this, observationUuid);
	}

	@Override
	public boolean updateWaypointPosition(WaypointQueryResultItem pw, Double x, Double y) throws Exception {
		return updater.updateWaypointPosition(pw, x, y);
	}

	public boolean updateObservation(ObservationQueryResultItem item, WaypointObservation wo) throws Exception {
		return updater.updateObservation(item, wo);
	}


	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception {
		return updater.update(column, item, newValue);
	}


	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		return ((AssetObservationEngine)engine).getDistinctWaypointQuery(prefix, includeObservation);
	}
	
}
