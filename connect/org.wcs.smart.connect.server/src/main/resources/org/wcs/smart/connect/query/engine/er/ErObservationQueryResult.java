/*
' * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.er.query.model.SurveyQueryAttachmentResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;

/**
 * Survey observation query results.
 * 
 * @author Emily
 *
 */
public class ErObservationQueryResult extends ErSurveyQueryResultSet implements IPagedImageResultSet {

	private boolean includeUuids;
	private String imageDataTable;
	private int imageCount;
	
	public ErObservationQueryResult(PsqlErObservationEngine engine, int itemcnt, boolean includeUuids){
		super(engine);
		this.includeUuids = includeUuids;
		setItemCount(itemcnt);
	}
	
	@Override
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = super.getQueryColumns(query, l, session, prj);
		if (!includeUuids) return cols;
		
		QueryColumn obsUuidCol = new QueryColumn(getObservationColumnName(l), OBS_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((SurveyQueryResultItem)item).getObservationUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((SurveyQueryResultItem)item).getObservationUuid());
			}
			
		};
		QueryColumn wpUuidCol = new QueryColumn(getWaypointColumnName(l), WP_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((SurveyQueryResultItem)item).getWaypointUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((SurveyQueryResultItem)item).getWaypointUuid());
			}
			
		};
		
		cols.add(obsUuidCol);
		cols.add(wpUuidCol);
		
		QueryColumn caUuidCol = new QueryColumn(getConservationAreaColumnName(l), CA_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((SurveyQueryResultItem)item).getConservationAreaUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((SurveyQueryResultItem)item).getConservationAreaUuid());
			}
		};
		cols.add(caUuidCol);
		
		return cols;
	}
	
	/**
	 * Gets results from the given result set.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	@Override
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			SurveyQueryResultItem it = asQueryResultItem(rs);
			items.add(it);
		}
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(items, c, session);
				attachMissionProperties(items, c, session);
				attachSamplingUnitAttributes(items, c, session);
			}
			
		});
		return items;
	}
	

	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		SurveyQueryResultItem i = ((SurveyQueryResultItem)rs);
		return gf.createPoint(new Coordinate(i.getWaypointX(null), i.getWaypointY(null))); 
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((SurveyQueryResultItem)rs).getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$ 
	}


	@Override
	public ResultSet getResultSet(final Session session) {
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if(sortColumn != null){
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable() + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT * FROM "); //$NON-NLS-1$
				sb.append(engine.getQueryDataTable());
				sb.append(" ORDER BY "); //$NON-NLS-1$
				sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION_START.getKey()));
				sb.append(" DESC, "); //$NON-NLS-1$
				sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION.getKey()));
				sb.append(","); //$NON-NLS-1$
				sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
				sb.append(" DESC "); //$NON-NLS-1$
				
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(sb.toString());
			}
		});
	}
	
	protected SurveyQueryAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyQueryAttachmentResultItem item = new SurveyQueryAttachmentResultItem();
		setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
	}
	
	protected SurveyQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		SurveyQueryResultItem item = new SurveyQueryResultItem();
		setFields(item, rs);
		return item;
	}
	
	protected void setFields(SurveyQueryResultItem it, ResultSet rs) throws SQLException{
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid((UUID)rs.getObject("ca_uuid")); //$NON-NLS-1$
		
		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionEnd(rs.getTimestamp("mission_enddate").toLocalDateTime()); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getTimestamp("mission_startdate").toLocalDateTime()); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
	
		it.setSamplingUnitUuid((UUID)rs.getObject("samplingunit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_date").toLocalDateTime()); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		it.setObservationGroupUuid((UUID)rs.getObject("wp_group_uuid")); //$NON-NLS-1$
		it.setObservationUuid((UUID)rs.getObject("ob_uuid")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
		
		//build categories
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < engine.getCategoryCnt(); i ++){
			String category = rs.getString("category_"+i); //$NON-NLS-1$
			if (category == null){
				break;
			}
			categories.add(category);
		}
		
		it.setCategory(categories.toArray(new String[categories.size()]));
	}
	
	@Override
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize){
		throw new UnsupportedOperationException("use getImageIterator"); //$NON-NLS-1$
	}
	@Override
	public int getImageCount() {
		return imageCount;
	}

	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		
		imageDataTable = engine.createTempTableName();		
		imageCount = createImageDataObservation(session, engine.getQueryDataTable(), imageDataTable);
		
		String query = getImageQueryObservation(engine.getQueryDataTable(), imageDataTable, 
				getDistinctWaypointQuery("r.", true),  //$NON-NLS-1$
				getDistinctWaypointQuery("r.", false)); //$NON-NLS-1$
		
		return new AttachmentResultSetIterator(session, 
				e->asAttachmentQueryResultItem(e, session),
				()->query);
	}
	
	
	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		if (imageDataTable != null) {
			engine.dropTable(session, imageDataTable);
		}
		engine.cleanUp(session);
	}
	

	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
	
	private String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
				"ca_id","ca_name","mission_uuid","mission_enddate", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"mission_id","mission_startdate","mission_leader", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"surveydesign_name","surveydesign_enddate", //$NON-NLS-1$ //$NON-NLS-2$
				"surveydesign_startdate","survey_id","survey_enddate", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"survey_startdate","samplingunit_uuid","samplingunit_id",  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				"wp_uuid","wp_id","wp_x","wp_y","wp_date","wp_direction", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				"wp_distance","wp_comment","wp_lastmodified","wp_lastmodifiedbyname" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(","); //$NON-NLS-1$
				sb.append(prefix);
				sb.append("category_" + i); //$NON-NLS-1$
			}
		
		}else {
			sb.append("cast(null as varchar(32000)) as ob_observer,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
}


