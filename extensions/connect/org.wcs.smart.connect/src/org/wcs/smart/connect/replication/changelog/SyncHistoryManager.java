package org.wcs.smart.connect.replication.changelog;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.hibernate.HibernateManager;

public enum SyncHistoryManager {
	
	INSTANCE;
	
	public void deleteAll(Session s, ConservationArea ca){
		String hsql = "DELETE FROM ConnectSyncHistoryRecord WHERE conservationArea = :ca";
		s.createQuery(hsql).setParameter("ca", ca).executeUpdate();
	}
	
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
	
	public ConnectSyncHistoryRecord getLastNonErrorSyncRecord(ConservationArea ca,
			ConnectSyncHistoryRecord.Type type){
		
		Session s = HibernateManager.openSession();
		try{
			ConnectSyncHistoryRecord record = (ConnectSyncHistoryRecord) (ConnectSyncHistoryRecord)s.createCriteria(ConnectSyncHistoryRecord.class)
					.add(Restrictions.eq("conservationArea", ca))
					.add(Restrictions.eq("type", type))
					.add(Restrictions.in("status", new Object[]{Status.ACTIVE, Status.DONE}))
					.addOrder(Order.desc("endRevision"))
					.setMaxResults(1).uniqueResult();
			if (record != null) record.getServer().getConservationArea().getUuid();
			return record;
		}finally{
			s.close();
		}
	}
}
