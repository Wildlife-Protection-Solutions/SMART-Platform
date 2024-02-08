/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.json.importer;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.api.DataQueueEventService;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem.Status;
import org.wcs.smart.connect.i18n.Messages;

import jakarta.persistence.Tuple;

/**
 * Job for processing smart mobile data.
 * 
 * @author Emily
 *
 */
public class SmartMobileJsonJob implements Runnable{

	public static final int MAX_TRIES = 5;
	
	private SessionFactory factory;
	
	private HashMap<UUID, Integer> processedCount;
	private Set<UUID> toSkip;
	
	public SmartMobileJsonJob(SessionFactory factory) {
		this.factory = factory;
		this.processedCount = new HashMap<>();
		toSkip = new HashSet<>();
	}
	
	@Override
	public void run() {
		List<ServerDataQueueItem> last = null;
		
		try {
			while(true) {
				
				List<ServerDataQueueItem> toProcess = null;
				try(Session session = factory.openSession()){
					toProcess = getItemsToProcess(session);
				}
				
				//nothing more to process
				if (toProcess == null || toProcess.isEmpty()) return;
				
				//if last array same as current array then stop
				if (last != null) {
					if (last.size() == toProcess.size()) {
						boolean aresame = true;
						for (int i = 0; i < last.size(); i ++) {
							if (!last.get(i).equals(toProcess.get(i))) {
								aresame = false;
								break;
							}
						}
						if (aresame) return;
					}
				}
				last = new ArrayList<>(toProcess);
				
				for (ServerDataQueueItem item : toProcess) {
					try(Session session = factory.openSession()){
						
						if (!processedCount.containsKey(item.getUuid())) {
							processedCount.put(item.getUuid(), 0);
						}
						int newcnt = processedCount.get(item.getUuid()) + 1;
						processedCount.put(item.getUuid(), newcnt);
						if (newcnt > MAX_TRIES) {
							//if we keep reprocessing the same item over and over again we probably want to just stop until a new file is uploaded
							//attempt more than X processing attempts skip this file for remainder of while loop
							toSkip.add(item.getUuid());
						}
					
						//check it out
						session.beginTransaction();
						Object x = session.createNativeQuery("UPDATE connect.data_queue SET status = :ps WHERE uuid = :uuid and status = :qu RETURNING *  ", Tuple.class) //$NON-NLS-1$
								.setParameter("ps", ServerDataQueueItem.Status.PROCESSING.name()) //$NON-NLS-1$
								.setParameter("uuid", item.getUuid()) //$NON-NLS-1$
								.setParameter("qu", ServerDataQueueItem.Status.QUEUED.name()) //$NON-NLS-1$
								.getSingleResult();
						session.getTransaction().commit();
						
						if (x == null) {
							//item already updated
							continue;
						}				
						//evict
						session.evict(item);
						
						//send event
						item = session.get(item.getClass(), item.getUuid());
						DataQueueEventService.addUpdateToQueue(item);
					}
	
					processItem(item);
				}
			}
		}finally {		
			cleanUp();
		}
		
	}
	
	
	private List<ServerDataQueueItem> getItemsToProcess(Session session) {
		//find all queued data queue items with appropriate type
		//in order of upload - processed older files first
		List<String> types = new ArrayList<>();
		types.add(SmartMobileJsonFileProcessor.CT_TYPE);
		types.add(SmartMobileJsonFileProcessor.CT_ZIP_TYPE);
			
		Query<ServerDataQueueItem> q = null;
		if (toSkip.isEmpty()) {
			q = session.createQuery("FROM ServerDataQueueItem WHERE type in (:types) and status = :status order by uploadedDate asc", ServerDataQueueItem.class) //$NON-NLS-1$
					.setParameter("types", types) //$NON-NLS-1$
					.setParameter("status", ServerDataQueueItem.Status.QUEUED); //$NON-NLS-1$
		}else {
			q = session.createQuery("FROM ServerDataQueueItem WHERE type in (:types) and status = :status and uuid not in (:toskip) order by uploadedDate asc", ServerDataQueueItem.class) //$NON-NLS-1$
					.setParameter("types", types) //$NON-NLS-1$
					.setParameter("status", ServerDataQueueItem.Status.QUEUED) //$NON-NLS-1$
					.setParameter("toskip", toSkip); //$NON-NLS-1$
					
		}
		return q.list();		
	}
	
	/*
	 * find any items that are old than x days as specified by
	 * environment variable  and set to error with associated message 
	 */
	private void cleanUp() {
		int days = -1;
		try {
			Object x = EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.DATA_QUEUE_ERROR_OUT_DAYS);
			if (x != null && x instanceof Number num) {
				days = num.intValue();
			}
		}catch (Exception ex) {
			//Logger.getLogger(SmartMobileJsonJob.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
		}
		if (days <= 0) return;
		 
		try(Session session = this.factory.openSession()){
			List<ServerDataQueueItem> toProcess = getItemsToProcess(session);
			for (ServerDataQueueItem item : toProcess) {
				if (item.getStatus() != Status.QUEUED) continue;
				
				if (item.getUploadedDate().isBefore(ZonedDateTime.now().minusDays(days))) {
					session.beginTransaction();
					try {
						item.setStatus(Status.ERROR);
						item.setStatusMessage(
							MessageFormat.format(Messages.getString("SmartMobileJsonJob_UnableToProcessAfterXDays", Locale.getDefault()), days)); //$NON-NLS-1$
							session.getTransaction().commit();
					}catch (Exception ex) {
						Logger.getLogger(SmartMobileJsonJob.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
					}					
				}
			}
		}
	}
	
	private void processItem(ServerDataQueueItem item) {
		if (item == null) return;
		if (!SmartMobileJsonFileProcessor.canProcess(item.getType())) return;
		
		Locale locale = Locale.getDefault();
		SmartMobileJsonFileProcessor.create(item, factory, locale).process();
	}

	
}
