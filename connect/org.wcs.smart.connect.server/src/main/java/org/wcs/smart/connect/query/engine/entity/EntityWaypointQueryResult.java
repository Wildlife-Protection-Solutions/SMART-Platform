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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.WaypointQueryResult;
import org.wcs.smart.entity.query.model.EntityWaypointResultItem;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityWaypointQueryResult  extends WaypointQueryResult<EntityWaypointResultItem> implements IPagedImageResultSet {

	
	public EntityWaypointQueryResult(PsqlEntityWaypointEngine engine, int itemCnt, boolean includeUuids){
		super(engine, itemCnt, includeUuids);
	}
	
	protected EntityWaypointResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		EntityWaypointResultItem it = new EntityWaypointResultItem();
		super.setFields(it, rs);
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		return it;
	}
//	
//	@Override
//	public void updateSortColumn(Session session) throws SQLException {
//		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid");		 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
//	}

	@Override
	protected IAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException {
		throw new UnsupportedOperationException("Attachment queries not supported for entity queries"); //$NON-NLS-1$
	}
}
