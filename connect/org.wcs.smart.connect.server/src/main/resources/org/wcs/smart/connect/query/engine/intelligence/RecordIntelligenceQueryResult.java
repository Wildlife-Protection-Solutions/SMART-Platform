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

import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
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
	
	public RecordIntelligenceQueryResult(PsqlRecordQueryIntelligenceEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT ca_id, ca_name, intel_uuid, intel_name, intel_datereceived, intel_fromdate, intel_todate, intel_sourceuuid, intel_source, intel_patrolid, intel_informantid, intel_description, st_asbinary(intel_locations) as intel_locations FROM " + engine.getQueryDataTable()); //$NON-NLS-1$
	}

	@Override
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	@Override
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_NAME.getKey())){
			return rs.getString("intel_name"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED.getKey())){
			return rs.getDate("intel_datereceived"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_FROM.getKey())){
			return rs.getDate("intel_fromdate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_TO.getKey())){
			return rs.getDate("intel_todate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_SOURCE.getKey())){
			return rs.getString("intel_source"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE.getKey())){
			return rs.getString("intel_patrolid"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID.getKey())){
			return rs.getString("intel_informantid"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION.getKey())){
			return rs.getString("intel_description"); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getGeometryType() {
		return MULTI_POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(ResultSet rs) throws Exception {
		byte[] b = rs.getBytes("intel_locations"); //$NON-NLS-1$
		if (b == null){
			return new GeometryCollection(new Geometry[]{}, gf);	
		}
		return reader.read(b);
	}

	@Override
	public String createId(ResultSet rs) throws Exception {
		String name=rs.getString("intel_name").toLowerCase(); //$NON-NLS-1$
		name = name.replaceAll("[^a-zA-Z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return name + "." + System.nanoTime(); //$NON-NLS-1$
	}
}
