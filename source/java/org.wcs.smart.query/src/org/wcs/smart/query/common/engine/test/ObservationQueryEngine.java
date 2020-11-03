package org.wcs.smart.query.common.engine.test;

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
import org.wcs.smart.query.common.engine.IObservationQueryResultItem;
import org.wcs.smart.util.UuidUtils;

public interface ObservationQueryEngine<T extends IObservationQueryResultItem> extends IDesktopWOEngine<T>  {
	
	public String getObservationLabelTable();

	public default void populateListTreeDataTable(Connection c, Session session) throws SQLException {
		
		
		StringBuilder sql = new StringBuilder();
		sql.append( "CREATE TABLE ");
		sql.append( getObservationLabelTable() );
		sql.append("(uuid char(16) for bit data, value varchar(1024))"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		sql = new StringBuilder();
		
		sql.append(" select distinct ");
		sql.append( "case when list_element_uuid is not null then list_element_uuid ");
		sql.append(" when tree_node_uuid is not null then tree_node_uuid else null end, ");
		sql.append( "case when list_element_uuid is not null then 'l' ");
		sql.append(" when tree_node_uuid is not null then 't' else null end, ");
		sql.append(" r.ca_uuid ");
		sql.append(" FROM ");
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join ");
		sql.append( getQueryDataTable() + " r on "); //$NON-NLS-1$
		sql.append( tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" WHERE list_element_uuid is not null or tree_node_uuid is not null ");
		sql.append(" UNION DISTINCT ");
		sql.append( "SELECT distinct ");
		sql.append( tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid, 'l', ");
		sql.append(" r.ca_uuid");
		sql.append(" FROM ");
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join ");
		sql.append( getQueryDataTable()  + " r on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" inner join ");
		sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
		sql.append(" on ");
		sql.append(tablePrefix(WaypointObservationAttribute.class) + ".uuid = ");
		sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid");
				
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
				if (type.equals("l")) {
					label = QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
				}else if (type.equals("t")) {
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
