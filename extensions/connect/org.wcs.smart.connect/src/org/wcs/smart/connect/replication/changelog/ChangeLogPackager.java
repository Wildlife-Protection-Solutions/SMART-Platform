package org.wcs.smart.connect.replication.changelog;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

public class ChangeLogPackager {

	private ConservationArea ca;
	
	private long startRevision;
	private long endRevision;
	
	public ChangeLogPackager(){
		ca = SmartDB.getCurrentConservationArea();
	}
	
	private void doSomething(){
		List<ChangeLogItem> items = getChangeLogItems();
		endRevision = items.get(items.size() - 1).getRevision();
		
		
	}
	
	private List<ChangeLogItem> getChangeLogItems(){
		Session s = HibernateManager.openSession();
		List<ChangeLogItem> items = s.doReturningWork(new ReturningWork<List<ChangeLogItem>>() {

			@Override
			public List<ChangeLogItem> execute(Connection connection)
					throws SQLException {
				
				StringBuilder query = new StringBuilder();
				query.append("SELECT revision, action, filename, tablename, ");
				query.append("key1_fieldname, key1, key2_fieldname, key2_str, ");
				query.append("key2_uuid, ca_uuid ");
				query.append("FROM smart.connect_change_log ");
				query.append("where ca_uuid = x'" + UuidUtils.uuidToString(ca.getUuid()) + "'");
				query.append(" AND revision IN (");
				query.append("select max(revision) as revision, filename, tablename, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid");
				query.append(" from smart.CONNECT_CHANGE_LOG ");
				query.append("group by filename,  tablename, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid)");			
				query.append(" AND revision > " + startRevision);
				query.append(" ORDER BY revision ");

				List<ChangeLogItem> items = new ArrayList<ChangeLogItem>();
				ResultSet rs = connection.createStatement().executeQuery(query.toString());
				while(rs.next()){
					ChangeLogItem ci = new ChangeLogItem();
					ci.setRevision(rs.getLong("revision"));
					ci.setAction(Action.valueOf(rs.getString("action")));
					ci.setFileName(rs.getString("filename"));
					ci.setTableName(rs.getString("tablename"));
					ci.setFieldName1(rs.getString("key1_fieldname"));
					ci.setKey1( UuidUtils.byteToUUID( rs.getBytes("key1")));
					ci.setFieldName2(rs.getString("key2_fieldname"));
					ci.setKey2( UuidUtils.byteToUUID( rs.getBytes("key2")));
					ci.setKey2String(rs.getString("key2_str"));
					ci.setConservationArea(ca.getUuid());
					items.add(ci);
				}
				return items;
			}
		});
		s.close();
		
		return items;
	}
}
