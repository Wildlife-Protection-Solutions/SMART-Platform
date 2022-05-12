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

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.datastore.FileStoreWatcher;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Action;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;

/**
 * Postgresql specific manager for interacting with the
 * change log table.
 * 
 * @author Emily
 *
 */
public enum ChangeLogManager {
	INSTANCE;
	
	private static final String CHANGE_LOG_TABLE = "connect.change_log"; //$NON-NLS-1$
	private static final String CHANGE_LOG_INFO_TABLE = "connect.change_log_history"; //$NON-NLS-1$
	
	
	private FileStoreWatcher fileWatcher = null;
	private Thread fileStoreReplication;
	private UUIDGenerator uuidGenerator = null;
	

	/**
	 * Uses Hibernate to generate uuid for an object.
	 * 
	 * @param session
	 * @param object
	 * @return
	 */
	public UUID generateUuid(Session session, Object object) {
		if (uuidGenerator != null) return (UUID) uuidGenerator.generate((SessionImplementor)session, object);
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
		UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
		return (UUID) uuidGenerator.generate((SessionImplementor)session, object);
		
	}
	
	public void watchFilestore(SessionFactory sf)  throws IOException {
		fileWatcher = new FileStoreWatcher(sf);

		/*
		 * We may want to add code here to ignore changes to informant
		 * files; however that is only necessaary if we are going to be editing informant
		 * files on the server
		 */
		fileWatcher.register( DataStoreManager.INSTANCE.getRootDirectory()  );
		//run filestore watcher in new thread (background)		
		fileStoreReplication = new Thread(fileWatcher);
		fileStoreReplication.start();
	}
	
	public void shutDownFilestoreWatcher() throws IOException {
		if (fileWatcher != null){
			fileWatcher.deregister();
			fileWatcher = null;
		}
		
		if (fileStoreReplication != null){
			fileStoreReplication.interrupt();
			fileStoreReplication = null;
		}
	}
	/**
	 * Disables the writing of changes to the change log table for a specific Conservation Area.  When 
	 * disabled all changes made to database are not logged to the log table (for the given Conservation
	 * Area).  Other Conservation Area modifications will still be written.
	 * Also disables the filestore watcher for the Conservation Area.
	 * 
	 */
	//ca.getuuid must be the uuid with -'s removed
	public void disableChangeTracking(ConservationAreaInfo ca, Session session)  throws IOException {
		session.createNativeQuery("SET  \"ca.trigger.t" + UuidUtils.uuidToString(ca.getUuid()) + "\" = false ").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
		fileWatcher.ignoreCa(ca);
	}
	
	
	/**
	 * Re-enables the writing of changes to the change log table for a specific Conservation Area.
	 * Also re-enabled the filestore watcher for the Conservation Area
	 *  
	 */
	//ca.getuuid must be the uuid with -'s removed
	public void enableChangeTracking(ConservationAreaInfo ca, Session session)  throws IOException {
		session.createNativeQuery("SET  \"ca.trigger.t" +UuidUtils.uuidToString(ca.getUuid()) + "\" = true ").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
		fileWatcher.addCa(ca);
	}
	
