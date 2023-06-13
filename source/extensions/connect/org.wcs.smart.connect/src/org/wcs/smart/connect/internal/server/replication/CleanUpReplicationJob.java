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
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * This job cleans up items from the change log that are
 * older than a given date and removes sync history items
 * that are older than that date as well.
 * 
 * @author Emily
 *
 */
public class CleanUpReplicationJob extends Job{

	public CleanUpReplicationJob(){
		super(Messages.CleanUpReplicationJob_jobname);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		LocalDateTime lastHour = ChronoUnit.DAYS.addTo(LocalDateTime.now(), -DerbyReplicationManager.REPLICATION_MAXTIME_DAYS );
		
		//download change log
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				//we always want at least one history record of the given type
				//identifying when last executed to prevent users from
				//syncing really old databases
				
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<ConnectSyncHistoryRecord> c = cb.createQuery(ConnectSyncHistoryRecord.class);
				Root<ConnectSyncHistoryRecord> from = c.from(ConnectSyncHistoryRecord.class);
				c.where(cb.and(
						cb.equal(from.get("conservationArea"), ca), //$NON-NLS-1$
						cb.equal(from.get("type"), ConnectSyncHistoryRecord.Type.UPLOAD), //$NON-NLS-1$
						cb.equal(from.get("status"), ConnectSyncHistoryRecord.Status.DONE), //$NON-NLS-1$
						cb.lessThan(from.get("datetime"), lastHour) //$NON-NLS-1$
						));
				c.orderBy(cb.desc(from.get("datetime"))); //$NON-NLS-1$
				
				List<ConnectSyncHistoryRecord> lastUploads = s.createQuery(c).setMaxResults(2).list();
				
				if (lastUploads.size() == 2){
				
					ConnectSyncHistoryRecord toDelete = lastUploads.get(1);
				
					//delete records
					ChangeLogTableManager.INSTANCE.deleteRecords(s, ca, toDelete.getEndRevision());
					SyncHistoryManager.INSTANCE.deleteRecords(s, ca, ConnectSyncHistoryRecord.Type.UPLOAD, toDelete.getDatetime());
				}
				CriteriaQuery<ConnectSyncHistoryRecord> c2 = cb.createQuery(ConnectSyncHistoryRecord.class);
				Root<ConnectSyncHistoryRecord> from2 = c2.from(ConnectSyncHistoryRecord.class);
				c2.where(cb.and(
						cb.equal(from2.get("conservationArea"), ca), //$NON-NLS-1$
						cb.equal(from2.get("type"), ConnectSyncHistoryRecord.Type.DOWNLOAD), //$NON-NLS-1$
						from2.get("status").in(ConnectSyncHistoryRecord.Status.DONE, ConnectSyncHistoryRecord.Status.NODATA), //$NON-NLS-1$
						cb.lessThan(from2.get("datetime"), lastHour) //$NON-NLS-1$
						));
				c2.orderBy(cb.desc(from2.get("datetime"))); //$NON-NLS-1$
				List<ConnectSyncHistoryRecord> lastDownloads = s.createQuery(c2).setMaxResults(2).list();

				if (lastDownloads.size() == 2){
					ConnectSyncHistoryRecord toDelete = lastDownloads.get(1);
					SyncHistoryManager.INSTANCE.deleteRecords(s, ca, ConnectSyncHistoryRecord.Type.DOWNLOAD, toDelete.getDatetime());
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				ConnectPlugIn.log("Unable to cleanup change log table", ex); //$NON-NLS-1$
				
				if (s.getTransaction().isActive()) s.getTransaction().rollback();
				
			}
		}
		return Status.OK_STATUS;
	}

}
