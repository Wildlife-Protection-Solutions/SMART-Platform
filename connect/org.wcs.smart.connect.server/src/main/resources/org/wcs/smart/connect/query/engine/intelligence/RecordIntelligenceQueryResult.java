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
package org.wcs.smart.connect.query.engine.intelligence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.query.common.engine.IResultItem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKBReader;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class RecordIntelligenceQueryResult extends AbstractDbFeatureResultSet {

	private PsqlRecordQueryIntelligenceEngine engine;
	private WKBReader reader = new WKBReader();
	
	public RecordIntelligenceQueryResult(PsqlRecordQueryIntelligenceEngine engine, int itemcnt){
		this.engine = engine;
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
			IntelligenceRecordResultItem it = asQueryResultItem(rs);
			items.add(it);
		}
		
		return items;
	}

	@Override
	public ResultSet getResultSet(final Session session) {
		final String dataQuerySort = "SELECT ca_id, ca_name, intel_uuid, intel_name, intel_datereceived, intel_fromdate, intel_todate, intel_sourceuuid, intel_source, intel_patrolid, intel_informantid, intel_description, st_asbinary(intel_locations) as intel_locations FROM " + engine.getQueryDataTable() + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql; //$NON-NLS-1$
		final String dataQuery = "SELECT ca_id, ca_name, intel_uuid, intel_name, intel_datereceived, intel_fromdate, intel_todate, intel_sourceuuid, intel_source, intel_patrolid, intel_informantid, intel_description, st_asbinary(intel_locations) as intel_locations FROM " + engine.getQueryDataTable();//$NON-NLS-1$

		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if(sortColumn != null){
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(dataQuerySort);
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(dataQuery);

			}
		});
	}
	
	@Override
	public String getGeometryType() {
		return MULTI_POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		IntelligenceRecordResultItem it = (IntelligenceRecordResultItem) rs;
		return it.asGeometry(IntelligenceRecordResultItem.GEOMCOLUMN_KEY);
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		String name = ((IntelligenceRecordResultItem)rs).getName();
		name = name.replaceAll("[^a-zA-Z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return name + "." + System.nanoTime(); //$NON-NLS-1$
	}
	
	public IntelligenceRecordResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		IntelligenceRecordResultItem item = new IntelligenceRecordResultItem();
		item.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		item.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		item.setUuid((UUID)rs.getObject("intel_uuid")); //$NON-NLS-1$
		item.setName(rs.getString("intel_name")); //$NON-NLS-1$
		item.setReceivedDate(rs.getDate("intel_datereceived")); //$NON-NLS-1$
		item.setFromDate(rs.getDate("intel_fromdate")); //$NON-NLS-1$
		item.setToDate(rs.getDate("intel_todate")); //$NON-NLS-1$
		
		item.setSourceUuid((UUID)rs.getObject("intel_sourceuuid")); //$NON-NLS-1$
		item.setSource(rs.getString("intel_source")); //$NON-NLS-1$
		
		item.setPatrolId(rs.getString("intel_patrolid")); //$NON-NLS-1$
		item.setInformantId(rs.getString("intel_informantid")); //$NON-NLS-1$
		item.setDescription(rs.getString("intel_description")); //$NON-NLS-1$
		
		try{
			byte[] geom = rs.getBytes("intel_locations"); //$NON-NLS-1$
			if (geom != null){
				MultiPoint mp = (MultiPoint)reader.read(geom);
				for (int i = 0; i < mp.getNumGeometries(); i ++){
					item.addCoordinate(((Point)mp.getGeometryN(i)).getX(), ((Point)mp.getGeometryN(i)).getY());
				}
			}
		}catch (Exception ex){
			throw new SQLException(ex);
		}
		return item;
		
	}


	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		engine.cleanUp(session);
	}
	
	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid");
	}
}
