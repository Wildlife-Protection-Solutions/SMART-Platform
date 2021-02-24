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
package org.wcs.smart.connect.query.engine.observation;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.ObservationQueryResult;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.ObservationAttachmentQueryResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class ObsObservationQueryResult extends ObservationQueryResult<ObservationQueryResultItem> implements IPagedImageResultSet {

	public ObsObservationQueryResult(PsqlObsObservationEngine engine, int resultcount, boolean includeUuids){
		super(engine, resultcount, includeUuids);
	}

	@Override
	protected ObservationAttachmentQueryResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		ObservationAttachmentQueryResultItem item = new ObservationAttachmentQueryResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	@Override
	protected ObservationQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		ObservationQueryResultItem item = new ObservationQueryResultItem();
		setFields(item, rs);
		return item;
	}
	@Override
	protected void setFields(ObservationQueryResultItem it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
	}
	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] { 
				"ca_uuid", "ca_id", "ca_name", "wp_source", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_uuid", "wp_id", "wp_x", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_y", "wp_time", "wp_direction", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_distance", "wp_comment", "wp_lastmodified", //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				"wp_lastmodifiedbyname" //$NON-NLS-1$
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
			for (int i = 0; i < engine.getCategoryCnt(); i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(prefix);
				sb.append("category_" + i); //$NON-NLS-1$
			}

		} else {
			sb.append("cast(null as varchar(32000)) as ob_observer,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i++) {
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

}


