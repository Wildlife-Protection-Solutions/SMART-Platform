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
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.query.common.engine.IResultItem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Survey mission track query results.
 * 
 * @author Emily
 *
 */
public class ErMissionTrackQueryResult extends ErSurveyQueryResultSet {

	private static final String TRACK_PROP_KEY = "org.wcs.smart.track"; //$NON-NLS-1$
	
	private WKBReader reader = new WKBReader();
	
	public ErMissionTrackQueryResult(PsqlErMissionTrackEngine engine, int itemcnt){
		super(engine);
		setItemCount(itemcnt);
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
			MissionTrackResultItem it = asQueryResultItem(rs, null);
			items.add(it);
		}
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachMissionProperties(items, c, session);
				attachSamplingUnitAttributes(items, c, session);
			}
			
		});
		return items;
	}
	

	@Override
	public String getGeometryType() {
		return MULTI_LINESTRING_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(IResultItem item) throws Exception {
		byte[] b = (byte[]) ((MissionTrackResultItem)item).getMissionPropertyValue(TRACK_PROP_KEY);
		if (b == null){
			return new GeometryCollection(new Geometry[]{}, gf);	
		}
		return reader.read(b);
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((MissionTrackResultItem)rs).getTrackId() + "." + System.nanoTime(); //$NON-NLS-1$
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
				//TODO: figure out 3d geometries
				String sql = "SELECT " + sb.toString() //$NON-NLS-1$
						+ "st_asbinary(st_force2d(st_collect(st_geomfromwkb(bar.geometry)))) as trackgeom FROM "  //$NON-NLS-1$
						+ engine.getQueryDataTable()
						+ " foo left join " + engine.tableName(MissionTrack.class) + " bar " //$NON-NLS-1$ //$NON-NLS-2$
						+ " on bar.mission_day_uuid = foo.missionday_uuid  " //$NON-NLS-1$
						+ " GROUP BY " //$NON-NLS-1$
						+ sb.toString().substring(0, sb.length() - 1);
				
				if(sortColumn != null){
					sql += " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql;//$NON-NLS-1$ //$NON-NLS-2$
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(sql); 
			}
		});
	}
	
	private MissionTrackResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		MissionTrackResultItem it = new MissionTrackResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		
		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_enddate")); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_startdate")); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyDesignEnd(rs.getDate("surveydesign_enddate")); //$NON-NLS-1$
		it.setSurveyDesignStart(rs.getDate("surveydesign_startdate")); //$NON-NLS-1$
		
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		it.setSurveyEnd(rs.getDate("survey_enddate")); //$NON-NLS-1$
		it.setSurveyStart(rs.getDate("survey_startdate")); //$NON-NLS-1$
		
		it.setTrackUuid((UUID)rs.getObject("mission_trackuuid")); //$NON-NLS-1$
		it.setTrackType(TrackType.valueOf(rs.getString("mission_tracktype"))); //$NON-NLS-1$
		it.setTrackDate(rs.getDate("missionday_date")); //$NON-NLS-1$
		it.setTrackId(rs.getString("mission_trackid")); //$NON-NLS-1$
		it.setTrackLength(rs.getDouble("mission_tracklength")); //$NON-NLS-1$
		
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		it.setSamplingUnitUuid((UUID)rs.getObject("samplingunit_uuid")); //$NON-NLS-1$
		
		it.addMissionPropertyValue(TRACK_PROP_KEY,rs.getBytes("trackgeom")); //$NON-NLS-1$
		return it;
	}
	
	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		engine.cleanUp(session);
	}
	
	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
	}
}

