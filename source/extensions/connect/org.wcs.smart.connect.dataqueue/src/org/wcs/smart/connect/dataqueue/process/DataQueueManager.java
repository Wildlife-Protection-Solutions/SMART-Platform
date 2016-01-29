package org.wcs.smart.connect.dataqueue.process;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem.Status;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public enum DataQueueManager {
	
	INSTANCE;
	
	public LocalDataQueueItem addItemToQueue(DataQueueItem item) throws Exception{
		if (!item.getConservationArea().equals(SmartDB.getCurrentConservationArea().getUuid())){
			throw new IllegalStateException("Conservation area of the data queue item does not match the current conservation area.  Cannot process items outside of their conservation area.");
		}
		if (item instanceof LocalDataQueueItem){
			throw new IllegalStateException("Cannot add items to the data queue that are already in the queue.");
		}
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			List<LocalDataQueueItem> localItems = s.createCriteria(LocalDataQueueItem.class)
					.add(Restrictions.eq("serverItemUuid", item.getUuid()))
					.list();
			
			for (LocalDataQueueItem i : localItems){
				//if we have a queued, downloading, or processing item 
				//then this is already in the queue and we do not want to add it again
				//if complete or error we can add again  TODO: consider a warning for the complete case
				//as we may be duplicating data
				if (i.getStatus() == Status.QUEUED ||
						i.getStatus() == Status.DOWNLOADING ||
						i.getStatus() == Status.PROCESSING){
					return null;
				}
			}
				
			Integer nextorder = (Integer) s.createCriteria(LocalDataQueueItem.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid()))
					.add(Restrictions.eq("status", LocalDataQueueItem.Status.QUEUED))
					.setProjection(Projections.max("order"))
					.uniqueResult();
			if (nextorder == null) nextorder = 0;
			nextorder++;
			
			LocalDataQueueItem local = new LocalDataQueueItem();
			local.setConservationArea(item.getConservationArea());
			local.setFile(null);
			local.setName(item.getName());
			local.setServerItemUuid(item.getUuid());
			local.setStatus(LocalDataQueueItem.Status.QUEUED);
			local.setType(item.getType());
			local.setOrder(nextorder);
			
			s.save(local);
			
			s.getTransaction().commit();
			return local;
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
		
	}

	public List<LocalDataQueueItem> getLocalItems(LocalDataQueueItem.Status... validStatus) throws Exception{

		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			Criteria c = s.createCriteria(LocalDataQueueItem.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid()));
			
			if (validStatus != null && validStatus.length > 0){
				c.add(Restrictions.in("status", validStatus));
			}
			List<LocalDataQueueItem> localItems = c.list();
			return localItems;
		}catch(Exception ex){
			throw ex;
			
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}
	
	public synchronized LocalDataQueueItem checkOutNextQueueItem(){
		Session s = HibernateManager.openSession();
		s.getTransaction().begin();
		try{
			
			LocalDataQueueItem item = (LocalDataQueueItem) s.createCriteria(LocalDataQueueItem.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid()))
			.add(Restrictions.eq("status", LocalDataQueueItem.Status.QUEUED))
			.addOrder(Order.asc("order"))
			.setMaxResults(1)
			.uniqueResult();
			
			if (item != null){
				item.setDateProcessed(new Date());
				item.setStatus(Status.PROCESSING);
			}
			s.getTransaction().commit();
			return item;
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
	}
	
	public void deleteAllHistory(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			
			Query q = s.createQuery("DELETE FROM LocalDataQueueItem WHERE conservationArea = :ca and status in (:status)");
			q.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
			q.setParameterList("status", new LocalDataQueueItem.Status[]{LocalDataQueueItem.Status.COMPLETE, LocalDataQueueItem.Status.ERROR});
			q.executeUpdate();
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			ConnectDataQueuePlugin.displayLog("Error clearning data queue history. " + ex.getMessage(), ex);
		}finally{
			s.close();
		}
	}
	
	public void deleteItems(List<LocalDataQueueItem> toDelete){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			for (LocalDataQueueItem i : toDelete){
				s.delete(i);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			ConnectDataQueuePlugin.displayLog("Error clearning data queue history. " + ex.getMessage(), ex);
		}finally{
			s.close();
		}
	}
}
