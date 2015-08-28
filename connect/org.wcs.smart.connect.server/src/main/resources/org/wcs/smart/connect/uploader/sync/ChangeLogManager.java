package org.wcs.smart.connect.uploader.sync;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;

public enum ChangeLogManager {
	INSTANCE;
	
	private static final String CHANGE_LOG_TABLE = "connect.change_log";
	
	public long getLastRevision(Session s, UUID ca){
		String sql = "SELECT max(revision) FROM " + CHANGE_LOG_TABLE + " WHERE ca_uuid = :ca";
		SQLQuery q = s.createSQLQuery(sql);
		q.setParameter("ca", ca, PostgresUUIDType.INSTANCE);
		Long bi = ((BigInteger)q.uniqueResult()).longValueExact();
		return bi;
	}
	
	public void insertItem(Session s, ChangeLogItem item){
		StringBuilder sb = new StringBuilder();
		sb.append(" INSERT INTO ");
		sb.append(CHANGE_LOG_TABLE);
		sb.append(" (uuid, action, filename, tablename, ca_uuid, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid)");
		sb.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		SQLQuery query = s.createSQLQuery(sb.toString());
		
		
		query.setParameter(0, item.getUuid(),PostgresUUIDType.INSTANCE);
		query.setParameter(1, item.getAction().name());
		query.setParameter(2, item.getFileName());
		query.setParameter(3, item.getTableName());
		query.setParameter(4, item.getConservationArea(),PostgresUUIDType.INSTANCE);
		query.setParameter(5, item.getFieldName1());
		query.setParameter(6, item.getKey1(),PostgresUUIDType.INSTANCE);
		query.setParameter(7, item.getFieldName2());
		query.setParameter(8, item.getKey2String());
		query.setParameter(9, item.getKey2(),PostgresUUIDType.INSTANCE);
		query.executeUpdate();
		
	}
	
	public List<ChangeLogItem> getItems(Session session, UUID caUuid, long startRevision){
		StringBuilder query = new StringBuilder();
		query.append("SELECT uuid, revision, action, filename, tablename, ");
		query.append("key1_fieldname, key1, key2_fieldname, key2_str, ");
		query.append("key2_uuid, ca_uuid ");
		query.append("FROM smart.connect_change_log ");
		query.append("where ca_uuid = :ca");
		query.append(" AND revision IN (");
		query.append("select max(revision) as revision ");
		query.append(" from smart.CONNECT_CHANGE_LOG ");
		query.append("group by filename,  tablename, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid)");			
		query.append(" AND revision > :revision");
		query.append(" ORDER BY revision ");
		
		SQLQuery hquery = session.createSQLQuery(query.toString());
		hquery.setParameter("ca", caUuid, PostgresUUIDType.INSTANCE);
		hquery.setLong("revision", startRevision);

		ScrollableResults results = hquery.scroll(ScrollMode.FORWARD_ONLY);
		List<ChangeLogItem> items = new ArrayList<ChangeLogItem>();
		
		while(results.next()){
			int i = 0;
			ChangeLogItem ci = new ChangeLogItem();
			ci.setUuid((UUID)results.get(i++));
			ci.setRevision( ((BigInteger)results.get(i++)).longValue() );
			ci.setAction(Action.valueOf((String)results.get(i++)));
			ci.setFileName((String)results.get(i++));
			ci.setTableName((String)results.get(i++));
			ci.setFieldName1((String)results.get(i++));
			ci.setKey1((UUID)results.get(i++));
			ci.setFieldName2((String)results.get(i++));
			ci.setKey2String((String)results.get(i++));
			ci.setKey2((UUID)results.get(i++));
			ci.setConservationArea(caUuid);
			items.add(ci);
		}	
		return items;
	}
}
