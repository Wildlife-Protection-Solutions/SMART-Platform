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
package org.wcs.smart.connect.server.replication;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ChangeLogItem;
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
		Query query = s.createQuery("DELETE FROM ChangeLogItem WHERE conservationArea = ?");
		query.setBinary(0, UuidUtils.uuidToByte(ca.getUuid()));
		query.executeUpdate();
	}
	
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
		String tableName = ((AbstractEntityPersister)s.getSessionFactory()
				.getClassMetadata(ChangeLogItem.class))
				.getTableName();
		
		
		StringBuilder sb = new StringBuilder();
		sb.append(" INSERT INTO ");
		sb.append(tableName);
		sb.append(" (uuid, action, filename, tablename, ca_uuid, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid, source)");
		sb.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		SQLQuery query = s.createSQLQuery(sb.toString());
		if (item.getUuid() == null){
			item.setUuid( UuidUtils.byteToUUID(DerbyUtils.createUuid()));
		}
		query.setBinary(0, UuidUtils.uuidToByte(item.getUuid()));
		query.setString(1, item.getAction().name());
		query.setString(2, item.getFileName());
		query.setString(3, item.getTableName());
		query.setBinary(4, UuidUtils.uuidToByte(item.getConservationArea()));
		query.setString(5, item.getFieldName1());
		query.setBinary(6, item.getKey1() == null ? null : UuidUtils.uuidToByte(item.getKey1()));
		query.setString(7, item.getFieldName2());
		query.setString(8, item.getKey2String());
		query.setBinary(9, item.getKey2() == null ? null : UuidUtils.uuidToByte(item.getKey2()));
		query.setString(10, item.getSource().name());
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
		Long x = (Long)s.createCriteria(ChangeLogItem.class)
				.add(Restrictions.eq("conservationArea", ca.getUuid()))
				.add(Restrictions.eq("source", ChangeLogItem.Source.LOCAL))
				.setProjection(Projections.max("revision"))
				.uniqueResult();
		if (x == null) return null;
		return x;
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
		String tableName = ((AbstractEntityPersister)s.getSessionFactory().getClassMetadata(ChangeLogItem.class)).getTableName();
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT a.uuid, a.revision, a.action, a.filename, a.tablename, ");
		query.append("a.key1_fieldname, a.key1, a.key2_fieldname, a.key2_str, a.key2_uuid, a.ca_uuid, a.source "); 
		query.append("FROM " + tableName + " a,"); 
		query.append("( SELECT max(revision) as maxrevision, ");
		query.append("action, filename, tablename, key1_fieldname, key1,"); 
		query.append("key2_fieldname, key2_str, key2_uuid  FROM ");
		query.append(tableName);
		query.append(" WHERE ca_uuid = :ca1 ");
		query.append(" AND revision > :revision1 ");
		query.append(" AND source = '" + ChangeLogItem.Source.LOCAL + "'");
		query.append("GROUP BY action, filename, tablename, key1_fieldname, key1,"); 
		query.append("key2_fieldname, key2_str, key2_uuid) c ");
		query.append("where a.ca_uuid = :ca2 "); 
		query.append("and a.action = c.action ");
		query.append("and (a.filename = c.filename or (a.filename is null and c.filename is null)) ");
		query.append("and (a.tablename = c.tablename or (a.tablename is null and c.tablename is null)) ");
		query.append("and (a.key1_fieldname = c.key1_fieldname or (a.key1_fieldname is null and c.key1_fieldname is null)) ");
		query.append("and (a.key1 = c.key1 or (a.key1 is null and c.key1 is null)) ");
		query.append("and (a.source = '" + ChangeLogItem.Source.LOCAL + "') ");
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
		
		hquery.addEntity(ChangeLogItem.class);
		
		return (List<ChangeLogItem>)hquery.list();
		
	}
}
