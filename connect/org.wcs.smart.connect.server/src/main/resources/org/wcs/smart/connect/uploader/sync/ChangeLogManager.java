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
		Object rev = q.uniqueResult();
		if (rev == null){
			return -1;
		}
		return ((BigInteger)rev).longValueExact();
	}
	
	public boolean constains(Session s, ChangeLogItem item){
		SQLQuery query = s.createSQLQuery("SELECT count(*) from " + CHANGE_LOG_TABLE + " WHERE uuid = ?");
		query.setParameter(0, item.getUuid(), PostgresUUIDType.INSTANCE);
		
		List<?> data = query.list();
		if (data.size() != 1){ return false; }
		if ( ((BigInteger)data.get(0)).longValueExact()  > 0) return true;
		return false;
		
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
		query.append("SELECT a.uuid, a.revision, a.action, a.filename, a.tablename, ");
		query.append("a.key1_fieldname, a.key1, a.key2_fieldname, a.key2_str, a.key2_uuid, a.ca_uuid "); 
		query.append("FROM " + CHANGE_LOG_TABLE + " a,"); 
		query.append("( SELECT max(revision) as maxrevision, ");
		query.append("action, filename, tablename, key1_fieldname, key1,"); 
		query.append("key2_fieldname, key2_str, key2_uuid  FROM ");
		query.append(CHANGE_LOG_TABLE);
		query.append(" WHERE ca_uuid = :ca1 ");
		query.append(" AND revision > :revision1 ");
		query.append("GROUP BY action, filename, tablename, key1_fieldname, key1,"); 
		query.append("key2_fieldname, key2_str, key2_uuid) c ");
		query.append("where a.ca_uuid = :ca2 "); 
		query.append("and a.action = c.action ");
		query.append("and (a.filename = c.filename or (a.filename is null and c.filename is null)) ");
		query.append("and (a.tablename = c.tablename or (a.tablename is null and c.tablename is null)) ");
		query.append("and (a.key1_fieldname = c.key1_fieldname or (a.key1_fieldname is null and c.key1_fieldname is null)) ");
		query.append("and (a.key1 = c.key1 or (a.key1 is null and c.key1 is null)) ");
		query.append("and (a.key2_fieldname = c.key2_fieldname or (a.key2_fieldname is null and c.key2_fieldname is null)) ");
		query.append("and (a.key2_uuid = c.key2_uuid or (a.key2_uuid is null and c.key2_uuid is null)) ");
		query.append("and (a.key2_str = c.key2_str or (a.key2_str is null and c.key2_str is null)) ");
		query.append("and c.maxrevision = a.revision ");
		query.append("AND a.revision > :revision2 ORDER BY c.maxrevision ");
//		System.out.println(query.toString());
		
		SQLQuery hquery = session.createSQLQuery(query.toString());
		hquery.setParameter("ca1", caUuid, PostgresUUIDType.INSTANCE);
		hquery.setLong("revision1", startRevision);
		hquery.setParameter("ca2", caUuid, PostgresUUIDType.INSTANCE);
		hquery.setLong("revision2", startRevision);
		hquery.addScalar("uuid", PostgresUUIDType.INSTANCE);
		hquery.addScalar("revision");
		hquery.addScalar("action");
		hquery.addScalar("filename");
		hquery.addScalar("tablename");
		hquery.addScalar("key1_fieldname");
		hquery.addScalar("key1", PostgresUUIDType.INSTANCE);
		hquery.addScalar("key2_fieldname");
		hquery.addScalar("key2_str");
		hquery.addScalar("key2_uuid", PostgresUUIDType.INSTANCE);
		hquery.addScalar("ca_uuid", PostgresUUIDType.INSTANCE);
		
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
			ci.setKey1( (UUID)results.get(i++));
			ci.setFieldName2((String)results.get(i++));
			ci.setKey2String((String)results.get(i++));
			ci.setKey2( (UUID)results.get(i++));
			ci.setConservationArea(caUuid);
			items.add(ci);
		}
		
		return items;
	}
}