	/**
	 * Delete all change log items with a revision 
	 * less than or equal to the maxrevision for the given conservation area.  
	 * Inserts a record into the change_log_history table to track
	 * the last deleted revision
	 * 
	 * @param s
	 * @param maxRevision a value greater than 0; representing the revisions to delete (inclusive)
	 * @return
	 */
	public void deleteItems(Session s, Long maxRevision, UUID caUuid){
		if (maxRevision < 0) return;
		String sql = "DELETE FROM " + CHANGE_LOG_TABLE + " WHERE revision <= :maxrevision and ca_uuid = :cauuid"; //$NON-NLS-1$ //$NON-NLS-2$
		NativeQuery<?> q = s.createNativeQuery(sql);
		q.setParameter("maxrevision", maxRevision); //$NON-NLS-1$
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		q.executeUpdate();
		
		q = s.createNativeQuery("SELECT count(*) FROM " + CHANGE_LOG_INFO_TABLE + " WHERE ca_uuid = :cauuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		Long cnt = ((BigInteger) q.uniqueResult()).longValue();
		
		if (cnt == 0){
			q = s.createNativeQuery("INSERT INTO " + CHANGE_LOG_INFO_TABLE + " (ca_uuid, last_delete_revision) VALUES (:cauuid, :revision)"); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			q = s.createNativeQuery("UPDATE " + CHANGE_LOG_INFO_TABLE + " SET last_delete_revision = :revision WHERE ca_uuid = :cauuid"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		q.setParameter("revision", maxRevision); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	/**
	 * The last delete revision from the change log history table.  This will
	 * return -1 if there is no record in the history table.
	 * @param s
	 * @param caUuid
	 * @return
	 */
	private Long getLastDeleteRevision(Session s, UUID caUuid){
		NativeQuery<?> q = s.createNativeQuery("SELECT last_delete_revision FROM " + CHANGE_LOG_INFO_TABLE + " where ca_uuid = :cauuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		BigInteger value = (BigInteger)q.uniqueResult();
		if (value == null){
			return -1l;
		}
		return value.longValue();
	}
	/**
	 * Delete all change log items for the given conservation area.  
	 * 
	 * @param s
	 * @param maxRevision a value greater than 0; representing the revisions to delete (inclusive)
	 * @return
	 */
	public void deleteItems(Session s, UUID caUuid){
		String sql = "DELETE FROM " + CHANGE_LOG_TABLE + " WHERE ca_uuid = :cauuid";  //$NON-NLS-1$//$NON-NLS-2$
		NativeQuery<?> q = s.createNativeQuery(sql);
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		q.executeUpdate();
		
		sql = "DELETE FROM " + CHANGE_LOG_INFO_TABLE + " WHERE ca_uuid = :cauuid"; //$NON-NLS-1$ //$NON-NLS-2$
		q = s.createNativeQuery(sql);
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
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
	public long getLastRevision(Session s, LocalDateTime maxDate, UUID caUuid){
		String sql = "SELECT max(revision) FROM " + CHANGE_LOG_TABLE + " WHERE datetime < :maxdate and ca_uuid = :cauuid"; //$NON-NLS-1$ //$NON-NLS-2$
		NativeQuery<?> q = s.createNativeQuery(sql);
		q.setParameter("maxdate", maxDate); //$NON-NLS-1$
		q.setParameter("cauuid", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		BigInteger revision = (BigInteger) q.uniqueResult();
		if (revision == null){
			return -1;
		}
		return revision.longValue();
	}
	
	public long getLastRevision(Session s, UUID ca){
		String sql = "SELECT max(revision) FROM " + CHANGE_LOG_TABLE + " WHERE ca_uuid = :ca"; //$NON-NLS-1$ //$NON-NLS-2$
		NativeQuery<?> q = s.createNativeQuery(sql);
		q.setParameter("ca", ca, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		Object rev = q.uniqueResult();
		if (rev == null){
			//there is nothing in the change log; check the history
			//as this will have the last revision item
			return getLastDeleteRevision(s, ca);
		}
		return ((BigInteger)rev).longValueExact();
	}
	
	/**
	 * Determines if the change log table contains an item with the
	 * smart uuid.
	 * 
	 * @param s
	 * @param item the item to search for 
	 * @return <code>true</code> if item exists, <code>false</code> otherwise
	 */
	public boolean constains(Session s, ChangeLogItem item){
		NativeQuery<?> query = s.createNativeQuery("SELECT count(*) from " + CHANGE_LOG_TABLE + " WHERE uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		query.setParameter("uuid", item.getUuid(), PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		
		List<?> data = query.list();
		if (data.size() != 1){ return false; }
		if ( ((BigInteger)data.get(0)).longValueExact()  > 0) return true;
		return false;
	}
	
	/**
	 * Inserts a new item into the change log table.
	 * @param s
	 * @param item
	 */
	public void insertItem(Session s, ChangeLogItem item){
		StringBuilder sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(CHANGE_LOG_TABLE);
		sb.append(" (uuid, action, filename, tablename, ca_uuid, key1_fieldname, key1, key2_fieldname, key2_str, key2_uuid)"); //$NON-NLS-1$
		sb.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
		
		NativeQuery<?> query = s.createNativeQuery(sb.toString());
		
		query.setParameter(1, item.getUuid(),PostgresUUIDType.INSTANCE);
		query.setParameter(2, item.getAction().name());
		query.setParameter(3, item.getFileName());
		query.setParameter(4, item.getTableName());
		query.setParameter(5, item.getConservationArea(),PostgresUUIDType.INSTANCE);
		query.setParameter(6, item.getFieldName1());
		query.setParameter(7, item.getKey1(),PostgresUUIDType.INSTANCE);
		query.setParameter(8, item.getFieldName2());
		query.setParameter(9, item.getKey2String());
		query.setParameter(10, item.getKey2(),PostgresUUIDType.INSTANCE);
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
	public List<ChangeLogItem> getItems(Session session, UUID caUuid, long startRevision, WorkItem item){

		//first check that the start revision is after the last clean up revision
		if (startRevision < getLastDeleteRevision(session, caUuid)){
//			//some change log items were removed so we cannot sync this class
			throw new SmartConnectException(Status.NOT_FOUND, Messages.getString("ChangeLogManager.ChangeLogError", item.getLocale())); //$NON-NLS-1$
		}
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT a.uuid, a.revision, a.action, a.filename, a.tablename, "); //$NON-NLS-1$
		query.append("a.key1_fieldname, a.key1, a.key2_fieldname, a.key2_str, a.key2_uuid, a.ca_uuid ");  //$NON-NLS-1$
		query.append("FROM " + CHANGE_LOG_TABLE + " a,");  //$NON-NLS-1$ //$NON-NLS-2$
		query.append("( SELECT max(revision) as maxrevision, "); //$NON-NLS-1$
		query.append("action, filename, tablename, key1_fieldname, key1,");  //$NON-NLS-1$
		query.append("key2_fieldname, key2_str, key2_uuid  FROM "); //$NON-NLS-1$
		query.append(CHANGE_LOG_TABLE);
		query.append(" WHERE ca_uuid = :ca1 "); //$NON-NLS-1$
		query.append(" AND revision > :revision1 "); //$NON-NLS-1$
		query.append("GROUP BY action, filename, tablename, key1_fieldname, key1,");  //$NON-NLS-1$
		query.append("key2_fieldname, key2_str, key2_uuid) c "); //$NON-NLS-1$
		query.append("where a.ca_uuid = :ca2 ");  //$NON-NLS-1$
		query.append("and a.action = c.action "); //$NON-NLS-1$
		query.append("and (a.filename = c.filename or (a.filename is null and c.filename is null)) "); //$NON-NLS-1$
		query.append("and (a.tablename = c.tablename or (a.tablename is null and c.tablename is null)) "); //$NON-NLS-1$
		query.append("and (a.key1_fieldname = c.key1_fieldname or (a.key1_fieldname is null and c.key1_fieldname is null)) "); //$NON-NLS-1$
		query.append("and (a.key1 = c.key1 or (a.key1 is null and c.key1 is null)) "); //$NON-NLS-1$
		query.append("and (a.key2_fieldname = c.key2_fieldname or (a.key2_fieldname is null and c.key2_fieldname is null)) "); //$NON-NLS-1$
		query.append("and (a.key2_uuid = c.key2_uuid or (a.key2_uuid is null and c.key2_uuid is null)) "); //$NON-NLS-1$
		query.append("and (a.key2_str = c.key2_str or (a.key2_str is null and c.key2_str is null)) "); //$NON-NLS-1$
		query.append("and c.maxrevision = a.revision "); //$NON-NLS-1$
		query.append("AND a.revision > :revision2 ORDER BY c.maxrevision "); //$NON-NLS-1$
//		System.out.println(query.toString());
		
		NativeQuery<?> hquery = session.createNativeQuery(query.toString());

		hquery.setParameter("ca1", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		hquery.setParameter("revision1", startRevision); //$NON-NLS-1$
		hquery.setParameter("ca2", caUuid, PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		hquery.setParameter("revision2", startRevision); //$NON-NLS-1$
		//TODO: review this
		hquery.addScalar("uuid", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		hquery.addScalar("revision"); //$NON-NLS-1$
		hquery.addScalar("action"); //$NON-NLS-1$
		hquery.addScalar("filename"); //$NON-NLS-1$
		hquery.addScalar("tablename"); //$NON-NLS-1$
		hquery.addScalar("key1_fieldname"); //$NON-NLS-1$
		hquery.addScalar("key1", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		hquery.addScalar("key2_fieldname"); //$NON-NLS-1$
		hquery.addScalar("key2_str"); //$NON-NLS-1$
		hquery.addScalar("key2_uuid", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		hquery.addScalar("ca_uuid", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
				
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
