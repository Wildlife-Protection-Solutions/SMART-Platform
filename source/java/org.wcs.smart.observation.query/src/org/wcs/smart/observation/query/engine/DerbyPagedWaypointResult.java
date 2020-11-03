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
package org.wcs.smart.observation.query.engine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.test.WaypointQueryResult;
import org.wcs.smart.query.common.engine.test.WaypointQueryResultItem;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class DerbyPagedWaypointResult extends WaypointQueryResult<WaypointQueryResultItem> {
	
	public DerbyPagedWaypointResult(DerbyWaypointEngine engine,
			int itemCount) {
		super( engine, itemCount);
	}

	public DerbyPagedWaypointResult(DerbyWaypointEngine engine) {
		super( engine, -1);
	}
	
	@Override
	public String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {
			//lets default sort by waypoint date/time
			return "order by " + FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()) + " DESC"; //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);
			
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
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
		WaypointAttachmentTooltipProvider job = new WaypointAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	
}
