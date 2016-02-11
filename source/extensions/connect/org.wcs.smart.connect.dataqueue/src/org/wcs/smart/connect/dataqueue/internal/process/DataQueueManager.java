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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.transaction.Synchronization;
import javax.ws.rs.WebApplicationException;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.internal.server.DataQueueApi;
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
	
	private List<IDataQueueListener> listeners = new ArrayList<IDataQueueListener>();
	
	/**
	 * Adds a data queue listener
	 * @param listener
	 */
	public void addListener(IDataQueueListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a data queue listener
	 * @param listener
	 */
	public void removeListener(IDataQueueListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Fires listeners to notify the queue has been modified
	 */
	public void fireModified(){
		for(IDataQueueListener l : listeners){
			l.dataQueueModified();
		}
	}
	
	@SuppressWarnings("unchecked")
	public LocalDataQueueItem addItemToQueue(DataQueueItem item) throws Exception{
		if (!item.getConservationArea().equals(SmartDB.getCurrentConservationArea().getUuid())){
			throw new IllegalStateException(Messages.DataQueueManager_CaError);
		}
		if (item instanceof LocalDataQueueItem){
			throw new IllegalStateException(Messages.DataQueueManager_InQueueError);
		}
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			List<LocalDataQueueItem> localItems = s.createCriteria(LocalDataQueueItem.class)
					.add(Restrictions.eq("serverItemUuid", item.getUuid())) //$NON-NLS-1$
					.list();
			
			for (LocalDataQueueItem i : localItems){
				//if we have a queued, downloading, or processing item 
				//then this is already in the queue and we do not want to add it again
				//if complete or error we can add again
				//as we may be duplicating data
				if (i.getStatus() == Status.QUEUED ||
						i.getStatus() == Status.REQUEUED ||
						i.getStatus() == Status.DOWNLOADING ||
						i.getStatus() == Status.PROCESSING){
					return null;
				}
			}
			
			LocalDataQueueItem local = new LocalDataQueueItem();
			local.setConservationArea(item.getConservationArea());
			local.setFile(null);
			local.setName(item.getName());
			local.setServerItemUuid(item.getUuid());
			local.setStatus(LocalDataQueueItem.Status.QUEUED);
			local.setType(item.getType());
			local.setOrder(getNextQueueOrder(s));
			
			s.save(local);
			
			s.getTransaction().commit();
			
			try{
				fireModified();
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(ex.getMessage(), ex);
			}
			return local;
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
		
	}

	private Integer getNextQueueOrder(Session s){
		Integer nextorder = (Integer) s.createCriteria(LocalDataQueueItem.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid())) //$NON-NLS-1$
				.add(Restrictions.in("status",  //$NON-NLS-1$
						new LocalDataQueueItem.Status[]{
						LocalDataQueueItem.Status.QUEUED,
						LocalDataQueueItem.Status.REQUEUED}))
				.setProjection(Projections.max("order")) //$NON-NLS-1$
				.uniqueResult();
		if (nextorder == null) nextorder = 0;
		nextorder++;
		return nextorder;
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
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid())); //$NON-NLS-1$
			
			if (validStatus != null && validStatus.length > 0){
				c.add(Restrictions.in("status", validStatus)); //$NON-NLS-1$
			}
			List<LocalDataQueueItem> localItems = c.list();
			return localItems;
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}
	
	/**
	 * Reprocesses any of the data queue items.  This will reset the state to
	 * reprocessing of any items currently not processing and not queued.  
	 * REPROCESSING items are not checked out from the server when reprocessed. 
	 * (It will try to download the file again if needed and will update the final status).
	 * 
	 * @return
	 */
	public synchronized void reprocessItems(List<LocalDataQueueItem> items){
		Session s = HibernateManager.openSession();
		s.getTransaction().begin();
		try{
			for (LocalDataQueueItem item : items){
				LocalDataQueueItem i = (LocalDataQueueItem) s.get(LocalDataQueueItem.class, 
						item.getUuid());
						
				if (i == null || i.getStatus() == LocalDataQueueItem.Status.DOWNLOADING ||
						i.getStatus() == LocalDataQueueItem.Status.QUEUED ||
						i.getStatus() == LocalDataQueueItem.Status.REQUEUED ||
						i.getStatus() == LocalDataQueueItem.Status.PROCESSING){
					//cannot requeue item
					continue;
				}
			
				LocalDataQueueItem clone = new LocalDataQueueItem();
				clone.setConservationArea(i.getConservationArea());
				clone.setName(i.getName());
				clone.setServerItemUuid(i.getServerItemUuid());
				clone.setType(i.getType());
				clone.setStatus(LocalDataQueueItem.Status.REQUEUED);
				clone.setDateProcessed(null);
				clone.setErrorMessage(Messages.DataQueueManager_ReQueuedMessage);
				clone.setOrder(getNextQueueOrder(s));
			
				if (i.getFullFilePath() != null && Files.exists(i.getFullFilePath())){
					//copy 
					Path copy = i.getFullFilePath().getParent().resolve(i.getFullFilePath().getFileName().toString() + ".copy" + System.nanoTime()); //$NON-NLS-1$
					Files.copy(i.getFullFilePath(), copy);
					clone.setFile(
							FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation())
							.relativize(copy).toString());
					
				}
				s.save(clone);
			}
			s.getTransaction().commit();
		}catch(Exception ex){
			
		}finally{
			s.close();
		}
		fireModified();
	}
	/**
	 * Checks out the next queue item for processing.
	 * Updates the status of the item to processing before returning.
	 * If no items to process, this returns null.
	 * 
	 * Synchronized so we don't check out the same item twice.
	 * 
	 * @return
	 */
	public synchronized LocalDataQueueItem checkOutNextQueueItem(SmartConnect connect){
		Session s = HibernateManager.openSession();
		s.getTransaction().begin();
		try{
			
			LocalDataQueueItem item = (LocalDataQueueItem) s.createCriteria(LocalDataQueueItem.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid())) //$NON-NLS-1$
			.add(Restrictions.in("status", new LocalDataQueueItem.Status[]{LocalDataQueueItem.Status.QUEUED, LocalDataQueueItem.Status.REQUEUED})) //$NON-NLS-1$
			.addOrder(Order.asc("order")) //$NON-NLS-1$
			.setMaxResults(1)
			.uniqueResult();
		
			if (item == null) return null;
			
			item.setDateProcessed(new Date());
			//check status on server
			if (item.getStatus() != LocalDataQueueItem.Status.REQUEUED){
				try{
					ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.PROCESSING);
					item.setStatus(Status.PROCESSING);
				}catch (Exception ex){
					String message = null;
					if (ex instanceof WebApplicationException){
						message = MessageFormat.format(Messages.DataQueueManager_ItemServerError, SmartConnect.parseErrorMessage(((WebApplicationException) ex).getResponse().readEntity(String.class)));
					}else{
						message = Messages.DataQueueManager_Error3;
					}
					item.setStatus(LocalDataQueueItem.Status.ERROR);
					item.setErrorMessage(message);
				}
			}else{
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
	 */
	public synchronized void deleteAllHistory(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();		
			deleteDataQueue(SmartDB.getCurrentConservationArea(), s);
			s.getTransaction().commit();
			
			try{
				fireModified();
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(ex.getMessage(), ex);
			}
		}catch (Exception ex){
			s.getTransaction().rollback();
			ConnectDataQueuePlugin.displayLog(Messages.DataQueueManager_Error4 + ex.getMessage(), ex);
		}finally{
			s.close();
		}
	}
	
	/**
	 * Deletes specific items from the data queue table (does not check the status or 
	 * conservation area).
	 * @param toDelete
	 */
	public synchronized void deleteItems(List<LocalDataQueueItem> toDelete){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			final List<Path> filesToDelete = new ArrayList<Path>();
			
			for (LocalDataQueueItem i : toDelete){
				s.delete(i);
				if (i.getFile() != null){
					filesToDelete.add(i.getFullFilePath());
				}
			}
			s.getTransaction().commit();
			
			for (Path p : filesToDelete){
				try{
					Files.deleteIfExists(p);
				}catch(Exception ex){
					ConnectDataQueuePlugin.displayLog(MessageFormat.format(Messages.DataQueueManager_DeleteError, p.toString()), ex);	
				}
			}
			try{
				fireModified();
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(ex.getMessage(), ex);
			}
		}catch (Exception ex){
			s.getTransaction().rollback();
			ConnectDataQueuePlugin.displayLog(Messages.DataQueueManager_CleanError + ex.getMessage(), ex);
		}finally{
			s.close();
		}
	}
	
	/**
	 * Deletes all items from the data queue for the current conservation
	 * area that are older than
	 * x days and not queued, downloading, or processing
	 *  
	 * @param toDelete
	 */
	public synchronized void deleteOldItems(int numDays){
		if (numDays < 0) return;
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -numDays);
			Date compareDate = c.getTime();
			
			List<LocalDataQueueItem> toDelete = s.createCriteria(LocalDataQueueItem.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea().getUuid())) //$NON-NLS-1$
				.add(Restrictions.in("status", new LocalDataQueueItem.Status[]{ //$NON-NLS-1$
						LocalDataQueueItem.Status.COMPLETE,
						LocalDataQueueItem.Status.COMPLETE_WARN,
						LocalDataQueueItem.Status.ERROR,
				}))
				.add(Restrictions.and(Restrictions.isNotNull("dateProcessed"), //$NON-NLS-1$
						Restrictions.lt("dateProcessed", compareDate))) //$NON-NLS-1$
				.list();
			final List<Path> filesToDelete = new ArrayList<Path>();
			
			for (LocalDataQueueItem i : toDelete){
				s.delete(i);
				if (i.getFile() != null){
					filesToDelete.add(i.getFullFilePath());
				}
			}
			s.getTransaction().commit();
			
			for (Path p : filesToDelete){
				try{
					Files.deleteIfExists(p);
				}catch(Exception ex){
					ConnectDataQueuePlugin.displayLog(MessageFormat.format(Messages.DataQueueManager_DeleteError, p.toString()), ex);	
				}
			}
			if (!toDelete.isEmpty()){
				try{	
					fireModified();
				}catch (Exception ex){
					ConnectDataQueuePlugin.displayLog(ex.getMessage(), ex);
				}
			}
		}catch (Exception ex){
			s.getTransaction().rollback();
			ConnectDataQueuePlugin.displayLog(Messages.DataQueueManager_CleanError + ex.getMessage(), ex);
		}finally{
			s.close();
		}
	}
	
	/**
	 * Removes all data queue processing options configured for the
	 * Conservation Area.
	 * @param ca
	 * @param session
	 */
	public void deleteDataQueueOptions(ConservationArea ca, Session session) {
		// delete all data queue items
		Query q = session
				.createQuery("DELETE FROM DataQueueProcessingOption WHERE id.conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	/**
	 * Removes all items in the data queue for a given conservation area.
	 * The session provided should be in an open active transaction.
	 * Data files are deleted once the transaction is complete.
	 * @param ca
	 * @param session
	 */
	public void deleteDataQueue(ConservationArea ca, Session session) {
		// delete all data queue items
		Query q = session
				.createQuery("DELETE FROM LocalDataQueueItem WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		q.executeUpdate();

		// delete associated files
		session.getTransaction().registerSynchronization(new Synchronization() {
			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(int status) {
				if (status == javax.transaction.Status.STATUS_COMMITTED) {
					// if the transaction was successful we also want to delete
					// the associated files
					Path dataQueueFiles = FileSystems.getDefault()
							.getPath(ca.getFileDataStoreLocation())
							.resolve(ConnectDataQueuePlugin.DATA_QUEUE_DIR);
					if (Files.exists(dataQueueFiles)) {
						try (DirectoryStream<Path> toDelete = Files
								.newDirectoryStream(dataQueueFiles)) {
							for (Path p : toDelete) {
								try {
									Files.delete(p);
								} catch (Exception ex) {
									ConnectDataQueuePlugin.log(
											MessageFormat
													.format(Messages.DataQueueManager_DeleteError,
															p.toString()), ex);
								}
							}
						} catch (IOException ex) {
							ConnectDataQueuePlugin.log(
									Messages.DataQueueManager_DeleteError2, ex);
						}
					}

					DataQueueManager.INSTANCE.fireModified();
				}

			}
		});
	}
}
