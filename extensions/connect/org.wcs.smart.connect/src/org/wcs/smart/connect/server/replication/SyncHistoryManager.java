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

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Tools for managing the sync history table.
 * 
 * @author Emily
 *
 */
public enum SyncHistoryManager {
	
	INSTANCE;
	
	/**
	 * Deletes all records for a given Conservation Area.
	 * 
	 * @param s
	 * @param ca
	 */
	public void deleteAll(Session s, ConservationArea ca){
		String hsql = "DELETE FROM ConnectSyncHistoryRecord WHERE conservationArea = :ca";
		s.createQuery(hsql).setParameter("ca", ca).executeUpdate();
	}
	
	/**
	 * Deletes all sync history records from the database
	 * of the given type that occur on or before the given
	 * date.
	 * 
	 * @param s
	 * @param ca
	 * @param type
	 * @param endDate
	 */
	public void deleteRecords(Session s, ConservationArea ca, Type type, Date endDate){
		Query query = s.createQuery("DELETE FROM ConnectSyncHistoryRecord WHERE type = :type and conservationArea = :ca and datetime <= :datetime");
		query.setParameter("ca", ca);
		query.setParameter("type", type);
		query.setParameter("datetime", endDate);
		query.executeUpdate();
	}
	
	/**
	 * Sets any active download records to a status of Error 
	 * for the given conservation area.
	 */
	public void errorActiveDownloadRecords(ConservationArea ca){
		List<ConnectSyncHistoryRecord> items = SyncHistoryManager.INSTANCE.getActiveSyncRecords(ca, Type.DOWNLOAD);

		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			for (ConnectSyncHistoryRecord r : items){
				r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
				s.saveOrUpdate(r);
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}
	
	/**
	 * Sets any active upload records to a status of Error 
	 * for the given conservation area.
	 */
	public void errorActiveUploadRecords(ConservationArea ca){
		List<ConnectSyncHistoryRecord> items = SyncHistoryManager.INSTANCE.getActiveSyncRecords(ca, Type.UPLOAD);

		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			for (ConnectSyncHistoryRecord r : items){
				r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
				s.saveOrUpdate(r);
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}
	
	/**
	 * Creates a new record for a Conservation Area
	 * @param ca
	 * @param server
	 * @param type
	 * @return
	 */
	public ConnectSyncHistoryRecord create(ConservationArea ca, ConnectServer server, 
			ConnectSyncHistoryRecord.Type type){
		
		ConnectSyncHistoryRecord record = new ConnectSyncHistoryRecord();
		record.setConservationArea(ca);
		record.setDatetime(new Date());
		record.setStartRevision(-1l);
		record.setEndRevision(-1l);
		record.setServer(server);
		record.setType(type);
		record.setStatus(ConnectSyncHistoryRecord.Status.ACTIVE);
		record.setStatusUrl(null);
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.save(record);
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		return record;
	}
	
	/**
	 * Gets the last non error sync record with the given type.
	 * 
	 * @param ca
	 * @param type
	 * @return
	 */
	public ConnectSyncHistoryRecord getLastNonErrorSyncRecord(ConservationArea ca,
			ConnectSyncHistoryRecord.Type type){
		
		Session s = HibernateManager.openSession();
		try{
			return getLastNonErrorSyncRecord(s, ca, type);
		}finally{
			s.close();
		}
	}
	
	/**
	 * Gets the last non error sync record with the given type.
	 * 
	 * @param ca
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<ConnectSyncHistoryRecord> getActiveSyncRecords(ConservationArea ca,
			ConnectSyncHistoryRecord.Type type){
		
		Session session = HibernateManager.openSession();
		try{
			return session.createCriteria(ConnectSyncHistoryRecord.class)
				.add(Restrictions.eq("conservationArea", ca))
				.add(Restrictions.eq("type", type))
				.add(Restrictions.eq("status", Status.ACTIVE))
				.list();
		}finally{
			session.close();
		}
	}
	
	/**
	 * Gets the last non error sync record with the given type.
	 * 
	 * @param ca
	 * @param type
	 * @return
	 */
	public ConnectSyncHistoryRecord getLastNonErrorSyncRecord(Session session, ConservationArea ca,
			ConnectSyncHistoryRecord.Type type){
		
		ConnectSyncHistoryRecord record = (ConnectSyncHistoryRecord) session.createCriteria(ConnectSyncHistoryRecord.class)
				.add(Restrictions.eq("conservationArea", ca))
				.add(Restrictions.eq("type", type))
				.add(Restrictions.in("status", new Object[]{Status.ACTIVE, Status.DONE}))
				.addOrder(Order.desc("endRevision"))
				.setMaxResults(1).uniqueResult();
		if (record != null) record.getServer().getConservationArea().getUuid();
		return record;
	}
}
