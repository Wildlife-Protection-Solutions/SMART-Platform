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

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

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
		String hsql = "DELETE FROM ConnectSyncHistoryRecord WHERE conservationArea = :ca"; //$NON-NLS-1$
		s.createMutationQuery(hsql).setParameter("ca", ca).executeUpdate(); //$NON-NLS-1$
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
	public void deleteRecords(Session s, ConservationArea ca, Type type, LocalDateTime endDate){
		s.createMutationQuery("DELETE FROM ConnectSyncHistoryRecord WHERE type = :type and conservationArea = :ca and datetime <= :datetime") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.setParameter("type", type) //$NON-NLS-1$
			.setParameter("datetime", endDate) //$NON-NLS-1$
			.executeUpdate();
	}
	
	/**
	 * Sets any active download records to a status of Error 
	 * for the given conservation area.
	 */
	public void errorActiveDownloadRecords(ConservationArea ca){
		List<ConnectSyncHistoryRecord> items = SyncHistoryManager.INSTANCE.getActiveSyncRecords(ca, Type.DOWNLOAD);

		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				for (ConnectSyncHistoryRecord r : items){
					r = s.getReference(r);
					r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}
	}
	
	/**
	 * Sets any active upload records to a status of Error 
	 * for the given conservation area.
	 */
	public void errorActiveUploadRecords(ConservationArea ca){
		List<ConnectSyncHistoryRecord> items = SyncHistoryManager.INSTANCE.getActiveSyncRecords(ca, Type.UPLOAD);

		try(Session s = HibernateManager.openSession()){
		
			s.beginTransaction();
			try {
				for (ConnectSyncHistoryRecord r : items){
					r = s.getReference(r);
					r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
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
		record.setDatetime(LocalDateTime.now());
		record.setStartRevision(-1l);
		record.setEndRevision(-1l);
		record.setServer(server);
		record.setType(type);
		record.setStatus(ConnectSyncHistoryRecord.Status.ACTIVE);
		record.setStatusUrl(null);
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				s.persist(record);
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
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
		try(Session s = HibernateManager.openSession()){
			return getLastNonErrorSyncRecord(s, ca, type);
		}
	}
	
	/**
	 * Gets the last non error sync record with the given type.
	 * 
	 * @param ca
	 * @param type
	 * @return
	 */
	public List<ConnectSyncHistoryRecord> getActiveSyncRecords(ConservationArea ca,
			ConnectSyncHistoryRecord.Type type){
		
		try(Session session = HibernateManager.openSession()){
			return QueryFactory.buildQuery(session, ConnectSyncHistoryRecord.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"type", type}, //$NON-NLS-1$
				new Object[] {"status", Status.ACTIVE}) //$NON-NLS-1$
				.list();
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
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<ConnectSyncHistoryRecord> c = cb.createQuery(ConnectSyncHistoryRecord.class);
		Root<ConnectSyncHistoryRecord> from = c.from(ConnectSyncHistoryRecord.class);
		c.where(cb.and(
				cb.equal(from.get("conservationArea"), ca), //$NON-NLS-1$
				cb.equal(from.get("type"), type), //$NON-NLS-1$
				from.get("status").in(Status.ACTIVE, Status.DONE) //$NON-NLS-1$
				));
		c.orderBy(cb.desc(from.get("endRevision"))); //$NON-NLS-1$
		ConnectSyncHistoryRecord record = session.createQuery(c).setMaxResults(1).uniqueResult();
		
		if (record != null) record.getServer().getConservationArea().getUuid();
		return record;
	}
}
