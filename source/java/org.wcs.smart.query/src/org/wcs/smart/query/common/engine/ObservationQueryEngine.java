/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.query.common.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.util.UuidUtils;

/**
 * Interface for queries that return observation results.
 * 
 * @author Emily
 *
 * @param <T>
 */
public interface ObservationQueryEngine<T extends IObservationQueryResultItem> extends IDesktopWOEngine<T>  {
	
	/**
	 * 
	 * @return the table which contains labels for data model and other 
	 * elements that are internationalized 
	 */
	public String getObservationLabelTable();

	/**
	 * Populates the observation label table with all the list elements
	 * and tree nodes in the result set.
	 * @param c
	 * @param session
	 * @throws SQLException
	 */
	public default void populateListTreeDataTable(Connection c, Session session) throws SQLException {
		
		
		StringBuilder sql = new StringBuilder();
		sql.append( "CREATE TABLE "); //$NON-NLS-1$
		sql.append( getObservationLabelTable() );
		sql.append("(uuid char(16) for bit data, value varchar(1024))"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		sql = new StringBuilder();
		
		sql.append(" select distinct "); //$NON-NLS-1$
		sql.append( "case when list_element_uuid is not null then list_element_uuid "); //$NON-NLS-1$
		sql.append(" when tree_node_uuid is not null then tree_node_uuid else null end, "); //$NON-NLS-1$
		sql.append( "case when list_element_uuid is not null then 'l' "); //$NON-NLS-1$
		sql.append(" when tree_node_uuid is not null then 't' else null end, "); //$NON-NLS-1$
		sql.append(" r.ca_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append( getQueryDataTable() + " r on "); //$NON-NLS-1$
		sql.append( tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" WHERE list_element_uuid is not null or tree_node_uuid is not null "); //$NON-NLS-1$
		sql.append(" UNION DISTINCT "); //$NON-NLS-1$
		sql.append( "SELECT distinct "); //$NON-NLS-1$
		sql.append( tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid, 'l', "); //$NON-NLS-1$
		sql.append(" r.ca_uuid"); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append( getQueryDataTable()  + " r on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttribute.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid"); //$NON-NLS-1$
				
		QueryPlugIn.logSql(sql.toString());
		
		
		String sql2 = "INSERT INTO " + getObservationLabelTable() + " VALUES (?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2);
		int count = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				byte[] t = rs.getBytes(1);
				if (t == null) continue;
				
				UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
				if (uuid == null) continue;
				
				String type = rs.getString(2);
				UUID cauuid = UuidUtils.byteToUUID(rs.getBytes(3));
				
				String label = null;
				if (type.equals("l")) { //$NON-NLS-1$
					label = QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
				}else if (type.equals("t")) { //$NON-NLS-1$
					label = QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
				}
				if (label == null) continue;
				
				statement.setBytes(1,  UuidUtils.uuidToByte((UUID)uuid));
				statement.setString(2, label);
				statement.addBatch();
				count++;
				if (count >= 100){
					statement.executeBatch();
					count = 0;
				}
			}
			statement.executeBatch();
		}
	}
}
