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

import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class RecordIntelligenceQueryResult implements IDbTableResultSet {

	private PsqlRecordQueryIntelligenceEngine engine;
	
	public RecordIntelligenceQueryResult(PsqlRecordQueryIntelligenceEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable());
	}

	@Override
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	@Override
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_NAME.getKey())){
			return rs.getString("intel_name");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED.getKey())){
			return rs.getDate("intel_datereceived");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_FROM.getKey())){
			return rs.getDate("intel_fromdate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DATE_TO.getKey())){
			return rs.getDate("intel_todate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_SOURCE.getKey())){
			return rs.getString("intel_source");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE.getKey())){
			return rs.getString("intel_patrolid");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID.getKey())){
			return rs.getString("intel_informantid");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION.getKey())){
			return rs.getString("intel_description");
		}
		return null;
	}
	
}
