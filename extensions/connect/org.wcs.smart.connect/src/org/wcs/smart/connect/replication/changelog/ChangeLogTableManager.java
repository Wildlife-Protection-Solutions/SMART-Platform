package org.wcs.smart.connect.replication.changelog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
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
	
	/**
	 * Load all change log items associated with a given conservation area.
	 * 
	 * @param s
	 * @param ca
	 * @param startRevision exclusive revision to start at (returns everything >= revision)
	 * @return
	 */
	public List<ChangeLogItem> getAll(Session s, ConservationArea ca, long startRevision){
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
		
		SQLQuery hquery = s.createSQLQuery(query.toString());
		hquery.setBinary("ca", UuidUtils.uuidToByte(ca.getUuid()));
		hquery.setLong("revision", startRevision);

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
