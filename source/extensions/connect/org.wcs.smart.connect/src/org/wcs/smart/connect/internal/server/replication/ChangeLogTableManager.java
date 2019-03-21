/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.internal.server.replication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.TextType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Class for managing the change log table. 
 * @author Emily
 *
 */
public enum ChangeLogTableManager {

	INSTANCE;
	
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
		Query<?> query = s.createQuery("DELETE FROM ChangeLogItem WHERE conservationArea = :ca"); //$NON-NLS-1$
		query.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		query.executeUpdate();
	}

	/**
	 * Deletes all change log items associated with the given
	 * conservation area that have a revision number less than
	 * or equals to the revision number.
	 * 
	 * @param s
	 * @param ca
	 * @param maxRevision
	 */
	public void deleteRecords(Session s, ConservationArea ca, long maxRevision){
		Query<?> query = s.createQuery("DELETE FROM ChangeLogItem WHERE conservationArea = :ca and revision <= :maxRevision"); //$NON-NLS-1$
		query.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		query.setParameter("maxRevision", maxRevision); //$NON-NLS-1$
		query.executeUpdate();
	}
	
	/**
	 * Determines in the change log table contains a given change
	 * log item.
	 * 
	 * @param s
	 * @param item
	 * @return
	 */
	public boolean constains(Session s, ChangeLogItem item){
		ChangeLogItem litem = (ChangeLogItem) s.get(ChangeLogItem.class, item.getUuid());
		if (litem == null) return false;
		return true;
	}
		
	/**
	 * Insert item into table.
	 * @param s
	 * @param item
	 */
	public void addItem(Session s, ChangeLogItem item){		
		//i tired doing just a s.save(item) however I could not
		//get this to work with the revision identify field
		String tableName = HibernateManager.getTableName(ChangeLogItem.class);		
		
		//wait until we get a lock on this table to
		//continue
		s.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				boolean doAgain = true;
				while(doAgain){
					try{
						c.createStatement().execute("LOCK TABLE " + ChangeLogItem.TABLENAME + " IN EXCLUSIVE MODE"); //$NON-NLS-1$ //$NON-NLS-2$
						doAgain = false;
					}catch (Exception ex){
						
					}
				}
				
			}});
		
		
		StringBuilder sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(tableName);
		sb.append(" (revision, uuid, action, filename, tablename, ca_uuid, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid, source)"); //$NON-NLS-1$
		sb.append(" VALUES (smart.next_revision_id(:cauuid), :uuid, :action, :filename, :tablename, :cauuid2, :key1field, :key1, :key2field, :key2str, :key2uuid, :source)"); //$NON-NLS-1$
		
		NativeQuery<?> query = s.createNativeQuery(sb.toString());
		if (item.getUuid() == null){
			item.setUuid( UuidUtils.byteToUUID(DerbyUtils.createUuid()));
		}
		query.setParameter("cauuid", UuidUtils.uuidToByte(item.getConservationArea())); //$NON-NLS-1$
		query.setParameter("uuid", UuidUtils.uuidToByte(item.getUuid())); //$NON-NLS-1$
		query.setParameter("action", item.getAction().name(), TextType.INSTANCE); //$NON-NLS-1$
		query.setParameter("filename", item.getFileName(), TextType.INSTANCE); //$NON-NLS-1$
		query.setParameter("tablename", item.getTableName(), TextType.INSTANCE); //$NON-NLS-1$
		query.setParameter("cauuid2", UuidUtils.uuidToByte(item.getConservationArea())); //$NON-NLS-1$
		query.setParameter("key1field", item.getFieldName1(), TextType.INSTANCE);  //$NON-NLS-1$
		query.setParameter("key1", item.getKey1() == null ? null : UuidUtils.uuidToByte(item.getKey1())); //$NON-NLS-1$
		query.setParameter("key2field", item.getFieldName2(), TextType.INSTANCE); //$NON-NLS-1$
		query.setParameter("key2str", item.getKey2String(), TextType.INSTANCE); //$NON-NLS-1$
		query.setParameter("key2uuid", item.getKey2() == null ? null : UuidUtils.uuidToByte(item.getKey2())); //$NON-NLS-1$
		query.setParameter("source", item.getSource().name()); //$NON-NLS-1$
		query.executeUpdate();
	}
	
	/**
	 * 
	 * @param s
	 * @param ca
	 * 
	 * @return the current maximum revision number from the change log 
	 * table for the conservation area or null
	 * if there is nothing in the change log table for the conservation area
	 */
	public Long getMaxLocalRevision(Session s, ConservationArea ca){
		return s.doReturningWork(new ReturningWork<Long>() {
			
			@Override
			public Long execute(Connection connection) throws SQLException {
				//prevents database locking with long save transactions
				String sql = "SELECT max(revision) FROM " //$NON-NLS-1$
						+ ChangeLogItem.TABLENAME 
						+  " WHERE source = 'LOCAL' and ca_uuid = ? ";  //$NON-NLS-1$
				try(PreparedStatement ps = connection.prepareStatement(sql)){
					ps.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
					try(ResultSet rs = ps.executeQuery()){
						if (rs.next()){
							Long value = rs.getLong(1);
							if (rs.wasNull()) return null;
							return value;
						}
					}
				}
				return null;
				
			}
		});

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
		String tableName = HibernateManager.getTableName(ChangeLogItem.class);
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT a.uuid, a.revision, a.action, a.filename, a.tablename, "); //$NON-NLS-1$
		query.append("a.key1_fieldname, a.key1, a.key2_fieldname, a.key2_str, a.key2_uuid, a.ca_uuid, a.source ");  //$NON-NLS-1$
		query.append("FROM " + tableName + " a,");  //$NON-NLS-1$ //$NON-NLS-2$
		query.append("( SELECT max(revision) as maxrevision, "); //$NON-NLS-1$
		query.append("action, filename, tablename, key1_fieldname, key1,");  //$NON-NLS-1$
		query.append("key2_fieldname, key2_str, key2_uuid  FROM "); //$NON-NLS-1$
		query.append(tableName);
		query.append(" WHERE ca_uuid = :ca1 "); //$NON-NLS-1$
		query.append(" AND revision > :revision1 "); //$NON-NLS-1$
		query.append(" AND source = '" + ChangeLogItem.Source.LOCAL + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		query.append("GROUP BY action, filename, tablename, key1_fieldname, key1,");  //$NON-NLS-1$
		query.append("key2_fieldname, key2_str, key2_uuid) c "); //$NON-NLS-1$
		query.append("where a.ca_uuid = :ca2 ");  //$NON-NLS-1$
		query.append("and a.action = c.action "); //$NON-NLS-1$
		query.append("and (a.filename = c.filename or (a.filename is null and c.filename is null)) "); //$NON-NLS-1$
		query.append("and (a.tablename = c.tablename or (a.tablename is null and c.tablename is null)) "); //$NON-NLS-1$
		query.append("and (a.key1_fieldname = c.key1_fieldname or (a.key1_fieldname is null and c.key1_fieldname is null)) "); //$NON-NLS-1$
		query.append("and (a.key1 = c.key1 or (a.key1 is null and c.key1 is null)) "); //$NON-NLS-1$
		query.append("and (a.source = '" + ChangeLogItem.Source.LOCAL + "') "); //$NON-NLS-1$ //$NON-NLS-2$
		query.append("and (a.key2_fieldname = c.key2_fieldname or (a.key2_fieldname is null and c.key2_fieldname is null)) "); //$NON-NLS-1$
		query.append("and (a.key2_uuid = c.key2_uuid or (a.key2_uuid is null and c.key2_uuid is null)) "); //$NON-NLS-1$
		query.append("and (a.key2_str = c.key2_str or (a.key2_str is null and c.key2_str is null)) "); //$NON-NLS-1$
		query.append("and c.maxrevision = a.revision "); //$NON-NLS-1$
		query.append("AND a.revision > :revision2 ORDER BY c.maxrevision "); //$NON-NLS-1$
//		System.out.println(query.toString());
		
		NativeQuery<ChangeLogItem> hquery = s.createNativeQuery(query.toString(), ChangeLogItem.class);
		hquery.setParameter("ca1", UuidUtils.uuidToByte(ca.getUuid())); //$NON-NLS-1$
		hquery.setParameter("revision1", startRevision); //$NON-NLS-1$
		hquery.setParameter("ca2", UuidUtils.uuidToByte(ca.getUuid())); //$NON-NLS-1$
		hquery.setParameter("revision2", startRevision); //$NON-NLS-1$
		
		return (List<ChangeLogItem>)hquery.list();
		
	}
}
