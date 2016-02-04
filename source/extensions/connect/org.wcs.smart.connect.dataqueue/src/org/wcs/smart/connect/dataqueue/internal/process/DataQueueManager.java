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
package org.wcs.smart.connect.dataqueue.internal.process;

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

/**
 * Manager for the local data queue.  Provides an interface for adding, removing
 * and checking out items from the data queue.
 * @author Emily
 *
 */
public enum DataQueueManager {
	
	INSTANCE;
	
	@SuppressWarnings("unchecked")
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
				//if complete or error we can add again
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

	/**
	 * Gets all items in the local datastore for the current conservation area
	 * with one of the status value provided.
	 * @param validStatus
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<LocalDataQueueItem> getLocalItems(LocalDataQueueItem.Status... validStatus) {

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
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}
	
	/**
	 * Checks out the next queue item for processing.
	 * Updates the status of the item to processing before returning.
	 * If no items to process, this returns null.
	 * 
	 * @return
	 */
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
	
	/**
	 * Deletes all items from the data queue table for the current conservation area
	 * with a status of complete or error.
	 */
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
	
	/**
	 * Deletes specific items from the data queue table (does not check the status or 
	 * conservation area).
	 * @param toDelete
	 */
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
