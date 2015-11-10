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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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
		super("clean up replication history");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Date d = new Date();
		Date lastHour = new Date(d.getTime() - DerbyReplicationManager.REPLICATION_MAXTIME_DAYS * 24 * 60 * 60 * 1000l);
		
		//download change log
		Session s = HibernateManager.openSession();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		try{
			s.beginTransaction();
			//we always want at least one history record of the given type
			//identifying when last executed to prevent users from
			//syncing really old databases
			List<ConnectSyncHistoryRecord> lastUploads = (List<ConnectSyncHistoryRecord>) s.createCriteria(ConnectSyncHistoryRecord.class)
					.add(Restrictions.eq("conservationArea", ca))
					.add(Restrictions.eq("type", ConnectSyncHistoryRecord.Type.UPLOAD))
					.add(Restrictions.eq("status", ConnectSyncHistoryRecord.Status.DONE))
					.add(Restrictions.lt("datetime", lastHour))
					.addOrder(Order.desc("datetime"))
					.setMaxResults(2)
					.list();
			
			if (lastUploads.size() == 2){
			
				ConnectSyncHistoryRecord toDelete = lastUploads.get(1);
			
				//delete records
				ChangeLogTableManager.INSTANCE.deleteRecords(s, ca, toDelete.getEndRevision());
				SyncHistoryManager.INSTANCE.deleteRecords(s, ca, ConnectSyncHistoryRecord.Type.UPLOAD, toDelete.getDatetime());
			}
			
			List<ConnectSyncHistoryRecord> lastDownloads = (List<ConnectSyncHistoryRecord>) s.createCriteria(ConnectSyncHistoryRecord.class)
					.add(Restrictions.eq("conservationArea", ca))
					.add(Restrictions.eq("type", ConnectSyncHistoryRecord.Type.DOWNLOAD))
					.add(Restrictions.in("status", new ConnectSyncHistoryRecord.Status[]{ConnectSyncHistoryRecord.Status.DONE, ConnectSyncHistoryRecord.Status.NODATA}))
					.add(Restrictions.lt("datetime", lastHour))
					.addOrder(Order.desc("datetime"))
					.setMaxResults(2)
					.list();
			if (lastDownloads.size() == 2){
				ConnectSyncHistoryRecord toDelete = lastDownloads.get(1);
				SyncHistoryManager.INSTANCE.deleteRecords(s, ca, ConnectSyncHistoryRecord.Type.DOWNLOAD, toDelete.getDatetime());
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.log("Unable to cleanup change log table", ex);
			
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			
		}finally{
			s.close();
		}
		return Status.OK_STATUS;
	}

}
