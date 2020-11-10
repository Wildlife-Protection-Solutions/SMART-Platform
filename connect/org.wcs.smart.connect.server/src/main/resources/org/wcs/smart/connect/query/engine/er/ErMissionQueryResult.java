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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;

/**
 * Survey Mission Query Results
 * @author Emily
 *
 */
public class ErMissionQueryResult extends AbstractDbFeatureResultSet<SurveyQueryResultItem>{

	private WKBReader reader = new WKBReader();
	private boolean includeUuids = false;
	
	private PsqlErMissionEngine engine;
	
	public ErMissionQueryResult(PsqlErMissionEngine engine, int itemcnt, boolean includeUuids){
		this.engine = engine;
		setItemCount(itemcnt);
		this.includeUuids = includeUuids;
	}
	
	@Override
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = super.getQueryColumns(query, l, session, prj);
		if (!includeUuids) return cols;
		
		QueryColumn missionUuidCol = new QueryColumn(Messages.getString("ErMissionQueryResult.MissionUuidColumnName", l), MISSION_UUID_COL_KEY, QueryColumn.ColumnType.STRING) { //$NON-NLS-1$
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((SurveyQueryResultItem)item).getMissionUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((SurveyQueryResultItem)item).getMissionUuid());
			}
		};
		cols.add(missionUuidCol);
		
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
	public List<SurveyQueryResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<SurveyQueryResultItem> items = new ArrayList<>();
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
				ErSurveyQueryResultSet.attachMissionProperties(items, c, session);
			}
			
		});
		return items;
	}
	

	@Override
	public String getGeometryType() {
		return MULTI_LINESTRING_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(SurveyQueryResultItem item) throws Exception {
		byte[] b = ((GeomSurveyQueryResultItem)item).getGeometry();
		if (b == null){
			return new GeometryCollection(new Geometry[]{}, gf);	
		}
		return reader.read(b);
	}

	@Override
	public String createId(SurveyQueryResultItem rs) throws Exception {
		return ((SurveyQueryResultItem)rs).getMissionId() + "." + System.nanoTime(); //$NON-NLS-1$
	}


	@Override
	public ResultSet getResultSet(final Session session) {

		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				StringBuilder sb = new StringBuilder();
				String schema = null;
				String tablename = engine.getQueryDataTable();
				int position = tablename.indexOf('.');
				if (position >= 0){
					schema = tablename.substring(0, position);
					tablename = tablename.substring(position+1);
				}
				try(ResultSet rs = c.getMetaData().getColumns(null, schema, tablename, null)){
					while(rs.next()){
						sb.append("foo." + rs.getString(4)); //$NON-NLS-1$
						sb.append(","); //$NON-NLS-1$
					}
				}
				
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT "); //$NON-NLS-1$
				sql.append(sb);
				sql.append("st_asbinary(st_force2d(st_collect(st_geomfromwkb(bar.geometry)))) as trackgeom "); //$NON-NLS-1$
				sql.append(" FROM ");  //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append(" foo left join "); //$NON-NLS-1$
				sql.append( engine.tableName(MissionDay.class) );
				sql.append(" c on c.mission_uuid = foo.mission_uuid  "); //$NON-NLS-1$
				sql.append(" left join "); //$NON-NLS-1$
				sql.append( engine.tableName(MissionTrack.class));
				sql.append(" bar on  bar.mission_day_uuid = c.uuid "); //$NON-NLS-1$
				sql.append( " GROUP BY " ); //$NON-NLS-1$
				sql.append( sb.toString().substring(0, sb.length() - 1));
				
				if(sortColumn != null){
					sql.append(" ORDER BY sortkeydbl "); //$NON-NLS-1$
					sql.append(direction.sql);
					sql.append(", sortkeytxt "); //$NON-NLS-1$
					sql.append( direction.sql);
				}else {
					//default sort by waypoint date/time
					sql.append(" ORDER BY "); //$NON-NLS-1$
					sql.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION_START.getKey()));
					sql.append(" DESC, "); //$NON-NLS-1$
					sql.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION.getKey()));
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(sql.toString());
			}
		});
	}
	
	protected GeomSurveyQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		GeomSurveyQueryResultItem it = new GeomSurveyQueryResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid((UUID)rs.getObject("ca_uuid")); //$NON-NLS-1$
		
		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_enddate").toLocalDate()); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_startdate").toLocalDate()); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		byte[] track = rs.getBytes("trackgeom"); //$NON-NLS-1$
		
		it.setGeometry(track);
		return it;
		
	}

	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		engine.cleanUp(session);
	}
	
	
	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(), null, engine.getCaFilter(), ".ob_"); //$NON-NLS-1$
	}
}

