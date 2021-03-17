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
import org.wcs.smart.connect.query.engine.WaypointQueryResult;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.WaypointAttachmentQueryResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;

/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class ObsWaypointQueryResult extends WaypointQueryResult<WaypointQueryResultItem> implements IPagedImageResultSet {

	
	public ObsWaypointQueryResult(PsqlObsWaypointEngine engine, int itemCnt, boolean includeUuids){
		super(engine, itemCnt, includeUuids);
	}
	
		
	protected WaypointAttachmentQueryResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		WaypointAttachmentQueryResultItem item = new WaypointAttachmentQueryResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	protected WaypointQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		WaypointQueryResultItem item = new WaypointQueryResultItem();
		setFields(item, rs);
		return item;
	}
	
	@Override
	protected void setFields(WaypointQueryResultItem it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
	}
//	
//	@Override
//	public List<IAttachmentResultItem> getImageData(int offset, int pageSize){
//		throw new UnsupportedOperationException("use getImageIterator"); //$NON-NLS-1$
//	}
//	
//	@Override
//	public int getImageCount() {
//		return imageCount;
//	}
//
//	@Override
//	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
//		
//		imageDataTable = engine.createTempTableName();
//		imageCount = createImageDataWaypoint(session, engine.getQueryDataTable(), imageDataTable);
//		
//		String query = getImageQueryWaypoint(engine.getQueryDataTable(), imageDataTable);
//		return new AttachmentResultSetIterator(session, 
//				e->asAttachmentQueryResultItem(e, session),
//				()->query);
//	}
//	
//	
//	@Override
//	public void dispose(Session session) throws SQLException {
//		super.dispose(session);
//		if (imageDataTable != null) {
//			engine.dropTable(session, imageDataTable);
//		}
//		engine.cleanUp(session);
//	}
//
//	@Override
//	public void updateSortColumn(Session session) throws SQLException {
//		//this is a weird one, it doesn't create any *_list or *_tree tables at all, not sure why yet... the last false just triggers an unsupported exception 
//		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "", "", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
//		
//	}
}