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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Result set of patrol waypoint queries.
 * 
 * @author Emily
 *
 */
public class PatrolQueryResult extends AbstractDbFeatureResultSet {

	private PsqlPatrolEngine engine;
	private WKBReader reader = new WKBReader();
	
	public PatrolQueryResult(PsqlPatrolEngine engine, int itemCnt){
		this.engine = engine;
		setItemCount(itemCnt);
	}

	@Override
	public String getGeometryType() {
		return MULTI_LINESTRING_GEOM_TYPE;
	}

	@Override
	public ResultSet getResultSet(final Session session) {
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if(sortColumn != null){
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY).executeQuery(engine.getDataQuery(new String[]{"sortkeydbl", "sortkeytxt"}) + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(engine.getDataQuery(null));
			}
		});
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
			PatrolQueryResultItem it = asQueryResultItem(rs, session);
			items.add(it);
		}
		return items;
	}
	
	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		List<byte[]> tracks = ((PatrolQueryResultItem)rs).getTrack();
		if (tracks == null || tracks.size() <= 0){
			return new GeometryCollection(new Geometry[]{}, gf);	
		}
		return reader.read(tracks.get(0));
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((PatrolQueryResultItem)rs).getPatrolId() + "." + System.nanoTime(); //$NON-NLS-1$
	}
	
	protected PatrolQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		PatrolQueryResultItem it = new PatrolQueryResultItem();
		//UUID cauuid = (UUID)rs.getObject("r_p_ca_uuid"); //$NON-NLS-1$
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setPatrolUuid((UUID)rs.getObject("r_p_uuid")); //$NON-NLS-1$
		it.setPatrolId(rs.getString("r_p_id")); //$NON-NLS-1$
		it.setPatrolStartDate(rs.getDate("r_p_start_date")); //$NON-NLS-1$
		it.setPatrolEndDate(rs.getDate("r_p_end_date")); //$NON-NLS-1$
		it.setStation(rs.getString("p_station"));				 //$NON-NLS-1$
		it.setTeam(rs.getString("p_team"));				 //$NON-NLS-1$
		it.setObjective(rs.getString("r_p_objective")); //$NON-NLS-1$
		it.setMandate(rs.getString("pl_mandate")); //$NON-NLS-1$
		it.setPatrolType(PatrolType.Type.valueOf(rs.getString("r_p_type"))); //$NON-NLS-1$
		it.setArmed(rs.getBoolean("r_p_is_armed")); //$NON-NLS-1$
		it.setTransportType(rs.getString("p_transporttype")); //$NON-NLS-1$
		it.setPatrolLegId(rs.getString("r_pl_id")); //$NON-NLS-1$
		it.setPatrolLegStartDate(rs.getDate("r_pl_start_date")); //$NON-NLS-1$
		it.setPatrolLegEndDate(rs.getDate("r_pl_end_date")); //$NON-NLS-1$
		it.setLeader(engine.getEmployeeName((UUID)rs.getObject("r_plm_leader"), session)); //$NON-NLS-1$
		it.setPilot(engine.getEmployeeName((UUID)rs.getObject("r_plm_pilot"), session)); //$NON-NLS-1$
		it.addTrack(rs.getBytes("track")); //$NON-NLS-1$
	
		return it;
	}
	
	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		engine.cleanUp(session);
	}


	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(),engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
	}
}
