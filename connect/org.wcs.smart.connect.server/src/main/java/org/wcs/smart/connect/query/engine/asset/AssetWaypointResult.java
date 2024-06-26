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
import org.wcs.smart.asset.query.model.AssetWaypointAttachmentResultItem;
import org.wcs.smart.asset.query.model.AssetWaypointResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.connect.query.engine.WaypointQueryResult;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;

public class AssetWaypointResult extends WaypointQueryResult<AssetWaypointResultItem> implements IPagedImageResultSet {

	
	public AssetWaypointResult(AssetWaypointEngine engine, int itemcnt, boolean includeUuids){
		super(engine, itemcnt, includeUuids);
	}
	
	@Override	
	protected AssetWaypointResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		AssetWaypointResultItem it = new AssetWaypointResultItem();
		((AssetWaypointEngine)engine).setFields(it, rs);
		return it;
	}

	@Override
	protected AssetWaypointAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		AssetWaypointAttachmentResultItem item = new AssetWaypointAttachmentResultItem();
		((AssetWaypointEngine)engine).setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
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
	
	
}
