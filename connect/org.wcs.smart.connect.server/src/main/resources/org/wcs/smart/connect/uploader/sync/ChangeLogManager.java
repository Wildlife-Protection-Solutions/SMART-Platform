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
package org.wcs.smart.connect.uploader.sync;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Action;

public enum ChangeLogManager {
	INSTANCE;
	
	private static final String CHANGE_LOG_TABLE = "connect.change_log";
	
	/**
	 * Delete all change log items with a revision 
	 * less than the maxrevision for the given conservation area.  
	 * This ensures there is always
	 * one record per CA in the change log table.  If the
	 * change log table is empty of a CA we have never cleaned up anything.
	 * 
	 * @param s
	 * @param maxRevision a value greater than 0; representing the revisions to delete (inclusive)
	 * @return
	 */
	public void deleteItems(Session s, Long maxRevision, UUID caUuid){
		if (maxRevision < 0) return;
		
		String sql = "DELETE FROM " + CHANGE_LOG_TABLE + " WHERE revision < :maxrevision and ca_uuid = :cauuid";
		SQLQuery q = s.createSQLQuery(sql);
		q.setParameter("maxrevision", maxRevision);
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE);
		q.executeUpdate();
	}
	
	/**
	 * Find the maximum revision number that is older than the given date for a 
	 * given conservation area
	 *  
	 * @param s
	 * @param maxDate
	 * @return
	 */
	public long getLastRevision(Session s, Date maxDate, UUID caUuid){
		String sql = "SELECT max(revision) FROM " + CHANGE_LOG_TABLE + " WHERE datetime < :maxdate and ca_uuid = :cauuid";
		SQLQuery q = s.createSQLQuery(sql);
		q.setParameter("maxdate", maxDate);
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE);
		BigInteger revision = (BigInteger) q.uniqueResult();
		if (revision == null){
			return -1;
		}
		return revision.longValue();
	}
	
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
	
	/**
	 * Finds all items in the change log table that occur for a given conservation
	 * area after the given start revision.  If there is a chance that items have been removed
	 * then this will thrown an exception.
	 * To determine if items may have been removed, it looks at the last revision cleaned up
	 * from the system in the change_log_info table and compares it to the request revision.  If
	 * it's greater then the requested version than there is a chance items have been removed. 
	 * @param session
	 * @param caUuid
	 * @param startRevision
	 * @return
	 */
	public List<ChangeLogItem> getItems(Session session, UUID caUuid, long startRevision){
		//first check that the start revision is after the last clean up revision
		
		String sql = "SELECT min(revision) FROM " +  CHANGE_LOG_TABLE + " WHERE ca_uuid = :cauuid";
		BigInteger lastDeleteRevision = (BigInteger)session.createSQLQuery(sql)
				.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE)
				.uniqueResult();
		Long lastDelete = -1l;
		if (lastDeleteRevision != null){
			lastDelete = lastDeleteRevision.longValue();
		}
		if (startRevision < lastDelete){
			//some change log items were removed so we cannot sync this class
			throw new SmartConnectException(Status.NOT_FOUND, "The change log table on server has been cleaned up since your last request.  You must re-download the entire conservation area from SMART Connect to reestablish replication.");
		}
		
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
