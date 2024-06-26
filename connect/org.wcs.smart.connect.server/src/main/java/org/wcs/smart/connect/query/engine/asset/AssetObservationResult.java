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
package org.wcs.smart.connect.query.engine.asset;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetObservationAttachmentResultItem;
import org.wcs.smart.asset.query.model.AssetObservationResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.connect.query.engine.ObservationQueryResult;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 */
public class AssetObservationResult extends ObservationQueryResult<AssetObservationResultItem> implements IPagedImageResultSet {

		
	public AssetObservationResult(AssetObservationEngine engine, int itemCount, boolean includeUuids) {
		super(engine, itemCount, includeUuids);
	}
	
//	@Override
//	public void updateSortColumn(Session session) throws SQLException{
//		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
//	}
		

	protected AssetObservationAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		AssetObservationAttachmentResultItem item = new AssetObservationAttachmentResultItem();
		setFields(item, rs, session);
		setAttachmentField(session, rs, item);
		return item;
	}
	

	@Override
	protected AssetObservationResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException {
		AssetObservationResultItem item = new AssetObservationResultItem();
		setFields(item, rs, session);
		return item;
	}
	
	protected void setFields(AssetObservationResultItem it, ResultSet rs, Session session) throws SQLException{
		super.setFields(it, rs, session);
		it.setAssets(rs.getString("asset_asset")); //$NON-NLS-1$
		it.setStation(rs.getString("asset_station")); //$NON-NLS-1$
		it.setLocations(rs.getString("asset_location")); //$NON-NLS-1$
		it.setIncidentLength(rs.getInt("incident_length")); //$NON-NLS-1$
		
	}
		
	@Override
	public String getDefaultSortBy() {
		StringBuilder sb = new StringBuilder();
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
		sb.append(" DESC, "); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.LOCATION.getKey()));
		sb.append(","); //$NON-NLS-1$
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.STATION.getKey()));
		sb.append(" DESC "); //$NON-NLS-1$
		return sb.toString();
	}
	

	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
				"ca_uuid","ca_id","ca_name","wp_uuid", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_id","wp_x","wp_y","wp_time", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_direction","wp_distance", //$NON-NLS-1$ //$NON-NLS-2$
				"wp_comment","asset_asset","asset_station", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"asset_location","incident_length", //$NON-NLS-1$ //$NON-NLS-2$
				"wp_lastmodified","wp_lastmodifiedbyname" //$NON-NLS-1$ //$NON-NLS-2$
						
		};
		for (String s : selectFields) {
			sb.append(prefix);
			sb.append(s);
			sb.append(","); //$NON-NLS-1$
		}
		
		if (includeObservation) {
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
			sb.append("cast(null as uuid) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

}
