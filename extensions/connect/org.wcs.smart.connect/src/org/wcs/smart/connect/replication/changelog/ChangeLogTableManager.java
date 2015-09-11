package org.wcs.smart.connect.replication.changelog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.BinaryType;
import org.hibernate.type.CharacterArrayType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

public enum ChangeLogTableManager {

	INSTANCE;
	
	public static final String CHANGE_LOG_TABLE = "smart.connect_change_log";
	
	/**
	 * Delete all records for a given conservation area.
	 * 
	 * Should be called when the conservation area is deleted, or
	 * re-uploaded to smart connect.
	 * 
	 * @param s
	 * @param ca
	 */
	public void deleteAll(Session s, ConservationArea ca){
		SQLQuery query = s.createSQLQuery("DELETE FROM " + CHANGE_LOG_TABLE + " WHERE ca_uuid = ?");
		query.setBinary(0, UuidUtils.uuidToByte(ca.getUuid()));
		query.executeUpdate();
	}
	
	public boolean constains(Session s, ChangeLogItem item){
		SQLQuery query = s.createSQLQuery("SELECT count(*) from " + CHANGE_LOG_TABLE + " WHERE uuid = ?");
		query.setBinary(0, UuidUtils.uuidToByte(item.getUuid()));
		
		List<?> data = query.list();
		if (data.size() != 1){ return false; }
		if ( (Integer)data.get(0)  > 0) return true;
		return false;
		
	}
	public void addItem(Session s, ChangeLogItem item){
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ");
		sb.append(CHANGE_LOG_TABLE);
		sb.append(" (uuid, action, filename, tablename, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid, ca_uuid) ");
		sb.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		SQLQuery query = s.createSQLQuery(sb.toString());
		if (item.getUuid() == null){
			item.setUuid(UuidUtils.byteToUUID(DerbyUtils.createUuid()));
		}
		
		query.setBinary(0, UuidUtils.uuidToByte(item.getUuid()));
		query.setString(1, item.getAction().name());
		query.setParameter(2, item.getFileName(),StringType.INSTANCE);
		query.setParameter(3, item.getTableName(),StringType.INSTANCE);
		query.setParameter(4, item.getFieldName1(),StringType.INSTANCE);
		query.setBinary(5, item.getKey1() == null ? null : UuidUtils.uuidToByte(item.getKey1()));
		query.setParameter(6, item.getFieldName2(),StringType.INSTANCE);
		query.setParameter(7, item.getKey2String(),StringType.INSTANCE);
		query.setBinary(8, item.getKey2() == null ? null : UuidUtils.uuidToByte(item.getKey2()));
		query.setBinary(9, UuidUtils.uuidToByte(item.getConservationArea()));
		
		query.executeUpdate();		
	}
	
	/**
	 * 
	 * @param s
	 * @param ca
	 * 
	 * @return the current maximum revision number from the change long table for the conservation area or null
	 * if there is nothing in the change log table for the conservation area
	 */
	public Long getMaxRevision(Session s, ConservationArea ca){
		SQLQuery query = s.createSQLQuery("SELECT max(revision) FROM " + CHANGE_LOG_TABLE + " WHERE ca_uuid = ?");
		query.setBinary(0, UuidUtils.uuidToByte(ca.getUuid()));
		Object x = query.uniqueResult();
		if (x == null){
			return null;
		}
		return ((BigInteger) x).longValue();
	}
	/**
	 * Load all change log items associated with a given conservation area.
	 * 
	 * @param s
	 * @param ca
	 * @param startRevision exclusive revision to start at (returns everything > startRevision)
	 * @return
	 */
	public List<ChangeLogItem> getAll(Session s, ConservationArea ca, long startRevision){
		
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
		
		SQLQuery hquery = s.createSQLQuery(query.toString());
		hquery.setBinary("ca1", UuidUtils.uuidToByte(ca.getUuid()));
		hquery.setLong("revision1", startRevision);
		hquery.setBinary("ca2", UuidUtils.uuidToByte(ca.getUuid()));
		hquery.setLong("revision2", startRevision);
		
		ScrollableResults results = hquery.scroll(ScrollMode.FORWARD_ONLY);
		List<ChangeLogItem> items = new ArrayList<ChangeLogItem>();
		
		while(results.next()){
			int i = 0;
			ChangeLogItem ci = new ChangeLogItem();
			ci.setUuid(UuidUtils.byteToUUID((byte[])results.get(i++)));
			ci.setRevision( ((BigInteger)results.get(i++)).longValue() );
			ci.setAction(Action.valueOf((String)results.get(i++)));
			ci.setFileName((String)results.get(i++));
			ci.setTableName((String)results.get(i++));
			ci.setFieldName1((String)results.get(i++));
			ci.setKey1( UuidUtils.byteToUUID( (byte[])results.get(i++)));
			ci.setFieldName2((String)results.get(i++));
			ci.setKey2String((String)results.get(i++));
			ci.setKey2( UuidUtils.byteToUUID( (byte[])results.get(i++)));
			ci.setConservationArea(ca.getUuid());
			items.add(ci);
		}
		
		return items;
	}
}
